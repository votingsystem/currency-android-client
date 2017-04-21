package org.currency.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.dto.ResponseDto;
import org.currency.ui.DialogButton;
import org.currency.util.Constants;
import org.currency.util.PasswordInputStep;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PinInputActivity extends AppCompatActivity {

    private static final String TAG = PinInputActivity.class.getSimpleName();

    //we are here because we want to validate a signature request
    //public static final int MODE_VALIDATE_INPUT = 0;
    //we are here because we want to set/change the password
    //public static final int MODE_CHANGE_PASSWORD = 1;
    //private int requestMode;

    private TextView msgTextView;
    private EditText pinText;
    private PasswordInputStep inputStep;
    private String firstPin;
    private OperationPassword operationPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pinText = (EditText) findViewById(R.id.pin);
        msgTextView = (TextView) findViewById(R.id.msg);
        String operationCode = getIntent().getStringExtra(Constants.OPERATION_CODE_KEY);
        if(savedInstanceState == null) {
            inputStep = (PasswordInputStep) getIntent().getSerializableExtra(Constants.STEP_KEY);
        }
        if (operationCode != null) {
            TextView operationCodeText = (TextView) findViewById(R.id.operation_code);
            operationCodeText.setText(operationCode);
            operationCodeText.setVisibility(View.VISIBLE);
        }
        if (getIntent().getStringExtra(Constants.MESSAGE_KEY) != null) {
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(Constants.MESSAGE_KEY)));
        }
        operationPassword = PrefUtils.getOperationPassword();
        processPassword(null);
        pinText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String pin = pinText.getText().toString();
                    if (pin != null && pin.length() == 4) {
                        if (processPassword(pin)) {
                            //this is to make soft keyboard allways visible when input needed
                            new Handler().post(new Runnable() {
                                @Override
                                public void run() {
                                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                                            .showSoftInput(pinText, InputMethodManager.SHOW_FORCED);
                                }
                            });
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    //returns false when no more inputs required
    private boolean processPassword(final String passw) {
        switch (inputStep) {
            case PIN_REQUEST:
                getSupportActionBar().setTitle(R.string.pin_lbl);
                if (passw != null) {
                    if (!operationPassword.validateInput(passw, this)) {
                        msgTextView.setText(getString(R.string.pin_error_lbl));
                        pinText.setText("");
                        return true;
                    }
                } else return true;
                break;
            case PIN_WITH_VALIDATION_REQUEST:
                getSupportActionBar().setTitle(R.string.pin_lbl);
                if (passw != null) {
                    if (firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.repeat_password));
                        pinText.setText("");
                        return true;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            pinText.setText("");
                            msgTextView.setText(getString(R.string.password_mismatch));
                            return true;
                        }
                    }
                    if (!operationPassword.validateInput(passw, this)) {
                        msgTextView.setText(getString(R.string.pin_error_lbl));
                        pinText.setText("");
                        return true;
                    }
                } else return true;
                break;
            case NEW_PIN_REQUEST:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                msgTextView.setText(getString(R.string.enter_new_passw_to_app_msg));
                if(passw != null) {
                    if (firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        pinText.setText("");
                        return true;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            pinText.setText("");
                            return true;
                        } else {
                            PrefUtils.putOperationPassword(new OperationPassword(
                                    OperationPassword.InputType.PIN, passw.toCharArray()));
                            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finishOK(passw);
                                        }
                                    });
                            UIUtils.showMessageDialog(getString(R.string.change_password_lbl), getString(
                                    R.string.new_password_ok_msg), positiveButton, null, this);
                            return false;
                        }
                    }
                }
                return true;

        }
        finishOK(passw);
        return false;
    }

    private void finishOK(String passw) {
        Intent resultIntent = new Intent();
        ResponseDto responseDto = ResponseDto.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(Constants.RESPONSE_KEY, responseDto);
        resultIntent.putExtra(Constants.MODE_KEY, OperationPassword.InputType.PIN);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
