package com.tuhoang.pocketmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tuhoang.pocketmind.ui.navigation.PocketMindNavHost
import com.tuhoang.pocketmind.ui.theme.PocketMindTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketMindTheme {
                PocketMindNavHost()
            }
        }
    }
}
