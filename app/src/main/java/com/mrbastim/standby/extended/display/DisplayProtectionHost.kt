package com.mrbastim.standby.extended.display

import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mrbastim.standby.extended.data.settings.StandBySettings

/** Observes real input before children, without consuming it or treating recomposition as activity. */
@Composable
fun DisplayProtectionHost(
    window: Window,
    active: Boolean,
    settings: StandBySettings,
    onHoldChanged: (Boolean) -> Unit = {},
    onRelease: () -> Unit = {},
    content: @Composable (DisplayPhase, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val currentSettings by rememberUpdatedState(settings)
    val hold by rememberUpdatedState(onHoldChanged)
    val release by rememberUpdatedState(onRelease)
    val controller = remember(window, context) {
        DisplayProtectionController(context, window, scope, { hold(it) }, { release() })
    }
    SideEffect { controller.update(settings) }
    DisposableEffect(controller, owner, active) {
        fun sync() {
            controller.update(currentSettings)
            if (active && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                controller.start()
            } else controller.stop()
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        owner.lifecycle.addObserver(observer)
        sync()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }
    val phase by controller.phase.collectAsState()
    val automaticNightMode by controller.automaticNightMode.collectAsState()
    Box(
        Modifier.fillMaxSize()
            .pointerInput(controller, active) {
                if (active) awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.pressed || it.previousPressed }) controller.interact()
                    }
                }
            }
            .onPreviewKeyEvent {
                if (active && it.type == KeyEventType.KeyDown) controller.interact()
                false
            },
    ) { content(phase, automaticNightMode) }
}
