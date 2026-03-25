package space.aioilight.quicknsscrap.settings

import android.content.Context
import android.widget.Toast
import space.aioilight.quicknsscrap.R

class AppSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var organizeFolderByGame: Boolean
        get() = prefs.getBoolean("organize_folder_by_game", false)
        set(value) { commit { putBoolean("organize_folder_by_game", value) } }

    var stripIdFromFilename: Boolean
        get() = prefs.getBoolean("strip_id_from_filename", false)
        set(value) { commit { putBoolean("strip_id_from_filename", value) } }

    var titleDbLanguage: String
        get() = prefs.getString("titledb_language", "ja") ?: "ja"
        set(value) { commit { putString("titledb_language", value) } }

    var titleDbDownloadedAt: Long?
        get() = prefs.getLong("titledb_downloaded_at", -1L).takeIf { it >= 0 }
        set(value) {
            if (value != null) commit { putLong("titledb_downloaded_at", value) }
            else commit { remove("titledb_downloaded_at") }
        }

    private fun commit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        val success = prefs.edit().apply(block).commit()
        if (!success) {
            Toast.makeText(context, R.string.settings_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
