package space.aioilight.quicknsscrap

import android.content.Intent
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.aioilight.quicknsscrap.qr.WifiCredentials
import space.aioilight.quicknsscrap.ui.screen.QrScannerScreen
import space.aioilight.quicknsscrap.ui.screen.TransferScreen
import space.aioilight.quicknsscrap.ui.theme.QuickNSScrapTheme
import space.aioilight.quicknsscrap.wifi.WifiConnector

class MainActivity : ComponentActivity() {

    private lateinit var wifiConnector: WifiConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiConnector = WifiConnector(this)
        enableEdgeToEdge()
        setContent {
            QuickNSScrapTheme {
                AppContent(
                    wifiConnector = wifiConnector,
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiConnector.release()
    }
}

// アプリ全体の接続フローをシールドクラスで表現するステートマシン
// Scanning → Connecting → Connected → (Transfer 完了後) → Scanning の流れが基本
// Settings / About は別 Activity として管理
private sealed class AppState {
    data object Scanning : AppState()
    data class Connecting(val credentials: WifiCredentials) : AppState()
    data class Connected(val credentials: WifiCredentials, val network: Network) : AppState()
    data class Error(val message: String) : AppState()
}

private fun navigationTransition(
    initialState: AppState,
    targetState: AppState,
): ContentTransform {
    // Scanning は起点、Connecting/Connected/Error は前進方向
    val forward = targetState !is AppState.Scanning || initialState is AppState.Scanning
    return if (forward) {
        (slideInHorizontally { it } + fadeIn())
            .togetherWith(slideOutHorizontally { -it } + fadeOut())
    } else {
        (slideInHorizontally { -it } + fadeIn())
            .togetherWith(slideOutHorizontally { it } + fadeOut())
    }
}

@Composable
private fun AppContent(wifiConnector: WifiConnector, onOpenSettings: () -> Unit) {
    val connectionLostMsg = stringResource(R.string.connection_lost)
    var state by remember { mutableStateOf<AppState>(AppState.Scanning) }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { navigationTransition(initialState, targetState) },
            contentKey = { it::class },
            label = "screenTransition",
            modifier = Modifier.padding(padding),
        ) { s ->
            when (s) {
                is AppState.Scanning -> QrScannerScreen(
                    onCredentialsFound = { credentials ->
                        state = AppState.Connecting(credentials)
                        wifiConnector.connect(credentials) { connState ->
                            state = when (connState) {
                                is WifiConnector.State.Connecting -> AppState.Connecting(credentials)
                                is WifiConnector.State.Connected  -> AppState.Connected(credentials, connState.network)
                                is WifiConnector.State.Lost       -> AppState.Error(connectionLostMsg)
                                is WifiConnector.State.Failed     -> AppState.Error(connState.reason)
                            }
                        }
                    },
                    onSettings = onOpenSettings,
                )

                is AppState.Connecting -> ConnectingScreen(s.credentials)

                is AppState.Connected -> TransferScreen {
                    wifiConnector.release()
                    state = AppState.Scanning
                }

                is AppState.Error -> ErrorScreen(s.message) {
                    state = AppState.Scanning
                }
            }
        }
    }
}

@Composable
private fun ConnectingScreen(credentials: WifiCredentials) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.connecting_to, credentials.ssid))
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
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
        Text(message)
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.vpn_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
