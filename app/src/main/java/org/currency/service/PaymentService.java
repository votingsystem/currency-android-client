package org.currency.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;

import com.fasterxml.jackson.core.type.TypeReference;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.contentprovider.OperationContentProvider;
import org.currency.contentprovider.TransactionContentProvider;
import org.currency.crypto.PEMUtils;
import org.currency.dto.DeviceDto;
import org.currency.dto.MessageDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.ResultListDto;
import org.currency.dto.currency.BalancesDto;
import org.currency.dto.currency.CurrencyBatchDto;
import org.currency.dto.currency.CurrencyBatchResponseDto;
import org.currency.dto.currency.CurrencyDto;
import org.currency.dto.currency.CurrencyRequestDto;
import org.currency.dto.currency.CurrencyStateDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.dto.currency.TransactionResponseDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.model.Currency;
import org.currency.model.CurrencyBundle;
import org.currency.model.Operation;
import org.currency.throwable.ValidationException;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.JSON;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.Utils;
import org.currency.util.Wallet;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PaymentService extends IntentService {

    public static final String TAG = PaymentService.class.getSimpleName();

    public static final long TRANSACTION_TTL = 60 * 4 * 1000;//four minutes

    public PaymentService() { super(TAG); }

    private App app;
    private MetadataDto currencyServer;
    private DeviceDto sessionDevice;

    @Override protected void onHandleIntent(Intent intent) {
        app = (App) getApplicationContext();
        currencyServer = app.getCurrencyService();
        if(currencyServer == null) {
            LOGD(TAG + ".updateUserInfo", "missing connection to Currency Server");
            app.broadcastResponse(ResponseDto.ERROR(getString(R.string.error_lbl),
                    getString(R.string.service_connection_error_msg)));
            return;
        }
        sessionDevice = app.getSessionInfo().getSessionDevice();
        final Bundle arguments = intent.getExtras();
        OperationType operation = (OperationType)arguments.getSerializable(Constants.OPERATION_KEY);
        String serviceCaller = arguments.getString(Constants.CALLER_KEY);
        char[] pin = arguments.getCharArray(Constants.PIN_KEY);
        String revocationHash = arguments.getString(Constants.REVOCATION_HASH);
        TransactionDto transactionDto = (TransactionDto) intent.getSerializableExtra(
                Constants.TRANSACTION_KEY);
        try {
            switch(operation) {
                case CURRENCY_ACCOUNTS_INFO:
                    updateUserInfo(serviceCaller);
                    break;
                case CURRENCY_STATE:
                    checkCurrency(serviceCaller, revocationHash);
                    break;
                case CURRENCY_REQUEST:
                    currencyRequest(serviceCaller, transactionDto, pin);
                    break;
                case CURRENCY_CHANGE:
                case TRANSACTION_FROM_USER:
                case CURRENCY_SEND:
                    processTransaction(serviceCaller, transactionDto);
                    break;
                default:
                    LOGD(TAG + ".onHandleIntent", "unknown operation: " + operation);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            app.broadcastResponse(Utils.getBroadcastResponse(operation, serviceCaller,
                    ResponseDto.EXCEPTION(ex, this), this));
        }
    }

    private void processTransaction(String serviceCaller, TransactionDto transactionDto) {
        LOGD(TAG + ".processTransaction", "operation: " + transactionDto.getOperation());
        ResponseDto responseDto = null;
        OperationTypeDto operationType = new OperationTypeDto(null, currencyServer.getEntity().getId());
        if(transactionDto.getDateCreated() != null && DateUtils.inRange(transactionDto.getDateCreated(),
                Calendar.getInstance().getTime(), TRANSACTION_TTL)) {
            try {
                switch (transactionDto.getOperation()) {
                    case TRANSACTION_FROM_USER:
                        Operation operation = new Operation(OperationType.TRANSACTION_FROM_USER,
                                transactionDto, Operation.State.PENDING);
                        Uri operationUri = getContentResolver().insert(
                                OperationContentProvider.CONTENT_URI,
                                OperationContentProvider.getContentValues(operation));
                        responseDto = sendTransaction(transactionDto.getTransactionFromUser());
                        if(ResponseDto.SC_OK == responseDto.getStatusCode() &&
                                transactionDto.getPaymentConfirmURL() != null) {
                            ResultListDto<TransactionDto> resultList = JSON.getMapper().readValue(
                                    responseDto.getMessageBytes(), new TypeReference<ResultListDto<TransactionDto>>(){});
                            String base64Receipt = resultList.getResultList().iterator().next().getCmsMessagePEM();
                            CMSSignedMessage receipt = new CMSSignedMessage(Base64.decode(base64Receipt, Base64.NO_WRAP));
                            String message = transactionDto.validateReceipt(receipt, false);
                            receipt.isValidSignature();
                            if(transactionDto.getSocketMessage() != null) {
                                //this is to send the signed receipts to the device with the QR code
                                MessageDto socketRespDto = transactionDto.getSocketMessage()
                                        .getResponse(ResponseDto.SC_OK, new String(receipt.toPEM()),
                                        sessionDevice.getUUID(), null,
                                        operationType.setType(OperationType.PAYMENT_CONFIRM));
                                socketRespDto.setSignedMessageBase64(base64Receipt);
                                //backup to recover from fails
                                transactionDto.setSocketMessage(socketRespDto);
                                App.getInstance().getSocketSession(socketRespDto.getUUID()).setData(transactionDto);
                                sendSocketMessage(socketRespDto);
                                responseDto.setMessage(message);
                            } else {
                                //this is to send the signed receipts to the online service server receptor
                                //of the transaction
                                TransactionResponseDto transactionResponse = new TransactionResponseDto();
                                transactionResponse.setOperation(OperationType.TRANSACTION_FROM_USER);
                                transactionResponse.setCMSMessage(base64Receipt);
                                responseDto = HttpConn.getInstance().doPostRequest(
                                        JSON.writeValueAsBytes(responseDto),
                                        ContentType.JSON, transactionDto.getPaymentConfirmURL());
                            }
                            operation.setState(Operation.State.FINISHED);
                            getContentResolver().delete(operationUri, null, null);
                        } else {
                            operation.setState(Operation.State.ERROR);
                            getContentResolver().update(operationUri, OperationContentProvider
                                    .getContentValues(operation), null, null);
                        }
                        break;
                    case CURRENCY_SEND:
                        responseDto = sendCurrencyBatch(transactionDto);
                        CMSSignedMessage pkcs7Response = null;
                        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                            pkcs7Response = CMSSignedMessage.FROM_PEM(responseDto.getMessageBytes());
                            String message = transactionDto.validateReceipt(pkcs7Response, false);
                            if(transactionDto.getSocketMessage() != null) {
                                MessageDto socketRespDto = transactionDto.getSocketMessage()
                                        .getResponse(ResponseDto.SC_OK, new String(pkcs7Response.toPEM()),
                                        sessionDevice.getUUID(),null,
                                        operationType.setType(OperationType.PAYMENT_CONFIRM));
                                sendSocketMessage(socketRespDto);
                                responseDto.setMessage(message);
                            } else {
                                pkcs7Response = CMSSignedMessage.FROM_PEM(responseDto.getMessageBytes());
                                TransactionResponseDto transactionResponse = new TransactionResponseDto(
                                        OperationType.CURRENCY_SEND, null, pkcs7Response);
                                responseDto = HttpConn.getInstance().doPostRequest(
                                        JSON.writeValueAsBytes(transactionResponse),
                                        ContentType.PKCS7_SIGNED, transactionDto.getPaymentConfirmURL());
                            }
                            responseDto.setMessage(message);
                        }
                        break;
                    case CURRENCY_CHANGE:
                        CMSSignedMessage currencyRequest = CMSSignedMessage.FROM_PEM(transactionDto.getCmsMessagePEM());
                        CurrencyDto currencyDto = new CurrencyDto(
                                PEMUtils.fromPEMToPKCS10CertificationRequest(
                                currencyRequest.getSignedContentStr().getBytes()));
                        if(!transactionDto.getTagName().equals(currencyDto.getTag()))
                            throw new ValidationException("Transaction tag: " + transactionDto.getTagName() +
                            " doesn't match currency request tag: " + currencyDto.getTag());
                        if(!transactionDto.getCurrencyCode().equals(currencyDto.getCurrencyCode()))
                                throw new ValidationException("Transaction CurrencyCode: " +
                                transactionDto.getCurrencyCode() + " doesn't match currency CurrencyCode " +
                                currencyDto.getCurrencyCode());
                        if(transactionDto.getAmount().compareTo(currencyDto.getAmount()) != 0)
                            throw new ValidationException("Transaction amount: " +
                                    transactionDto.getAmount() +
                                    " doesn't match currency amount " + currencyDto.getAmount());
                        if(!transactionDto.getQrMessageDto().getRevocationHash().equals(
                                currencyDto.getRevocationHash()))
                            throw new ValidationException("currency request hash mismatch");
                        responseDto = sendCurrencyBatch(transactionDto);
                        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                            pkcs7Response = CMSSignedMessage.FROM_PEM(responseDto.getMessageBytes());
                            String message = transactionDto.validateReceipt(pkcs7Response, false);
                            if(transactionDto.getSocketMessage() != null) {
                                String currencyChangeCert = (String) responseDto.getData();
                                MessageDto socketRespDto = transactionDto.getSocketMessage()
                                        .getResponse(ResponseDto.SC_OK, currencyChangeCert,
                                        sessionDevice.getUUID(), new String(pkcs7Response.toPEM()),
                                        operationType.setType(OperationType.PAYMENT_CONFIRM));
                                sendSocketMessage(socketRespDto);
                                responseDto.setMessage(message);
                            } else {
                                //String currencyChangeCert = transactionDto.getQrMessageDto().getCurrencyChangeCert();
                                String currencyChangeCert = null;
                                TransactionResponseDto transResponse = new TransactionResponseDto(
                                        OperationType.CURRENCY_CHANGE, currencyChangeCert,
                                        pkcs7Response);
                                responseDto = HttpConn.getInstance().doPostRequest(
                                        JSON.writeValueAsBytes(transResponse),
                                        ContentType.PKCS7_SIGNED, transactionDto.getPaymentConfirmURL());
                            }
                            responseDto.setMessage(message);
                        }
                        break;
                    default:
                        LOGD(TAG + ".processTransaction", "unknown operation: " + transactionDto.getOperation());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                responseDto = ResponseDto.EXCEPTION(ex, this);
            }
        } else responseDto = new ResponseDto(ResponseDto.SC_ERROR,
                getString(R.string.session_expired_msg));
        app.broadcastResponse(Utils.getBroadcastResponse(transactionDto.getOperation(),
                serviceCaller, responseDto, app));
    }

    private void sendSocketMessage(MessageDto socketMessage) {
        LOGD(TAG + ".sendSocketMessage() ", "sendSocketMessage");
        try {
            Intent startIntent = new Intent(this, SocketService.class);
            startIntent.putExtra(Constants.OPERATION_KEY, OperationType.MSG_TO_DEVICE);
            startIntent.putExtra(Constants.MESSAGE_KEY,
                    JSON.writeValueAsString(socketMessage));
            startService(startIntent);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    private ResponseDto currencyRequest(String serviceCaller, TransactionDto transactionDto, char[] pin){
        MetadataDto currencyServer = app.getCurrencyService();
        ResponseDto responseDto = null;
        try {
            LOGD(TAG + ".currencyRequest", "amount: " + transactionDto.getAmount());
            String messageSubject = getString(R.string.currency_request_msg_subject);
            CurrencyRequestDto requestDto = CurrencyRequestDto.CREATE_REQUEST(transactionDto,
                    transactionDto.getAmount(), currencyServer.getEntity().getId());
            byte[] contentToSign =  JSON.writeValueAsBytes(requestDto);
            CMSSignedMessage cmsMessage = app.signCMSMessage(contentToSign);
            Map<String, String> mapToSend = new HashMap<>();
            mapToSend.put(Constants.CSR_FILE_NAME, JSON.writeValueAsString(requestDto.getRequestCSRSet()));
            mapToSend.put(Constants.CURRENCY_REQUEST_FILE_NAME, new String(cmsMessage.toPEM()));
            responseDto = HttpConn.getInstance().doPostMultipartRequest(mapToSend,
                    OperationType.CURRENCY_REQUEST.getUrl(currencyServer.getEntity().getId()));
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                ResultListDto<String> resultListDto = JSON.getMapper().readValue(
                        responseDto.getMessageBytes(), new TypeReference<ResultListDto<String>>(){});
                requestDto.loadCurrencyCerts(resultListDto.getResultList());
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                responseDto.setCaption(getString(R.string.currency_request_ok_caption)).setNotificationMessage(
                        getString(R.string.currency_request_ok_msg, requestDto.getTotalAmount(),
                        requestDto.getCurrencyCode()));
                Wallet.save(requestDto.getCurrencyMap().values(), pin);
                updateUserInfo(serviceCaller);
            } else responseDto.setCaption(getString(
                    R.string.currency_request_error_caption));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            app.broadcastResponse(
                    responseDto.setOperationType(OperationType.CURRENCY_REQUEST).setServiceCaller(serviceCaller));
            return responseDto;
        }
    }

    private ResponseDto sendTransaction(TransactionDto transactionDto) {
        LOGD(TAG + ".sendTransaction", "transactionDto: " + transactionDto.toString());
        ResponseDto responseDto = null;
        try {
            CMSSignedMessage cmsMessage = app.signCMSMessage(JSON.writeValueAsBytes(transactionDto));
            responseDto = HttpConn.getInstance().doPostRequest(cmsMessage.toPEM(),
                    ContentType.PKCS7_SIGNED,
                    OperationType.TRANSACTION_FROM_USER.getUrl(currencyServer.getEntity().getId()));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            return responseDto;
        }
    }

    private ResponseDto sendCurrencyBatch(TransactionDto transactionDto) {
        LOGD(TAG + ".sendCurrencyBatch", "sendCurrencyBatch");
        ResponseDto response = null;
        try {
            CurrencyBundle currencyBundle = Wallet.getCurrencyBundleForTag(
                    transactionDto.getCurrencyCode(), transactionDto.getTagName());
            CurrencyBatchDto requestDto = null;
            if(OperationType.CURRENCY_CHANGE == transactionDto.getOperation()) {
                transactionDto.setToUserIBAN(null);
                requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
                CMSSignedMessage cmsMessage = transactionDto.getCMSMessage();
                requestDto.setCurrencyChangeCSR(cmsMessage.getSignedContentStr());
            } else requestDto = currencyBundle.getCurrencyBatchDto(transactionDto);
            Operation operation = new Operation(transactionDto.getOperation(), requestDto,
                    Operation.State.PENDING);
            Uri operationUri = getContentResolver().insert(OperationContentProvider.CONTENT_URI,
                    OperationContentProvider.getContentValues(operation));
            response = HttpConn.getInstance().doPostRequest(JSON.writeValueAsBytes(requestDto),
                    ContentType.JSON, OperationType.GET_CURRENCY_TRANSACTION.getUrl(
                    currencyServer.getEntity().getId()));
            if(ResponseDto.SC_OK == response.getStatusCode()) {
                CurrencyBatchResponseDto responseDto = JSON.getMapper().readValue(
                        response.getMessageBytes(), CurrencyBatchResponseDto.class);
                //CMSSignedMessage cmsMessage = requestDto.validateResponse(responseDto,
                //        currencyServer.getTrustAnchors());
                response.setData(responseDto.getCurrencyChangeCert());
                Wallet.updateWallet(requestDto);
                operation.setState(Operation.State.FINISHED);
                getContentResolver().delete(operationUri, null, null);
            } else {
                operation.setState(Operation.State.ERROR).setStatusCode(response.getStatusCode())
                        .setMessage(response.getMessage());
                getContentResolver().update(operationUri, OperationContentProvider
                        .getContentValues(operation), null, null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            response = ResponseDto.EXCEPTION(ex, this);
        } finally {
            return response;
        }
    }

    private void updateUserInfo(String serviceCaller) {
        LOGD(TAG + ".updateUserInfo", "updateUserInfo");
        ResponseDto responseDto = null;
        try {
            String targetService = OperationType.CURRENCY_ACCOUNTS_INFO.getUrl(currencyServer.getEntity().getId());
            responseDto = HttpConn.getInstance().doGetRequest(targetService, ContentType.JSON);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                BalancesDto accountsInfo = JSON.getMapper().readValue(responseDto.getMessageBytes(),
                        BalancesDto.class);
                PrefUtils.putBalances(accountsInfo);
                TransactionContentProvider.updateUserTransactionList(app, accountsInfo);
            } else responseDto.setCaption(getString(R.string.error_lbl));
        } catch(Exception ex) {
            ex.printStackTrace();
            responseDto = ResponseDto.EXCEPTION(ex, this);
        } finally {
            if(ResponseDto.SC_OK == responseDto.getStatusCode())
                responseDto.setNotificationMessage(getString(R.string.currency_accounts_updated));
            responseDto.setServiceCaller(serviceCaller).setOperationType(OperationType.CURRENCY_ACCOUNTS_INFO);
            app.broadcastResponse(responseDto);
        }
    }

    private void checkCurrency(String serviceCaller, String revocationHash) {
        LOGD(TAG + ".checkCurrency", "checkCurrency");
        ResponseDto responseDto = null;
        try {
            Set<String> revocationHashSet = null;
            if(revocationHash != null) revocationHashSet = new HashSet<>(Arrays.asList(revocationHash));
            else revocationHashSet = Wallet.getRevocationHashSet();
            if(revocationHashSet == null) {
                LOGD(TAG + ".checkCurrency", "empty revocationHashSet nothing to check");
                return;
            }
            responseDto = HttpConn.getInstance().doPostRequest(
                    JSON.writeValueAsBytes(revocationHashSet), ContentType.JSON,
                    OperationType.BUNDLE_STATE.getUrl(currencyServer.getEntity().getId()));
            Set<CurrencyStateDto> currencyStateResponse = JSON.getMapper().readValue(
                    responseDto.getMessageBytes(), new TypeReference<Set<CurrencyStateDto>>() {});
            Set<CurrencyStateDto> currencyWithErrors = new HashSet<>();
            Set<String> currencyOKSet = new HashSet<>();
            for(CurrencyStateDto currencyDto: currencyStateResponse) {
                if(Currency.State.OK == currencyDto.getState()) {
                    currencyOKSet.add(currencyDto.getRevocationHash());
                } else currencyWithErrors.add(currencyDto);
            }
            if(!currencyOKSet.isEmpty()) {
                Wallet.updateCurrencyState(currencyOKSet, Currency.State.OK);
            }
            if(!currencyWithErrors.isEmpty()) {
                Set<Currency> removedSet = Wallet.removeErrors(currencyWithErrors);
                if(!removedSet.isEmpty()) {
                    responseDto = new ResponseDto(ResponseDto.SC_ERROR,
                            MsgUtils.getUpdateCurrencyWithErrorMsg(removedSet, app));
                    responseDto.setCaption(getString(R.string.error_lbl)).setServiceCaller(
                            serviceCaller).setOperationType(OperationType.CURRENCY_STATE);
                    app.broadcastResponse(responseDto);
                }
            } else {
                responseDto = new ResponseDto(ResponseDto.SC_OK).setServiceCaller(serviceCaller)
                        .setOperationType(OperationType.CURRENCY_STATE);
                app.broadcastResponse(responseDto);
            }
        } catch(Exception ex) {  ex.printStackTrace(); }
    }

}