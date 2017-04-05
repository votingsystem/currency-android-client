package org.currency.crypto;

import android.content.Context;

import org.currency.android.R;
import org.currency.dto.UserDto;
import org.currency.util.OperationType;
import org.currency.xades.XmlSignature;

import java.io.Serializable;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptContainer implements Serializable {

    public static final String TAG = ReceiptContainer.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    public enum State { ACTIVE, CANCELLED }

    private Long localId = -1L;
    private byte[] receipt;
    private OperationType operationType;
    private String subject;
    private String url;
    private String systemEntityId;

    public ReceiptContainer() { }

    public ReceiptContainer(OperationType operationType, String url) {
        this.operationType = operationType;
        this.url = url;
    }

    public String getTypeDescription(Context context) {
        switch (getOperationType()) {
            default:
                return context.getString(R.string.receipt_lbl) + ": " + getOperationType().toString();
        }
    }

    public String getCardSubject(Context context) {
        switch (getOperationType()) {
            default:
                return context.getString(R.string.receipt_lbl) + ": " + subject;
        }
    }

    public int getLogoId() {
        switch (getOperationType()) {
            default:
                return R.drawable.receipt_32;
        }
    }

    public String getURL() {
        return url;
    }

    public byte[] getReceipt() {
        return receipt;
    }

    public void setReceipt(byte[] receipt) {
        this.receipt = receipt;
    }

    public String getSubject() {
        return subject;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public UserDto getSigner() {
        return null;
    }

    public Set<UserDto> getSigners() {
        return null;
    }

    public Set<XmlSignature> getSignatures() {
        return null;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public String getSystemEntityId() {
        return systemEntityId;
    }

    public void setSystemEntityId(String systemEntityId) {
        this.systemEntityId = systemEntityId;
    }

}