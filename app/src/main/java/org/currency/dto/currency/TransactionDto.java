package org.currency.dto.currency;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.dto.MessageDto;
import org.currency.dto.OperationDto;
import org.currency.dto.QRMessageDto;
import org.currency.dto.TagDto;
import org.currency.dto.UserDto;
import org.currency.throwable.ValidationException;
import org.currency.util.DateUtils;
import org.currency.util.JSON;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.PaymentStep;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private OperationType operation;
    private PaymentStep paymentStep;
    private Long id;
    private Long localId;
    private Long userId;
    private UserDto fromUser;
    private UserDto toUser;
    private Date validTo;
    private Date dateCreated;
    private String subject;
    private String description;
    private String currencyCode;
    private String fromUserName;
    private String toUserName;
    private String fromUserIBAN;
    private String receipt;
    private String bankIBAN;
    private String cmsMessagePEM;
    private String cmsCancelationMessagePEM;
    private String UUID;
    private BigDecimal amount;
    private Boolean timeLimited = Boolean.FALSE;
    private Integer numReceptors;
    private Set<String> tags;
    private Set<String> toUserIBAN = new HashSet<>();
    private Long numChildTransactions;

    private String infoURL;
    private List<OperationType> paymentOptions;
    private TransactionDetailsDto details;
    private UserDto.Type userToType;

    @JsonIgnore private TagDto tag;
    @JsonIgnore private CMSSignedMessage cmsMessage;
    @JsonIgnore private CMSSignedMessage cancelationCmsMessage;
    @JsonIgnore private List<UserDto> toUserList;
    @JsonIgnore private MessageDto socketMessage;
    @JsonIgnore private UserDto signer;
    @JsonIgnore private UserDto receptor;
    @JsonIgnore private QRMessageDto qrMessageDto;


    public TransactionDto() {}

    public static TransactionDto PAYMENT_REQUEST(String toUser, UserDto.Type userToType,
             BigDecimal amount, String currencyCode, String toUserIBAN, String subject, String tag){
        TransactionDto dto = new TransactionDto();
        dto.setOperation(OperationType.TRANSACTION_INFO);
        dto.setUserToType(userToType);
        dto.setToUserName(toUser);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setSubject(subject);
        dto.setToUserIBAN(new HashSet<>(Arrays.asList(toUserIBAN)));
        dto.setTags(new HashSet<>(Arrays.asList(tag)));
        dto.setDateCreated(new Date());
        dto.setUUID(java.util.UUID.randomUUID().toString());
        return dto;
    }

    public static TransactionDto CURRENCY_REQUEST(BigDecimal amount, String currencyCode,
              TagDto tagVS, boolean timeLimited) {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(OperationType.CURRENCY_REQUEST);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setTag(tagVS);
        dto.setTimeLimited(timeLimited);
        return dto;
    }

    public void validate() throws ValidationException {
        if(operation == null)
            throw new ValidationException("missing param 'operation'");
        if(amount == null)
            throw new ValidationException("missing param 'amount'");
        if(getCurrencyCode() == null)
            throw new ValidationException("missing param 'currencyCode'");
        if(subject == null)
            throw new ValidationException("missing param 'subject'");
        if(timeLimited) validTo = DateUtils.getCurrentWeekPeriod().getDateTo();
        if (tags.size() != 1) { //for now transactions can only have one tag associated
            throw new ValidationException("invalid number of tags:" + tags.size());
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInfoURL() {
        return infoURL;
    }

    @JsonIgnore
    public String getPaymentConfirmURL() {
        return infoURL + "/" + "payment";
    }

    public MessageDto getSocketMessage() {
        return socketMessage;
    }

    public void setSocketMessage(MessageDto socketMessage) {
        this.socketMessage = socketMessage;
    }

    public void setInfoURL(String infoURL) {
        this.infoURL = infoURL;
    }

    public QRMessageDto getQrMessageDto() {
        return qrMessageDto;
    }

    public void setQrMessageDto(QRMessageDto qrMessageDto) {
        this.infoURL = qrMessageDto.getUrl();
        this.qrMessageDto = qrMessageDto;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

    public CMSSignedMessage getCMSMessage() throws Exception {
        if(cmsMessage == null && cmsMessagePEM != null) {
            byte[] cmsMessageBytes = Base64.decode(cmsMessagePEM.getBytes(), Base64.NO_WRAP);
            cmsMessage = new CMSSignedMessage(cmsMessageBytes);
        }
        return cmsMessage;
    }

    public void setCMSMessage(CMSSignedMessage cmsMessage) throws IOException {
        this.cmsMessage = cmsMessage;
        this.cmsMessagePEM = cmsMessage.toPEMStr();
    }

    public CMSSignedMessage getCancelationCmsMessage() throws Exception {
        if(cancelationCmsMessage == null && cmsCancelationMessagePEM != null) {
            cancelationCmsMessage = CMSSignedMessage.FROM_PEM(cmsCancelationMessagePEM);
        }
        return cancelationCmsMessage;
    }

    public void setCancelationCmsMessage(CMSSignedMessage cancelationCmsMessage) throws IOException {
        this.cancelationCmsMessage = cancelationCmsMessage;
        this.cmsCancelationMessagePEM = cancelationCmsMessage.toPEMStr();
    }

    public String getCmsCancelationMessagePEM() {
        return cmsCancelationMessagePEM;
    }

    public void setCmsCancelationMessagePEM(String cmsCancelationMessagePEM) {
        this.cmsCancelationMessagePEM = cmsCancelationMessagePEM;
    }

    @JsonIgnore public String getTagName() {
        if(tag != null) return tag.getName();
        else if (tags != null && !tags.isEmpty()) return tags.iterator().next();
        return null;
    }

    public UserDto getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserDto fromUser) {
        this.fromUser = fromUser;
    }

    public UserDto getToUser() {
        return toUser;
    }

    public void setToUser(UserDto toUser) {
        this.toUser = toUser;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Long getNumChildTransactions() {
        return numChildTransactions;
    }

    public void setNumChildTransactions(Long numChildTransactions) {
        this.numChildTransactions = numChildTransactions;
    }

    public String getCmsMessagePEM() {
        return cmsMessagePEM;
    }

    public void setCmsMessagePEM(String cmsMessagePEM) {
        this.cmsMessagePEM = cmsMessagePEM;
    }

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getFromUserIBAN() {
        return fromUserIBAN;
    }

    public void setFromUserIBAN(String fromUserIBAN) {
        this.fromUserIBAN = fromUserIBAN;
    }

    public Integer getNumReceptors() {
        return numReceptors;
    }

    public void setNumReceptors(Integer numReceptors) {
        this.numReceptors = numReceptors;
    }

    public List<UserDto> getToUserList() {
        return toUserList;
    }

    public void setToUserList(List<UserDto> toUserList) {
        this.toUserList = toUserList;
        this.numReceptors = toUserList.size();
    }

    public TagDto getTag() {
        if(tag != null) return tag;
        else if(tags != null && !tags.isEmpty()) tag = new TagDto(tags.iterator().next());
        return tag;
    }

    public void setTag(TagDto tag) {
        this.tag = tag;
    }

    public Set<String> getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(Set<String> toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public UserDto getSigner() {
        return signer;
    }

    public void setSigner(UserDto signer) {
        this.signer = signer;
    }

    public UserDto getReceptor() {
        return receptor;
    }

    public void setReceptor(UserDto receptor) {
        this.receptor = receptor;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public String getBankIBAN() {
        return bankIBAN;
    }

    public void setBankIBAN(String bankIBAN) {
        this.bankIBAN = bankIBAN;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public UserDto.Type getUserToType() {
        return userToType;
    }

    public void setUserToType(UserDto.Type userToType) {
        this.userToType = userToType;
    }

    public List<OperationType> getPaymentOptions() {
        return paymentOptions;
    }

    public void setPaymentOptions(List<OperationType> paymentOptions) {
        this.paymentOptions = paymentOptions;
    }

    public PaymentStep getPaymentStep() {
        return paymentStep;
    }

    public TransactionDto setPaymentStep(PaymentStep paymentStep) {
        this.paymentStep = paymentStep;
        return this;
    }

    public static List<String> getPaymentMethods(Context context) {
        //preserve the same order
        List<String> result = Arrays.asList(
                context.getString(R.string.signed_transaction_lbl),
                context.getString(R.string.currency_send_lbl),
                context.getString(R.string.currency_change_lbl));
        return result;
    }

    public static List<String> getPaymentMethods(List<OperationType> paymentOptions) {
        //preserve the same order
        List<String> result = new ArrayList<>();
        if(paymentOptions.contains(OperationType.TRANSACTION_FROM_USER)) result.add(App.getInstance()
                .getString(R.string.signed_transaction_lbl));
        if(paymentOptions.contains(OperationType.CURRENCY_SEND)) result.add(App.getInstance()
                .getString(R.string.currency_send_lbl));
        if(paymentOptions.contains(OperationType.CURRENCY_CHANGE)) result.add(App.getInstance()
                .getString(R.string.currency_change_lbl));
        return result;
    }

    public static OperationType getByDescription(String description) throws ValidationException {
        if(App.getInstance().getString(R.string.signed_transaction_lbl).equals(description))
            return OperationType.TRANSACTION_FROM_USER;
        if(App.getInstance().getString(R.string.currency_send_lbl).equals(description))
            return OperationType.CURRENCY_SEND;
        if(App.getInstance().getString(R.string.currency_change_lbl).equals(description))
            return OperationType.CURRENCY_CHANGE;
        throw new ValidationException("type not found for description: " + description);
    }

    public static OperationType getByPosition(int position) {
        if(position == 0) return OperationType.TRANSACTION_FROM_USER;
        if(position == 1) return OperationType.CURRENCY_SEND;
        if(position == 2) return OperationType.CURRENCY_CHANGE;
        return null;
    }

    public String getDescription(Context context) {
        switch(operation) {
            case TRANSACTION_FROM_USER: return context.getString(R.string.signed_transaction_lbl);
            case CURRENCY_SEND: return context.getString(R.string.currency_send_lbl);
            case CURRENCY_CHANGE: return context.getString(R.string.currency_change_lbl);
        }
        return null;
    }

    public String validateReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        TransactionDto dto = cmsMessage.getSignedContent(TransactionDto.class);
        switch(dto.getOperation()) {
            case TRANSACTION_FROM_USER:
                return validateFromUserReceipt(cmsMessage, isIncome);
            case CURRENCY_SEND:
                return validateCurrencySendReceipt(cmsMessage, isIncome);
            case CURRENCY_CHANGE:
                return validateCurrencyChangeReceipt(cmsMessage, isIncome);
            default: throw new ValidationException("unknown receipt type: " + dto.getOperation());
        }
    }

    private String validateCurrencyChangeReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = cmsMessage.getSignedContent(CurrencyBatchDto.class);
        if(OperationType.CURRENCY_CHANGE != receiptDto.getOperation()) throw new ValidationException(
                "ERROR - expected type: " +
                OperationType.CURRENCY_CHANGE + " - found: " + receiptDto.getOperation());
        if(paymentStep == PaymentStep.TRANSACTION_INFO) {
            if(!paymentOptions.contains(OperationType.CURRENCY_CHANGE)) throw new ValidationException(
                    "unexpected type: " + receiptDto.getOperation());
        }
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome? App.getInstance().getString(R.string.income_lbl):
                App.getInstance().getString(R.string.expense_lbl);
        String result = App.getInstance().getString(R.string.currency_change_receipt_ok_msg,
                action, receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagMessage(receiptDto.getTag()));
        if(receiptDto.timeLimited()) {
            result = result + " - " + App.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    private String validateCurrencySendReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        CurrencyBatchDto receiptDto = cmsMessage.getSignedContent(CurrencyBatchDto.class);
        if(OperationType.CURRENCY_SEND != receiptDto.getOperation()) throw new ValidationException("ERROR - expected type: " +
                OperationType.CURRENCY_SEND + " - found: " + receiptDto.getOperation());
        if(paymentStep == PaymentStep.TRANSACTION_INFO) {
            if(!paymentOptions.contains(OperationType.CURRENCY_SEND)) throw new ValidationException(
                    "unexpected type: " + receiptDto.getOperation());
        }
        Set<String> receptorsSet = new HashSet<>(Arrays.asList(receiptDto.getToUserIBAN()));
        if(!toUserIBAN.equals(receptorsSet)) throw new ValidationException(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(amount.compareTo(receiptDto.getBatchAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getBatchAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getBatchUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getBatchUUID());
        String action = isIncome? App.getInstance().getString(R.string.income_lbl):
                App.getInstance().getString(R.string.expense_lbl);
        String result = App.getInstance().getString(R.string.currency_send_receipt_ok_msg,
                action, receiptDto.getBatchAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagMessage(receiptDto.getTag()));
        if(receiptDto.timeLimited()) {
            result = result + " - " + App.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    private String validateFromUserReceipt(CMSSignedMessage cmsMessage, boolean isIncome) throws Exception {
        TransactionDto receiptDto = cmsMessage.getSignedContent(TransactionDto.class);
        if(operation != receiptDto.getOperation()) throw new ValidationException("expected type "
                + operation + " found " + receiptDto.getOperation());
        if(operation == receiptDto.getOperation()) {
            if(!paymentOptions.contains(receiptDto.getOperation())) throw new ValidationException(
                    "unexpected type " + receiptDto.getOperation());
        }
        if(!new HashSet<>(toUserIBAN).equals(new HashSet<>(receiptDto.getToUserIBAN())) ||
                toUserIBAN.size() != receiptDto.getToUserIBAN().size()) throw new ValidationException(
                "expected toUserIBAN " + toUserIBAN + " found " + receiptDto.getToUserIBAN());
        if(!subject.equals(receiptDto.getSubject())) throw new ValidationException("expected subject " + subject +
                " found " + receiptDto.getSubject());
        if(!toUserName.equals(receiptDto.getToUserName())) throw new ValidationException(
                "expected toUserName " + toUserName + " found " + receiptDto.getToUserName());
        if(amount.compareTo(receiptDto.getAmount()) != 0) throw new ValidationException(
                "expected amount " + amount + " amount " + receiptDto.getAmount());
        if(!currencyCode.equals(receiptDto.getCurrencyCode())) throw new ValidationException(
                "expected currencyCode " + currencyCode + " found " + receiptDto.getCurrencyCode());
        if(!UUID.equals(receiptDto.getUUID())) throw new ValidationException(
                "expected UUID " + UUID + " found " + receiptDto.getUUID());
        if(details != null && !details.equals(receiptDto.getDetails())) throw new ValidationException(
                "expected details " + details + " found " + receiptDto.getDetails());
        String action = isIncome? App.getInstance().getString(R.string.income_lbl):
                App.getInstance().getString(R.string.expense_lbl);
        String result = App.getInstance().getString(R.string.from_user_receipt_ok_msg,
                action, receiptDto.getAmount() + " " + receiptDto.getCurrencyCode(),
                MsgUtils.getTagMessage(receiptDto.getTagName()));
        if(receiptDto.isTimeLimited()) {
            result = result + " - " + App.getInstance().getString(R.string.time_remaining_lbl);
        }
        return result;
    }

    public TransactionDetailsDto getDetails() {
        return details;
    }

    public void setDetails(TransactionDetailsDto details) {
        this.details = details;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public int getIconId(Context context) {
        switch(operation) {
            case CURRENCY_REQUEST:
                return R.drawable.edit_undo_24;
            case CURRENCY_SEND:
                return R.drawable.fa_money_24;
            default:
                return R.drawable.pending;
        }
    }

    public static String getDescription(Context context, OperationType type) {
        switch(type) {
            case TRANSACTION_FROM_BANK:
                return context.getString(R.string.account_input_from_bank);
            case TRANSACTION_FROM_USER:
                return context.getString(R.string.account_input);
            case CURRENCY_REQUEST:
                return context.getString(R.string.account_output);
            case CURRENCY_SEND:
                return context.getString(R.string.currency_send);
            default:
                return type.toString();
        }
    }

    public String getFormatted(Context context) throws Exception {
        String result = null;
        switch(operation) {
            default:
                result = JSON.writeValueAsString(this);
        }
        return result;
    }

    public static TransactionDto fromUri(Uri uriData) {
        TransactionDto transaction = new TransactionDto();
        transaction.setAmount(new BigDecimal(uriData.getQueryParameter("amount")));
        TagDto tag = null;
        if(uriData.getQueryParameter("tag") != null) tag = new TagDto(uriData.getQueryParameter("tag"));
        else tag = new TagDto(TagDto.WILDTAG);
        transaction.setTag(tag);
        transaction.setCurrencyCode(uriData.getQueryParameter("currencyCode"));
        transaction.setSubject(uriData.getQueryParameter("subject"));
        UserDto toUser = new UserDto();
        toUser.setGivenName(uriData.getQueryParameter("toUserName"));
        toUser.setIBAN(uriData.getQueryParameter("toUserIBAN"));
        transaction.setToUser(toUser);
        return transaction;
    }

    public static TransactionDto fromOperation(OperationDto operation) throws Exception {
        TransactionDto transactionDto = operation.getSignedContent(TransactionDto.class);
        if(transactionDto.getTag() == null) {
            transactionDto.setTag(new TagDto(TagDto.WILDTAG));
        }
        UserDto toUser = new UserDto();
        toUser.setGivenName(transactionDto.getFromUserName());
        if(operation.getOperation().getType() == OperationType.TRANSACTION_FROM_USER) {
            if(transactionDto.getToUserIBAN().size() != 1)
                    throw new ValidationException("FROM_USER must have " +
                    "'one' receptor and it has '" + transactionDto.getToUserIBAN().size() + "'");
            toUser.setIBAN(transactionDto.getToUserIBAN().iterator().next());
        }
        transactionDto.setToUser(toUser);
        UserDto fromUser = new UserDto();
        fromUser.setGivenName(transactionDto.getFromUserName());
        fromUser.setIBAN(transactionDto.getFromUserIBAN());
        transactionDto.setFromUser(fromUser);
        return transactionDto;
    }

    @JsonIgnore public TransactionDto getTransactionFromUser() {
        TransactionDto dto = new TransactionDto();
        dto.setOperation(OperationType.TRANSACTION_FROM_USER);
        dto.setSubject(subject);
        dto.setAmount(amount);
        dto.setCurrencyCode(currencyCode);
        dto.setToUserName(toUserName);
        dto.setToUserIBAN(toUserIBAN);
        dto.setTimeLimited(timeLimited);
        dto.setTags(tags);
        dto.setUUID(UUID);
        return dto;
    }

}