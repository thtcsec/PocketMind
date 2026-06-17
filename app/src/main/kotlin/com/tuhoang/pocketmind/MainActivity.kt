package com.tuhoang.pocketmind

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tuhoang.pocketmind.ui.navigation.PocketMindNavHost
import com.tuhoang.pocketmind.ui.theme.PocketMindTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketMindTheme {
                PocketMindNavHost()
            }
        }
    }
}
