package org.currency.model;

import org.currency.throwable.ValidationException;
import org.currency.wallet.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyBundle {

    private List<Currency> currencyList = new ArrayList<>();
    private String currencyCode;


    public CurrencyBundle(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public static CurrencyBundle load(Collection<Currency> currencyList) throws Exception {
        CurrencyBundle currencyBundle = null;
        for(Currency currency : currencyList) {
            if(currencyBundle == null)  {
                currencyBundle = new CurrencyBundle(currency.getCurrencyCode());
            }
            currencyBundle.addCurrency(currency);
        }
        return currencyBundle;
    }

    public void addCurrency(Currency currency) throws ValidationException {
        if(!currency.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("CurrencyBundle for currencyCode: " + currencyCode +
                    " - can't have items with currencyCode: " + currency.getCurrencyCode());
        currencyList.add(currency);
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getTotalAmount() {
        return Wallet.getTotalAmount(currencyList);
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

}