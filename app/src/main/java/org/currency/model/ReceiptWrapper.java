package org.currency.model;

import android.content.Context;

import com.fasterxml.jackson.core.type.TypeReference;

import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.dto.currency.TransactionDto;
import org.currency.util.OperationType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptWrapper implements Serializable {

    public static final String TAG = ReceiptWrapper.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}

    private Long localId = -1L;
    private transient CMSSignedMessage receipt;
    private OperationType operationType;
    private String subject;

    public ReceiptWrapper() {}

    public ReceiptWrapper(OperationType operationType) {
        this.operationType = operationType;
    }

    public ReceiptWrapper(TransactionDto transaction) {
        this.operationType = transaction.getOperation();
    }

    public String getTypeDescription(Context context) {
        switch(getOperationType()) {
            case CURRENCY_REQUEST:
                return context.getString(R.string.currency_request_subtitle);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + getOperationType().toString();
        }
    }

    public String getCardSubject(Context context) {
        switch(getOperationType()) {
            case CURRENCY_REQUEST:
                return context.getString(R.string.currency_request_subtitle);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + subject;
        }
    }

    public int getLogoId() {
        switch(getOperationType()) {
            default:
                return R.drawable.receipt_32;
        }
    }

    public CMSSignedMessage getReceipt() throws Exception {
        return receipt;
    }

    public void setReceipt(CMSSignedMessage cmsSignedMessage) throws Exception {
        this.receipt = cmsSignedMessage;
        Map dataMap = receipt.getSignedContent(new TypeReference<Map<String, Object>>() { });
        if(dataMap.containsKey("operation"))
            this.operationType = OperationType.valueOf((String) dataMap.get("operation"));
        if(dataMap.containsKey("subject")) subject = (String) dataMap.get("subject");
    }

    public boolean hashReceipt() {
        return (receipt != null);
    }

    public String getSubject() {
        return subject;
    }

    public OperationType getOperationType() {
        if(operationType == null && receipt != null) {
            try {
                Map signedContent = receipt.getSignedContent(new TypeReference<Map<String, Object>>() {});
                if (signedContent.containsKey("operation")) {
                    operationType = OperationType.valueOf((String) signedContent.get("operation"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Date getDateFrom() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getX509Certificate().getNotBefore();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Date getDateTo() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getX509Certificate().getNotAfter();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(receipt != null) s.writeObject(receipt.getEncoded());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] receiptBytes = (byte[]) s.readObject();
        if(receiptBytes != null) receipt = new CMSSignedMessage(receiptBytes);
    }

}