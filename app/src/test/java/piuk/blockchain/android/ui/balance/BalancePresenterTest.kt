package piuk.blockchain.android.ui.balance

import com.nhaarman.mockito_kotlin.*
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should equal to`
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.access.AuthEvent
import piuk.blockchain.android.data.contacts.ContactsDataManager
import piuk.blockchain.android.data.contacts.models.ContactTransactionModel
import piuk.blockchain.android.data.contacts.models.ContactsEvent
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.transactions.Displayable
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.base.UiState
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.ExchangeRateFactory
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import java.math.BigInteger

class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var view: BalanceView = mock()
    private var exchangeRateFactory: ExchangeRateFactory = mock()
    private var transactionListDataManager: TransactionListDataManager = mock()
    private var contactsDataManager: ContactsDataManager = mock()
    private var swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var payloadDataManager: PayloadDataManager = mock()
    private var buyDataManager: BuyDataManager = mock()
    private var stringUtils: StringUtils = mock()
    private var prefsUtil: PrefsUtil = mock()
    private var accessState: AccessState = mock()
    private var currencyState: CurrencyState = mock()
    private var rxBus: RxBus = mock()
    private var ethDataManager: EthDataManager = mock()

    @Before
    fun setUp() {

        subject = BalancePresenter(
                exchangeRateFactory,
                transactionListDataManager,
                contactsDataManager,
                ethDataManager,
                swipeToReceiveHelper,
                payloadDataManager,
                buyDataManager,
                stringUtils,
                prefsUtil,
                rxBus,
                currencyState
        )
        subject.initView(view)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReady() {
        // This *could* be tested but would be absolutely enormous, and most of it's child functions
        // have been tested elsewhere in this class.
    }

    @Test
    @Throws(Exception::class)
    fun onViewDestroyed() {
        // Arrange
        val contactsEventObservable = Observable.just(ContactsEvent.INIT)
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.contactsEventObservable = contactsEventObservable
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable
        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(ContactsEvent::class.java, contactsEventObservable)
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

    @Test
    @Throws(Exception::class)
    fun onResume() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.chosenAccount = itemAccount
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2717.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(318.0)
        // Act
        subject.onResume()
        // Assert
        verify(currencyState, times(2)).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verify(exchangeRateFactory).getLastEthPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(view).onViewTypeChanged(true, 0)
        verify(view).onExchangeRateUpdated(2717.0, 318.0, true)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen success update ui with content state`() {
        // Arrange
        val itemAccount = ItemAccount()
        val transactionSummary: Displayable = mock()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2717.0)
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(currencyState).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.LOADING)
        verify(view).setUiState(UiState.CONTENT)
        verify(view).onTransactionsUpdated(listOf(transactionSummary))
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen success empty account update ui with empty state`() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(emptyList()))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2717.0)
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(currencyState).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.LOADING)
        verify(view).setUiState(UiState.EMPTY)
        verify(view).onTransactionsUpdated(emptyList())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen failure`() {
        // Arrange
        val itemAccount = ItemAccount()
        subject.activeAccountAndAddressList.add(itemAccount)
        whenever(payloadDataManager.updateAllBalances())
                .thenReturn(Completable.error { Throwable() })
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(emptyList()))
        // Act
        subject.onAccountChosen(0)
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(view).setUiState(UiState.LOADING)
        verify(view).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onRefreshRequested failure`() {
        val itemAccount = ItemAccount()
        val transactionSummary: Displayable = mock()
        subject.chosenAccount = itemAccount
        whenever(payloadDataManager.updateAllBalances())
                .thenReturn(Completable.error { Throwable() })
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(contactsDataManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).isContactsEnabled
        verify(view).setUiState(UiState.FAILURE)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onRefreshRequested contacts not enabled`() {
        val itemAccount = ItemAccount()
        val transactionSummary: Displayable = mock()
        subject.chosenAccount = itemAccount
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2717.0)
        whenever(contactsDataManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests()).thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions()).thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(currencyState).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(view).isContactsEnabled
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view).setUiState(UiState.CONTENT)
        verify(view, times(2)).onTransactionsUpdated(emptyList())
        verify(view).onContactsHashMapUpdated(any())
        verify(view).showFctxRequiringAttention(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onRefreshRequested contacts enabled`() {
        val itemAccount = ItemAccount()
        val transactionSummary: Displayable = mock()
        subject.chosenAccount = itemAccount
        val contactName = "CONTACT_NAME"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
        }
        val transactionModel = ContactTransactionModel(contactName, fctx)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.getBtcBalance(itemAccount)).thenReturn(0L)
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
                .thenReturn(Observable.just(listOf(transactionSummary)))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)).thenReturn(true)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2717.0)
        whenever(contactsDataManager.fetchContacts()).thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.just(transactionModel))
        whenever(contactsDataManager.getTransactionDisplayMap()).thenReturn(HashMap())
        whenever(stringUtils.getString(R.string.contacts_pending_transaction)).thenReturn("")
        whenever(stringUtils.getString(R.string.contacts_transaction_history)).thenReturn("")
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.onRefreshRequested()
        // Assert
        verify(payloadDataManager).updateAllBalances()
        verifyNoMoreInteractions(payloadDataManager)
        verify(transactionListDataManager).getBtcBalance(itemAccount)
        verify(transactionListDataManager).fetchTransactions(itemAccount, 50, 0)
        verifyNoMoreInteractions(transactionListDataManager)
        verify(currencyState).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(stringUtils).getString(R.string.contacts_pending_transaction)
        verify(stringUtils).getString(R.string.contacts_transaction_history)
        verifyNoMoreInteractions(stringUtils)
        verify(view).isContactsEnabled
        verify(view).onTotalBalanceUpdated("0.0 BTC")
        verify(view, times(2)).setUiState(UiState.CONTENT)
        verify(view, times(2)).onTransactionsUpdated(any())
        verify(view).onContactsHashMapUpdated(HashMap())
        verify(view).showFctxRequiringAttention(1)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun setViewType() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(0.0)
        // Act
        subject.setViewType(true)
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(3)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(currencyState).isDisplayingCryptoCurrency = true
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(true, 0)
        verify(view).onTotalBalanceUpdated("0.0 BTC")
    }

    @Test
    @Throws(Exception::class)
    fun invertViewType() {
        // Arrange
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
                .thenReturn(0)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(0.0)
        // Act
        subject.invertViewType()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(2)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        verify(exchangeRateFactory).getLastBtcPrice("USD")
        verifyNoMoreInteractions(exchangeRateFactory)
        verify(currencyState).isDisplayingCryptoCurrency
        verify(currencyState).isDisplayingCryptoCurrency = false
        verifyNoMoreInteractions(accessState)
        verify(view).onViewTypeChanged(false, 0)
        verify(view).onTotalBalanceUpdated("0.00 USD")
    }

    @Test
    @Throws(Exception::class)
    fun areLauncherShortcutsEnabled() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true))
                .thenReturn(false)
        // Act
        val result = subject.areLauncherShortcutsEnabled()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
        verifyNoMoreInteractions(prefsUtil)
        result `should equal to` false
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked contact not found`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(R.string.contacts_not_found_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked transaction not found`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val contact = Contact()
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for address & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showWaitingForAddressDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for payment & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply { this.facilitatedTransactions = facilitatedTransactions }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showWaitingForPaymentDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for address & receiver, only one account`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val amount = 1000L
        val txNote = "NOTE"
        val name = "NAME"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
            intendedAmount = amount
            note = txNote
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.name = name
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        val account = Account().apply { label = "" }
        whenever(payloadDataManager.accounts).thenReturn(listOf(account))
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(0.0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showSendAddressDialog(fctxId, "0.00001 BTC", name, txNote)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for address & receiver, multiple accounts`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val amount = 1000L
        val txNote = "NOTE"
        val name = "NAME"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_RECEIVER
            intendedAmount = amount
            note = txNote
        }
        facilitatedTransactions.put(fctxId, fctx)
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.name = name
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        val account = Account().apply { label = "" }
        whenever(payloadDataManager.accounts).thenReturn(listOf(account, account))
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(0.0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showAccountChoiceDialog(listOf("", ""), fctxId, "0.00001 BTC", name, txNote)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for payment & rpr initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val txId = "TX_ID"
        val bitcoinUri = "BITCOIN_URI"
        val fctx: FacilitatedTransaction = mock()
        whenever(fctx.state).thenReturn(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
        whenever(fctx.role).thenReturn(FacilitatedTransaction.ROLE_RPR_INITIATOR)
        whenever(fctx.id).thenReturn(txId)
        whenever(fctx.toBitcoinURI()).thenReturn(bitcoinUri)
        facilitatedTransactions.put(fctxId, fctx)
        val mdid = "MDID"
        val id = "ID"
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
            this.id = id
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).initiatePayment(bitcoinUri, id, mdid, txId)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionClicked waiting for payment & pr receiver`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val facilitatedTransactions = HashMap<String, FacilitatedTransaction>()
        val txId = "TX_ID"
        val note = "NOTE"
        val fctx: FacilitatedTransaction = mock()
        whenever(fctx.state).thenReturn(FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT)
        whenever(fctx.role).thenReturn(FacilitatedTransaction.ROLE_PR_RECEIVER)
        whenever(fctx.id).thenReturn(txId)
        whenever(fctx.intendedAmount).thenReturn(1000L)
        whenever(fctx.note).thenReturn(note)
        facilitatedTransactions.put(fctxId, fctx)
        val mdid = "MDID"
        val id = "ID"
        val name = "NAME"
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
            this.id = id
            this.name = name
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(0.0)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        // Act
        subject.onPendingTransactionClicked(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showPayOrDeclineDialog(fctxId, "0.00001 BTC", name, note)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionLongClicked waiting for address & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_ADDRESS
            role = FacilitatedTransaction.ROLE_RPR_INITIATOR
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.getFacilitatedTransactions())
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).getFacilitatedTransactions()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionCancelDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onPendingTransactionLongClicked waiting for payment & initiator`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val fctx = FacilitatedTransaction().apply {
            state = FacilitatedTransaction.STATE_WAITING_FOR_PAYMENT
            role = FacilitatedTransaction.ROLE_PR_INITIATOR
            id = fctxId
        }
        val transactionModel = ContactTransactionModel("Contact name", fctx)
        val facilitatedTransactions = mutableListOf<ContactTransactionModel>().apply {
            add(transactionModel)
        }
        whenever(contactsDataManager.getFacilitatedTransactions())
                .thenReturn(Observable.fromIterable(facilitatedTransactions))
        // Act
        subject.onPendingTransactionLongClicked(fctxId)
        // Assert
        verify(contactsDataManager).getFacilitatedTransactions()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showTransactionCancelDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun declineTransaction() {
        // Arrange
        val fctxId = "FCTX_ID"
        // Act
        subject.declineTransaction(fctxId)
        // Assert
        verify(view).showTransactionDeclineDialog(fctxId)
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen for payment contact not found`() {
        // Arrange
        val accountPosition = 0
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(R.string.contacts_transaction_not_found_error, ToastCustom.TYPE_ERROR)
        verify(view).showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen for payment successful`() {
        // Arrange
        val accountPosition = 0
        val correctedPosition = 0
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val intendedAmount = 100L
        val address = "ADDRESS"
        val fctx = FacilitatedTransaction().apply {
            id = fctxId
            this.intendedAmount = intendedAmount
        }
        val facilitatedTransactions =
                HashMap<String, FacilitatedTransaction>().apply { put(fctxId, fctx) }
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.empty())
        whenever(payloadDataManager.getPositionOfAccountInActiveList(accountPosition))
                .thenReturn(correctedPosition)
        whenever(payloadDataManager.getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )).thenReturn(Observable.just(address))
        whenever(contactsDataManager.sendPaymentRequestResponse(
                eq(mdid),
                any(),
                eq(fctxId))
        ).thenReturn(Completable.complete())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verify(contactsDataManager).sendPaymentRequestResponse(eq(mdid), any(), eq(fctxId))
        verifyNoMoreInteractions(contactsDataManager)
        verify(payloadDataManager).getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )
        verify(payloadDataManager).getPositionOfAccountInActiveList(accountPosition)
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).isContactsEnabled
        verify(view).showProgressDialog()
        verify(view).showToast(R.string.contacts_address_sent_success, ToastCustom.TYPE_OK)
        verify(view).dismissProgressDialog()
        // There'll be more interactions here as the transactions are refreshed
    }

    @Test
    @Throws(Exception::class)
    fun `onAccountChosen for payment failed`() {
        // Arrange
        val accountPosition = 0
        val correctedPosition = 0
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val intendedAmount = 100L
        val address = "ADDRESS"
        val fctx = FacilitatedTransaction().apply {
            id = fctxId
            this.intendedAmount = intendedAmount
        }
        val facilitatedTransactions =
                HashMap<String, FacilitatedTransaction>().apply { put(fctxId, fctx) }
        val contact = Contact().apply {
            this.facilitatedTransactions = facilitatedTransactions
            this.mdid = mdid
        }
        whenever(contactsDataManager.getContactFromFctxId(fctxId)).thenReturn(Single.just(contact))
        whenever(payloadDataManager.getPositionOfAccountInActiveList(accountPosition))
                .thenReturn(correctedPosition)
        whenever(payloadDataManager.getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )).thenReturn(Observable.just(address))
        whenever(contactsDataManager.sendPaymentRequestResponse(
                eq(mdid),
                any(),
                eq(fctxId))
        ).thenReturn(Completable.error { Throwable() })
        // Act
        subject.onAccountChosen(accountPosition, fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentRequestResponse(eq(mdid), any(), eq(fctxId))
        verifyNoMoreInteractions(contactsDataManager)
        verify(payloadDataManager).getNextReceiveAddressAndReserve(
                correctedPosition,
                "Payment request $fctxId"
        )
        verify(payloadDataManager).getPositionOfAccountInActiveList(accountPosition)
        verifyNoMoreInteractions(payloadDataManager)
        verify(view).showProgressDialog()
        verify(view).showToast(R.string.contacts_address_sent_failed, ToastCustom.TYPE_ERROR)
        verify(view).dismissProgressDialog()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `confirmDeclineTransaction successful`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.just(contact))
        whenever(contactsDataManager.sendPaymentDeclinedResponse(mdid, fctxId))
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.confirmDeclineTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentDeclinedResponse(mdid, fctxId)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).isContactsEnabled
        verify(view).showToast(
                R.string.contacts_pending_transaction_decline_success,
                ToastCustom.TYPE_OK
        )
        verify(view).showFctxRequiringAttention(any())
        verify(view).onContactsHashMapUpdated(any())
        verify(view).onTransactionsUpdated(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `confirmDeclineTransaction failed`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.confirmDeclineTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager, times(2)).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(
                R.string.contacts_pending_transaction_decline_failure,
                ToastCustom.TYPE_ERROR
        )
        verify(view).isContactsEnabled
        verify(view).showFctxRequiringAttention(any())
        verify(view).onContactsHashMapUpdated(any())
        verify(view).onTransactionsUpdated(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `confirmCancelTransaction successful`() {
        // Arrange
        val fctxId = "FCTX_ID"
        val mdid = "MDID"
        val contact = Contact().apply { this.mdid = mdid }
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.just(contact))
        whenever(contactsDataManager.sendPaymentCancelledResponse(mdid, fctxId))
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.confirmCancelTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager).sendPaymentCancelledResponse(mdid, fctxId)
        verify(contactsDataManager).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(
                R.string.contacts_pending_transaction_cancel_success,
                ToastCustom.TYPE_OK
        )
        verify(view).isContactsEnabled
        verify(view).showFctxRequiringAttention(any())
        verify(view).onContactsHashMapUpdated(any())
        verify(view).onTransactionsUpdated(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `confirmCancelTransaction failed`() {
        // Arrange
        val fctxId = "FCTX_ID"
        whenever(contactsDataManager.getContactFromFctxId(fctxId))
                .thenReturn(Single.error { Throwable() })
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.fetchContacts())
                .thenReturn(Completable.complete())
        whenever(contactsDataManager.getContactsWithUnreadPaymentRequests())
                .thenReturn(Observable.empty())
        whenever(contactsDataManager.refreshFacilitatedTransactions())
                .thenReturn(Observable.empty())
        whenever(view.isContactsEnabled).thenReturn(true)
        // Act
        subject.confirmCancelTransaction(fctxId)
        // Assert
        verify(contactsDataManager).getContactFromFctxId(fctxId)
        verify(contactsDataManager, times(2)).fetchContacts()
        verify(contactsDataManager).getContactsWithUnreadPaymentRequests()
        verify(contactsDataManager).refreshFacilitatedTransactions()
        verify(contactsDataManager).getTransactionDisplayMap()
        verifyNoMoreInteractions(contactsDataManager)
        verify(view).showToast(
                R.string.contacts_pending_transaction_cancel_failure,
                ToastCustom.TYPE_ERROR
        )
        verify(view).isContactsEnabled
        verify(view).showFctxRequiringAttention(any())
        verify(view).onContactsHashMapUpdated(any())
        verify(view).onTransactionsUpdated(any())
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `getBitcoinClicked API less than 19`() {
        // Arrange
        whenever(view.shouldShowBuy).thenReturn(false)
        // Act
        subject.getBitcoinClicked()
        // Assert
        verify(view).startReceiveFragment()
        verify(view).shouldShowBuy
        verifyNoMoreInteractions(view)
        verifyZeroInteractions(buyDataManager)
    }

    @Test
    @Throws(Exception::class)
    fun `getBitcoinClicked canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        whenever(view.shouldShowBuy).thenReturn(true)
        // Act
        subject.getBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startBuyActivity()
        verify(view).shouldShowBuy
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `getBitcoinClicked canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        whenever(view.shouldShowBuy).thenReturn(true)
        // Act
        subject.getBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragment()
        verify(view).shouldShowBuy
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun getAllDisplayableAccounts() {
        // Arrange
        val legacyAddrArchived = LegacyAddress().apply { tag = LegacyAddress.ARCHIVED_ADDRESS }
        val legacyAddr = LegacyAddress().apply { tag = LegacyAddress.NORMAL_ADDRESS }
        val legacyAddresses = listOf(legacyAddrArchived, legacyAddr)
        whenever(payloadDataManager.legacyAddresses).thenReturn(legacyAddresses)
        val xPub = "X_PUB"
        val label = "LABEL"
        val accountArchived = Account().apply { isArchived = true }
        val account1 = Account().apply {
            xpub = xPub
            this.label = label
        }
        val account2 = Account().apply {
            xpub = xPub
            this.label = label
        }
        val accounts = listOf(accountArchived, account1, account2)
        whenever(payloadDataManager.accounts).thenReturn(accounts)
        whenever(payloadDataManager.getAddressBalance(xPub)).thenReturn(BigInteger.TEN)
        whenever(payloadDataManager.walletBalance).thenReturn(BigInteger.valueOf(1_000_000L))
        whenever(payloadDataManager.importedAddressesBalance)
                .thenReturn(BigInteger.valueOf(1_000_000L))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(0)
        // Act
        val result = subject.getAllDisplayableAccounts()
        // Assert
        verify(payloadDataManager).legacyAddresses
        verify(payloadDataManager).accounts
        verify(payloadDataManager, times(2)).getAddressBalance(xPub)
        verify(payloadDataManager).walletBalance
        verify(payloadDataManager).importedAddressesBalance
        verifyNoMoreInteractions(payloadDataManager)
        verify(currencyState, times(5)).isDisplayingCryptoCurrency
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil, times(5)).getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        verify(prefsUtil, times(5)).getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)
        verifyNoMoreInteractions(prefsUtil)
        // 3 accounts, "All", "Imported" and "Ethereum"
        result.size `should equal to` 5
    }

}
