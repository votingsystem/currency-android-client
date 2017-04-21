package org.currency.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.currency.App;
import org.currency.activity.SignAndSendActivity;
import org.currency.debug.DebugAction;
import org.currency.dto.OperationDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.OperationType;

import java.util.UUID;

import static org.currency.util.LogUtils.LOGD;

public class SignDocumentAndSendAction implements DebugAction {

    private static final String TAG = SignDocumentAndSendAction.class.getSimpleName();

    public SignDocumentAndSendAction() { }

    @Override
    public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override
            protected Void doInBackground(Context... contexts) {
                String targetURL = "https://voting.ddns.net/currency-server/api/test-pkcs7/sign";
                LOGD(TAG, "targetURL: " + targetURL);
                Intent intent = new Intent(App.getInstance(), SignAndSendActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                MetadataDto currencyService = App.getInstance().getCurrencyService();
                OperationDto operation = new OperationDto(new OperationTypeDto(
                        OperationType.CURRENCY_REQUEST,
                        currencyService.getEntity().getId())).setUUID(UUID.randomUUID().toString());
                try {
                    intent.putExtra(Constants.MESSAGE_CONTENT_KEY, JSON.getMapper().writeValueAsBytes(operation));
                    intent.putExtra(Constants.URL_KEY, targetURL);
                } catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }
                App.getInstance().startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override
    public String getLabel() {
        return "SIGN DOCUMENT AND SEND";
    }

}
