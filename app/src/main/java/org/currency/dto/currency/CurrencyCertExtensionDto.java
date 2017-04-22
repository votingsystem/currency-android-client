package org.currency.dto.currency;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCertExtensionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currencyServerURL;
    private String revocationHash;
    private String currencyCode;
    private BigDecimal amount;

    public CurrencyCertExtensionDto() {}

    public CurrencyCertExtensionDto(BigDecimal amount, String currencyCode, String revocationHash,
                String currencyServerURL) {
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.revocationHash = revocationHash;
        this.currencyServerURL = currencyServerURL;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}