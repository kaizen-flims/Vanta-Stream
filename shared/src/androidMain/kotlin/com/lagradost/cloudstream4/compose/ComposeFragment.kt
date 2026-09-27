package com.lagradost.cloudstream4.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lagradost.cloudstream4.rememberAppSettings
import com.lagradost.cloudstream4.theme.CloudStreamTheme
import com.lagradost.cloudstream4.theme.CloudStreamPrimaryColor
import com.lagradost.cloudstream4.theme.CloudStreamThemeMode
import com.mihon.presentation.LocalBackPress
import com.mihon.presentation.settings.collectAsState

/** Backwards compatible fragment for compose, before we switch entirely to compose navigation */
fun Screen.createComposeView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
): View = ComposeView(inflater.context).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

    setContent {
        val settings = rememberAppSettings()
        val layout by settings.ui.layout.collectAsState()
        val layoutFlag = DeviceLayout.layoutToFlag(LocalContext.current, layout)

        CloudStreamTheme(
            mode = CloudStreamThemeMode.Amoled,
            primaryColor = CloudStreamPrimaryColor.RED,
        ) {
            val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
                "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
            }.onBackPressedDispatcher

            CompositionLocalProvider(
                LocalBackPress provides backDispatcher::onBackPressed,
                DeviceLayout.LocalLayout provides layoutFlag
            ) {
                this@createComposeView.Content()
            }
        }
    }
}
