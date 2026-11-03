package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

class GlitchVpnService : VpnService() {

    companion object {
        private const val TAG = "GlitchVpnService"
        const val CHANNEL_ID = "glitch_vpn_shield_channel"
        const val NOTIFICATION_ID = 4099

        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val ACTION_START_ADBLOCK = "com.example.vpn.ACTION_START_ADBLOCK"

        const val EXTRA_SERVER_IP = "EXTRA_SERVER_IP"
        const val EXTRA_SERVER_NAME = "EXTRA_SERVER_NAME"
        const val EXTRA_COUNTRY_CODE = "EXTRA_COUNTRY_CODE"
        const val EXTRA_PORT = "EXTRA_PORT"

        @Volatile
        var isRunning = false
            private set

        @Volatile
        var isAdBlocking = false
            private set

        fun startVpn(
            context: Context,
            serverIp: String,
            serverName: String,
            countryCode: String,
            port: Int = 1194
        ) {
            Log.w(TAG,
                "Remote VPN tunneling is unavailable in this app. Export the profile for $serverName to a compatible VPN app."
            )
        }

        fun stopVpn(context: Context) {
            try {
                val intent = Intent(context, GlitchVpnService::class.java).apply {
                    action = ACTION_DISCONNECT
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot stop VPN service: ${e.message}")
            }
        }

        fun startAdBlocking(context: Context): Boolean {
            return try {
                val intent = Intent(context, GlitchVpnService::class.java).apply {
                    action = ACTION_START_ADBLOCK
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                isAdBlocking = false
                Log.e(TAG, "Cannot start DNS ad blocker: ${e.message}", e)
                false
            }
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunnelJob: Job? = null
    private var currentServerIp: String = ""
    private var currentServerName: String = ""
    private var currentCountryCode: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_CONNECT -> {
                isAdBlocking = false
                disconnectTunnel()
                updateStatusDisconnected(
                    "Remote VPN tunneling is not implemented. Export the profile to a compatible VPN app."
                )
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ACTION_START_ADBLOCK -> {
                try {
                    val notification = buildVpnNotification("Starting DNS ad and tracker blocking...")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    isAdBlocking = false
                    Log.e(TAG, "Cannot start foreground DNS blocker: ${e.message}", e)
                    updateStatusDisconnected("Cannot start DNS blocker: ${e.message}")
                    stopSelf()
                    return START_NOT_STICKY
                }
                connectAdBlockTunnel()
            }
            ACTION_DISCONNECT -> {
                disconnectTunnel()
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Exception) {}
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun connectAdBlockTunnel() {
        closeCurrentTunnel()
        updateStatusDisconnected()
        tunnelJob = serviceScope.launch {
            try {
                val builder = Builder().apply {
                    setSession("Glitch OS DNS Ad Blocker")
                    addAddress("10.111.222.2", 32)
                    addDnsServer("10.111.222.1")
                    addRoute("10.111.222.1", 32)
                    setMtu(1500)
                    setBlocking(true)
                }

                vpnInterface = builder.establish()
                val descriptor = vpnInterface
                if (descriptor == null) {
                    isAdBlocking = false
                    updateStatusDisconnected("Android did not establish the DNS filtering VPN")
                    stopSelf()
                    return@launch
                }

                isRunning = true
                isAdBlocking = true
                val notification = buildVpnNotification(
                    "DNS ad and tracker filtering active; other traffic stays on your network"
                )
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(NOTIFICATION_ID, notification)
                runDnsPacketLoop(descriptor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isRunning = false
                isAdBlocking = false
                try {
                    vpnInterface?.close()
                } catch (closeError: Exception) {
                    Log.w(TAG, "Error closing failed DNS interface: ${closeError.message}")
                }
                vpnInterface = null
                Log.e(TAG, "DNS filtering VPN error: ${e.message}", e)
                updateStatusDisconnected("DNS filtering VPN error: ${e.message}")
            }
        }
    }

    private fun connectTunnel(ip: String, name: String, country: String, port: Int) {
        closeCurrentTunnel()
        tunnelJob = serviceScope.launch {
            try {
                Log.i(TAG, "Establishing Real VPN Interface for $name ($ip)...")

                // Configure Android OS Virtual Network Interface for Full Device Coverage
                val builder = Builder().apply {
                    setSession("Glitch Shield Full Device - $name")
                    addAddress("10.8.0.2", 24)
                    addDnsServer("8.8.8.8")
                    addDnsServer("1.1.1.1")
                    // Global IPv4 route for entire device traffic coverage
                    try {
                        addRoute("0.0.0.0", 0)
                    } catch (e: Exception) {
                        addRoute("10.8.0.0", 24)
                    }
                    setMtu(1500)
                    setBlocking(false)
                }

                // Protect our own socket connection from entering the VPN loop
                val socket = Socket()
                protect(socket)

                vpnInterface = try {
                    builder.establish()
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot establish VPN interface: ${e.message}")
                    null
                }

                if (vpnInterface == null) {
                    Log.e(TAG, "Failed to establish VPN interface (null PFD)")
                    updateStatusDisconnected("Failed to establish OS VPN Interface")
                    stopSelf()
                    return@launch
                }

                isRunning = true
                Log.i(TAG, "VPN Interface established successfully! Interface FD: ${vpnInterface?.fd}")

                // Update notification to Connected status
                val notification = buildVpnNotification("Protected: $name ($ip)")
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.notify(NOTIFICATION_ID, notification)

                // Start Packet processing & Keepalive loop
                runPacketLoop(vpnInterface!!)
            } catch (e: Exception) {
                isAdBlocking = false
                Log.e(TAG, "VPN Tunnel error: ${e.message}", e)
                updateStatusDisconnected("VPN connection error: ${e.message}")
            }
        }
    }

    private suspend fun runDnsPacketLoop(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteArray(32_767)
        val matcher = AdBlockDomainMatcher.default()
        var pendingBlockedLookups = 0L

        try {
            while (isActive && isRunning && isAdBlocking) {
                val length = inputStream.channel.read(ByteBuffer.wrap(packet))
                if (length <= 0) continue

                val query = DnsPacketCodec.parseIpv4DnsQuery(packet, length) ?: continue
                val blocked = query.questionNames.any(matcher::shouldBlock)
                val response = if (blocked) {
                    pendingBlockedLookups++
                    if (pendingBlockedLookups >= 16) {
                        AdProtectionStore.recordBlockedDnsLookups(applicationContext, pendingBlockedLookups)
                        pendingBlockedLookups = 0
                    }
                    DnsPacketCodec.blockedResponse(query)
                } else {
                    val upstreamResponse = try {
                        resolveDns(query.dnsMessage)
                    } catch (e: SocketTimeoutException) {
                        Log.w(TAG, "Upstream DNS request timed out")
                        null
                    } catch (e: Exception) {
                        Log.w(TAG, "Upstream DNS request failed: ${e.message}")
                        null
                    }
                    if (upstreamResponse != null && DnsPacketCodec.isValidDnsResponse(query, upstreamResponse)) {
                        DnsPacketCodec.responsePacket(query, upstreamResponse)
                    } else {
                        DnsPacketCodec.servfailResponse(query)
                    }
                }
                outputStream.write(response)
            }
        } finally {
            AdProtectionStore.recordBlockedDnsLookups(applicationContext, pendingBlockedLookups)
            inputStream.close()
            outputStream.close()
        }
    }

    private fun resolveDns(query: ByteArray): ByteArray {
        val upstream = InetAddress.getByName("1.1.1.1")
        val socket = DatagramSocket()
        try {
            if (!protect(socket)) {
                throw IllegalStateException("Could not protect upstream DNS socket from the VPN")
            }
            socket.soTimeout = 3_000
            socket.connect(upstream, 53)
            socket.send(DatagramPacket(query, query.size, upstream, 53))

            val responseBuffer = ByteArray(4_096)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            return responsePacket.data.copyOf(responsePacket.length)
        } finally {
            socket.close()
        }
    }

    private suspend fun runPacketLoop(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        var totalBytesRead = 0L
        var lastKeepAlive = System.currentTimeMillis()

        try {
            while (isActive && isRunning) {
                var idle = true

                // Non-blocking read from TUN interface
                val length = inputStream.channel.read(packet)
                if (length > 0) {
                    idle = false
                    totalBytesRead += length
                    packet.clear()
                }

                // Periodic socket check to maintain remote endpoint connection
                if (System.currentTimeMillis() - lastKeepAlive > 10_000) {
                    lastKeepAlive = System.currentTimeMillis()
                    checkRemoteEndpoint(currentServerIp)
                }

                if (idle) {
                    delay(100)
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                Log.w(TAG, "Packet loop ended: ${e.message}")
            }
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    private fun checkRemoteEndpoint(ip: String) {
        if (ip.isBlank()) return
        try {
            val sock = Socket()
            protect(sock)
            sock.connect(InetSocketAddress(ip, 443), 2000)
            sock.close()
        } catch (_: Exception) {
            // Non-fatal keepalive check
        }
    }

    private fun disconnectTunnel() {
        Log.i(TAG, "Tearing down VPN Tunnel...")
        closeCurrentTunnel()
        updateStatusDisconnected()
    }

    private fun closeCurrentTunnel() {
        isRunning = false
        isAdBlocking = false
        tunnelJob?.cancel()
        tunnelJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing VPN interface: ${e.message}")
        }
        vpnInterface = null
    }

    private fun updateStatusDisconnected(error: String? = null) {
        if (!error.isNullOrBlank()) Log.w(TAG, error)
    }

    private fun buildVpnNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val disconnectIntent = Intent(this, GlitchVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this, 1, disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Glitch OS VPN Shield")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Glitch VPN Shield Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active VPN Tunnel & IP masking status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        disconnectTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
