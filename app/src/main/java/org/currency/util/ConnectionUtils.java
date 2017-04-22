package org.currency.util;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import org.currency.activity.IdCardNFCReaderActivity;
import org.currency.android.R;
import org.currency.dto.MessageDto;
import org.currency.dto.OperationPassword;
import org.currency.dto.ResponseDto;
import org.currency.fragment.MessageDialogFragment;
import org.currency.service.SocketService;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ConnectionUtils {

    public static final String TAG = ConnectionUtils.class.getSimpleName();

    //high enough in order to avoid collisions with other request codes
    public static final int RC_INIT_CONNECTION_REQUEST = 1000;
    public static final int RC_PASSWORD_REQUEST        = 1001;

    private static MessageDto initSessionMessageDto;

    public static void initUnathenticatedConnection(AppCompatActivity activity, OperationType sessionType) {
        Intent startIntent = new Intent(activity, SocketService.class);
        //startIntent.putExtra(Constants.OPERATION_KEY, OperationType.WEB_SOCKET_INIT);
        startIntent.putExtra(Constants.SESSION_KEY, sessionType);
        activity.startService(startIntent);
    }

    public static void initConnection(final AppCompatActivity activity) {
        OperationPassword passwordAccessMode = PrefUtils.getOperationPassword();
        if(passwordAccessMode != null) {
            Utils.launchPasswordInputActivity(activity.getString(R.string.connection_passw_msg),
                    null, PasswordInputStep.PIN_REQUEST, RC_PASSWORD_REQUEST, activity);
        } else {
            launchNFC_IDCard(activity, null);
        }
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data,
                        AppCompatActivity activity) {
        LOGD(TAG, " --- onActivityResult --- requestCode: " + requestCode);
        if(data == null) return;
        ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
        switch (requestCode) {
            case RC_INIT_CONNECTION_REQUEST:
                /*if(responseDto != null && responseDto.getCMS() != null) {
                    try {
                        initSessionMessageDto = MessageDto.INIT_SIGNED_SESSION_REQUEST();
                        initSessionMessageDto.setCMS(responseDto.getCMS());
                        Intent startIntent = new Intent(activity, SocketService.class);
                        startIntent.putExtra(Constants.MESSAGE_KEY,
                                JSON.writeValueAsString(initSessionMessageDto));
                        activity.startService(startIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }*/
                break;
            case RC_PASSWORD_REQUEST:
                if(ResponseDto.SC_OK == responseDto.getStatusCode())
                    launchNFC_IDCard(activity, new String(responseDto.getMessageBytes()).toCharArray());
                break;
        }
    }

    private static void launchNFC_IDCard(AppCompatActivity activity, char[] accessModePassw) {
        try {
            Intent intent = new Intent(activity, IdCardNFCReaderActivity.class);
            /*MessageDto initSessionDto = MessageDto.REQUEST(PrefUtils.getDeviceId(),
                    HttpConn.getInstance().getSessionId(
                    StringUtils.getDomain(App.getInstance().getCurrencyServerURL())));
            intent.putExtra(Constants.MESSAGE_CONTENT_KEY, JSON.writeValueAsString(initSessionDto));
            intent.putExtra(Constants.MESSAGE_SUBJECT_KEY,
                    activity.getString(R.string.init_authenticated_session_msg_subject));*/
            intent.putExtra(Constants.PIN_KEY, accessModePassw);
            activity.startActivityForResult(intent, RC_INIT_CONNECTION_REQUEST);
        } catch (Exception ex) {
            MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, activity.getString(R.string.error_lbl),
                    ex.getMessage(), activity.getSupportFragmentManager());
        }
    }

}