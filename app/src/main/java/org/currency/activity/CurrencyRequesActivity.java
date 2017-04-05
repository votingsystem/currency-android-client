package org.currency.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.dto.TagDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.fragment.MessageDialogFragment;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.fragment.SelectTagDialogFragment;
import org.currency.service.PaymentService;
import org.currency.util.Constants;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.UIUtils;
import org.currency.util.Utils;

import java.math.BigDecimal;

import static org.currency.util.LogUtils.LOGD;


/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyRequesActivity extends AppCompatActivity {
	
	public static final String TAG = CurrencyRequesActivity.class.getSimpleName();

    public static final int RC_PASSW          = 0;

    private LinearLayout tag_info;
    private TextView tag_text;
    private TagDto tag;
    private TextView msgTextView;
    private TextView currency_text;
    private Button add_tag_btn;
    private Button submit_form_btn;
    private TextView errorMsgTextView;
    private String broadCastId = CurrencyRequesActivity.class.getSimpleName();
    private EditText amount;
    private CheckBox time_limited_checkbox;
    private BigDecimal maxValue;
    private BigDecimal defaultValue;
    private String currencyCode = null;
    private TransactionDto transactionDto = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            final ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            TagDto tag = (TagDto) intent.getSerializableExtra(Constants.TAG_KEY);
            if(tag != null)
                setTag(tag);
            if(responseDto != null){
                switch(responseDto.getOperationType()) {
                    case CURRENCY_REQUEST:
                        AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                responseDto.getCaption(), responseDto.getNotificationMessage(),
                                CurrencyRequesActivity.this);
                        builder.setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                                        CurrencyRequesActivity.this.setResult(Activity.RESULT_OK, null);
                                        finish();
                                    }
                                }
                            });
                        UIUtils.showMessageDialog(builder);
                        break;
                }
                setProgressDialogVisible(false, null, null);
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.currency_request_activity);
        UIUtils.setSupportActionBar(this);
        maxValue = (BigDecimal) getIntent().getSerializableExtra(Constants.MAX_VALUE_KEY);
        defaultValue = (BigDecimal) getIntent().getSerializableExtra(Constants.DEFAULT_VALUE_KEY);
        currencyCode = getIntent().getStringExtra(Constants.CURRENCY_KEY);
        tag_text = (TextView)findViewById(R.id.tag_text);
        tag_info = (LinearLayout)findViewById(R.id.tag_info);
        msgTextView = (TextView)findViewById(R.id.msg);
        amount = (EditText)findViewById(R.id.amount);
        if(defaultValue != null) amount.setText(defaultValue.toString());
        currency_text = (TextView)findViewById(R.id.currency_text);
        currency_text.setText(currencyCode);
        time_limited_checkbox = (CheckBox)findViewById(R.id.time_limited_checkbox);
        errorMsgTextView = (TextView)findViewById(R.id.errorMsg);
        add_tag_btn = (Button)findViewById(R.id.add_tag_btn);
        add_tag_btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if(tag == null) SelectTagDialogFragment.showDialog(broadCastId,
                        getSupportFragmentManager(), SelectTagDialogFragment.TAG);
                else setTag(null);
            }
        });
        submit_form_btn = (Button)findViewById(R.id.submit_form_btn);
        submit_form_btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                submitForm();
            }
        });
        amount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //if (actionId == EditorInfo.IME_ACTION_DONE) return submitForm();
                return false;
            }
        });

        if(savedInstanceState != null) {
            setTag((TagDto) savedInstanceState.getSerializable(Constants.TAG_KEY));
        }
        if(getIntent().getStringExtra(Constants.MESSAGE_KEY) == null) {
            msgTextView.setVisibility(View.GONE);
        } else {
            msgTextView.setVisibility(View.VISIBLE);
            msgTextView.setText(Html.fromHtml(getIntent().getStringExtra(Constants.MESSAGE_KEY)));
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.request_cash_lbl));
    }

    private boolean submitForm() {
        if(TextUtils.isEmpty(amount.getText().toString())) return true;
        BigDecimal selectedAmount = new BigDecimal(amount.getText().toString());
        if(selectedAmount.compareTo(maxValue) > 0) {
            MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.error_lbl),
                    getString(R.string.available_exceded_error_msg), getSupportFragmentManager());
        } else {
            if(selectedAmount.compareTo(new BigDecimal(0)) > 0) {
                if(tag == null) tag = new TagDto(TagDto.WILDTAG);
                transactionDto = TransactionDto.CURRENCY_REQUEST(selectedAmount,
                        currencyCode, tag, time_limited_checkbox.isChecked());
                Utils.launchPasswordInputActivity(RC_PASSW, MsgUtils.getCurrencyRequestMessage(
                        transactionDto, CurrencyRequesActivity.this), null, this);
            } else errorMsgTextView.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private void setTag(TagDto tag) {
        this.tag = tag;
        if(tag != null) {
            add_tag_btn.setText(getString(R.string.change_lbl));
            tag_text.setText(getString(R.string.selected_tag_lbl, tag.getName()));
            tag_info.setVisibility(View.VISIBLE);
        } else {
            add_tag_btn.setText(getString(R.string.add_tag_lbl));
            tag_info.setVisibility(View.GONE);
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    private void sendCurrencyRequest(char[] pin) {
        Intent startIntent = new Intent(this, PaymentService.class);
        startIntent.putExtra(Constants.OPERATION_KEY, OperationType.CURRENCY_REQUEST);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.PIN_KEY, pin);
        try {
            startIntent.putExtra(Constants.TRANSACTION_KEY, transactionDto);
        } catch (Exception ex) { ex.printStackTrace();}
        setProgressDialogVisible(true, getString(R.string.currency_request_msg_subject),
                MsgUtils.getCurrencyRequestMessage(transactionDto, this));
        startService(startIntent);
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_PASSW:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    sendCurrencyRequest(new String(responseDto.getMessageBytes()).toCharArray());
                }
                break;
        }
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(Constants.TAG_KEY, tag);
    }

}