package org.currency.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.contentprovider.ReceiptContentProvider;
import org.currency.dto.ResponseDto;
import org.currency.dto.currency.CurrencyDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.http.ContentType;
import org.currency.model.ReceiptWrapper;
import org.currency.util.Constants;
import org.currency.util.ObjectUtils;
import org.currency.util.OperationType;
import org.currency.util.UIUtils;
import org.currency.util.Utils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptFragment extends Fragment {

    public static final String TAG = ReceiptFragment.class.getSimpleName();

    private App app;
    private ReceiptWrapper receiptWrapper;
    private TransactionDto transactionDto;
    private TextView receiptSubject;
    private WebView receipt_content;
    private CMSSignedMessage receiptWrapperCMS;
    private String broadCastId;
    private Menu menu;


    public static Fragment newInstance(int cursorPosition) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(String receiptURL, OperationType type) {
        ReceiptFragment fragment = new ReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(Constants.OPERATION_KEY, type);
        args.putString(Constants.URL_KEY, receiptURL);
        fragment.setArguments(args);
        return fragment;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            setProgressDialogVisible(null, null, false);
            MessageDialogFragment.showDialog(responseDto, getFragmentManager());
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        int cursorPosition =  getArguments().getInt(Constants.CURSOR_POSITION_KEY);
        broadCastId = ReceiptFragment.class.getSimpleName() + "_" + cursorPosition;
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.receipt_fragment, container, false);
        receipt_content = (WebView)rootView.findViewById(R.id.receipt_content);
        receiptSubject = (TextView)rootView.findViewById(R.id.receipt_subject);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        OperationType type = (OperationType) getArguments().getSerializable(Constants.OPERATION_KEY);
        receiptWrapper = (ReceiptWrapper) getArguments().getSerializable(Constants.RECEIPT_KEY);
        transactionDto = (TransactionDto) getArguments().getSerializable(Constants.TRANSACTION_KEY);
        if(transactionDto != null) receiptWrapper = new ReceiptWrapper(transactionDto);
        if(receiptWrapper != null) {
            if(receiptWrapper.hashReceipt())
                initReceiptScreen(receiptWrapper);
        }
        if(savedInstanceState != null) {
            receiptWrapper = (ReceiptWrapper) savedInstanceState.getSerializable(
                    Constants.RECEIPT_KEY);
            initReceiptScreen(receiptWrapper);
        } else {
            Integer cursorPosition =  getArguments().getInt(Constants.CURSOR_POSITION_KEY);
            Cursor cursor = getActivity().getContentResolver().query(
                    ReceiptContentProvider.CONTENT_URI, null, null, null, null);
            cursor.moveToPosition(cursorPosition);
            byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                    ReceiptContentProvider.SERIALIZED_OBJECT_COL));
            Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
            try {
                receiptWrapper = (ReceiptWrapper) ObjectUtils.
                        deSerializeObject(serializedReceiptContainer);
                receiptWrapper.setLocalId(receiptId);
                initReceiptScreen(receiptWrapper);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void initReceiptScreen (ReceiptWrapper receiptWrapper) {
        LOGD(TAG + ".initReceiptScreen", "type: " + receiptWrapper.getOperationType());
        try {
            String contentFormatted = "";
            String dateStr = null;
            receiptWrapperCMS = receiptWrapper.getReceipt();
            String receiptSubjectStr = null;
            switch(receiptWrapper.getOperationType()) {
                case CURRENCY_REQUEST:
                    CurrencyDto currencyDto = receiptWrapper.getReceipt()
                            .getSignedContent(CurrencyDto.class);
                    contentFormatted = getString(R.string.currency_request_formatted,
                            currencyDto.getAmount(), currencyDto.getCurrencyCode(),
                            currencyDto.getCurrencyServerURL());
                    break;
                default:
                    contentFormatted = receiptWrapper.getReceipt().getSignedContentStr();

            }
            receiptSubject.setText(receiptSubjectStr);
            contentFormatted = "<html><body style='background-color:#eeeeee;margin:0 auto;font-size:1.2em;'>" +
                    contentFormatted + "</body></html>";
            receipt_content.loadData(contentFormatted, "text/html; charset=UTF-8", null);
            ((AppCompatActivity)getActivity()).getSupportActionBar().setLogo(
                    UIUtils.getEmptyLogo(getActivity()));
            ((AppCompatActivity)getActivity()).setTitle(getString(R.string.receipt_lbl));
            setActionBarMenu(menu);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setActionBarMenu(Menu menu) {
        if(menu == null) {
            LOGD(TAG + ".setActionBarMenu", "menu null");
            return;
        }
        if(receiptWrapper == null || receiptWrapper.getOperationType() == null) {
            LOGD(TAG + ".receiptWrapper", "receiptWrapper undefined");
            return;
        }
        switch(receiptWrapper.getOperationType()) {
            default: LOGD(TAG + ".setActionBarMenu", "unprocessed type: " +
                    receiptWrapper.getOperationType());
        }
        if(receiptWrapper.getLocalId() < 0) menu.removeItem(R.id.delete_item);
        else menu.removeItem(R.id.save_receipt);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                receiptWrapper.getTypeDescription(getActivity()));
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(receiptWrapper != null) outState.putSerializable(Constants.RECEIPT_KEY,receiptWrapper);
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected receipt type:" +
                receiptWrapper.getOperationType());
        this.menu = menu;
        menuInflater.inflate(R.menu.receipt_fragment, menu);
        if(receiptWrapper != null) setActionBarMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        AlertDialog dialog = null;
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    break;
                case R.id.show_signers_info:
                    UIUtils.showCMSSignersInfoDialog(receiptWrapperCMS.getSigners(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.show_timestamp_info:
                    UIUtils.showTimeStampInfoDialog(receiptWrapperCMS.getSigner().getTimeStampToken(),
                            getFragmentManager(), getActivity());
                    break;
                case R.id.share_receipt:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.setType(ContentType.TEXT.getName());
                    sendIntent.putExtra(Intent.EXTRA_STREAM, Utils.createTempFile("Receipt",
                            receiptWrapperCMS.toPEM(), getActivity()));
                    startActivity(sendIntent);
                    return true;
                case R.id.save_receipt:
                    ContentValues values = new ContentValues();
                    values.put(ReceiptContentProvider.SERIALIZED_OBJECT_COL,
                            ObjectUtils.serializeObject(receiptWrapper));
                    values.put(ReceiptContentProvider.TYPE_COL, receiptWrapper.getOperationType().toString());
                    values.put(ReceiptContentProvider.STATE_COL, ReceiptWrapper.State.ACTIVE.toString());
                    menu.removeItem(R.id.save_receipt);
                    break;
                case R.id.signature_content:
                    try {
                        MessageDialogFragment.showDialog(ResponseDto.SC_OK, getString(
                                R.string.signature_content), receiptWrapperCMS.getSignedContentStr(),
                                getFragmentManager());
                    } catch(Exception ex) { ex.printStackTrace();}
                    break;
                case R.id.check_receipt:
                    LOGD(TAG + ".onOptionsItemSelected", " ====== TODO: " + receiptWrapper.getClass().getName());
                    return true;
                case R.id.delete_item:
                    dialog = new AlertDialog.Builder(getActivity()).setTitle(
                            getString(R.string.delete_receipt_lbl)).setMessage(Html.fromHtml(
                            getString(R.string.delete_receipt_msg, receiptWrapper.getSubject()))).
                            setPositiveButton(getString(R.string.ok_lbl), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().getContentResolver().delete(ReceiptContentProvider.
                                            getReceiptURI(receiptWrapper.getLocalId()), null, null);
                                    getActivity().onBackPressed();
                                }
                            }).setNegativeButton(getString(R.string.cancel_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }).show();
                    //to avoid avoid dissapear on screen orientation change
                    dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    return true;

            }
        } catch(Exception ex) { ex.printStackTrace();}
        return super.onOptionsItemSelected(item);
    }

}