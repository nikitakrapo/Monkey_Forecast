package com.nikitakrapo.monkeybusiness.finances.accounts.opening

import com.arkivanov.decompose.ComponentContext
import com.nikitakrapo.decompose.coroutines.coroutineScope
import com.nikitakrapo.monkeybusiness.finance.models.Currency
import com.nikitakrapo.monkeybusiness.finances.accounts.opening.BankAccountOpeningComponent.State
import com.nikitakrapo.mvi.feature.FeatureFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class BankAccountOpeningComponentImpl(
    componentContext: ComponentContext,
    featureFactory: FeatureFactory = FeatureFactory(),
    private val closeComponent: () -> Unit,
    dependencies: BankAccountOpeningDependencies,
) : BankAccountOpeningComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)

    private val repository = dependencies.bankAccountsRepository

    private val feature = featureFactory.create<Intent, Intent, Effect, State, Event>(
        name = "Bank Account Opening",
        initialState = State(
            currencyList = Currency.values().toList().sortedBy { it.code },
            selectedCurrency = null,
            query = "",
            isLoading = false,
            isSearchOpened = false,
        ),
        intentToAction = { it },
        reducer = { effect ->
            when (effect) {
                is Effect.CurrencySelected -> {
                    val toSelect = currencyList[effect.index]
                    val newSelected = if (toSelect == selectedCurrency) null else toSelect
                    copy(selectedCurrency = newSelected)
                }

                Effect.ScreenClosed -> copy()
            }
        },
        actor = { action, state ->
            when (action) {
                is Intent.SelectCurrency -> flowOf(Effect.CurrencySelected(action.index))
                Intent.GoBack -> flowOf(Effect.ScreenClosed)
                Intent.Proceed -> TODO()
            }
        },
        eventsPublisher = { _, effect, _ ->
            if (effect is Effect.ScreenClosed) Event.CloseScreen else null
        }
    )

    init {
        scope.launch {
            feature.events.collect {
                when (it) {
                    Event.CloseScreen -> closeComponent()
                }
            }
        }
    }

    override val state: StateFlow<State> get() = feature.state

    override fun onCurrencySelected(index: Int) {
        feature.accept(Intent.SelectCurrency(index = index))
    }

    override fun onBackClicked() {
        feature.accept(Intent.GoBack)
    }

    override fun onProceedClicked() {
        feature.accept(Intent.Proceed)
    }

    private sealed class Intent {
        class SelectCurrency(val index: Int) : Intent()
        object Proceed : Intent()
        object GoBack : Intent()
    }

    private sealed class Effect {
        class CurrencySelected(val index: Int) : Effect()
        object ScreenClosed : Effect()
    }

    private sealed class Event {
        object CloseScreen : Event()
    }
}