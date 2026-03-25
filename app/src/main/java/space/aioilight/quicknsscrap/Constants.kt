package space.aioilight.quicknsscrap

internal object Constants {
    // Network timeouts (milliseconds)
    const val CONNECT_TIMEOUT_MS = 10_000
    const val FETCH_READ_TIMEOUT_MS = 10_000
    const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
    const val WIFI_CONNECT_TIMEOUT_MS = 30_000

    // Download buffer size (bytes)
    const val DOWNLOAD_BUFFER_SIZE = 8 * 1024

    // MediaStore relative paths
    const val PICTURES_RELATIVE_PATH = "Pictures/QuickNSScrap"
    const val MOVIES_RELATIVE_PATH = "Movies/QuickNSScrap"
}
