package org.currency.debug.actions;

import android.content.Context;

import org.currency.debug.DebugAction;

public class PrefsAction implements DebugAction {

    private static final String TAG = PrefsAction.class.getSimpleName();

    @Override
    public void run(final Context context, final Callback callback) {
        /*PrefUtils.putCsrRequest(1L, null,context);
        PrefUtils.putPin("1234".toCharArray(), context);
        PrefUtils.putAppCertState(((App)context.getApplicationContext()).
                getAccessControl().getServerURL(), Constants.AppState.WITHOUT_CSR, null, context);*/
    }

    @Override
    public String getLabel() {
        return "Change PrefsUtil";
    }

}
