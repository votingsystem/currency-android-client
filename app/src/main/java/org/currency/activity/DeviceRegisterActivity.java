package org.currency.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.currency.App;
import org.currency.android.R;
import org.currency.crypto.CertificationRequest;
import org.currency.crypto.PEMUtils;
import org.currency.dto.ResponseDto;
import org.currency.dto.identity.RegisterDto;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.http.MediaType;
import org.currency.ui.DialogButton;
import org.currency.util.Constants;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;
import org.currency.util.Utils;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DeviceRegisterActivity extends AppCompatActivity {

    public static final String TAG = DeviceRegisterActivity.class.getSimpleName();

    public static final int REGISTER_RESULT_CODE = 0;

    private TextView captionTextView;
    private TextView messagenTextView;
    private CertificationRequest csrRequest;
    private Button confirm_button;

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

        try {
            Utils.launchIdCardSignatureActivity(null, getString(R.string.register_user_msg),
                    MediaType.JSON, IdCardNFCReaderActivity.Step.REGISTER_DEVICE,
                    REGISTER_RESULT_CODE, null, this);
        } catch (Exception ex) {
            ex.printStackTrace();
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), ex.getMessage(),
                    positiveButton, null, this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case REGISTER_RESULT_CODE:
                if(Activity.RESULT_OK == resultCode) {
                    LOGD(TAG, "onActivityResult - CertificationRequest: " +  data.getSerializableExtra(Constants.CERT_REQUEST_KEY));
                    csrRequest = (CertificationRequest) data.getSerializableExtra(Constants.CERT_REQUEST_KEY);
                    ResponseDto result = (ResponseDto) data.getParcelableExtra(Constants.RESPONSE_KEY);
                    new DeviceRegisterTask(result.getMessage(),
                    data.getStringExtra(Constants.USER_KEY)).execute();
                } else {
                    captionTextView.setText(getString(R.string.error_lbl));
                    messagenTextView.setText(getString(R.string.device_registered_error_msg));
                    confirm_button.setVisibility(View.VISIBLE);
                }
                break;
            default:
                LOGD(TAG, "onActivityResult - unknown requestCode: " + requestCode);
        }
    }

    public class DeviceRegisterTask extends AsyncTask<String, String, ResponseDto> {

        private String registerDoc;
        private String dnieCertPEM;

        public DeviceRegisterTask(String registerDoc, String dnieCertPEM) {
            this.registerDoc = registerDoc;
            this.dnieCertPEM = dnieCertPEM;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.sending_register_data_msg), getSupportFragmentManager());
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            ResponseDto response = HttpConn.getInstance().doPostRequest(
                    registerDoc.getBytes(), ContentType.PKCS7_SIGNED,
                    OperationType.REGISTER_DEVICE.getUrl(
                    App.getInstance().getCurrencyService().getFirstIdentityProvider()));
            return response;
        }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    RegisterDto registerDto = (RegisterDto) responseDto.getMessage(RegisterDto.class);
                    X509Certificate issuedCert = PEMUtils.fromPEMToX509Cert(registerDto.getIssuedCertificate().getBytes());
                    PrefUtils.putDNIeCert(dnieCertPEM);

                    KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                    keyStore.load(null);
                    PrivateKey privateKey = csrRequest.getPrivateKey();
                    X509Certificate[] certsArray = new X509Certificate[]{issuedCert};
                    keyStore.setKeyEntry(Constants.USER_CERT_ALIAS, privateKey, null, certsArray);

                    captionTextView.setText(getString(R.string.operation_ok_msg));
                    messagenTextView.setText(getString(R.string.device_registered_ok_msg));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    captionTextView.setText(getString(R.string.error_lbl));
                    messagenTextView.setText(ex.getMessage());
                }
            } else {
                captionTextView.setText(getString(R.string.error_lbl));
                messagenTextView.setText(responseDto.getMessage());
            }
            confirm_button.setVisibility(View.VISIBLE);
        }

    }

}