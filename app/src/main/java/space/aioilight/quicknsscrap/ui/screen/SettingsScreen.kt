package space.aioilight.quicknsscrap.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import space.aioilight.quicknsscrap.R
import space.aioilight.quicknsscrap.settings.AppSettings
import space.aioilight.quicknsscrap.settings.TitleDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val titleDatabase = remember { TitleDatabase(context) }
    val scope = rememberCoroutineScope()

    var organizeByGame by remember { mutableStateOf(settings.organizeFolderByGame) }
    var stripId by remember { mutableStateOf(settings.stripIdFromFilename) }
    var dbLanguage by remember { mutableStateOf(settings.titleDbLanguage) }
    var downloadedAt by remember { mutableStateOf(settings.titleDbDownloadedAt) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Text(stringResource(R.string.save_location), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = organizeByGame,
                        onValueChange = {
                            organizeByGame = it
                            settings.organizeFolderByGame = it
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.organize_by_game))
                    Text(
                        stringResource(R.string.organize_by_game_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = organizeByGame, onCheckedChange = null)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = stripId,
                        enabled = organizeByGame,
                        onValueChange = {
                            stripId = it
                            settings.stripIdFromFilename = it
                        },
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val enabled = organizeByGame
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.strip_id_from_filename),
                        color = if (enabled) LocalContentColor.current
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                    Text(
                        stringResource(R.string.strip_id_from_filename_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }
                Switch(checked = stripId, onCheckedChange = null, enabled = organizeByGame)
            }
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.game_database), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.db_language), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            val languages = listOf("ja", "en")
            val languageLabels = listOf(
                stringResource(R.string.db_language_ja),
                stringResource(R.string.db_language_en),
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                languages.forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = dbLanguage == lang,
                        onClick = {
                            if (dbLanguage != lang) {
                                dbLanguage = lang
                                settings.titleDbLanguage = lang
                                // 言語変更時はキャッシュと取得済み日時をリセット
                                TitleDatabase.clearCache()
                                settings.titleDbDownloadedAt = null
                                downloadedAt = null
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size),
                        enabled = !isDownloading,
                    ) {
                        Text(languageLabels[index])
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (downloadedAt != null)
                    stringResource(R.string.last_updated, formatDate(downloadedAt!!))
                else
                    stringResource(R.string.not_downloaded),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    isDownloading = true
                    downloadError = null
                    scope.launch {
                        try {
                            titleDatabase.download(dbLanguage)
                            val now = System.currentTimeMillis()
                            settings.titleDbDownloadedAt = now
                            downloadedAt = now
                        } catch (e: Exception) {
                            downloadError = context.getString(R.string.download_failed, e.message.orEmpty())
                        } finally {
                            isDownloading = false
                        }
                    }
                },
                enabled = !isDownloading,
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.download_database))
                }
            }
            if (downloadError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    downloadError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.game_db_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.about))
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
