package com.nikitakrapo.monkeybusiness.finances.transactions

import com.arkivanov.decompose.ComponentContext
import com.nikitakrapo.decompose.coroutines.coroutineScope
import com.nikitakrapo.monkeybusiness.finance.models.Currency
import com.nikitakrapo.monkeybusiness.finance.models.MoneyAmount
import com.nikitakrapo.monkeybusiness.finance.models.Transaction
import com.nikitakrapo.monkeybusiness.finances.transactions.TransactionAddComponent.AmountError
import com.nikitakrapo.monkeybusiness.finances.transactions.TransactionAddComponent.NameError
import com.nikitakrapo.monkeybusiness.finances.transactions.TransactionAddComponent.State
import com.nikitakrapo.monkeybusiness.finances.transactions.TransactionAddComponent.TransactionType
import com.nikitakrapo.mvi.feature.FeatureFactory
import com.nikitakrapo.randomUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class TransactionAddComponentImpl(
    componentContext: ComponentContext,
    private val dependencies: TransactionAddDependencies,
    private val closeTransactionAdd: () -> Unit,
    featureFactory: FeatureFactory = FeatureFactory(),
) : TransactionAddComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)

    private val transactionsRepository get() = dependencies.transactionsRepository

    private val feature = featureFactory.create<Intent, Intent, Effect, State, Event>(
        name = "TransactionAddFeature",
        initialState = State(
            nameText = "",
            nameError = null,
            selectedTransactionType = TransactionType.Default,
            amountText = "",
            amountError = null,
            selectedCurrency = Currency.USD,
            isLoading = false,
            error = null,
        ),
        intentToAction = { it },
        reducer = { effect ->
            when (effect) {
                is Effect.CurrencySelected -> copy(
                    selectedCurrency = effect.currency,
                    error = null
                )
                is Effect.MoneyAmountTextChanged -> copy(
                    amountText = effect.text,
                    amountError = null,
                    error = null
                )
                is Effect.NameTextChanged -> copy(
                    nameText = effect.text,
                    nameError = null,
                    error = null
                )
                is Effect.TransactionTypeSelected -> copy(
                    selectedTransactionType = effect.type,
                    error = null
                )
                is Effect.AddingStarted -> copy(
                    isLoading = true,
                    error = null
                )
                is Effect.AddingFinished -> copy(
                    isLoading = false,
                    error = effect.result.exceptionOrNull()?.message
                )
                is Effect.ReceivedInputError -> copy(
                    nameError = effect.nameError,
                    amountError = effect.amountError,
                )
            }
        },
        actor = { action, state ->
            when (action) {
                is Intent.ChangeMoneyAmountText -> flowOf(Effect.MoneyAmountTextChanged(action.text))
                is Intent.ChangeNameText -> flowOf(Effect.NameTextChanged(action.text))
                is Intent.SelectCurrency -> flowOf(Effect.CurrencySelected(action.currency))
                is Intent.SelectTransactionType -> flowOf(Effect.TransactionTypeSelected(action.type))
                Intent.AddTransaction -> flow {
                    checkForInputErrors(state)?.let { inputErrors ->
                        emit(inputErrors)
                        return@flow
                    }
                    emit(Effect.AddingStarted)
                    val result = runCatching {
                        val isCredit = state.selectedTransactionType == TransactionType.Credit
                        var amount = state.amountText.toDouble()
                        if (isCredit) amount *= -1
                        val moneyAmount = MoneyAmount(
                            amount = amount,
                            currency = state.selectedCurrency
                        )
                        val transaction = Transaction(
                            id = randomUuid(),
                            name = state.nameText.trim(),
                            moneyAmount = moneyAmount,
                            timestampMs = Clock.System.now().toEpochMilliseconds()
                        )
                        transactionsRepository.addTransaction(transaction)
                    }
                    emit(Effect.AddingFinished(result))
                }
            }
        },
        eventsPublisher = { action, effect, state ->
            if (effect is Effect.AddingFinished && effect.result.isSuccess) {
                Event.AddingFinishedSuccessfully
            } else {
                null
            }
        }
    )

    init {
        scope.launch {
            feature.events.collect {
                when (it) {
                    Event.AddingFinishedSuccessfully -> closeTransactionAdd()
                }
            }
        }
    }

    override val state: StateFlow<State> get() = feature.state

    override fun onNameTextChanged(text: String) {
        feature.accept(Intent.ChangeNameText(text))
    }

    override fun onTransactionTypeSelected(type: TransactionType) {
        feature.accept(Intent.SelectTransactionType(type))
    }

    override fun onAmountTextChanged(text: String) {
        feature.accept(Intent.ChangeMoneyAmountText(text))
    }

    override fun onCurrencySelected(currency: Currency) {
        feature.accept(Intent.SelectCurrency(currency))
    }

    override fun onBackClicked() {
        closeTransactionAdd()
    }

    override fun onAddClicked() {
        feature.accept(Intent.AddTransaction)
    }

    private sealed class Intent {
        class ChangeNameText(val text: String) : Intent()
        class SelectTransactionType(val type: TransactionType) : Intent()
        class ChangeMoneyAmountText(val text: String) : Intent()
        class SelectCurrency(val currency: Currency) : Intent()
        object AddTransaction : Intent()
    }

    private sealed class Effect {
        class NameTextChanged(val text: String) : Effect()
        class TransactionTypeSelected(val type: TransactionType) : Effect()
        class MoneyAmountTextChanged(val text: String) : Effect()
        class CurrencySelected(val currency: Currency) : Effect()
        class ReceivedInputError(
            val nameError: NameError?,
            val amountError: AmountError?,
        ) : Effect()

        object AddingStarted : Effect()
        class AddingFinished(val result: Result<Unit>) : Effect()
    }

    private sealed class Event {
        object AddingFinishedSuccessfully : Event()
    }

    private fun checkForInputErrors(state: State): Effect.ReceivedInputError? {
        val nameError = when {
            state.nameText.trim().isEmpty() -> NameError.Empty
            else -> null
        }
        val amountError = when {
            state.amountText.trim().isEmpty() -> AmountError.Empty
            !state.amountText.isDouble -> AmountError.Incorrect
            else -> null
        }
        if (nameError == null && amountError == null) return null
        return Effect.ReceivedInputError(
            nameError = nameError,
            amountError = amountError,
        )
    }

    private val String.isDouble get() =
        Regex("^(-?)(0|([1-9]\\d*))(\\.\\d+)?$").matches(this)
}