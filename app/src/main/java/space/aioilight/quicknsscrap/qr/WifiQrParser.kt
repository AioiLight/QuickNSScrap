package space.aioilight.quicknsscrap.qr

data class WifiCredentials(
    val ssid: String,
    val password: String,
    val security: String = "WPA",
)

object WifiQrParser {
    // Format: WIFI:T:WPA;S:ssid;P:password;;
    fun parse(rawValue: String): WifiCredentials? {
        if (!rawValue.startsWith("WIFI:")) return null

        val fields = mutableMapOf<String, String>()
        val content = rawValue.removePrefix("WIFI:")

        // "キー:値;" の繰り返しを手動パースする
        // 標準ライブラリの split では値中のエスケープ済み ";" を正しく扱えないため独自実装
        var i = 0
        while (i < content.length) {
            val colon = content.indexOf(':', i)
            if (colon < 0) break
            val key = content.substring(i, colon)

            val sb = StringBuilder()
            var j = colon + 1
            while (j < content.length) {
                when {
                    // バックスラッシュエスケープ: "\;" や "\\" などをそのまま次の文字として扱う
                    content[j] == '\\' && j + 1 < content.length -> {
                        sb.append(content[j + 1]); j += 2
                    }
                    // ";" はフィールドの区切り文字なのでループを抜ける
                    content[j] == ';' -> { j++; break }
                    else -> { sb.append(content[j]); j++ }
                }
            }
            fields[key] = sb.toString()
            i = j
        }

        val ssid = fields["S"] ?: return null
        val password = fields["P"] ?: ""
        val security = fields["T"] ?: "WPA"
        return WifiCredentials(ssid, password, security)
    }
}
