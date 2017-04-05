package org.currency.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.util.Constants;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PasswordTypeSelectorActivity extends AppCompatActivity {

    public static final String TAG = PasswordTypeSelectorActivity.class.getSimpleName();

    public static final int RC_PATERN_LOCK = 0;
    public static final int RC_PIN         = 1;

    private OperationPassword operationPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crypto_device_access_mode_selector);
        final RadioGroup radio_group = (RadioGroup) findViewById(R.id.radio_group);
        RadioButton radio_pin = (RadioButton) findViewById(R.id.radio_pin);
        RadioButton radio_pattern = (RadioButton) findViewById(R.id.radio_pattern);
        RadioButton radio_dnie = (RadioButton) findViewById(R.id.radio_dnie);
        radio_dnie.setVisibility(View.GONE);
        UIUtils.setSupportActionBar(this, getString(R.string.crypto_device_access_mode_lbl));
        operationPassword = PrefUtils.getOperationPassword();
        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int radioButtonID = radio_group.getCheckedRadioButtonId();
                switch (radioButtonID) {
                    case R.id.radio_pin: {
                        if (operationPassword == null || operationPassword.getInputType() !=
                                OperationPassword.InputType.PIN) {
                            Intent intent = new Intent(PasswordTypeSelectorActivity.this,
                                    PinInputActivity.class);
                            intent.putExtra(Constants.STEP_KEY, getIntent().getExtras().getSerializable(Constants.STEP_KEY));
                            startActivityForResult(intent, RC_PIN);
                        }
                        finish();
                        break;
                    }
                    case R.id.radio_pattern:
                        if (operationPassword == null || operationPassword.getInputType() !=
                                OperationPassword.InputType.PATTER_LOCK) {
                            Intent intent = new Intent(PasswordTypeSelectorActivity.this,
                                    PatternLockInputActivity.class);
                            intent.putExtra(Constants.MODE_KEY, PatternLockInputActivity.MODE_CHANGE_PASSWORD);
                            intent.putExtra(Constants.PASSWORD_CONFIRM_KEY, true);
                            startActivityForResult(intent, RC_PATERN_LOCK);
                        } else finish();
                        break;
                    case R.id.radio_dnie:
                        PrefUtils.putOperationPassword(new OperationPassword(
                                OperationPassword.InputType.DNIE_PASSW, null));
                        setResult(Activity.RESULT_CANCELED, null);
                        finish();
                        break;
                    default:
                        LOGD(TAG, "OnClick - unknown radioButtonID: " + radioButtonID);
                }
            }
        });
        if (operationPassword != null) {
            switch (operationPassword.getInputType()) {
                case PATTER_LOCK:
                    radio_group.check(radio_pattern.getId());
                    break;
                case PIN:
                    radio_group.check(radio_pin.getId());
                    break;
                case DNIE_PASSW:
                    radio_group.check(radio_dnie.getId());
                    break;
            }
        } else radio_group.check(radio_dnie.getId());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

}
