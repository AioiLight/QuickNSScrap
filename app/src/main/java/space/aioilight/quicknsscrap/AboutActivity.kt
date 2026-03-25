package space.aioilight.quicknsscrap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import space.aioilight.quicknsscrap.ui.screen.AboutScreen
import space.aioilight.quicknsscrap.ui.theme.QuickNSScrapTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickNSScrapTheme {
                AboutScreen(onBack = { finish() })
            }
        }
    }
}
