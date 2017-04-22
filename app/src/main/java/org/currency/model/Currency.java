package org.currency.model;

import android.content.Context;

import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.crypto.CertUtils;
import org.currency.crypto.CertificationRequest;
import org.currency.crypto.PEMUtils;
import org.currency.dto.currency.CurrencyCertExtensionDto;
import org.currency.dto.currency.CurrencyDto;
import org.currency.throwable.ValidationException;
import org.currency.util.Constants;
import org.currency.util.HashUtils;
import org.currency.util.OperationType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Currency extends ReceiptWrapper {

    public static final String TAG = Currency.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State { OK, EXPENDED, LAPSED, UNKNOWN, ERROR;}

    public enum Type { LEFT_OVER, CHANGE, REQUEST}

    private Long localId = -1L;
    private Long id;
    private OperationType operation;
    private BigDecimal batchAmount;
    private transient CMSSignedMessage receipt;
    private transient CMSSignedMessage cmsMessage;
    private transient X509Certificate x509AnonymousCert;
    private CertificationRequest certificationRequest;
    private byte[] receiptBytes;
    private String originRevocationHash;
    private String revocationHash;
    private BigDecimal amount;
    private String subject;
    private State state;
    private String currencyCode;
    private String url;
    private String currencyServerURL;
    private Date validFrom;
    private Date validTo;
    private Date dateCreated;
    private String toUserIBAN;
    private String toUserName;
    private String batchUUID;
    private CurrencyCertExtensionDto certExtensionDto;
    private Long serialNumber;
    private byte[] content;

    public Currency() {}

    public Currency(String currencyServerURL, BigDecimal amount, String currencyCode,
                    String revocationHash) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        try {
            this.revocationHash = revocationHash;
            certificationRequest = CertificationRequest.getCurrencyRequest(
                    Constants.SIGNATURE_ALGORITHM, Constants.PROVIDER,
                    currencyServerURL, revocationHash, amount, this.currencyCode);
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    public Currency(BigDecimal amount, String currencyCode, String currencyServerURL) {
        this.amount = amount;
        this.currencyServerURL = currencyServerURL;
        this.currencyCode = currencyCode;
        try {
            this.originRevocationHash = UUID.randomUUID().toString();
            this.revocationHash = HashUtils.getHashBase64(originRevocationHash.getBytes(),
                    Constants.DATA_DIGEST_ALGORITHM);
            certificationRequest = CertificationRequest.getCurrencyRequest(
                    Constants.SIGNATURE_ALGORITHM, Constants.PROVIDER,
                    currencyServerURL, revocationHash, amount, this.currencyCode);
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Long getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Long serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
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

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public void validateReceipt(CMSSignedMessage cmsReceipt, Set<TrustAnchor> trustAnchor)
            throws Exception {
        if(!cmsMessage.getSigner().getSignedContentDigestBase64().equals(
                cmsReceipt.getSigner().getSignedContentDigestBase64())){
            throw new ValidationException("Signer content digest mismatch");
        }
        for(X509Certificate cert : cmsReceipt.getSignersCerts()) {
            CertUtils.verifyCertificate(trustAnchor, false, Arrays.asList(cert));
            LOGD(TAG, "validateReceipt - Cert validated: " + cert.getSubjectDN().toString());
        }
        this.cmsMessage = cmsReceipt;
    }

    public Currency(CMSSignedMessage cmsMessage) throws Exception {
        cmsMessage.isValidSignature();
        this.cmsMessage = cmsMessage;
        initCertData(cmsMessage.getCurrencyCert());
        CurrencyDto batchItemDto = cmsMessage.getSignedContent(CurrencyDto.class);
        this.batchUUID = batchItemDto.getBatchUUID();
        this.batchAmount = batchItemDto.getBatchAmount();
        if(OperationType.CURRENCY_SEND != batchItemDto.getOperation())
                throw new ValidationException("Expected operation 'CURRENCY_SEND' - found: " +
                batchItemDto.getOperation() + "'");
        if(!this.currencyCode.equals(batchItemDto.getCurrencyCode())) {
                throw new ValidationException(getErrorPrefix() +
                "expected currencyCode '" + currencyCode + "' - found: '" + batchItemDto.getCurrencyCode());
        }

        Date signatureTime = cmsMessage.getTimeStampToken(cmsMessage.getCurrencyCert())
                .getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter()))
                throw new ValidationException(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        this.subject = batchItemDto.getSubject();
        this.toUserIBAN = batchItemDto.getToUserIBAN();
        this.toUserName = batchItemDto.getToUserName();
    }

    public CMSSignedMessage getCMS() {
        return cmsMessage;
    }

    public void setCMS(CMSSignedMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public void initSigner (byte[] signedCsr) throws Exception {
        certificationRequest.setSignedCsr(signedCsr);
        initCertData(certificationRequest.getCertificate());
    }

    public static Currency fromCertificationRequest(CertificationRequest certificationRequest)
            throws Exception {
        Currency currency = new Currency();
        currency.setCertificationRequest(certificationRequest);
        if(certificationRequest.getSignedCsr() != null) currency.initSigner(certificationRequest.getSignedCsr());
        else LOGD(TAG + ".fromCertificationRequest", "CertificationRequest with NULL SignedCSR");
        return currency;
    }

    public byte[] getIssuedCertPEM() throws IOException {
        return PEMUtils.getPEMEncoded(x509AnonymousCert);
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setCertificationRequest(CertificationRequest certificationRequest) {
        this.certificationRequest = certificationRequest;
    }

    public String getStateMsg(Context context) {
        if(state == null) return null;
        switch (state) {
            case OK: return context.getString(R.string.active_lbl);
            case EXPENDED: return context.getString(R.string.expended_lbl);
            case LAPSED: return context.getString(R.string.lapsed_lbl);
            default: return state.toString();
        }
    }

    public Integer getStateColor(Context context) {
        if(state == null) return context.getResources().getColor(R.color.orange_vs);
        switch (state) {
            case OK: return context.getResources().getColor(R.color.active_vs);
            default: return context.getResources().getColor(R.color.red_vs);
        }
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

    private String getErrorPrefix() {
        return "ERROR - Currency with hash: " + revocationHash + " - ";
    }

    public CertificationRequest getCertificationRequest() {
        return certificationRequest;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override public String getSubject() {
        return subject;
    }

    @Override public Date getDateFrom() {
        try {
            return certificationRequest.getCertificate().getNotBefore();
        }catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    @Override public Date getDateTo() {
        try {
            return certificationRequest.getCertificate().getNotAfter();
        }catch (Exception ex) { ex.printStackTrace(); }
        return null;
    }

    @Override public Long getLocalId() {
        return localId;
    }

    @Override public void setLocalId(Long localId) {
        this.localId = localId;
    }

    @Override public CMSSignedMessage getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) receipt = new CMSSignedMessage(receiptBytes);
        return receipt;
    }

    public State getState() {
        return state;
    }

    public Currency setState(State state) {
        this.state = state;
        return this;
    }

    public void setReceiptBytes(byte[] receiptBytes) {
        this.state = State.EXPENDED;
        this.receiptBytes = receiptBytes;
    }

    public String getOriginRevocationHash() {
        return originRevocationHash;
    }

    public void setOriginRevocationHash(String originRevocationHash) {
        this.originRevocationHash = originRevocationHash;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Currency initCertData(X509Certificate x509AnonymousCert) throws Exception {
        this.x509AnonymousCert = x509AnonymousCert;
        content = x509AnonymousCert.getEncoded();
        serialNumber = x509AnonymousCert.getSerialNumber().longValue();
        certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509AnonymousCert, Constants.CURRENCY_OID);
        if(certExtensionDto == null) throw new ValidationException("error missing cert extension data");
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        revocationHash = certExtensionDto.getRevocationHash();
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        validFrom = x509AnonymousCert.getNotBefore();
        validTo = x509AnonymousCert.getNotAfter();
        String subjectDN = x509AnonymousCert.getSubjectDN().toString();
        CurrencyDto certSubjectDto = CurrencyDto.getCertSubjectDto(subjectDN, revocationHash);
        if(!certSubjectDto.getCurrencyServerURL().equals(certExtensionDto.getCurrencyServerURL()))
            throw new ValidationException("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationException("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        return this;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(cmsMessage != null)
                s.writeObject(cmsMessage.getEncoded());
            else
                s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] cmsMessageBytes = (byte[]) s.readObject();
        if(cmsMessageBytes != null)
            cmsMessage = new CMSSignedMessage(cmsMessageBytes);
        if(certificationRequest != null)
            fromCertificationRequest(certificationRequest);
    }

}