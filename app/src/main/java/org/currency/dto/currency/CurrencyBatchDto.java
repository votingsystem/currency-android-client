package org.currency.dto.currency;

import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.currency.cms.CMSSignedMessage;
import org.currency.crypto.CertUtils;
import org.currency.model.Currency;
import org.currency.model.CurrencyBatch;
import org.currency.throwable.ValidationException;
import org.currency.util.OperationType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperationType operation = OperationType.CURRENCY_SEND;
    private Set<String> currencySet;
    private String leftOverCSR;
    private String currencyChangeCSR;
    private String toUserIBAN;
    private String toUserName;
    private String subject;
    private String currencyCode;
    private String batchUUID;
    private BigDecimal batchAmount;
    private BigDecimal leftOver = BigDecimal.ZERO;
    @JsonIgnore private Currency leftOverCurrency;
    @JsonIgnore private Collection<Currency> currencyCollection;
    
    
    public CurrencyBatchDto() {}


    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUser().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    @JsonIgnore
    public void checkCurrencyData(Currency currency) throws ValidationException {
        String currencyData = "Currency with hash '" + currency.getRevocationHash() + "' ";
        if(operation != currency.getOperation()) throw new ValidationException(
                currencyData + "expected operation " + getOperation() + " found " + currency.getOperation());
        if(!getSubject().equals(currency.getSubject())) throw new ValidationException(
                currencyData + "expected subject " + getSubject() + " found " + currency.getSubject());
        if(!getToUserIBAN().equals(currency.getToUserIBAN())) throw new ValidationException(
                currencyData + "expected subject " + getToUserIBAN() + " found " + currency.getToUserIBAN());
        if(getBatchAmount().compareTo(currency.getBatchAmount()) != 0) throw new ValidationException(
                currencyData + "expected batchAmount " + getBatchAmount().toString() + " found " + currency.getBatchAmount().toString());
        if(!getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationException(
                currencyData + "expected currencyCode " + getCurrencyCode() + " found " + currency.getCurrencyCode());
        if(!getBatchUUID().equals(currency.getBatchUUID())) throw new ValidationException(
                currencyData + "expected batchUUID " + getBatchUUID() + " found " + currency.getBatchUUID());
    }

    @JsonIgnore
    public CMSSignedMessage validateResponse(CurrencyBatchResponseDto responseDto,
                                         Set<TrustAnchor> trustAnchor) throws Exception {
        CMSSignedMessage receipt = new CMSSignedMessage(
                Base64.decode(responseDto.getReceipt().getBytes(), Base64.NO_WRAP));
        receipt.isValidSignature();
        CertUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(receipt.getSignersCerts()));
        if(responseDto.getLeftOverCert() != null) {
            leftOverCurrency.initSigner(responseDto.getLeftOverCert().getBytes());
            leftOverCurrency.setState(Currency.State.OK);
        }
        CurrencyBatchDto signedDto = receipt.getSignedContent(CurrencyBatchDto.class);
        if(signedDto.getBatchAmount().compareTo(batchAmount) != 0) throw new ValidationException(MessageFormat.format(
                "ERROR - batchAmount ''{0}'' - receipt amount ''{1}''", batchAmount, signedDto.getBatchAmount()));
        if(!signedDto.getCurrencyCode().equals(signedDto.getCurrencyCode())) throw new ValidationException(MessageFormat.format(
                "ERROR - batch currencyCode ''{0}'' - receipt currencyCode ''{1}''",  currencyCode, signedDto.getCurrencyCode()));
        if(!currencySet.equals(signedDto.getCurrencySet())) throw new ValidationException("ERROR - currencySet mismatch");
        return receipt;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) throws Exception {
        this.leftOver = leftOverCurrency.getAmount();
        this.leftOverCurrency = leftOverCurrency;
        this.leftOverCSR = new String(leftOverCurrency.getCertificationRequest().getCsrPEM(), "UTF-8");
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public Set<String> getCurrencySet() {
        return currencySet;
    }

    public void setCurrencySet(Set<String> currencySet) {
        this.currencySet = currencySet;
    }

    public String getLeftOverCSR() {
        return leftOverCSR;
    }

    public void setLeftOverCSR(String leftOverCSR) {
        this.leftOverCSR = leftOverCSR;
    }

    public Collection<Currency> getCurrencyCollection() {
        return currencyCollection;
    }

    public void setCurrencyCollection(Collection<Currency> currencyCollection) {
        this.currencyCollection = currencyCollection;
    }

    public String getCurrencyChangeCSR() {
        return currencyChangeCSR;
    }

    public void setCurrencyChangeCSR(String currencyChangeCSR) {
        this.currencyChangeCSR = currencyChangeCSR;
    }

}