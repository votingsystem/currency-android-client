package org.currency.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.crypto.CertificationRequest;
import org.currency.dto.ResponseDto;
import org.currency.fragment.ProgressDialogFragment;

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

    }


    public class SignAndSendTask extends AsyncTask<String, String, ResponseDto> {

        private byte[] documentToSign;

        public SignAndSendTask(byte[] documentToSign) {
            this.documentToSign = documentToSign;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.wait_msg),
                    getString(R.string.sending_register_data_msg), getSupportFragmentManager());
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            ResponseDto response = null;
            return response;
        }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                captionTextView.setText(getString(R.string.operation_ok_msg));
                messagenTextView.setText("OK");
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