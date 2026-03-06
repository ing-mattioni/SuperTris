package it.claudio.supertris

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import it.claudio.supertris.ui.SuperTrisApp
import it.claudio.supertris.ui.theme.SuperTrisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            SuperTrisTheme {
                SuperTrisApp()
            }
        }
    }
}