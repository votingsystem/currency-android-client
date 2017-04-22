package org.currency.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.crypto.CertificationRequest;
import org.currency.dto.ResponseDto;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.util.Constants;
import org.currency.util.PasswordInputStep;
import org.currency.util.Utils;

import java.io.IOException;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignAndSendActivity extends AppCompatActivity {

    public static final String TAG = SignAndSendActivity.class.getSimpleName();

    public static final int REGISTER_RESULT_CODE = 0;

    private TextView captionTextView;
    private TextView messagenTextView;
    private CertificationRequest csrRequest;
    private Button confirm_button;
    private byte[] contentToSign;
    private String targetURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_activity);
        captionTextView = (TextView) findViewById(R.id.caption_text);
        messagenTextView = (TextView) findViewById(R.id.message_text);
        confirm_button = (Button) findViewById(R.id.confirm_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        contentToSign = getIntent().getExtras().getByteArray(Constants.MESSAGE_CONTENT_KEY);
        targetURL = getIntent().getExtras().getString(Constants.URL_KEY);
        Utils.launchPasswordInputActivity(null, null,
                PasswordInputStep.PIN_REQUEST, Constants.RC_REQUEST_OPERATION_PASSW, this);
    }

    //    public static void launchPasswordInputActivity(String message, String operationCode, PasswordInputStep step, Integer requestCode, Activity context)

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case Constants.RC_REQUEST_OPERATION_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    new SignAndSendTask(contentToSign, targetURL).execute();
                } else {
                    LOGD(TAG, "signature operation cancelled");
                    captionTextView.setText(getString(R.string.error_lbl));
                    messagenTextView.setText(R.string.operation_cancelled_msg);
                }
                break;
            default:
                LOGD(TAG, "onActivityResult - unknown requestCode: " + requestCode);
        }
    }

    public class SignAndSendTask extends AsyncTask<String, String, ResponseDto> {

        private byte[] contentToSign;
        private String targetURL;

        public SignAndSendTask(byte[] contentToSign, String targetURL) {
            this.contentToSign = contentToSign;
            this.targetURL = targetURL;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.wait_msg),
                    getString(R.string.sending_data_lbl), getSupportFragmentManager());
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            try {
                CMSSignedMessage cmsMessage = App.getInstance().signCMSMessage(contentToSign);
                return HttpConn.getInstance().doPostRequest(
                        cmsMessage.toPEM(), ContentType.PKCS7_SIGNED, targetURL);
            } catch (Exception ex) {
                ex.printStackTrace();
                return ResponseDto.ERROR(getString(R.string.error_lbl), ex.getMessage());
            }
        }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                captionTextView.setText(getString(R.string.operation_ok_msg));
                messagenTextView.setText(responseDto.getMessage());
            } else {
                captionTextView.setText(getString(R.string.error_lbl));
                try {
                    responseDto = responseDto.getErrorResponse();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                messagenTextView.setText(responseDto.getMessage());
            }
            confirm_button.setVisibility(View.VISIBLE);
        }

    }

}