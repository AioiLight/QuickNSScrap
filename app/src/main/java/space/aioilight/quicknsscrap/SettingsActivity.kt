package space.aioilight.quicknsscrap

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import space.aioilight.quicknsscrap.ui.screen.SettingsScreen
import space.aioilight.quicknsscrap.ui.theme.QuickNSScrapTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickNSScrapTheme {
                SettingsScreen(
                    onBack = { finish() },
                    onAbout = { startActivity(Intent(this, AboutActivity::class.java)) },
                )
            }
        }
    }
}
