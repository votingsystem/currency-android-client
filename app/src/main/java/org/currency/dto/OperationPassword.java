package org.currency.dto;

import android.support.v7.app.AppCompatActivity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.currency.android.R;
import org.currency.fragment.MessageDialogFragment;
import org.currency.throwable.ExceptionBase;
import org.currency.util.Constants;
import org.currency.util.HashUtils;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;

import java.io.Serializable;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperationPassword implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum InputType {PIN, PATTER_LOCK, DNIE_PASSW}

    private InputType inputType;
    private String hashBase64;

    public OperationPassword(InputType inputType, char[] passw) {
        try {
            this.inputType = inputType;
            if (passw != null)
                this.hashBase64 = HashUtils.getHashBase64(new String(passw).getBytes(),
                        Constants.DATA_DIGEST_ALGORITHM);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public InputType getInputType() {
        return inputType;
    }

    public boolean validateInput(String passw, AppCompatActivity activity) {
        int numRetries = -1;
        try {
            String passwHash = HashUtils.getHashBase64(passw.getBytes(), Constants.DATA_DIGEST_ALGORITHM);
            if (!hashBase64.equals(passwHash)) {
                numRetries = PrefUtils.incrementPasswordRetries();
                throw new ExceptionBase(activity.getString(R.string.password_error_msg) + ", " +
                        activity.getString(R.string.enter_password_retry_msg,
                                Integer.valueOf(Constants.NUM_MAX_PASSW_RETRIES - numRetries).toString()));
            }
            PrefUtils.resetPasswordRetries();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if ((Constants.NUM_MAX_PASSW_RETRIES - numRetries) == 0) {
                UIUtils.launchMessageActivity(ResponseDto.ERROR(activity.getString(
                        R.string.retries_exceeded_caption), null).setNotificationMessage(
                        activity.getString(R.string.retries_exceeded_msg)));
                activity.setResult(ResponseDto.SC_ERROR);
                activity.finish();
            } else
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, activity.getString(R.string.error_lbl),
                        ex.getMessage(), activity.getSupportFragmentManager());
            return false;
        }
    }
}
