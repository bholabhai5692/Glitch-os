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
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoverScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var destinationUri by remember { mutableStateOf<Uri?>(null) }
    var isRecovering by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose a disk image and a folder for recovered files.") }

    val sourcePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        sourceUri = uri
        sourceName = uri?.let { queryDisplayName(context.contentResolver, it) }
        status = if (uri == null) "No image selected." else "Image selected. Choose an output folder to continue."
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
            text = "PhotoRec scans a user-selected disk image. The source image is opened read-only; recovered files are written to a new folder.",
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
                val image = sourceUri ?: return@Button
                val destination = destinationUri ?: return@Button
                coroutineScope.launch {
                    isRecovering = true
                    isCancelling = false
                    status = "Preparing image..."
                    try {
                        val recoveredCount = withContext(Dispatchers.IO) {
                            runPhotoRec(context, image, destination)
                        }
                        status = if (recoveredCount == 0) {
                            "Scan finished. No recoverable files were found."
                        } else {
                            "Scan finished. $recoveredCount recovered files exported."
                        }
                    } catch (error: PhotoRecCancelledException) {
                        status = "Scan cancelled. Partial recovery files were discarded."
                    } catch (error: LinkageError) {
                        status = "PhotoRec native engine is unavailable: ${error.message ?: "load failure"}"
                    } catch (error: Exception) {
                        status = "Recovery failed: ${error.localizedMessage ?: "Unknown error"}"
                    } finally {
                        isRecovering = false
                        isCancelling = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sourceUri != null && destinationUri != null && !isRecovering,
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
        ) {
            if (isRecovering) {
                CircularProgressIndicator(
                    modifier = Modifier.width(18.dp),
                    strokeWidth = 2.dp,
                    color = NeonGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Scanning image...", color = TextPrimary)
            } else {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Start PhotoRec scan", color = TextPrimary)
            }
        }
        if (isRecovering) {
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
            text = "This uses the upstream PhotoRec recovery engine on disk images only. It cannot scan Android's protected internal partitions.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
            }
        }
}

private suspend fun runPhotoRec(context: Context, imageUri: Uri, destinationTreeUri: Uri): Int {
    val outputDirectory = File(context.filesDir, "photorec/${UUID.randomUUID()}")
    if (!outputDirectory.mkdirs()) {
        throw IOException("Could not create the private recovery workspace.")
    }

    try {
        val sourceDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")
            ?: throw IOException("Could not open the selected image.")
        val nativeResult = sourceDescriptor.use { descriptor ->
            val seekable = try {
                Os.lseek(descriptor.fileDescriptor, 0L, OsConstants.SEEK_CUR)
                true
            } catch (_: ErrnoException) {
                false
            }

            if (seekable) {
                runPhotoRecScan(descriptor, outputDirectory.absolutePath)
            } else {
                descriptor.close()
                val cachedImage = File(context.cacheDir, "photorec-${UUID.randomUUID()}.img")
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    cachedImage.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Could not copy the selected image for scanning.")
                try {
                    ParcelFileDescriptor.open(cachedImage, ParcelFileDescriptor.MODE_READ_ONLY).use { localImage ->
                        runPhotoRecScan(localImage, outputDirectory.absolutePath)
                    }
                } finally {
                    cachedImage.delete()
                }
            }
        }
        when (nativeResult) {
            0 -> Unit
            -5 -> throw PhotoRecCancelledException()
            else -> throw IOException("PhotoRec returned error code $nativeResult.")
        }

        val rootDocumentId = DocumentsContract.getTreeDocumentId(destinationTreeUri)
        val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(destinationTreeUri, rootDocumentId)
        val exportDirectory = DocumentsContract.createDocument(
            context.contentResolver,
            rootDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            "PhotoRec-${System.currentTimeMillis()}"
        ) ?: throw IOException("Could not create an output folder in the selected location.")

        return copyRecoveredFiles(context, outputDirectory, exportDirectory)
    } finally {
        outputDirectory.deleteRecursively()
    }
}

private fun runPhotoRecScan(descriptor: ParcelFileDescriptor, outputDirectory: String): Int {
    val fd = descriptor.detachFd()
    return ParcelFileDescriptor.adoptFd(fd).use {
        PhotoRecNative.runScan(fd, outputDirectory)
    }
}

private class PhotoRecCancelledException : IOException()

private fun copyRecoveredFiles(context: Context, sourceDirectory: File, targetDirectory: Uri): Int {
    var copiedCount = 0
    sourceDirectory.listFiles().orEmpty().forEach { source ->
        val mimeType = if (source.isDirectory) {
            DocumentsContract.Document.MIME_TYPE_DIR
        } else {
            "application/octet-stream"
        }
        val target = DocumentsContract.createDocument(
            context.contentResolver,
            targetDirectory,
            mimeType,
            source.name
        ) ?: throw IOException("Could not export ${source.name}.")

        if (source.isDirectory) {
            copiedCount += copyRecoveredFiles(context, source, target)
        } else {
            context.contentResolver.openOutputStream(target)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: throw IOException("Could not write ${source.name}.")
            copiedCount++
        }
    }
    return copiedCount
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}