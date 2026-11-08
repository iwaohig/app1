package dev.moonlightfield.talktimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.ambient.AmbientLifecycleObserver
import dev.moonlightfield.talktimer.ui.AboutScreen
import dev.moonlightfield.talktimer.ui.SettingsScreen
import dev.moonlightfield.talktimer.ui.TimerScreen

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_START = "start"
    }

    private var ambient by mutableStateOf(false)
    private var showAbout by mutableStateOf(false)
    private var showSettings by mutableStateOf(false)
    /** Bumped once a minute in ambient mode so the low-power screen redraws. */
    private var ambientTick by mutableIntStateOf(0)

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                ambient = true
            }

            override fun onExitAmbient() {
                ambient = false
            }

            override fun onUpdateAmbient() {
                ambientTick++
            }
        },
    )

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Show the ongoing activity right away if the timer is already running.
            if (granted) {
                val s = TimerStore.get(this)
                if (s.status != Status.IDLE) OngoingNotifier.show(this, s)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)

        setContent {
            val state by TimerStore.state(this).collectAsStateWithLifecycle()
            val prefs by Settings.state(this).collectAsStateWithLifecycle()
            val holdScreen = TimerLogic.shouldKeepScreenOn(state.status, prefs.keepScreenOn)
            LaunchedEffect(holdScreen) {
                if (holdScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            if (showAbout && !ambient) {
                BackHandler { showAbout = false }
                AboutScreen(
                    versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "",
                    onOpenPolicy = { Links.openOnPhone(this, Links.PRIVACY_POLICY_URL) },
                    onClose = { showAbout = false },
                )
                return@setContent
            }
            if (showSettings && !ambient) {
                BackHandler { showSettings = false }
                SettingsScreen(
                    prefs = prefs,
                    onToggleKeepScreenOn = { Settings.update(this) { it.copy(keepScreenOn = !it.keepScreenOn) } },
                    onCycleVibration = {
                        Settings.update(this) { it.copy(vibration = it.vibration.next()) }
                        // Let the user feel the new strength right away.
                        Haptics.play(this, TimerLogic.Pattern.SHORT_2, Settings.get(this).vibration)
                    },
                    onToggleLongPress = { Settings.update(this) { it.copy(longPressToPause = !it.longPressToPause) } },
                    onClose = { showSettings = false },
                )
                return@setContent
            }
            TimerScreen(
                state = state,
                ambient = ambient,
                ambientTick = ambientTick,
                onToggle = { TimerController.toggle(this) },
                onReset = { TimerController.reset(this) },
                onChangeMinutes = { delta -> TimerController.changeMinutes(this, delta) },
                onInfo = { showAbout = true },
                onSettings = { showSettings = true },
                longPressToPause = prefs.longPressToPause,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** The tile's "Start" button opens the app with this extra. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_ACTION) == ACTION_START &&
            TimerStore.get(this).status == Status.IDLE
        ) {
            TimerController.start(this)
        }
        intent?.removeExtra(EXTRA_ACTION)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
