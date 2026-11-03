package com.example.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tools.recover.PhotoRecNative
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var destinationUri by remember { mutableStateOf<Uri?>(null) }
    var isRecovering by remember { mutableStateOf(false) }
    var isScanningNative by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf<PhotoRecProgress?>(null) }
    var recoveredFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf("Choose a disk image and a folder for recovered files.") }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        sourceUri = uri
        sourceName = uri?.let { queryDisplayName(context.contentResolver, it) }
        status = if (uri == null) {
            "No disk image selected."
        } else {
            "Source selected. Use a raw disk image (not an individual media/document file); choose an output folder to continue."
        }
    }
    val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        destinationUri = uri
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            status = "Output folder selected."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Recover files", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
      Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.Start
      ) {
        Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = null,
            tint = NeonGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "PhotoRec File Recovery",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a raw disk image (for example .img or .dd) and a separate output folder. The source is read-only and will not be changed. Android does not allow this app to scan protected phone storage directly. PhotoRec can find file signatures in the image, but cannot always tell deleted files from files that still exist.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = { sourcePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecovering
        ) {
            Icon(imageVector = Icons.Default.ImageSearch, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sourceName ?: "Choose disk image",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { destinationPicker.launch(null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecovering
        ) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (destinationUri == null) "Choose output folder" else "Output folder selected")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val image = sourceUri
                if (image == null) {
                    status = "Choose a raw disk image first. Android does not allow recovery scanning of protected phone storage directly."
                    return@Button
                }
                val destination = destinationUri
                if (destination == null) {
                    status = "Choose a separate output folder before starting recovery. Do not select the source device or image."
                    return@Button
                }
                coroutineScope.launch {
                    isRecovering = true
                    isScanningNative = false
                    isCancelling = false
                    scanProgress = null
                    recoveredFiles = emptyList()
                    status = "Preparing image..."
                    try {
                        val result = withContext(Dispatchers.IO) {
                            runPhotoRec(
                                context = context,
                                imageUri = image,
                                destinationTreeUri = destination,
                                onScanStarted = {
                                    withContext(Dispatchers.Main) {
                                        isScanningNative = true
                                        status = "Scanning image..."
                                    }
                                },
                                onProgress = { progress ->
                                    withContext(Dispatchers.Main) {
                                        scanProgress = progress
                                        val eta = progress.estimatedTime
                                        status = if (eta != null) {
                                            "Scanning image: ${progress.percent}% · ${progress.filesFound} files found · ETA $eta"
                                        } else {
                                            "Scanning image: ${progress.percent}% · ${progress.filesFound} files found"
                                        }
                                    }
                                }
                            )
                        }
                        recoveredFiles = result.files
                        status = when {
                            result.cancelled && result.files.isNotEmpty() ->
                                "Scan stopped. ${result.files.size} file signatures were exported to the selected folder."
                            result.cancelled ->
                                "Scan stopped before PhotoRec found any file signatures."
                            result.files.isEmpty() ->
                                "Scan finished. PhotoRec found no recognizable file signatures."
                            else ->
                                "Scan finished. ${result.files.size} file signatures were exported. Check the list below."
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: LinkageError) {
                        status = "PhotoRec native engine is unavailable: ${error.message ?: "load failure"}"
                    } catch (error: Exception) {
                        status = "Recovery failed: ${error.localizedMessage ?: "Unknown error"}"
                    } finally {
                        isRecovering = false
                        isScanningNative = false
                        isCancelling = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRecovering,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
        ) {
            if (isRecovering) {
                if (scanProgress == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonGreen
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { (scanProgress?.percent ?: 0) / 100f },
                        modifier = Modifier.width(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonGreen
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Scanning image...", color = TextPrimary)
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Start PhotoRec scan", color = TextPrimary)
            }
        }
        if (isScanningNative) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching { PhotoRecNative.cancelScan() }
                    isCancelling = true
                    status = "Stopping PhotoRec..."
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCancelling
            ) {
                Text(text = if (isCancelling) "Stopping..." else "Cancel scan")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = status,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "PhotoRec scans only the raw image you select. It cannot safely determine whether each match was deleted or is still present in that image, and scanning this app does not change the source. It cannot access Android's protected internal partitions, recover cloud-only files, or guarantee every deleted file. Save results to a separate drive or folder.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
        if (recoveredFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PhotoRec matches (${recoveredFiles.size})",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            recoveredFiles.take(100).forEach { fileName ->
                Text(
                    text = fileName,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
            if (recoveredFiles.size > 100) {
                Text(
                    text = "Showing the first 100 names. All ${recoveredFiles.size} exported matches are in the selected output folder.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
            }
        }
}

private data class PhotoRecProgress(
    val percent: Int,
    val filesFound: Long,
    val estimatedTime: String?
)

private data class PhotoRecResult(
    val files: List<String>,
    val cancelled: Boolean
)

private suspend fun runPhotoRec(
    context: Context,
    imageUri: Uri,
    destinationTreeUri: Uri,
    onScanStarted: suspend () -> Unit,
    onProgress: suspend (PhotoRecProgress) -> Unit
): PhotoRecResult {
    val outputDirectory = File(context.filesDir, "photorec/${UUID.randomUUID()}")
    if (!outputDirectory.mkdirs()) {
        throw IOException("Could not create the private recovery workspace.")
    }

    try {
        val rootDocumentId = DocumentsContract.getTreeDocumentId(destinationTreeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(destinationTreeUri, rootDocumentId)
        val exportDirectory = DocumentsContract.createDocument(
            context.contentResolver,
            rootDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            "PhotoRec-${System.currentTimeMillis()}"
        ) ?: throw IOException("Could not create an output folder in the selected location.")

        val progressLog = File(outputDirectory.parentFile, "${outputDirectory.name}-progress.jsonl")
        val sourceDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")
            ?: throw IOException("Could not open the selected image.")
        val nativeResult = sourceDescriptor.use { descriptor ->
            val seekable = try {
                Os.lseek(descriptor.fileDescriptor, 0L, OsConstants.SEEK_SET)
                true
            } catch (_: ErrnoException) {
                false
            }

            if (seekable) {
                runPhotoRecScan(
                    descriptor,
                    outputDirectory,
                    progressLog,
                    onScanStarted,
                    onProgress
                )
            } else {
                descriptor.close()
                val cachedImage = File(context.cacheDir, "photorec-${UUID.randomUUID()}.img")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    cachedImage.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Could not copy the selected image for scanning.")
                try {
                    ParcelFileDescriptor.open(cachedImage, ParcelFileDescriptor.MODE_READ_ONLY).use { localImage ->
                        runPhotoRecScan(
                            localImage,
                            outputDirectory,
                            progressLog,
                            onScanStarted,
                            onProgress
                        )
                    }
                } finally {
                    cachedImage.delete()
                }
            }
        }
        val cancelled = nativeResult == -5
        if (nativeResult != 0 && !cancelled) {
            throw IOException("PhotoRec returned error code $nativeResult.")
        }

        return PhotoRecResult(
            files = copyRecoveredFiles(context, outputDirectory, exportDirectory),
            cancelled = cancelled
        )
    } finally {
        outputDirectory.deleteRecursively()
        File(outputDirectory.parentFile, "${outputDirectory.name}-progress.jsonl").delete()
    }
}

private suspend fun runPhotoRecScan(
    descriptor: ParcelFileDescriptor,
    outputDirectory: File,
    progressLog: File,
    onScanStarted: suspend () -> Unit,
    onProgress: suspend (PhotoRecProgress) -> Unit
): Int = coroutineScope {
    onScanStarted()
    val scanJob = async(Dispatchers.IO) {
        val fd = descriptor.detachFd()
        ParcelFileDescriptor.adoptFd(fd).use {
            PhotoRecNative.runScan(fd, outputDirectory.absolutePath, progressLog.absolutePath)
        }
    }

    var logPosition = 0L
    var completed = false
    try {
        while (scanJob.isActive) {
            logPosition = readPhotoRecProgress(progressLog, logPosition, onProgress)
            if (scanJob.isActive) delay(500)
        }
        logPosition = readPhotoRecProgress(progressLog, logPosition, onProgress)
        val result = scanJob.await()
        completed = true
        result
    } finally {
        if (!completed) {
            PhotoRecNative.cancelScan()
            withContext(NonCancellable) {
                scanJob.join()
            }
        }
    }
}

private suspend fun readPhotoRecProgress(
    progressLog: File,
    currentPosition: Long,
    onProgress: suspend (PhotoRecProgress) -> Unit
): Long {
    if (!progressLog.isFile) return currentPosition
    var logPosition = currentPosition
    RandomAccessFile(progressLog, "r").use { log ->
        if (log.length() <= logPosition) return logPosition
        log.seek(logPosition)
        while (true) {
            val lineStart = log.filePointer
            val line = log.readLine() ?: break
            if (!line.endsWith('}')) {
                log.seek(lineStart)
                break
            }
            logPosition = log.filePointer
            val progress = parsePhotoRecProgress(line)
            if (progress != null) onProgress(progress)
        }
    }
    return logPosition
}

private fun parsePhotoRecProgress(line: String): PhotoRecProgress? {
    if (line.isBlank()) return null
    return try {
        val event = JSONObject(line)
        if (event.optString("type") != "progress") return null
        val totalSectors = event.optLong("total_sectors")
        if (totalSectors <= 0L) return null
        val currentSector = event.optLong("current_sector").coerceIn(0L, totalSectors)
        PhotoRecProgress(
            percent = (currentSector.toDouble() / totalSectors * 100.0).toInt().coerceIn(0, 100),
            filesFound = event.optLong("files_found").coerceAtLeast(0L),
            estimatedTime = event.optString("estimated_time").takeUnless {
                it.isBlank() || it == "null"
            }
        )
    } catch (_: JSONException) {
        null
    }
}

private fun copyRecoveredFiles(context: Context, sourceDirectory: File, targetDirectory: Uri): List<String> {
    val copiedFiles = mutableListOf<String>()
    sourceDirectory.listFiles().orEmpty().forEach { source ->
        val mimeType = if (source.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            recoveredFileMimeType(source)
        }
        val target = DocumentsContract.createDocument(
            context.contentResolver,
            targetDirectory,
            mimeType,
            source.name
        ) ?: throw IOException("Could not export ${source.name}.")

        if (source.isDirectory) {
            copiedFiles += copyRecoveredFiles(context, source, target)
        } else {
            context.contentResolver.openOutputStream(target)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Could not write ${source.name}.")
            copiedFiles += source.name
        }
    }
    return copiedFiles
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}

private fun recoveredFileMimeType(file: File): String {
    val extension = file.extension.lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        ?: when (extension) {
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "application/octet-stream"
        }
}
