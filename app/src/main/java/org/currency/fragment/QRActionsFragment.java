package org.currency.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.currency.App;
import org.currency.activity.ActivityBase;
import org.currency.activity.FragmentContainerActivity;
import org.currency.activity.IdCardNFCReaderActivity;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.crypto.Encryptor;
import org.currency.crypto.PEMUtils;
import org.currency.dto.MessageDto;
import org.currency.dto.OperationDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.QRMessageDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.identity.SessionCertificationDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.http.MediaType;
import org.currency.util.ActivityResult;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.QRUtils;
import org.currency.util.Utils;

import okhttp3.FormBody;
import okhttp3.RequestBody;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class QRActionsFragment extends Fragment {

    public static final String TAG = QRActionsFragment.class.getSimpleName();

    private enum Action {READ_QR, CREATE_QR, OPERATION, AUTHENTICATED_OPERATION}

    private static int RC_INIT_CERTIFICATION_SESSION   = 0;

    private String broadCastId = QRActionsFragment.class.getSimpleName();
    private QRMessageDto qrMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_actions_fragment, container, false);

        Button gen_qr_btn = (Button) rootView.findViewById(R.id.gen_qr_btn);
        gen_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processAction(Action.CREATE_QR);
            }
        });

        Button read_qr_btn = (Button) rootView.findViewById(R.id.read_qr_btn);
        read_qr_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Utils.launchQRScanner(QRActionsFragment.this);
            }
        });
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_codes_lbl));
        setHasOptionsMenu(true);
        if(savedInstanceState != null) {
            qrMessage = (QRMessageDto) savedInstanceState.getSerializable(
                    Constants.QR_CODE_KEY);
        }
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.QR_CODE_KEY, qrMessage);
    }

    private void processAction(Action action) {
        switch (action) {
            case READ_QR:
                Utils.launchQRScanner(this);
                break;
            case CREATE_QR:
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(Constants.FRAGMENT_KEY, TransactionFormFragment.class.getName());
                intent.putExtra(Constants.OPERATION_KEY, TransactionFormFragment.Type.QR_FORM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case AUTHENTICATED_OPERATION:
            case OPERATION:
                //processQRCode(qrMessageDto);
                break;
        }
    }

    private void processQRCode(QRMessageDto qrMessageDto) {
        try {
            MessageDto socketMessage = null;
            /*switch (qrMessageDto.getOperation()) {
                case GENERATE_BROWSER_CERTIFICATE:
                    if(!App.getInstance().isWithSocketConnection()) {
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(null,
                                getString(R.string.connection_required_msg),
                                getActivity()).setPositiveButton(getString(R.string.connect_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        pendingAction = Action.AUTHENTICATED_OPERATION;
                                        if(getActivity() != null) ConnectionUtils.initConnection(
                                                ((ActivityBase)getActivity()));
                                        dialog.cancel();
                                    }
                                });
                        UIUtils.showMessageDialog(builder);
                    } else {
                        qrMessageDto.setAesParams(AESParamsDto.CREATE());
                        socketMessage = SocketMessageDto.getQRInfoRequest(qrMessageDto, true);
                        sendSocketMessage(socketMessage, qrMessageDto.getSessionType());
                    }
                    break;
                case OPERATION_PROCESS:
                    if(App.getInstance().getConnectedDevice() != null) {
                        socketMessage = SocketMessageDto.getQRInfoRequest(qrMessageDto, false);
                        sendSocketMessage(socketMessage, qrMessageDto.getSessionType());
                    } else {
                        pendingAction = Action.OPERATION;
                        this.qrMessageDto = qrMessageDto;
                        ConnectionUtils.initUnathenticatedConnection((ActivityBase)getActivity(),
                                qrMessageDto.getSessionType());
                    }
                    break;

                default: LOGD(TAG, "processQRCode: " + qrMessageDto.getOperation());
            }*/
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if(requestCode == RC_INIT_CERTIFICATION_SESSION) {
            if(Activity.RESULT_OK == resultCode) {
                ResponseDto signatureResponse = data.getParcelableExtra(Constants.RESPONSE_KEY);
                new RequestCertsTask(signatureResponse.getMessageBytes()).execute();
            }
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null && result.getContents() != null) {
                String qrMessageStr = result.getContents();
                LOGD(TAG, "onActivityResult - qrMessage: " + qrMessageStr);
                qrMessage = QRMessageDto.FROM_QR_CODE(qrMessageStr);
                if (qrMessage.getOperation() != null) {
                    new GetQRCodeTask(qrMessage).execute();
                } else {
                    new GetQRCodeTask(qrMessage).execute();
                }
            }
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    private void setProgressDialogVisible(final boolean isVisible, String caption, String message) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(caption, message, broadCastId, getFragmentManager());
        } else ProgressDialogFragment.hide(broadCastId, getFragmentManager());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityResult activityResult = ((ActivityBase) getActivity()).getActivityResult();
        if (activityResult != null) {
            onActivityResult(activityResult.getRequestCode(),
                    activityResult.getResultCode(), activityResult.getData());
        }
    }

    public class GetQRCodeTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = GetQRCodeTask.class.getSimpleName();

        private QRMessageDto qrMessage;

        public GetQRCodeTask(QRMessageDto qrMessage) {
            this.qrMessage = qrMessage;
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.wait_msg),
                    getString(R.string.loading_data_msg));
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            ResponseDto result = null;
            MetadataDto entityMetadata = App.getInstance().getSystemEntity(
                    qrMessage.getSystemEntityId(), true);
            try {
                switch (qrMessage.getOperation()) {
                    case QRUtils.GET_BROWSER_CERTIFICATE:
                        RequestBody formBody = new FormBody.Builder()
                                .add("UUID", qrMessage.getUUID())
                                .add("operation", qrMessage.getOperation()).build();
                        result = HttpConn.getInstance().doPostForm(formBody,
                                OperationType.QR_INFO.getUrl(qrMessage.getSystemEntityId()));
                        SessionCertificationDto browserPublicKey = JSON.getMapper().readValue(
                                result.getMessageBytes(), SessionCertificationDto.class);
                        App.getInstance().setBrowserPublicKey(PEMUtils.fromPEMToRSAPublicKey(
                                browserPublicKey.getPublicKeyPEM().getBytes()));
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG, "onPostExecute - statusCode: " + response.getStatusCode());
            try {
                if(ResponseDto.SC_OK == response.getStatusCode()) {
                    signDocument(null, getString(R.string.init_session_msg), MediaType.JSON,
                            IdCardNFCReaderActivity.Step.INIT_SESSION, RC_INIT_CERTIFICATION_SESSION);
                }
            } catch (Exception ex) {
                MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                        ex.getMessage(), getFragmentManager());
            }
        }
    }


    public class RequestCertsTask extends AsyncTask<String, Void, ResponseDto> {

        public final String TAG = RequestCertsTask.class.getSimpleName();

        private byte[] sessionCertificationRequest;

        public RequestCertsTask(byte[] sessionCertificationRequest) {
            this.sessionCertificationRequest = sessionCertificationRequest;
        }

        @Override
        protected void onPreExecute() {
            setProgressDialogVisible(true, getString(R.string.wait_msg),
                    getString(R.string.loading_data_msg));
        }

        @Override
        protected ResponseDto doInBackground(String... urls) {
            ResponseDto response = null;
            try {
                response = HttpConn.getInstance().doPostRequest(
                        sessionCertificationRequest, ContentType.PKCS7_SIGNED,
                        OperationType.SESSION_CERTIFICATION.getUrl(
                        App.getInstance().getCurrencyService().getFirstIdentityProvider()));
                CMSSignedMessage sessionCertification = CMSSignedMessage.FROM_PEM(response.getMessageBytes());
                SessionCertificationDto certificationResponse = JSON.getMapper().readValue(
                        sessionCertification.getSignedContentStr(), SessionCertificationDto.class);

                App.getInstance().getSessionInfo().loadIssuedCerts(certificationResponse);
                MessageDto messageDto = new MessageDto();
                //messageDto.setOperation(OperationType.MSG_TO_DEVICE).setDeviceToUUID(qrMessage.getUUID());

                MessageDto messageContent = new MessageDto();
                messageContent.setOperation(new OperationTypeDto(
                        OperationType.SESSION_CERTIFICATION, null)).setMessage("");
                SessionCertificationDto certificationDto =
                        App.getInstance().getSessionInfo().buildBrowserCertificationDto();
                String base64Data = Base64.encodeToString(JSON.getMapper()
                        .writeValueAsString(certificationDto).getBytes(), Base64.NO_WRAP);
                messageContent.setBase64Data(base64Data);
                byte[] encryptedMessage = Encryptor.encryptToCMS(
                        JSON.getMapper().writeValueAsBytes(messageContent),
                        App.getInstance().getBrowserPublicKey());
                messageDto.setEncryptedMessage(new String(encryptedMessage));

                RequestBody formBody = new FormBody.Builder()
                        .add("browserUUID", certificationDto.getBrowserUUID())
                        .add("cmsMessage", response.getMessage())
                        .add("socketMsg", JSON.getMapper().writeValueAsString(messageDto)).build();
                ResponseDto responseDto = HttpConn.getInstance().doPostForm(formBody,
                        OperationType.SESSION_CERTIFICATION_DATA.getUrl(
                        App.getInstance().getCurrencyService().getEntity().getId()));
                LOGD(TAG, "status: " + responseDto.getStatusCode() + " - " + responseDto.getMessage() );
                if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                    //var msgDto = {operation:{type:"SESSION_CERTIFICATION"}, httpSessionId:sessionId, userUUID:vs.userUUID};

                    //OperationType operationType = new OperationType();
                    //OperationType type, String entityId
                    OperationTypeDto operationType = new OperationTypeDto(
                            OperationType.SESSION_CERTIFICATION,
                            App.getInstance().getCurrencyService().getEntity().getId());
                    OperationDto operation = new OperationDto(operationType).setUserUUID(
                            certificationResponse.getMobileUUID());
                    byte[] signedContent = App.getInstance().getSessionInfo().getBrowserCsrReq()
                        .signDataWithTimeStamp(JSON.writeValueAsBytes(operation) , MediaType.JSON);
                    LOGD(TAG, "signed-content: " + new String(signedContent));

                    responseDto = HttpConn.getInstance().doPostRequest(signedContent,
                            ContentType.JSON, OperationType.INIT_MOBILE_SESSION.getUrl(
                            App.getInstance().getCurrencyService().getEntity().getId()));

                } else {
                    MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                            responseDto.getMessage(), getFragmentManager());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(ResponseDto response) {
            LOGD(TAG, "onPostExecute - statusCode: " + response.getStatusCode());
            setProgressDialogVisible(false, null, null);
        }

    }

    private void signDocument(byte[] documentToSign, String messagePrefix, String contentType,
            IdCardNFCReaderActivity.Step step, int activityResultCode) throws Exception {
        Intent intent = new Intent(getActivity(), IdCardNFCReaderActivity.class);
        intent.putExtra(Constants.MESSAGE_KEY, messagePrefix);
        intent.putExtra(Constants.MESSAGE_CONTENT_KEY, documentToSign);
        intent.putExtra(Constants.STEP_KEY, step);
        intent.putExtra(Constants.CONTENT_TYPE_KEY, contentType);
        if (qrMessage != null && qrMessage.getUUID() != null) {
            intent.putExtra(Constants.QR_CODE_KEY, qrMessage.getUUID().substring(0, 4));
        }
        startActivityForResult(intent, activityResultCode);
    }

}