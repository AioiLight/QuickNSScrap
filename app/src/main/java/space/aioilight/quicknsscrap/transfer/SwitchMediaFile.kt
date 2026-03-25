package space.aioilight.quicknsscrap.transfer

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class SwitchMediaFile(val name: String, val isVideo: Boolean) {
    val mimeType: String get() = if (isVideo) "video/mp4" else "image/jpeg"

    // ファイル名フォーマット: YYYYMMDDHHMMSSII-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.ext
    // X 部分(32 char hex): AES/ECB で暗号化された title ID。復号して平文 16 char hex を返す。

    // ID 部分(-XXXXXXXX...)を除いたファイル名を返す。ID が含まれない場合は元のファイル名をそのまま返す。
    val nameWithoutId: String get() {
        val base = name.substringBeforeLast(".")
        val ext = name.substringAfterLast(".", "")
        val parts = base.split("-")
        if (parts.size < 2 || parts.last().length != 32) return name
        val stripped = parts.dropLast(1).joinToString("-")
        return if (ext.isEmpty()) stripped else "$stripped.$ext"
    }

    val titleId: String? get() {
        val parts = name.substringBeforeLast(".").split("-")
        if (parts.size < 2) return null
        val encryptedHex = parts.last()
        if (encryptedHex.length != 32) return null
        return runCatching {
            val encBytes = encryptedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(ALBUM_KEY, "AES"))
            val dec = cipher.doFinal(encBytes) // 16 bytes: [reversed_title_id(8)] + [zero_padding(8)]
            dec.take(8).reversed()             // 先頭 8 バイトを un-reverse → 平文 title ID バイト列
                .joinToString("") { "%02X".format(it) }
        }.getOrNull()
    }

    companion object {
        // Nintendo Switch capsrv NSO から抽出されたアルバム暗号化鍵 (AES-128)
        private val ALBUM_KEY = byteArrayOf(
            0xB7.toByte(), 0xED.toByte(), 0x7A.toByte(), 0x66.toByte(),
            0xC8.toByte(), 0x0B.toByte(), 0x4B.toByte(), 0x00.toByte(),
            0x8B.toByte(), 0xAF.toByte(), 0x7F.toByte(), 0x05.toByte(),
            0x89.toByte(), 0xC0.toByte(), 0x82.toByte(), 0x24.toByte(),
        )
    }
}

data class SwitchFileList(
    val consoleName: String,
    val files: List<SwitchMediaFile>,
)
