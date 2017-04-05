package org.currency.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.util.Constants;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageActivity extends AppCompatActivity {

    public static final String TAG = MessageActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_activity);
        /*((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(
                App.SIGN_AND_SEND_SERVICE_NOTIFICATION_ID);*/
        ResponseDto responseDto = getIntent().getParcelableExtra(Constants.RESPONSE_KEY);
        ((TextView) findViewById(R.id.caption_text)).setText(responseDto.getCaption());
        ((TextView) findViewById(R.id.message_text)).setText(Html.fromHtml(
                responseDto.getNotificationMessage()));
        ProgressDialogFragment.hide(getSupportFragmentManager());
    }

}