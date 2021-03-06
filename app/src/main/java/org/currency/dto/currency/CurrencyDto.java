package org.currency.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.bouncycastle2.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.currency.crypto.CertUtils;
import org.currency.crypto.CertificationRequest;
import org.currency.model.Currency;
import org.currency.throwable.ValidationException;
import org.currency.util.Constants;
import org.currency.util.ObjectUtils;
import org.currency.util.OperationType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperationType operation = OperationType.CURRENCY_SEND;
    private Long id;
    private BigDecimal amount;
    private BigDecimal batchAmount;
    private Currency.State state;
    private String currencyCode;
    private String currencyServerURL;
    private String revocationHash;
    private String subject;
    private String toUserIBAN;
    private String toUserName;
    private String batchUUID;
    private String object;
    private Date notBefore;
    private Date notAfter;
    private Date dateCreated;

    @JsonIgnore private PKCS10CertificationRequest csrPKCS10;

    public CurrencyDto() {}

    public CurrencyDto(Currency currency) {
        this.id = currency.getId();
        this.revocationHash = currency.getRevocationHash();
        this.amount = currency.getAmount();
        this.currencyCode = currency.getCurrencyCode();
        this.dateCreated = currency.getDateCreated();
        this.notBefore = currency.getValidFrom();
        this.notAfter = currency.getValidTo();
    }

    public CurrencyDto(PKCS10CertificationRequest csrPKCS10) throws Exception {
        this.csrPKCS10 = csrPKCS10;
        CertificationRequestInfo info = csrPKCS10.getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                CurrencyCertExtensionDto.class, csrPKCS10, Constants.CURRENCY_OID);
        if(certExtensionDto == null) throw new ValidationException("error missing cert extension data");
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        revocationHash = certExtensionDto.getRevocationHash();
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        CurrencyDto certSubjectDto = getCertSubjectDto(subjectDN, revocationHash);
        if(!certSubjectDto.getCurrencyServerURL().equals(currencyServerURL))
            throw new ValidationException("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationException("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
    }

    public static CurrencyDto BATCH_ITEM(CurrencyBatchDto currencyBatchDto, Currency currency) throws ValidationException {
        if(!currencyBatchDto.getCurrencyCode().equals(currency.getCurrencyCode()))
                throw new ValidationException("CurrencyBatch currencyCode: " +
                currencyBatchDto.getCurrencyCode() + " - Currency currencyCode: " +
                currency.getCurrencyCode());
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyDto.subject = currencyBatchDto.getSubject();
        currencyDto.toUserIBAN = currencyBatchDto.getToUserIBAN();
        currencyDto.toUserName = currencyBatchDto.getToUserName();
        currencyDto.batchAmount = currencyBatchDto.getBatchAmount();
        currencyDto.batchUUID = currencyBatchDto.getBatchUUID();
        return currencyDto;
    }

    public static CurrencyDto serialize(Currency currency) throws Exception {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setAmount(currency.getAmount());
        currencyDto.setCurrencyCode(currency.getCurrencyCode());
        currencyDto.setRevocationHash(currency.getRevocationHash());
        currencyDto.setState(currency.getState());
        //CertificationRequest instead of Currency to make it easier deserialization on JavaFX
        currencyDto.setObject(ObjectUtils.serializeObjectToString(currency.getCertificationRequest()));
        return currencyDto;
    }

    public static Set<CurrencyDto> serializeCollection(Collection<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> result = new HashSet<>();
        for(Currency currency : currencyCollection) {
            result.add(CurrencyDto.serialize(currency));
        }
        return result;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Currency deSerialize() throws Exception {
        try {
            CertificationRequest certificationRequest =
                    (CertificationRequest) ObjectUtils.deSerializeObject(object.getBytes());
            Currency currency = Currency.fromCertificationRequest(certificationRequest);
            currency.setState(state);
            return currency;
        }catch (Exception ex) {
            return (Currency) ObjectUtils.deSerializeObject(object.getBytes());
        }
    }

    public static Set<Currency> deSerialize(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public static Set<Currency> deSerializeCollection(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public static Set<Currency> getCurrencySet(Collection<CurrencyDto> currencyDtoCollection) throws Exception {
        Set<Currency> currencySet = new HashSet<>();
        for(CurrencyDto currencyDto : currencyDtoCollection) {
            currencySet.add(currencyDto.deSerialize());
        }
        return currencySet;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
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

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public PKCS10CertificationRequest getCsrPKCS10() {
        return csrPKCS10;
    }

    public void setCsrPKCS10(PKCS10CertificationRequest csrPKCS10) {
        this.csrPKCS10 = csrPKCS10;
    }

    public static CurrencyDto getCertSubjectDto(String subjectDN, String revocationHash) {
        CurrencyDto currencyDto = new CurrencyDto();
        if (subjectDN.contains("CURRENCY_CODE:"))
            currencyDto.setCurrencyCode(subjectDN.split("CURRENCY_CODE:")[1].split(",")[0]);
        if (subjectDN.contains("CURRENCY_VALUE:"))
            currencyDto.setAmount(new BigDecimal(subjectDN.split("CURRENCY_VALUE:")[1].split(",")[0]));
        if (subjectDN.contains("currencyServerURL:"))
            currencyDto.setCurrencyServerURL(subjectDN.split("currencyServerURL:")[1].split(",")[0]);
        currencyDto.setRevocationHash(revocationHash);
        return currencyDto;
    }

    public Currency.State getState() {
        return state;
    }

    public void setState(Currency.State state) {
        this.state = state;
    }

}
