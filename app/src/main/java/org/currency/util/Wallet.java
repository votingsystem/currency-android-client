package org.currency.util;

import org.currency.App;
import org.currency.android.R;
import org.currency.contentprovider.CurrencyContentProvider;
import org.currency.dto.TagDto;
import org.currency.dto.currency.CurrencyBatchDto;
import org.currency.dto.currency.CurrencyStateDto;
import org.currency.dto.currency.IncomesDto;
import org.currency.model.Currency;
import org.currency.model.CurrencyBundle;
import org.currency.throwable.ValidationException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public static Map<String, Map<String, IncomesDto>> getCurrencyTagMap() {
        Map<String, Map<String, IncomesDto>> result = new HashMap<>();
        for(Currency currency : currencySet) {
            if(result.containsKey(currency.getCurrencyCode())) {
                Map<String, IncomesDto> tagMap = result.get(currency.getCurrencyCode());
                if(tagMap.containsKey(currency.getTag())) {
                    IncomesDto incomesDto = tagMap.get(currency.getTag());
                    incomesDto.addTotal(currency.getAmount());
                    if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                } else {
                    IncomesDto incomesDto = new IncomesDto();
                    incomesDto.addTotal(currency.getAmount());
                    if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                    tagMap.put(currency.getTag(), incomesDto);
                }
            } else {
                Map<String, IncomesDto> tagMap = new HashMap<>();
                IncomesDto incomesDto = new IncomesDto();
                incomesDto.addTotal(currency.getAmount());
                if(currency.isTimeLimited()) incomesDto.addTimeLimited(currency.getAmount());
                tagMap.put(currency.getTag(), incomesDto);
                result.put(currency.getCurrencyCode(), tagMap);
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

    public static BigDecimal getAvailableForTag(String currencyCode, String tag) {
        Map<String, Map<String, IncomesDto>> balancesCashMap = getCurrencyTagMap();
        BigDecimal cash = BigDecimal.ZERO;
        if(balancesCashMap.containsKey(currencyCode)) {
            Map<String, IncomesDto> currencyMap = balancesCashMap.get(currencyCode);
            if(currencyMap.containsKey(TagDto.WILDTAG)) cash = cash.add(
                    currencyMap.get(TagDto.WILDTAG).getTotal());
            if(!TagDto.WILDTAG.equals(tag)) {
                if(currencyMap.containsKey(tag)) cash =
                        cash.add(currencyMap.get(tag).getTotal());
            }
        }
        return cash;
    }

    public static CurrencyBundle getCurrencyBundleForTag(String currencyCode, String tag)
            throws ValidationException {
        CurrencyBundle currencyBundle = new CurrencyBundle(currencyCode, tag);
        for(Currency currency : currencySet) {
            if(currency.getCurrencyCode().equals(currencyCode)) {
                if(tag.equals(currency.getTag()) || TagDto.WILDTAG.equals(currency.getTag())) {
                    currencyBundle.addCurrency(currency);
                }
            }
        }
        return currencyBundle;
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