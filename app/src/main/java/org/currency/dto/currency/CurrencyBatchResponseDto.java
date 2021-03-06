package org.currency.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.currency.cms.CMSSignedMessage;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchResponseDto {

    private String leftOverCert;
    private String currencyChangeCert;
    private String receipt;
    private String message;

    public CurrencyBatchResponseDto() {};

    public CurrencyBatchResponseDto(CMSSignedMessage receipt, String leftOverCert) throws Exception {
        this.receipt = receipt.toPEMStr();
        this.leftOverCert = leftOverCert;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public String getLeftOverCert() {
        return leftOverCert;
    }

    public void setLeftOverCert(String leftOverCert) {
        this.leftOverCert = leftOverCert;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCurrencyChangeCert() {
        return currencyChangeCert;
    }

    public void setCurrencyChangeCert(String currencyChangeCert) {
        this.currencyChangeCert = currencyChangeCert;
    }

}