package com.nikitakrapo.monkeybusiness

import com.nikitakrapo.account.AccountManager
import com.nikitakrapo.account.BearerTokensProviderImpl
import com.nikitakrapo.analytics.AnalyticsManager
import com.nikitakrapo.application.PlatformContext
import com.nikitakrapo.monkeybusiness.finances.TransactionAddRouter
import com.nikitakrapo.monkeybusiness.home.HomeDependencies
import com.nikitakrapo.monkeybusiness.profile.auth.AuthDependencies
import com.nikitakrapo.monkeybusiness.profile.edit.ProfileEditDependencies
import com.nikitakrapo.monkeybusiness.profile.edit.ProfileEditRouter

class CoreDependencies(
    val analyticsManager: AnalyticsManager,
    val accountManager: AccountManager,
    val platformContext: PlatformContext,
) {
    fun homeDependencies(
        transactionAddRouter: TransactionAddRouter,
        profileEditRouter: ProfileEditRouter,
    ): HomeDependencies {
        val bearerTokensProvider = BearerTokensProviderImpl(accountManager)
        return HomeDependencies(
            analyticsManager = analyticsManager,
            accountManager = accountManager,
            bearerTokensProvider = bearerTokensProvider,
            transactionAddRouter = transactionAddRouter,
            profileEditRouter = profileEditRouter,
            platformContext = platformContext,
        )
    }

    fun authDependencies() = AuthDependencies(
        accountManager = accountManager,
    )

    fun profileEditDependencies() = ProfileEditDependencies(
        accountManager = accountManager,
    )
}
