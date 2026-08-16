package com.pathsathi.app
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pathsathi.app.core.LanguageManager
import com.pathsathi.app.ui.navigation.PathSathiNavGraph
import com.pathsathi.app.ui.theme.PathSathiTheme
class MainActivity:ComponentActivity(){
 private val notificationLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun attachBaseContext(newBase:Context){super.attachBaseContext(LanguageManager.wrap(newBase))}
 override fun onCreate(state:Bundle?){super.onCreate(state);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);setContent{PathSathiTheme{Surface(Modifier.fillMaxSize()){PathSathiNavGraph()}}}}
}
