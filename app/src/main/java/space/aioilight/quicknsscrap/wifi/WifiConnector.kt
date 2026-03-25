package space.aioilight.quicknsscrap.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import space.aioilight.quicknsscrap.Constants
import space.aioilight.quicknsscrap.R
import space.aioilight.quicknsscrap.qr.WifiCredentials

class WifiConnector(context: Context) {

    private val appContext = context.applicationContext
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    sealed interface State {
        data object Connecting : State
        data class Connected(val network: Network) : State
        data object Lost : State
        data class Failed(val reason: String) : State
    }

    fun connect(credentials: WifiCredentials, onState: (State) -> Unit) {
        // 既存の接続・コールバックを先にクリーンアップしてから新規接続を開始する
        release()

        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(credentials.ssid)
        // オープンネットワーク (NOPASS) の場合はパスフレーズを設定しない
        if (credentials.password.isNotEmpty() && credentials.security.uppercase() != "NOPASS") {
            specifierBuilder.setWpa2Passphrase(credentials.password)
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            // NET_CAPABILITY_INTERNET を除去することで「インターネットなし」通知を抑制する
            // Switch のホットスポットはインターネット接続を持たないが、これがないと
            // Android が自動的に別のネットワークに切り替えてしまう場合がある
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifierBuilder.build())
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // プロセス全体の通信を Switch のネットワークに向ける
                // これにより URL().openConnection() が Switch 経由になる
                connectivityManager.bindProcessToNetwork(network)
                onState(State.Connected(network))
            }

            override fun onLost(network: Network) {
                // ネットワーク喪失時はプロセスバインドを解除してデフォルト経路に戻す
                connectivityManager.bindProcessToNetwork(null)
                onState(State.Lost)
            }

            override fun onUnavailable() {
                onState(State.Failed(appContext.getString(R.string.wifi_connect_failed)))
            }
        }

        networkCallback = callback
        onState(State.Connecting)
        // タイムアウト付きでネットワーク要求を発行する（タイムアウト時は onUnavailable が呼ばれる）
        connectivityManager.requestNetwork(request, callback, Constants.WIFI_CONNECT_TIMEOUT_MS)
    }

    fun release() {
        // プロセスバインドを解除し、コールバック登録も取り消す
        // unregisterNetworkCallback は既に解除済みの場合に例外を投げることがあるため runCatching で保護
        connectivityManager.bindProcessToNetwork(null)
        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }
        networkCallback = null
    }
}
