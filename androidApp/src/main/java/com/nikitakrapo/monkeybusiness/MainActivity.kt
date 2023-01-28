package com.nikitakrapo.monkeybusiness

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.analytics.FirebaseAnalytics
import com.nikitakrapo.account.AccountManagerImpl
import com.nikitakrapo.analytics.FirebaseAnalyticsManager
import com.nikitakrapo.application.PlatformContext
import com.nikitakrapo.monkeybusiness.ClipboardCopyManager.copyToClipboard
import com.nikitakrapo.monkeybusiness.design.theme.MonkeyTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private lateinit var mainActivityComponent: MainActivityComponent
    private lateinit var coreComponent: CoreComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val analyticsManager = FirebaseAnalyticsManager(FirebaseAnalytics.getInstance(this))
        mainActivityComponent = MainActivityComponent(
            analyticsManager = analyticsManager,
            accountManager = AccountManagerImpl(analyticsManager),
        )

        val componentContext = defaultComponentContext()

        coreComponent = CoreComponentImpl(
            componentContext = componentContext,
            dependencies = CoreDependencies(
                analyticsManager = mainActivityComponent.analyticsManager,
                accountManager = mainActivityComponent.accountManager,
                platformContext = PlatformContext(context = applicationContext),
            )
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val systemUiController = rememberSystemUiController()
            val useDarkIcons = !isSystemInDarkTheme()

            LaunchedEffect(systemUiController, useDarkIcons) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = useDarkIcons,
                    isNavigationBarContrastEnforced = false
                )
            }

            MonkeyTheme {
                Surface {
                    ConstraintLayout(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val (coreScreen, debugButton) = createRefs()

                        CoreScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .constrainAs(coreScreen) {
                                    centerTo(parent)
                                },
                            component = coreComponent
                        )

                        // TODO: make testing abstraction
                        if (BuildConfig.DEBUG) {
                            DebugButton(
                                modifier = Modifier
                                    .constrainAs(debugButton) {
                                        centerVerticallyTo(parent)
                                        end.linkTo(parent.end)
                                    },
                                onUuidClick = {
                                    lifecycleScope.launch {
                                        val token = mainActivityComponent.accountManager.getToken()
                                            .getOrNull()
                                        this@MainActivity.copyToClipboard("UUID", token.toString())
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
