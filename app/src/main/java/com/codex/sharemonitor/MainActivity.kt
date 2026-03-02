package com.codex.sharemonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codex.sharemonitor.data.AppContainer

/**
 * Android 入口 Activity。
 *
 * 说明：此处仅负责创建 [AppContainer] 并设置 Compose 内容，具体导航与页面逻辑在 UI 层完成。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = AppContainer(applicationContext)
        setContent {
            ShareMonitorApp(appContainer = appContainer)
        }
    }
}
