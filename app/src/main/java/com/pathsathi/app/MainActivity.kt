package com.pathsathi.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.pathsathi.app.ai.AssistantLanguageManager
import com.pathsathi.app.data.local.AppPreferences
import com.pathsathi.app.data.local.LocaleHelper
import com.pathsathi.app.data.local.ThemeMode
import com.pathsathi.app.data.network.NetworkModeManager
import com.pathsathi.app.navigation.PathSathiNavGraph
import com.pathsathi.app.navigation.Routes
import com.pathsathi.app.ui.theme.PathSathiTheme
import kotlinx.coroutines.flow.map

class MainActivity : ComponentActivity() {

    private var pendingDeepLink: String? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkModeManager.init(applicationContext)
        AssistantLanguageManager.init(applicationContext)
        pendingDeepLink = deepLinkFromIntent(intent)

        setContent {
            val themeMode by AppPreferences.themeMode(this).collectAsState(initial = ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            PathSathiTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PathSathiNavGraph(startDeepLinkRoute = pendingDeepLink)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Deep links arriving while the app is already open (e.g. tapping a
        // check-in notification) are handled by re-launching MainActivity;
        // a full nav-graph deep-link dispatch is out of scope for a running
        // instance, so we simply store it for the next cold start.
        pendingDeepLink = deepLinkFromIntent(intent)
    }

    private fun deepLinkFromIntent(intent: Intent?): String? = when {
        intent?.getBooleanExtra("open_checkin", false) == true -> Routes.CHECKIN_PROMPT
        intent?.getBooleanExtra("open_sos", false) == true -> Routes.SOS
        else -> null
    }
}
