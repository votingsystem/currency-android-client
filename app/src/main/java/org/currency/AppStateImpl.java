package org.currency;

import android.content.DialogInterface;
import android.content.Intent;

import org.currency.activity.DeviceRegisterActivity;
import org.currency.activity.PasswordTypeSelectorActivity;
import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.ui.DialogButton;
import org.currency.util.Constants;
import org.currency.util.PasswordInputStep;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum AppStateImpl implements AppState {

    START {
        @Override
        public boolean process(final android.app.Activity context) {
            if (App.getInstance().isRootedPhone()) {
                DialogButton positiveButton = new DialogButton(context.getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                context.finish();
                            }
                        });
                UIUtils.showMessageDialog(context.getString(R.string.msg_lbl), context.getString(
                        R.string.non_rooted_phones_required_msg), positiveButton, null, context);
                return false;
            }
            OperationPassword operationPassword = PrefUtils.getOperationPassword();
            if(operationPassword == null) {
                DialogButton positiveButton = new DialogButton(context.getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent(context, PasswordTypeSelectorActivity.class);
                                intent.putExtra(Constants.STEP_KEY, PasswordInputStep.NEW_PIN_REQUEST);
                                context.startActivity(intent);
                            }
                        });
                UIUtils.showMessageDialog(context.getString(R.string.msg_lbl), context.getString(
                        R.string.access_mode_passw_required_msg), positiveButton, null, context);
                return false;
            }

            if(App.getInstance().getDnieCertPEM() == null) {
                //device is not registered
                context.startActivity(new Intent(context, DeviceRegisterActivity.class));
                return false;
            } else {
                App.getInstance().setAppState(NOT_IDENTIFIED);
                return false;
            }
        }
    },
    NOT_IDENTIFIED {
        @Override
        public boolean process(android.app.Activity context) {
            LOGD(AppStateImpl.class.getSimpleName() + "AppStateImpl.WITHOUT_PASSWORD.process", "finished");
            return false;
        }
    },
    IDENTIFIED {
        @Override
        public boolean process(android.app.Activity context) {
            LOGD(AppStateImpl.class.getSimpleName() + "AppStateImpl.IDENTIFIED.process", "finished");
            return false;
        }
    }
}