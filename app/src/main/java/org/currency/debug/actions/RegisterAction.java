package org.currency.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import org.currency.activity.DeviceRegisterActivity;
import org.currency.debug.DebugAction;

import static org.currency.util.LogUtils.LOGD;

public class RegisterAction implements DebugAction {

    private static final String TAG = RegisterAction.class.getSimpleName();

    public RegisterAction() { }

    @Override
    public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground - registering device");
                context.startActivity(new Intent(context, DeviceRegisterActivity.class));
                return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "Register device";
    }

}
