package org.currency.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.currency.App;
import org.currency.activity.FragmentContainerActivity;
import org.currency.android.R;
import org.currency.contentprovider.MessageContentProvider;
import org.currency.dto.MessageDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.ResponseDto;
import org.currency.model.Currency;
import org.currency.service.SocketService;
import org.currency.throwable.ValidationException;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.PasswordInputStep;
import org.currency.util.UIUtils;
import org.currency.util.Utils;
import org.currency.wallet.Wallet;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MessageFragment extends Fragment {

    public static final String TAG = MessageFragment.class.getSimpleName();

    private static final int CONTENT_VIEW_ID = 1000000;

    public static final int RC_OPEN_WALLET          = 0;

    private WeakReference<CurrencyFragment> currencyRef;
    private MessageDto socketMessage;
    private OperationType operationType;
    private Currency currency;
    private MessageContentProvider.State messageState;
    private TextView message_content;
    private LinearLayout fragment_container;
    private Long messageId;
    private String broadCastId;
    private Cursor cursor;


    public static Fragment newInstance(int cursorPosition) {
        MessageFragment fragment = new MessageFragment();
        Bundle args = new Bundle();
        args.putInt(Constants.CURSOR_POSITION_KEY, cursorPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.message_fragment, container, false);
        fragment_container = (LinearLayout)rootView.findViewById(R.id.fragment_container);
        message_content = (TextView)rootView.findViewById(R.id.message_content);
        int cursorPosition =  getArguments().getInt(Constants.CURSOR_POSITION_KEY);
        cursor = getActivity().getContentResolver().query(
                MessageContentProvider.CONTENT_URI, null, null, null, null);
        cursor.moveToPosition(cursorPosition);
        Long createdMillis = cursor.getLong(cursor.getColumnIndex(
                MessageContentProvider.TIMESTAMP_CREATED_COL));
        String dateInfoStr = DateUtils.getDayWeekDateStr(new Date(createdMillis), "HH:mm:ss");
        ((TextView)rootView.findViewById(R.id.date)).setText(dateInfoStr);
        try {
            socketMessage = JSON.readValue(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.JSON_COL)), MessageDto.class);
            messageId = cursor.getLong(cursor.getColumnIndex(MessageContentProvider.ID_COL));
            operationType =  OperationType.valueOf(cursor.getString(cursor.getColumnIndex(
                    MessageContentProvider.TYPE_COL)));
            switch (operationType) {
                case MSG_TO_DEVICE:
                    ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(
                            R.string.msg_lbl));
                    ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                            socketMessage.getDeviceFromName());
                    message_content.setText(socketMessage.getMessage());
                    break;
                case CURRENCY_WALLET_CHANGE:
                    loadCurrencyWalletChangeData();
                    break;
            }
            messageState =  MessageContentProvider.State.valueOf(cursor.getString(
                    cursor.getColumnIndex(MessageContentProvider.STATE_COL)));
            broadCastId = MessageFragment.class.getSimpleName() + "_" + cursorPosition;
            LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                    " - arguments: " + getArguments());
        } catch(Exception ex) { ex.printStackTrace(); }
        setHasOptionsMenu(true);
        //setUserVisibleHint is called before onCreateView
        setUserVisibleHint(getUserVisibleHint());
        return rootView;
    }

    @Override public void setUserVisibleHint(boolean isVisibleToUser) {
        LOGD(TAG + ".setUserVisibleHint", "setUserVisibleHint: " + isVisibleToUser +
                " - messageState: " + messageState);
        super.setUserVisibleHint(isVisibleToUser);
        try {
            if(isVisibleToUser) {
                if(messageState == MessageContentProvider.State.NOT_READED) {
                    getActivity().getContentResolver().update(MessageContentProvider.getMessageURI(
                            messageId), MessageContentProvider.getContentValues(
                            socketMessage.getOperation().getType(), JSON.writeValueAsString(socketMessage),
                            MessageContentProvider.State.READED), null, null);
                }
            }
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadCurrencyWalletChangeData() throws Exception {
        if(socketMessage != null && socketMessage.getCurrencySet() != null) {
            currency = socketMessage.getCurrencySet().iterator().next();
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(
                    R.string.wallet_change_lbl));
            ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(null);
            String fragmentTag = CurrencyFragment.class.getSimpleName() + messageId;
            message_content.setVisibility(View.GONE);
            if(currencyRef == null || currencyRef.get() == null) {
                fragment_container.removeAllViews();
                LinearLayout tempView = new LinearLayout(getActivity());
                tempView.setId(CONTENT_VIEW_ID + messageId.intValue());
                fragment_container.addView(tempView);
                currencyRef = new WeakReference<>(new CurrencyFragment());
                Bundle args = new Bundle();
                args.putSerializable(Constants.CURRENCY_KEY, currency);
                currencyRef.get().setArguments(args);
                getFragmentManager().beginTransaction().add(tempView.getId(),
                        currencyRef.get(), fragmentTag).commit();
            }
        } else LOGD(TAG + ".loadCurrencyWalletChangeData", "NULL DATA");
    }

    @Override public void onPause() {
        super.onPause();
        if(cursor != null && !cursor.isClosed()) cursor.close();
    }

    private void setProgressDialogVisible(String caption, String message, boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        LOGD(TAG + ".onCreateOptionsMenu", " selected message type:" + socketMessage.getOperation());
        menuInflater.inflate(R.menu.message_fragment, menu);
        switch (socketMessage.getOperation().getType()) {
            case MSG_TO_DEVICE:
                menu.setGroupVisible(R.id.message_items, true);
                break;
            case CURRENCY_WALLET_CHANGE:
                menu.setGroupVisible(R.id.currency_change_items, true);
                break;
        }
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                break;
            case R.id.send_message:
                Intent resultIntent = new Intent(getActivity(), FragmentContainerActivity.class);
                resultIntent.putExtra(Constants.FRAGMENT_KEY, MessageFormFragment.class.getName());
                resultIntent.putExtra(Constants.MESSAGE_KEY, socketMessage);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                return true;
            case R.id.delete_wallet_message:
            case R.id.delete_message:
                AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.delete_lbl),
                        getString(R.string.delete_confirm_msg), getActivity())
                        .setPositiveButton(getString(R.string.ok_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    MessageContentProvider.deleteById(messageId, getActivity());
                                    getActivity().onBackPressed();
                                }
                            });
                UIUtils.showMessageDialog(builder);
                return true;
            case R.id.save_to_wallet:
                updateWallet();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWallet() {
        LOGD(TAG + ".updateWallet", "updateWallet");
        if(Wallet.getCurrencySet() == null) {
            Utils.launchPasswordInputActivity(getString(R.string.enter_wallet_password_msg),
                    null, PasswordInputStep.PIN_REQUEST, RC_OPEN_WALLET,
                    (AppCompatActivity)getActivity());
        } else {
            try {
                MessageDto socketMessageDto = null;
                try {
                    Wallet.updateWallet(new HashSet(Arrays.asList(currency)));
                    String msg = getString(R.string.save_to_wallet_ok_msg, currency.getAmount().toString() + " " +
                            currency.getCurrencyCode());
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                            getString(R.string.save_to_wallet_lbl), msg, getActivity());
                    builder.setPositiveButton(getString(R.string.accept_lbl),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    getActivity().onBackPressed();
                                }
                            });
                    UIUtils.showMessageDialog(builder);

                    socketMessageDto = socketMessage.getResponse(ResponseDto.SC_OK, currency.getRevocationHash(),
                            App.getInstance().getSessionInfo().getSessionDevice().getUUID(),
                            null, new OperationTypeDto(OperationType.CURRENCY_WALLET_CHANGE, null));
                } catch (ValidationException ex) {
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.error_lbl), ex.getMessage(),
                            getFragmentManager());
                    socketMessageDto = socketMessage.getResponse(ResponseDto.SC_ERROR, ex.getMessage(),
                            App.getInstance().getSessionInfo().getSessionDevice().getUUID(),
                            null, new OperationTypeDto(OperationType.CURRENCY_WALLET_CHANGE,
                            App.getInstance().getCurrencyService().getEntity().getId()));
                }
                if(socketMessageDto != null) {
                    Intent startIntent = new Intent(getActivity(), SocketService.class);
                    startIntent.putExtra(Constants.OPERATION_KEY,
                            socketMessageDto.getOperation().getType());
                    startIntent.putExtra(Constants.MESSAGE_KEY,
                            JSON.writeValueAsString(socketMessageDto));
                    startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
                    getActivity().startService(startIntent);
                }
                MessageContentProvider.deleteById(messageId, getActivity());
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_OPEN_WALLET:
                try {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    Set<Currency> currencyList = Wallet.getCurrencySet(
                            new String(responseDto.getMessageBytes()).toCharArray());
                    if(currencyList != null) updateWallet();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                }
                break;
        }
    }

}