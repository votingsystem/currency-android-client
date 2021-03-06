package org.currency.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.currency.App;
import org.currency.activity.IdCardNFCReaderActivity;
import org.currency.debug.DebugAction;
import org.currency.util.Constants;

import static org.currency.util.LogUtils.LOGD;

public class NFCActivityAction implements DebugAction {
    private static final String TAG = NFCActivityAction.class.getSimpleName();

    private App appContext;

    public NFCActivityAction(App context) {
        this.appContext = context;
    }

    @Override
    public void run(final Context context, final Callback callback) {
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(appContext, IdCardNFCReaderActivity.class);
                //intent.putExtra(Constants.OPERATION_KEY, OperationType.CURRENCY_REQUEST);
                intent.putExtra(Constants.MESSAGE_CONTENT_KEY, "message content");
                intent.putExtra(Constants.MESSAGE_SUBJECT_KEY, "cms message subject");
                intent.putExtra(Constants.MESSAGE_KEY, "Do you want to sign the message?");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "Launch NFC Activity";
    }

}
