package org.currency.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.currency.activity.CurrencyRequesActivity;
import org.currency.activity.FragmentContainerActivity;
import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.dto.currency.BalancesDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.model.Currency;
import org.currency.service.PaymentService;
import org.currency.util.Constants;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;
import org.currency.util.Utils;
import org.currency.util.Wallet;

import java.math.BigDecimal;
import java.util.Set;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PaymentFragment extends Fragment {

	public static final String TAG = PaymentFragment.class.getSimpleName();

    private static final int RC_OPEN_WALLET       = 0;
    private static final int RC_CURRENCY_REQUEST  = 1;
    private static final int RC_SEND_TRANSACTION  = 2;

    private String broadCastId = PaymentFragment.class.getSimpleName();
    private TextView receptor;
    private TextView subject;
    private TextView amount;
    private TextView tag;
    private TransactionDto transactionDto;
    private Spinner payment_method_spinner;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            setProgressDialogVisible(false);
            String caption = ResponseDto.SC_OK == responseDto.getStatusCode()? getString(
                    R.string.payment_ok_caption):getString(R.string.error_lbl);
            getActivity().finish();
            UIUtils.launchMessageActivity(ResponseDto.SC_ERROR, responseDto.getMessage(), caption);
        }
    };

    private void launchTransaction(OperationType operationType) {
        LOGD(TAG + ".launchTransaction() ", "launchTransaction");
        Intent startIntent = new Intent(getActivity(), PaymentService.class);
        try {
            transactionDto.setOperation(operationType);
            startIntent.putExtra(Constants.TRANSACTION_KEY, transactionDto);
            startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
            startIntent.putExtra(Constants.OPERATION_KEY, transactionDto.getOperation());
            getActivity().startService(startIntent);
            setProgressDialogVisible(true);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        // if set to true savedInstanceState will be allways null
        setHasOptionsMenu(true);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.payment_fragment, container, false);
        getActivity().setTitle(getString(R.string.payment_lbl));
        receptor = (TextView)rootView.findViewById(R.id.receptor);
        subject = (TextView)rootView.findViewById(R.id.subject);
        amount= (TextView)rootView.findViewById(R.id.amount);
        tag = (TextView)rootView.findViewById(R.id.tagvs);
        payment_method_spinner = (Spinner)rootView.findViewById(R.id.payment_method_spinner);
        try {
            transactionDto = (TransactionDto) getArguments().getSerializable(Constants.TRANSACTION_KEY);
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(getActivity(),
                    R.layout.payment_spinner_item,
                    TransactionDto.getPaymentMethods(transactionDto.getPaymentOptions()));
            dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            payment_method_spinner.setAdapter(dataAdapter);
            receptor.setText(transactionDto.getToUserName());
            subject.setText(transactionDto.getSubject());
            amount.setText(transactionDto.getAmount().toString() + " " + transactionDto.getCurrencyCode());
            String tagvsInfo = getString(R.string.selected_tag_lbl,
                    MsgUtils.getTagMessage(transactionDto.getTagName()));
            if(transactionDto.isTimeLimited()) tagvsInfo = tagvsInfo + " " +
                    getString(R.string.time_remaining_tagvs_info_lbl);
            tag.setText(tagvsInfo);
            if(transactionDto.getOperation() != null) {
                switch(transactionDto.getOperation()) {
                    case DELIVERY_WITH_PAYMENT:
                    case DELIVERY_WITHOUT_PAYMENT:
                        UIUtils.fillAddressInfo((LinearLayout)rootView.findViewById(R.id.address_info));
                        break;
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        Button save_button = (Button) rootView.findViewById(R.id.save_button);
        save_button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                submitForm();
            }
        });
        return rootView;
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
		LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
		switch (item.getItemId()) {
	    	case android.R.id.home:
                getActivity().onBackPressed();
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
		}
	}

    private void submitForm() {
        try {
            transactionDto.setOperation(TransactionDto.getByDescription(
                    (String) payment_method_spinner.getSelectedItem()));
            BalancesDto userInfo = PrefUtils.getBalances();
            final BigDecimal availableForTagVS = userInfo.getAvailableForTag(
                    transactionDto.getCurrencyCode(), transactionDto.getTag().getName());
            switch (transactionDto.getOperation()) {
                case TRANSACTION_FROM_USER:
                    try {
                        if(availableForTagVS.compareTo(transactionDto.getAmount()) < 0) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_cash_msg,
                                            transactionDto.getCurrencyCode(),
                                            transactionDto.getAmount().toString(),
                                            availableForTagVS.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.check_available_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(Constants.REFRESH_KEY, true);
                                            intent.putExtra(Constants.FRAGMENT_KEY,
                                                    CurrencyAccountsFragment.class.getName());
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                            return;
                        } else {
                            Utils.launchPasswordInputActivity(RC_SEND_TRANSACTION,
                                    MsgUtils.getTransactionConfirmMessage(transactionDto, getActivity()),
                                    null, (AppCompatActivity)getActivity());
                        }
                    } catch(Exception ex) { ex.printStackTrace();}
                    break;
                case CURRENCY_CHANGE:
                case CURRENCY_SEND:
                    if(Wallet.getCurrencySet() == null) {
                        Utils.launchPasswordInputActivity(RC_OPEN_WALLET,
                                getString(R.string.enter_wallet_password_msg),
                                null, (AppCompatActivity)getActivity());
                        return;
                    }
                    final BigDecimal availableForTagVSWallet = Wallet.getAvailableForTag(
                            transactionDto.getCurrencyCode(), transactionDto.getTag().getName());
                    if(availableForTagVSWallet.compareTo(transactionDto.getAmount()) < 0) {
                        final BigDecimal amountToRequest = transactionDto.getAmount().subtract(
                                availableForTagVSWallet);
                        if(availableForTagVSWallet.compareTo(amountToRequest) < 0) {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_anonymous_money_msg,
                                            transactionDto.getCurrencyCode(),
                                            availableForTagVSWallet.toString(),
                                            amountToRequest.toString(),
                                            availableForTagVSWallet.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.check_available_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    FragmentContainerActivity.class);
                                            intent.putExtra(Constants.REFRESH_KEY, true);
                                            intent.putExtra(Constants.FRAGMENT_KEY,
                                                    CurrencyAccountsFragment.class.getName());
                                            startActivity(intent);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                            return;
                        } else {
                            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                                    getString(R.string.insufficient_cash_caption),
                                    getString(R.string.insufficient_anonymous_cash_msg,
                                            transactionDto.getCurrencyCode(),
                                            transactionDto.getAmount().toString(),
                                            availableForTagVSWallet.toString()), getActivity());
                            builder.setPositiveButton(getString(R.string.request_cash_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Intent intent = new Intent(getActivity(),
                                                    CurrencyRequesActivity.class);
                                            intent.putExtra(Constants.MAX_VALUE_KEY,
                                                    availableForTagVS);
                                            intent.putExtra(Constants.DEFAULT_VALUE_KEY,
                                                    amountToRequest);
                                            intent.putExtra(Constants.CURRENCY_KEY,
                                                    transactionDto.getCurrencyCode());
                                            intent.putExtra(Constants.MESSAGE_KEY,
                                                    getString(R.string.cash_for_payment_dialog_msg,
                                                    transactionDto.getCurrencyCode(),
                                                    amountToRequest.toString(),
                                                    availableForTagVS.toString()));
                                            startActivityForResult(intent, RC_CURRENCY_REQUEST);
                                        }
                                    });
                            UIUtils.showMessageDialog(builder);
                        }
                    } else {
                        launchTransaction(transactionDto.getOperation());
                    }
                    break;
            }
        } catch(Exception ex) { ex.printStackTrace();}
    }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.sending_payment_lbl),
                    getString(R.string.wait_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_OPEN_WALLET:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    try {
                        Set<Currency> currencySet = Wallet.getCurrencySet(
                                new String(responseDto.getMessageBytes()).toCharArray());
                        submitForm();
                    } catch(Exception ex) { ex.printStackTrace(); }
                }
                break;
            case RC_CURRENCY_REQUEST:
                if(Activity.RESULT_OK == resultCode) {
                    submitForm();
                }
                break;
            case RC_SEND_TRANSACTION:
                if(Activity.RESULT_OK == resultCode) {
                    launchTransaction(transactionDto.getOperation());
                }
                break;
        }

    }

}