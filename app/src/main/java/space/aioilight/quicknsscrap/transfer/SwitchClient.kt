package space.aioilight.quicknsscrap.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import space.aioilight.quicknsscrap.Constants

class SwitchClient {

    companion object {
        private const val BASE = "http://192.168.0.1"
        private const val DATA_JSON = "$BASE/data.json"
        private const val IMG_BASE = "$BASE/img"
    }

    // data.json の実際のフォーマット:
    // { "FileType": "photo"|"movie", "ConsoleName": "...", "FileNames": ["2024XXXX.jpg", ...] }
    suspend fun fetchFileList(): SwitchFileList = withContext(Dispatchers.IO) {
        val conn = (URL(DATA_JSON).openConnection() as HttpURLConnection).apply {
            connectTimeout = Constants.CONNECT_TIMEOUT_MS
            readTimeout = Constants.FETCH_READ_TIMEOUT_MS
        }
        try {
            val json = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(json)
            val isVideo = obj.optString("FileType") == "movie"
            val consoleName = obj.optString("ConsoleName", "Nintendo Switch")
            val arr = obj.getJSONArray("FileNames")
            val files = List(arr.length()) { SwitchMediaFile(arr.getString(it), isVideo) }
            SwitchFileList(consoleName, files)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun downloadFile(
        file: SwitchMediaFile,
        dest: OutputStream,
        onProgress: (Float) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val conn = (URL("$IMG_BASE/${file.name}").openConnection() as HttpURLConnection).apply {
            connectTimeout = Constants.CONNECT_TIMEOUT_MS
            readTimeout = Constants.DOWNLOAD_READ_TIMEOUT_MS
        }
        try {
            conn.connect()
            // contentLengthLong を使うことで 2GB 超ファイルでも Long の範囲で正しく扱える
            val total = conn.contentLengthLong
            var downloaded = 0L
            val buffer = ByteArray(Constants.DOWNLOAD_BUFFER_SIZE)
            conn.inputStream.use { input ->
                var read: Int
                // バッファを使ったストリーミング読み込み: メモリに全展開せず少量ずつ書き出す
                while (input.read(buffer).also { read = it } >= 0) {
                    dest.write(buffer, 0, read)
                    downloaded += read
                    // プログレスコールバックは UI スレッド (Main) で呼ぶ必要がある
                    // Content-Length が不明 (total <= 0) な場合は進捗を通知しない
                    if (total > 0) withContext(Dispatchers.Main) {
                        onProgress(downloaded.toFloat() / total)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
