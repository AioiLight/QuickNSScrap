package space.aioilight.quicknsscrap.settings

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class TitleEntry(val name: String, val nsuId: Long)

class TitleDatabase(private val context: Context) {

    companion object {
        private const val DB_FILE = "titledb.json"

        private fun dbUrl(language: String): String {
            val region = if (language == "ja") "JP" else "US"
            return "https://raw.githubusercontent.com/blawar/titledb/refs/heads/master/$region.$language.json"
        }

        @Volatile
        private var cache: Map<String, TitleEntry>? = null

        fun clearCache() {
            synchronized(Companion) { cache = null }
        }
    }

    suspend fun download(language: String) = withContext(Dispatchers.IO) {
        val conn = URL(dbUrl(language)).openConnection()
        context.openFileOutput(DB_FILE, Context.MODE_PRIVATE).bufferedWriter().use { out ->
            JsonReader(conn.getInputStream().bufferedReader()).use { reader ->
                JsonWriter(out).use { writer ->
                    val seen = HashSet<String>()
                    writer.beginObject()
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName() // outer key (ignored)
                        var id: String? = null
                        var name: String? = null
                        var nsuId: Long = 0
                        reader.beginObject()
                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "id"    -> id    = if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null } else reader.nextString()
                                "name"  -> name  = if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null } else reader.nextString()
                                "nsuId" -> nsuId = if (reader.peek() == JsonToken.NULL) { reader.nextNull(); 0L  } else reader.nextLong()
                                else    -> reader.skipValue()
                            }
                        }
                        reader.endObject()
                        val validId = id?.takeIf { it.isNotBlank() }?.uppercase() ?: continue
                        val validName = name?.takeIf { it.isNotBlank() } ?: continue
                        if (!seen.add(validId)) continue
                        writer.name(validId)
                        writer.beginObject()
                        writer.name("name").value(validName)
                        writer.name("nsuId").value(nsuId)
                        writer.endObject()
                    }
                    reader.endObject()
                    writer.endObject()
                }
            }
        }
        synchronized(Companion) { cache = null }
    }

    suspend fun lookup(titleId: String): TitleEntry? = withContext(Dispatchers.IO) {
        val map = cache ?: loadCache()
        map?.get(titleId.uppercase())
    }

    private fun loadCache(): Map<String, TitleEntry>? {
        // ダブルチェックロッキング: 最初のチェックで既に読み込み済みなら同期ブロックに入らない
        cache?.let { return it }
        synchronized(Companion) {
            // synchronized 内でも再チェック: 競合スレッドが先に読み込んでいた場合に備える
            cache?.let { return it }
            val file = context.getFileStreamPath(DB_FILE)
            if (!file.exists()) return null
            return try {
                val obj = JSONObject(file.readText())
                val result = HashMap<String, TitleEntry>(obj.length())
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val entry = obj.optJSONObject(key) ?: continue
                    val name = entry.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val nsuId = entry.optLong("nsuId")
                    result.putIfAbsent(key, TitleEntry(name = name, nsuId = nsuId))
                }
                result.also { cache = it }
            } catch (_: Exception) {
                null
            }
        }
    }
}
