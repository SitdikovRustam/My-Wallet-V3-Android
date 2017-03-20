package piuk.blockchain.android.data.datamanagers;

import android.support.annotation.NonNull;
import android.util.Log;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.account.ConsolidatedAccount;
import piuk.blockchain.android.ui.account.ConsolidatedAccount.Type;

public class TransactionListDataManager {

    private static final String TAG = TransactionListDataManager.class.getSimpleName();

    private PayloadManager payloadManager;
    private TransactionListStore transactionListStore;
    private RxBus rxBus;

    public TransactionListDataManager(PayloadManager payloadManager,
                                      TransactionListStore transactionListStore,
                                      RxBus rxBus) {
        this.payloadManager = payloadManager;
        this.transactionListStore = transactionListStore;
        this.rxBus = rxBus;
    }

    public Observable<List<TransactionSummary>> fetchTransactions(Object object, int limit, int offset) {
        return Observable.fromCallable(() -> {
            List<TransactionSummary> result;

            if (object instanceof ConsolidatedAccount) {
                ConsolidatedAccount consolidate = (ConsolidatedAccount) object;
                if (consolidate.getType() == Type.ALL_ACCOUNTS) {
                    result = payloadManager.getAllTransactions(limit, offset);
                } else if (consolidate.getType() == Type.ALL_IMPORTED_ADDRESSES) {
                    result = payloadManager.getImportedAddressesTransactions(limit, offset);
                } else {
                    throw new IllegalArgumentException("ConsolidatedAccount did not have a type set");
                }
            } else if (object instanceof Account) {
                // V3
                result = payloadManager.getAccountTransactions(((Account) object).getXpub(), limit, offset);
            } else {
                throw new IllegalArgumentException("Cannot fetch transactions for object type: " + object.getClass().getSimpleName());
            }

            insertTransactionList(result);

            return transactionListStore.getList();
        }).compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Returns a list of {@link TransactionSummary} objects generated by {@link
     * #getTransactionList()}
     *
     * @return A list of Txs sorted by date.
     */
    @NonNull
    public List<TransactionSummary> getTransactionList() {
        return transactionListStore.getList();
    }

    /**
     * Resets the list of Transactions.
     */
    public void clearTransactionList() {
        transactionListStore.clearList();
    }

    /**
     * Allows insertion of a single new {@link TransactionSummary} into the main transaction list.
     *
     * @param transaction A new, most likely temporary {@link TransactionSummary}
     * @return An updated list of Txs sorted by date
     */
    @NonNull
    public List<TransactionSummary> insertTransactionIntoListAndReturnSorted(TransactionSummary transaction) {
        transactionListStore.insertTransactionIntoListAndSort(transaction);
        return transactionListStore.getList();
    }

    /**
     * Get total BTC balance from an {@link Account} or {@link LegacyAddress}.
     *
     * @param object Either a {@link Account} or a {@link LegacyAddress}
     * @return A BTC value as a long.
     */
    public long getBtcBalance(Object object) {

        long result = 0;

        if (object instanceof ConsolidatedAccount) {
            ConsolidatedAccount consolidate = (ConsolidatedAccount) object;

            if (consolidate.getType() == Type.ALL_ACCOUNTS) {
                result = payloadManager.getWalletBalance().longValue();
            } else if (consolidate.getType() == Type.ALL_IMPORTED_ADDRESSES) {
                result = payloadManager.getImportedAddressesBalance().longValue();
            } else {
                Log.e(TAG, "ConsolidatedAccount did not have a type set");
            }
        } else if (object instanceof Account) {
            // V3
            result = payloadManager.getAddressBalance(((Account) object).getXpub()).longValue();
        } else if (object instanceof LegacyAddress) {
            // V2
            result = payloadManager.getAddressBalance(((LegacyAddress) object).getAddress()).longValue();
        } else {
            Log.e(TAG, "Cannot fetch transactions for object type: " + object.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * Get a specific {@link TransactionSummary} from a hash
     *
     * @param transactionHash The hash of the Tx to be returned
     * @return An Observable object wrapping a Tx. Will call onError if not found with a
     * NullPointerException
     */
    public Observable<TransactionSummary> getTxFromHash(String transactionHash) {
        return Observable.create(emitter -> {
            //noinspection Convert2streamapi
            for (TransactionSummary tx : getTransactionList()) {
                if (tx.getHash().equals(transactionHash)) {
                    if (!emitter.isDisposed()) {
                        emitter.onNext(tx);
                        emitter.onComplete();
                    }
                    return;
                }
            }

            if (!emitter.isDisposed()) emitter.onError(new NullPointerException("Tx not found"));
        });
    }

    /**
     * Update notes for a specific transaction hash and then sync the payload to the server
     *
     * @param transactionHash The hash of the transaction to be updated
     * @param notes           Transaction notes
     * @return If save was successful
     */
    public Observable<Boolean> updateTransactionNotes(String transactionHash, String notes) {
        payloadManager.getPayload().getTxNotes().put(transactionHash, notes);
        return Observable.fromCallable(() -> payloadManager.save())
                .compose(RxUtil.applySchedulersToObservable());
    }

    private void insertTransactionList(List<TransactionSummary> txList) {
        List<TransactionSummary> pendingTxs = getRemainingPendingTransactionList(txList);
        clearTransactionList();
        txList.addAll(pendingTxs);
        transactionListStore.insertTransactions(txList);
        transactionListStore.sort(new TransactionSummary.TxMostRecentDateComparator());
        rxBus.emitEvent(List.class, transactionListStore.getList());
    }

    /**
     * Gets list of transactions that have been published but delivery has not yet been confirmed.
     * @param newlyFetchedTxs
     * @return
     */
    private List<TransactionSummary> getRemainingPendingTransactionList(List<TransactionSummary> newlyFetchedTxs) {
        HashMap<String, TransactionSummary> pendingMap = new HashMap<>();
        for (TransactionSummary transactionSummary : transactionListStore.getList()) {
            if (transactionSummary.isPending()) {
                pendingMap.put(transactionSummary.getHash(), transactionSummary);
            }
        }

        if (!pendingMap.isEmpty()) {
            filterProcessed(newlyFetchedTxs, pendingMap);
        }

        return new ArrayList<>(pendingMap.values());
    }

    private void filterProcessed(List<TransactionSummary> newlyFetchedTxs, HashMap<String, TransactionSummary> pendingMap) {
        for (TransactionSummary tx : newlyFetchedTxs) {
            if (pendingMap.containsKey(tx.getHash())) {
                pendingMap.remove(tx.getHash());
            }
        }
    }

}
