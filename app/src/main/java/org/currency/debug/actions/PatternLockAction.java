package org.currency.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.currency.App;
import org.currency.activity.PatternLockInputActivity;
import org.currency.debug.DebugAction;
import org.currency.util.Constants;
import org.currency.util.PasswordInputStep;

import static org.currency.util.LogUtils.LOGD;

public class PatternLockAction implements DebugAction {

    private static final String TAG = PatternLockAction.class.getSimpleName();

    @Override
    public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(App.getInstance(), PatternLockInputActivity.class);
                intent.putExtra(Constants.MESSAGE_KEY, "Enter pattern lock");
                intent.putExtra(Constants.STEP_KEY, PasswordInputStep.PIN_REQUEST);
                intent.putExtra(Constants.OPERATION_CODE_KEY, "OP_CODE");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                App.getInstance().startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "Pattern lock view";
    }

}