package com.example.vpn

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * VPN Gate Manager for University of Tsukuba Academic Project.
 * Direct Live CSV API parser: http://www.vpngate.net/api/iphone/
 */
object VpnGateManager {

    private const val VPN_GATE_MIRROR_URL = "https://www.vpngate.net/api/iphone/"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _servers = MutableStateFlow<List<VpnServer>>(emptyList())
    val servers: StateFlow<List<VpnServer>> = _servers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isShowingCachedProfiles = MutableStateFlow(false)
    val isShowingCachedProfiles: StateFlow<Boolean> = _isShowingCachedProfiles.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Long>(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    /**
     * Fetches and parses the live CSV feed from University of Tsukuba.
     */
    suspend fun fetchServers(context: Context, forceRefresh: Boolean = false): List<VpnServer> = withContext(Dispatchers.IO) {
        if (!forceRefresh && _servers.value.isNotEmpty() && System.currentTimeMillis() - _lastUpdated.value < 10 * 60 * 1000) {
            return@withContext _servers.value
        }

        _isLoading.value = true
        var csvContent: String? = null
        var isCachedData = false

        try {
            val req = Request.Builder().url(VPN_GATE_MIRROR_URL).build()
            httpClient.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    csvContent = response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.w("VpnGateManager", "Could not fetch live VPN Gate profiles: ${e.message}", e)
        }

        // A cache entry is previously fetched live data; never invent server records.
        if (csvContent.isNullOrBlank()) {
            val cacheFile = File(context.cacheDir, "vpngate_cache.csv")
            if (cacheFile.exists()) {
                csvContent = try { cacheFile.readText() } catch (_: Exception) { null }
                isCachedData = !csvContent.isNullOrBlank()
            }
        }

        val parsedList = csvContent?.takeIf(String::isNotBlank)?.let(::parseCsv).orEmpty()

        if (parsedList.isNotEmpty()) {
            _servers.value = parsedList
            _isShowingCachedProfiles.value = isCachedData
            _lastUpdated.value = System.currentTimeMillis()
            // Save to cache
            if (!csvContent.isNullOrBlank()) {
                try {
                    File(context.cacheDir, "vpngate_cache.csv").writeText(csvContent!!)
                } catch (_: Exception) {}
            }
        } else {
            _servers.value = emptyList()
            _isShowingCachedProfiles.value = false
        }

        _isLoading.value = false
        parsedList
    }

    /**
     * Parses the CSV rows returned by VPN Gate.
     * Header format:
     * #HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
     */
    fun parseCsv(csv: String): List<VpnServer> {
        val result = mutableListOf<VpnServer>()
        val lines = csv.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("#")) {
                continue
            }

            val parts = trimmed.split(",")
            if (parts.size >= 15) {
                try {
                    val hostName = parts[0].trim()
                    val ip = parts[1].trim()
                    val score = parts[2].trim().toLongOrNull() ?: continue
                    val pingMs = parts[3].trim().toIntOrNull() ?: continue
                    val speedBps = parts[4].trim().toLongOrNull() ?: continue
                    val countryLong = parts[5].trim()
                    val countryShort = parts[6].trim().uppercase()
                    val numSessions = parts[7].trim().toIntOrNull() ?: continue
                    val uptime = parts[8].trim().toLongOrNull() ?: continue
                    val totalUsers = parts[9].trim().toLongOrNull() ?: continue
                    val totalTraffic = parts[10].trim().toLongOrNull() ?: continue
                    val logType = parts[11].trim()
                    val operator = parts[12].trim()
                    val message = parts[13].trim()
                    val openVpnConfigBase64 = parts[14].trim()

                    if (ip.isNotBlank() && openVpnConfigBase64.isNotBlank() &&
                        pingMs >= 0 && speedBps >= 0
                    ) {
                        result.add(
                            VpnServer(
                                hostName = hostName,
                                ip = ip,
                                score = score,
                                pingMs = pingMs,
                                speedBps = speedBps,
                                countryLong = countryLong,
                                countryShort = countryShort,
                                numVpnSessions = numSessions,
                                uptimeSec = uptime,
                                totalUsers = totalUsers,
                                totalTrafficBytes = totalTraffic,
                                logType = logType,
                                operator = operator,
                                message = message,
                                openVpnConfigBase64 = openVpnConfigBase64
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        // Default sort: lowest ping and highest speed
        return result.sortedWith(
            compareBy<VpnServer> { it.pingMs }.thenByDescending { it.speedBps }
        )
    }

    /**
     * Exports and shares the OpenVPN (.ovpn) configuration file.
     */
    fun exportOvpnFile(context: Context, server: VpnServer): File? {
        try {
            val vpnDir = File(context.cacheDir, "vpn_configs")
            vpnDir.mkdirs()
            val fileName = "vpngate_${server.countryShort.lowercase()}_${server.ip.replace('.', '_')}.ovpn"
            val file = File(vpnDir, fileName)

            val configText = server.decodedConfig
            if (configText.isBlank()) return null

            file.writeText(configText)
            return file
        } catch (e: Exception) {
            Log.e("VpnGateManager", "Could not export VPN profile for ${server.ip}", e)
            return null
        }
    }

    fun shareOvpnFile(context: Context, server: VpnServer) {
        val file = exportOvpnFile(context, server) ?: run {
            Toast.makeText(context, "Could not export this VPN profile.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-openvpn-profile"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VPN Gate Profile - ${server.countryLong} (${server.ip})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Export OpenVPN Profile"))
        } catch (e: Exception) {
            Log.e("VpnGateManager", "Could not open a VPN profile exporter", e)
            Toast.makeText(context, "No app could open this VPN profile.", Toast.LENGTH_LONG).show()
        }
    }

}
