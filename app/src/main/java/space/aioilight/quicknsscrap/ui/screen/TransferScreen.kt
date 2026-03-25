package space.aioilight.quicknsscrap.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.aioilight.quicknsscrap.Constants
import space.aioilight.quicknsscrap.R
import space.aioilight.quicknsscrap.settings.AppSettings
import space.aioilight.quicknsscrap.settings.TitleDatabase
import space.aioilight.quicknsscrap.transfer.SwitchClient
import space.aioilight.quicknsscrap.transfer.SwitchMediaFile
import androidx.compose.foundation.Image as BitmapImage

private sealed class TransferUiState {
    data object Loading : TransferUiState()
    data class Downloading(
        val currentFile: String,
        val fileProgress: Float,
        val done: Int,
        val total: Int,
    ) : TransferUiState()
    data class Done(
        val savedFiles: List<Pair<SwitchMediaFile, Uri>>,
        val failedFiles: List<String>,
    ) : TransferUiState()
    data class Error(val message: String) : TransferUiState()
}

@Composable
fun TransferScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val client = remember { SwitchClient() }
    val settings = remember { AppSettings(context) }
    val titleDatabase = remember { TitleDatabase(context) }
    var uiState by remember { mutableStateOf<TransferUiState>(TransferUiState.Loading) }

    LaunchedEffect(Unit) {
        val fileList = try {
            client.fetchFileList()
        } catch (e: Exception) {
            uiState = TransferUiState.Error(context.getString(R.string.fetch_file_list_failed, e.message.orEmpty()))
            return@LaunchedEffect
        }

        val organizeByGame = settings.organizeFolderByGame
        val stripId = settings.stripIdFromFilename
        val savedFiles = mutableListOf<Pair<SwitchMediaFile, Uri>>()
        val failedFiles = mutableListOf<String>()
        fileList.files.forEachIndexed { index, file ->
            uiState = TransferUiState.Downloading(
                currentFile = file.name,
                fileProgress = 0f,
                done = index,
                total = fileList.files.size,
            )
            val gameFolder = if (organizeByGame) {
                file.titleId?.let { id ->
                    titleDatabase.lookup(id)?.let { sanitizeFolderName(it.name) }
                }
            } else null
            val displayName = if (gameFolder != null && stripId) file.nameWithoutId else file.name
            val uri = insertMediaStoreEntry(context, file, gameFolder, displayName) ?: run {
                failedFiles.add(file.name)
                return@forEachIndexed
            }
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    client.downloadFile(file, outputStream) { progress ->
                        uiState = TransferUiState.Downloading(
                            currentFile = file.name,
                            fileProgress = progress,
                            done = index,
                            total = fileList.files.size,
                        )
                    }
                }
                savedFiles.add(file to uri)
            } catch (_: Exception) {
                context.contentResolver.delete(uri, null, null)
                failedFiles.add(file.name)
            }
        }
        uiState = TransferUiState.Done(savedFiles.toList(), failedFiles.toList())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        when (val s = uiState) {
            is TransferUiState.Loading -> {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.fetching_file_list))
                }
            }

            is TransferUiState.Downloading -> {
                Column(
                    Modifier.align(Alignment.Center).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.downloading_progress, s.done + 1, s.total))
                    Spacer(Modifier.height(8.dp))
                    Text(s.currentFile, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { s.fileProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { s.done.toFloat() / s.total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.total_progress, s.done, s.total),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            is TransferUiState.Done -> {
                Column(Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (s.failedFiles.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (s.failedFiles.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.done_title), style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.saved_count, s.savedFiles.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (s.failedFiles.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.failed_count, s.failedFiles.size),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(s.savedFiles) { (file, uri) ->
                            SavedFileItem(file = file, uri = uri)
                        }
                        if (s.failedFiles.isNotEmpty()) {
                            items(s.failedFiles) { name ->
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (s.savedFiles.size > 1) {
                            FilledTonalButton(
                                onClick = {
                                    val mimeType = if (s.savedFiles.first().first.isVideo) "video/*" else "image/*"
                                    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = mimeType
                                        putParcelableArrayListExtra(
                                            Intent.EXTRA_STREAM,
                                            ArrayList(s.savedFiles.map { it.second }),
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, null))
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(stringResource(R.string.share_all))
                            }
                        }
                        Button(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }

            is TransferUiState.Error -> {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.error_title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(s.message)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            }
        }
    }
}

@Composable
private fun SavedFileItem(file: SwitchMediaFile, uri: Uri) {
    val context = LocalContext.current
    val thumbnail by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(320, 180), null)
            }.getOrNull()
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(128.dp)
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = thumbnail
                if (bmp != null) {
                    BitmapImage(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = file.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = file.mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
            }
        }
    }
}

// ゲームタイトル名をフォルダ名として安全に使えるよう無効文字を除去する
private fun sanitizeFolderName(name: String): String =
    name.replace(Regex("[/\\\\:*?\"<>| ]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')

// MediaStore にファイルのメタ情報を登録して書き込み先 URI を返す
private fun insertMediaStoreEntry(context: Context, file: SwitchMediaFile, gameFolder: String?, displayName: String): Uri? {
    val baseDir = if (file.isVideo) Constants.MOVIES_RELATIVE_PATH else Constants.PICTURES_RELATIVE_PATH
    val relativePath = if (gameFolder != null) "$baseDir/$gameFolder" else baseDir
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, file.mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
    }
    val collection = if (file.isVideo) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    return context.contentResolver.insert(collection, contentValues)
}
