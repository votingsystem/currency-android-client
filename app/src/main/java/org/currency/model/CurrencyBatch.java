package org.currency.model;

import android.util.Base64;

import org.currency.cms.CMSSignedMessage;
import org.currency.crypto.CertUtils;
import org.currency.crypto.PEMUtils;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.UserDto;
import org.currency.dto.currency.CurrencyCertExtensionDto;
import org.currency.throwable.ValidationException;
import org.currency.util.Constants;
import org.currency.util.OperationType;

import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyBatch {

    public static final String TAG = CurrencyBatch.class.getSimpleName();

    private OperationTypeDto currencyServer;

    private UserDto toUser;
    private BigDecimal batchAmount = null;
    private BigDecimal currencyAmount = null;
    private String batchUUID;
    private String subject;

    private List<Currency> currencyList;
    private Currency leftOverCurrency;
    private BigDecimal leftOver;
    private OperationType operation;
    private String currencyCode;
    private String toUserIBAN;
    private String tag;
    private CMSSignedMessage cmsMessage;
    private Map<String, Currency> currencyMap;

    public CurrencyBatch() {}

    public CurrencyBatch(BigDecimal batchAmount, BigDecimal currencyAmount, String currencyCode,
            OperationTypeDto currencyServer) throws Exception {
        this.batchAmount = batchAmount;
        this.currencyAmount = currencyAmount;
        this.setCurrencyServerDto(currencyServer);
        this.setCurrencyCode(currencyCode);
    }

    public CurrencyBatch(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public void addCurrency(Currency currency) {
        if(currencyList == null) currencyList = new ArrayList<Currency>();
        currencyList.add(currency);
    }

    public void validateTransactionResponse(Map dataMap, Set<TrustAnchor> trustAnchor) throws Exception {
        CMSSignedMessage receipt = new CMSSignedMessage(Base64.decode(
                ((String) dataMap.get("receipt")).getBytes(), Base64.NO_WRAP));
        if(dataMap.containsKey("leftOverCoin")) {

        }

        Map<String, Currency> currencyMap = getCurrencyMap();
        if(currencyMap.size() != dataMap.size()) throw new ValidationException("Num. currency: '" +
                currencyMap.size() + "' - num. receipts: " + dataMap.size());
        for(int i = 0; i < dataMap.size(); i++) {
            Map receiptData = (Map) dataMap.get(i);
            //TODO
            String revocationHash = (String) receiptData.keySet().iterator().next();
            CMSSignedMessage cmsReceipt = new CMSSignedMessage(
                    Base64.decode(((String) receiptData.get(revocationHash)).getBytes(), Base64.NO_WRAP));
            CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    cmsReceipt.getCurrencyCert(), Constants.CURRENCY_OID);
            Currency currency = currencyMap.remove(certExtensionDto.getRevocationHash());
            currency.validateReceipt(cmsReceipt, trustAnchor);
        }
        if(currencyMap.size() != 0) throw new ValidationException(currencyMap.size() +
                " Currency transactions without receipt");
    }

    public static CurrencyBatch getAnonymousSignedTransactionBatch(BigDecimal totalAmount,
            String currencyCode, OperationTypeDto currencyServer) throws Exception {
        CurrencyBatch result = new CurrencyBatch(totalAmount, null, currencyCode, currencyServer);
        return result;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public OperationTypeDto getCurrencyServer() {
        return currencyServer;
    }

    public void setCurrencyServerDto(OperationTypeDto currencyServer) {
        this.currencyServer = currencyServer;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Currency initCurrency(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(
                signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ValidationException(
                "Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                CurrencyCertExtensionDto.class, x509Certificate, Constants.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getRevocationHash()).setState(Currency.State.OK);
        currency.initSigner(signedCsr.getBytes());
        return currency;
    }

    public void initCurrency(Collection<String> issuedCurrencyCollection) throws Exception {
        LOGD(TAG + ".initCurrency", "num currency: " + issuedCurrencyCollection.size());
        if(issuedCurrencyCollection.size() != currencyMap.size()) {
            LOGD(TAG + ".initCurrency", "num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + issuedCurrencyCollection.size());
        }
        for(String issuedCurrency : issuedCurrencyCollection) {
            Currency currency = initCurrency(issuedCurrency);
            currencyMap.put(currency.getRevocationHash(), currency);
        }
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

    public UserDto getToUser() {
        return toUser;
    }

    public void setToUser(UserDto toUser) {
        this.toUser = toUser;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public CMSSignedMessage getCMSMessage() {
        return cmsMessage;
    }

    public void setCMSMessage(CMSSignedMessage cmsMessage) {
        this.cmsMessage = cmsMessage;
    }

    public List<Currency> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

}