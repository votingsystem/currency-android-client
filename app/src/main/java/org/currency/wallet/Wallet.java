package org.currency.wallet;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.contentprovider.CurrencyContentProvider;
import org.currency.dto.currency.CurrencyBatchDto;
import org.currency.dto.currency.CurrencyStateDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.model.Currency;
import org.currency.throwable.ValidationException;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static final String TAG = Wallet.class.getSimpleName();

    private static Set<Currency> currencySet = null;

    public static Set<Currency> getCurrencySet() {
        if(currencySet == null) return null;
        return new HashSet<>(currencySet);
    }

    public static Set<String> getRevocationHashSet() {
        Set<String> result = new HashSet<>();
        for(Currency currency : currencySet) {
            result.add(currency.getRevocationHash());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(char[] pin) throws Exception {
        currencySet = PrefUtils.getWallet(pin, App.getInstance().getToken());
        return new HashSet<>(currencySet);
    }

    public static Currency getCurrency(String revocationHash) throws Exception {
        for(Currency currency : currencySet) {
            if(currency.getRevocationHash().equals(revocationHash)) return currency;
        }
        return null;
    }

    public static void save(Collection<Currency> currencyCollection, char[] pin)
            throws Exception {
        Set<Currency> newCurrencySet = new HashSet<>();
        if(currencySet != null)
            newCurrencySet.addAll(currencySet);
        newCurrencySet.addAll(currencyCollection);
        Wallet.saveWallet(newCurrencySet, pin);
    }

    public static void remove(Collection<Currency> currencyCollection) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getRevocationHash(), currency);
        }
        for(Currency currency : currencyCollection) {
            if(currencyMap.remove(currency.getRevocationHash()) != null) {
                LOGD(TAG +  ".remove", "removed currency: " + currency.getRevocationHash());
                CurrencyContentProvider.updateDatabase(currency);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }


    public static Set<Currency> removeErrors(Collection<CurrencyStateDto> currencyWithErrors)
            throws Exception {
        Set<Currency> removedSet = new HashSet<>();
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(CurrencyStateDto currencyStateDto : currencyWithErrors) {
            Currency removedCurrency = currencyMap.remove(currencyStateDto.getRevocationHash());
            if(removedCurrency != null)  {
                LOGD(TAG +  ".removeErrors", "removed currency: " + currencyStateDto.getRevocationHash());
                CurrencyContentProvider.updateDatabase(removedCurrency.setState(currencyStateDto.getState()));
                removedSet.add(removedCurrency);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
        return removedSet;
    }

    public static Map<String, Currency> getCurrencyMap() {
        Map<String, Currency> currencyMap = new HashMap<>();
        for(Currency currency : currencySet) {
            currencyMap.put(currency.getRevocationHash(), currency);
        }
        return currencyMap;
    }

    public static void updateCurrencyState(Set<String> currencySet, Currency.State state)
            throws Exception {
        Map<String, Currency> currencyMap = getCurrencyMap();
        for(String revocationHash : currencySet) {
            Currency currency = currencyMap.get(revocationHash);
            if(currency != null)  {
                LOGD(TAG + ".updateCurrencyState", "hash: " + revocationHash + " - state:" + state);
                currency.setState(state);
            }
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }

    public static Map<String, BigDecimal> getCurrencyCodeMap() {
        Map<String, BigDecimal> result = new HashMap<>();
        for(Currency currency : currencySet) {
            if(result.containsKey(currency.getCurrencyCode())) {
                BigDecimal total = result.get(currency.getCurrencyCode()).add(currency.getAmount());
                result.put(currency.getCurrencyCode(), total);
            } else {
                result.put(currency.getCurrencyCode(), currency.getAmount());
            }
        }
        return result;
    }

    public static void saveWallet(Collection<Currency> currencyCollection, char[] passw) throws Exception {
        PrefUtils.putWallet(currencyCollection, passw, App.getInstance().getToken());
        currencySet = new HashSet<>(currencyCollection);
    }

    public static void updateWallet(Collection<Currency> currencyCollection) throws Exception {
        Map<String, Currency> currencyMap = new HashMap<String, Currency>();
        for(Currency currency : currencyCollection) {
            currencyMap.put(currency.getRevocationHash(), currency);
        }
        for(Currency currency : currencySet) {
            if(currencyMap.containsKey(currency.getRevocationHash())) throw new ValidationException(
                    App.getInstance().getString(R.string.currency_repeated_wallet_error_msg,
                            currency.getAmount().toString() + " " + currency.getCurrencyCode()));
            else currencyMap.put(currency.getRevocationHash(), currency);
        }
        Wallet.saveWallet(currencyMap.values(), null);
    }

    public static BigDecimal getAvailableForCurrencyCode(String currencyCode) {
        Map<String, BigDecimal> balancesCashMap = getCurrencyCodeMap();
        BigDecimal cash = BigDecimal.ZERO;
        if(balancesCashMap.containsKey(currencyCode)) {
            return balancesCashMap.get(currencyCode);
        }
        return cash;
    }

    public static List<Currency> getCurrencyListByCurrencyCode(String currencyCode)
            throws ValidationException {
        List<Currency> result = new ArrayList<>();
        for(Currency currency : currencySet) {
            if(currency.getCurrencyCode().equals(currencyCode)) {
                result.add(currency);
            }
        }
        return result;
    }

    public static CurrencyBatchDto getCurrencyBatch(TransactionDto transactionDto) throws Exception {
        String toUserIBAN = transactionDto.getToUserIBAN() == null ? null:
                transactionDto.getToUserIBAN().iterator().next();
        return getCurrencyBatch(transactionDto.getOperation(),
                transactionDto.getSubject(), toUserIBAN, transactionDto.getAmount(),
                transactionDto.getCurrencyCode(), transactionDto.getUUID());
    }

    public static CurrencyBatchDto getCurrencyBatch(OperationType operation, String subject,
            String toUserIBAN, BigDecimal batchAmount, String currencyCode, String uuid)
            throws Exception {
        CurrencyBatchDto dto = new CurrencyBatchDto();
        dto.setOperation(operation);
        dto.setSubject(subject);
        dto.setToUserIBAN(toUserIBAN);
        dto.setBatchAmount(batchAmount);
        dto.setCurrencyCode(currencyCode);
        if(uuid == null)
            dto.setBatchUUID(UUID.randomUUID().toString());
        else
            dto.setBatchUUID(uuid);
        Set<String> currencySetSignatures = new HashSet<>();
        Set<Currency> currencySet = new HashSet<>();

        List<Currency> currencyList = getCurrencyListByCurrencyCode(currencyCode);
        Collections.sort(currencyList, currencyComparator);
        BigDecimal availableAmount = getTotalAmount(currencyList);

        BigDecimal bundleAccumulated = BigDecimal.ZERO;
        Currency lastCurrencyAdded = null;
        if(batchAmount.compareTo(availableAmount) < 0) {
            while(bundleAccumulated.compareTo(batchAmount) < 0) {
                lastCurrencyAdded = currencyList.remove(0);
                currencySet.add(lastCurrencyAdded);
                bundleAccumulated = bundleAccumulated.add(lastCurrencyAdded.getAmount());
            }
        } else throw new ValidationException(App.getInstance().getString(R.string.currencyAmountErrorMsg,
                 currencyCode, batchAmount, availableAmount));
        if(bundleAccumulated.compareTo(batchAmount) > 0) {
            BigDecimal leftOverAmount = bundleAccumulated.subtract(batchAmount);
            dto.setLeftOverCurrency(new Currency(leftOverAmount, lastCurrencyAdded.getCurrencyCode(),
                    App.getInstance().getCurrencyService().getEntity().getId()));
        }
        for(Currency currency : currencySet) {
            MetadataDto metadataDto = App.getInstance().getCurrencyService();
            CMSSignedMessage cmsMessage = currency.getCertificationRequest().signData(
                    JSON.writeValueAsBytes(dto),
                    OperationType.TIMESTAMP_REQUEST.getUrl(metadataDto.getFirstTimeStampEntityId()));
            currency.setCMS(cmsMessage);
            currencySetSignatures.add(currency.getCMS().toPEMStr());
        }
        dto.setCurrencySet(currencySetSignatures);
        dto.setCurrencyCollection(currencySet);
        return dto;
    }


    public static BigDecimal getTotalAmount(Collection<Currency> currencyCollection) {
        BigDecimal result = BigDecimal.ZERO;
        for(Currency currency : currencyCollection) {
            result = result.add(currency.getAmount());
        }
        return result;
    }

    public BigDecimal getTotalAmountByCorrencyCode(String currencyCode) throws ValidationException {
        return getTotalAmount(getCurrencyListByCurrencyCode(currencyCode));
    }

    /**
     * Method that remove expended currencies and add the leftover of the operation
     */
    public static void updateWallet(CurrencyBatchDto currencyBatchDto) throws Exception {
        for(Currency currency : currencyBatchDto.getCurrencyCollection()) {
            currency.setState(Currency.State.EXPENDED);
        }
        Wallet.remove(currencyBatchDto.getCurrencyCollection());
        if(currencyBatchDto.getLeftOverCurrency() != null) Wallet.updateWallet(
                Arrays.asList(currencyBatchDto.getLeftOverCurrency()));
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

}