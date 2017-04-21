package org.currency.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.dto.ResponseDto;
import org.currency.ui.DialogButton;
import org.currency.ui.PatternLockView;
import org.currency.util.Constants;
import org.currency.util.PasswordInputStep;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PatternLockInputActivity extends AppCompatActivity {

    private static final String TAG = PatternLockInputActivity.class.getSimpleName();


    private PatternLockView mCircleLockView;
    private TextView msgTextView;
    private PasswordInputStep inputStep;
    private String firstPin;
    private OperationPassword operationPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pattern_lock_activity);
        UIUtils.setSupportActionBar(this);
        mCircleLockView = (PatternLockView) findViewById(R.id.lock_view_circle);
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
        mCircleLockView.setCallBack(new PatternLockView.CallBack() {
            @Override
            public int onFinish(PatternLockView.Password password) {
                LOGD(TAG, "password length " + password.list.size());
                processPassword(password.string);
                return PatternLockView.CODE_PASSWORD_CORRECT;
            }
        });
        mCircleLockView.setOnNodeTouchListener(new PatternLockView.OnNodeTouchListener() {
            @Override
            public void onNodeTouched(int NodeId) {
                //LOGD(TAG, "node " + NodeId + " has touched!");
            }
        });
    }

    private void processPassword(final String passw) {
        switch (inputStep) {
            case PIN_REQUEST:
                getSupportActionBar().setTitle(R.string.pattern_lock_lbl);
                if (passw != null) {
                    if (!operationPassword.validateInput(passw, this)) {
                        msgTextView.setText(getString(R.string.pin_error_lbl));
                        return;
                    }
                } else return;
                break;
            case PIN_WITH_VALIDATION_REQUEST:
                getSupportActionBar().setTitle(R.string.pattern_lock_lbl);
                if (passw != null) {
                    if (firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.repeat_password));
                        return;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            msgTextView.setText(getString(R.string.password_mismatch));
                            return;
                        }
                    }
                    if (!operationPassword.validateInput(passw, this)) {
                        msgTextView.setText(getString(R.string.pin_error_lbl));
                        return;
                    }
                } else return;
                break;
            case NEW_PIN_REQUEST:
                getSupportActionBar().setTitle(R.string.change_password_lbl);
                if(passw != null) {
                    if (firstPin == null) {
                        firstPin = passw;
                        msgTextView.setText(getString(R.string.confirm_new_passw_msg));
                        return;
                    } else {
                        if (!firstPin.equals(passw)) {
                            firstPin = null;
                            msgTextView.setText(getString(R.string.new_password_error_msg));
                            return;
                        } else {
                            PrefUtils.putOperationPassword(new OperationPassword(
                                    OperationPassword.InputType.PATTER_LOCK, passw.toCharArray()));
                            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            finishOK(passw);
                                        }
                                    });
                            UIUtils.showMessageDialog(getString(R.string.change_password_lbl), getString(
                                    R.string.new_password_ok_msg), positiveButton, null, this);
                            return;
                        }
                    }
                }
                return;
        }
        finishOK(passw);
    }

    private void finishOK(String passw) {
        Intent resultIntent = new Intent();
        ResponseDto response = ResponseDto.OK().setMessageBytes(passw.getBytes());
        resultIntent.putExtra(Constants.RESPONSE_KEY, response);
        resultIntent.putExtra(Constants.MODE_KEY, OperationPassword.InputType.PATTER_LOCK);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

}
