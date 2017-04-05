package org.currency.service;

import android.app.IntentService;
import android.content.Intent;

import org.currency.App;
import org.currency.dto.metadata.MetadataDto;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SocketService extends IntentService {

    public static final String TAG = SocketService.class.getSimpleName();



    public SocketService() { super(TAG); }

    private App app;
    private MetadataDto currencyServer;

    @Override protected void onHandleIntent(Intent intent) {

    }

}
