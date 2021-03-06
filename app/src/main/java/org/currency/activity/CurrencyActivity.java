package org.currency.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.currency.App;
import org.currency.android.R;
import org.currency.dto.DeviceDto;
import org.currency.dto.MessageDto;
import org.currency.dto.ResponseDto;
import org.currency.fragment.CurrencyFragment;
import org.currency.fragment.MessageDialogFragment;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.fragment.SelectDeviceDialogFragment;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.model.Currency;
import org.currency.service.SocketService;
import org.currency.ui.DialogButton;
import org.currency.util.ConnectionUtils;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.UIUtils;
import org.currency.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyActivity extends AppCompatActivity {
	
	public static final String TAG = CurrencyActivity.class.getSimpleName();

    private WeakReference<CurrencyFragment> currencyRef;
    private App app;
    private Currency currency;
    private String broadCastId = CurrencyActivity.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            MessageDto socketMsg = (MessageDto) intent.getSerializableExtra(Constants.MESSAGE_KEY);
            if(socketMsg != null){
                setProgressDialogVisible(false, null, null);
                switch(socketMsg.getOperation().getType()) {
                    case MSG_TO_DEVICE:
                        break;
                    case CURRENCY_WALLET_CHANGE:
                        if(ResponseDto.SC_OK == socketMsg.getStatusCode()) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.send_to_wallet),
                                    getString(R.string.item_sended_ok_msg),
                                    CurrencyActivity.this).setPositiveButton(getString(R.string.accept_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(CurrencyActivity.this, ActivityBase.class);
                                            intent.putExtra(Constants.FRAGMENT_KEY, R.id.wallet);
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                        } else MessageDialogFragment.showDialog(socketMsg.getStatusCode(),
                                getString(R.string.error_lbl), getString(R.string.device_not_found_error_msg),
                                getSupportFragmentManager());
                        break;
                    default:
                        LOGD(TAG + ".broadcastReceiver", "socketMsg: " + socketMsg.getOperation());
                }
            } else {
                setProgressDialogVisible(false, null, null);
                switch(responseDto.getOperationType()) {
                    case SELECT_DEVICE:
                        try {
                            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                                DeviceDto deviceTo = JSON.getMapper().readValue(
                                        responseDto.getMessageBytes(), DeviceDto.class);
                                MessageDto socketMessage = MessageDto.getCurrencyWalletChangeRequest(
                                        App.getInstance().getSessionInfo().getSessionDevice(),
                                        deviceTo, Arrays.asList(currency),
                                        App.getInstance().getCurrencyService().getEntity().getId());
                                Intent startIntent = new Intent(CurrencyActivity.this,  SocketService.class);
                                startIntent.putExtra(Constants.MESSAGE_KEY, JSON.writeValueAsString(socketMessage));
                                startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
                                startIntent.putExtra(Constants.OPERATION_KEY, OperationType.CURRENCY_WALLET_CHANGE);
                                setProgressDialogVisible(true, getString(R.string.send_to_wallet),
                                        getString(R.string.connecting_lbl));
                                startService(startIntent);
                                Toast.makeText(CurrencyActivity.this,
                                        getString(R.string.send_to_wallet) + " - " +
                                        getString(R.string.check_target_device_lbl),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch(Exception ex) {ex.printStackTrace();}
                        break;
                    default: MessageDialogFragment.showDialog(responseDto.getStatusCode(),
                            responseDto.getCaption(), responseDto.getNotificationMessage(),
                            getSupportFragmentManager());
                }
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
    	super.onCreate(savedInstanceState);
        app = (App) getApplicationContext();
        currency = (Currency) getIntent().getSerializableExtra(Constants.CURRENCY_KEY);
        setContentView(R.layout.fragment_container_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState == null) {
            currencyRef = new WeakReference<>(new CurrencyFragment());
            currencyRef.get().setArguments(Utils.intentToFragmentArguments(getIntent()));
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, currencyRef.get(),
                    CurrencyFragment.class.getSimpleName()).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //ConnectionUtils.onActivityResult(requestCode, resultCode, data, this);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    this.finish();
                    return true;
                case R.id.cert_info:
                    MessageDialogFragment.showDialog(null, getString(R.string.currency_cert_caption),
                            MsgUtils.getCertInfoMessage(currency.getCertificationRequest().
                                    getCertificate(), this), getSupportFragmentManager());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(currency.getReceipt().getSigner().
                            getTimeStampToken(), getSupportFragmentManager(), this);
                    break;
                case R.id.send_to_wallet:
                    if(!app.isSocketConnectionEnabled()) {
                        DialogButton positiveButton = new DialogButton(getString(R.string.connect_lbl),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ConnectionUtils.initConnection(CurrencyActivity.this);
                                    }
                                });
                        UIUtils.showMessageDialog(getString(R.string.send_to_wallet), getString(
                            R.string.send_to_wallet_connection_required_msg), positiveButton, null, this);
                    } else SelectDeviceDialogFragment.showDialog(broadCastId, this.
                            getSupportFragmentManager(), SelectDeviceDialogFragment.TAG);
                    break;
                case R.id.share_currency:
                    try {
                        Intent sendIntent = new Intent();
                        String receiptStr = currency.getReceipt().toPEMStr();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, receiptStr);
                        sendIntent.setType(ContentType.TEXT.getName());
                        startActivity(sendIntent);
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            if(caption == null) caption = getString(R.string.wait_msg);
            if(message == null) message = getString(R.string.loading_data_msg);
            ProgressDialogFragment.showDialog(caption, message, getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected model type:" + currency.getOperationType());
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.currency_fragment, menu);
        try {
            if(currency.getReceipt() == null) {
                menu.removeItem(R.id.show_timestamp_info);
                menu.removeItem(R.id.share_currency);
            }
            if(currency.getState() != Currency.State.OK) {
                menu.removeItem(R.id.send_to_wallet);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                broadcastReceiver, new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }

    public class CurrencyFetcher extends AsyncTask<String, String, ResponseDto> {

        public CurrencyFetcher() { }

        @Override protected void onPreExecute() { setProgressDialogVisible(true,
                getString(R.string.wait_msg), getString(R.string.loading_data_msg)); }

        @Override protected ResponseDto doInBackground(String... urls) {
            String currencyURL = urls[0];
            return HttpConn.getInstance().doGetRequest(currencyURL, null);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    currency.setReceiptBytes(responseDto.getMessageBytes());
                    if(currencyRef.get() != null) currencyRef.get().initCurrencyScreen(currency);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getSupportFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                        getString(R.string.error_lbl), responseDto.getMessage(),
                        getSupportFragmentManager());
            }
            setProgressDialogVisible(false, null, null);
        }
    }

}