package org.currency.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.currency.activity.ActivityBase;
import org.currency.activity.CurrencyRequesActivity;
import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.dto.currency.BalancesDto;
import org.currency.service.PaymentService;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.StringUtils;
import org.currency.util.UIUtils;
import org.currency.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAccountsFragment extends Fragment {

	public static final String TAG = CurrencyAccountsFragment.class.getSimpleName();

    private static final int CURRENCY_REQUEST   = 1;

    private View rootView;
    private String broadCastId = CurrencyAccountsFragment.class.getSimpleName();
    private TextView last_request_date;
    private TextView message;
    private RecyclerView accounts_container;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            switch(responseDto.getOperationType()) {
                case CURRENCY_ACCOUNTS_INFO:
                    if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                        loadUserInfo();
                    }
                    break;
                default: MessageDialogFragment.showDialog(responseDto.getStatusCode(),
                        responseDto.getCaption(), responseDto.getNotificationMessage(),
                        getFragmentManager());
            }
            if(ResponseDto.SC_FORBIDDEN == responseDto.getStatusCode()) {
                if(getActivity() instanceof ActivityBase) {
                    UIUtils.showConnectionRequiredDialog((ActivityBase) getActivity());
                }
            }
            setProgressDialogVisible(false, null, null);
        }
    };

    public static Fragment newInstance(Long userId) {
        CurrencyAccountsFragment fragment = new CurrencyAccountsFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.USER_KEY, userId);
        fragment.setArguments(args);
        return fragment;
    }

    public static Fragment newInstance(Bundle args) {
        CurrencyAccountsFragment fragment = new CurrencyAccountsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.currency_accounts, container, false);
        last_request_date = (TextView)rootView.findViewById(R.id.last_request_date);
        message = (TextView)rootView.findViewById(R.id.message);
        //https://developer.android.com/training/material/lists-cards.html
        accounts_container = (RecyclerView) rootView.findViewById(R.id.accounts_container);
        accounts_container.setHasFixedSize(true);
        accounts_container.setLayoutManager(new LinearLayoutManager(getActivity()));
        setHasOptionsMenu(true);
        loadUserInfo();
        if(getActivity().getIntent().getBooleanExtra(Constants.REFRESH_KEY, false))
            updateCurrencyAccountsInfo();
        return rootView;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        if(Activity.RESULT_OK == resultCode) {
            loadUserInfo();
        }
    }

    private void loadUserInfo() {
        message.setVisibility(View.GONE);
        Date lastCheckedTime = PrefUtils.getCurrencyAccountsLastCheckDate();
        if(lastCheckedTime != null) {
            try {
                last_request_date.setText(Html.fromHtml(getString(R.string.currency_last_request_info_lbl,
                        DateUtils.getDayWeekDateStr(lastCheckedTime, "HH:mm"))));
                BalancesDto userBalances = PrefUtils.getBalances();
                accounts_container.setAdapter(new AccountInfoAdapter(getActivity(), userBalances));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void showMessage(String msg) {
        message.setText(msg);
        message.setVisibility(View.VISIBLE);
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.currency_accounts, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case R.id.update_currency_accounts:
                updateCurrencyAccountsInfo();
                return true;
            case R.id.connect:
                Utils.launchSessionIndentificationActivity(getActivity(),
                        CurrencyAccountsPagerFragment.RC_IDENTIFICATE_CLIENT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setProgressDialogVisible(boolean isVisible, String caption, String message) {
        if(isVisible){
            ProgressDialogFragment.showDialog(caption, message, getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    private void updateCurrencyAccountsInfo() {
        LOGD(TAG + ".updateCurrencyAccountsInfo", "updateCurrencyAccountsInfo");
        Toast.makeText(getActivity(), getString(R.string.fetching_user_accounts_info_msg),
                Toast.LENGTH_SHORT).show();
        Intent startIntent = new Intent(getActivity(), PaymentService.class);
        startIntent.putExtra(Constants.OPERATION_KEY, OperationType.CURRENCY_ACCOUNTS_INFO);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        setProgressDialogVisible(true, getString(R.string.wait_msg),
                getString(R.string.fetching_user_accounts_info_msg));
        getActivity().startService(startIntent);
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

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public class AccountInfoAdapter extends RecyclerView.Adapter<AccountInfoAdapter.ViewHolder>{

        private final Context context;
        private BalancesDto userBalances;
        private List<String> currencyCodes;


        public class ViewHolder extends RecyclerView.ViewHolder {
            public View accountView;
            public ViewHolder(View accountView) {
                super(accountView);
                this.accountView = accountView;
            }
        }

        public AccountInfoAdapter(Context context, BalancesDto userBalances) {
            this.context = context;
            this.userBalances = userBalances;
            this.currencyCodes = new ArrayList<>(userBalances.getCurrencyCodes());
        }

        @Override public AccountInfoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                          int viewType) {
            View accountView = ((LayoutInflater) context.getSystemService(Context.
                    LAYOUT_INFLATER_SERVICE)).inflate(R.layout.currency_account_card, parent, false);
            return new ViewHolder(accountView);
        }

        @Override public void onBindViewHolder(ViewHolder holder, int position) {
            try {
                View accountView = holder.accountView;
                final String currencyCode = currencyCodes.get(position);
                GridView tag_container = (GridView) accountView.findViewById(R.id.tag_container);
                Map<String, BigDecimal> balancesCash = userBalances.getBalancesCash();
                if(balancesCash != null) {
                    TagInfoAdapter tagInfoAdapter = new TagInfoAdapter(context, balancesCash);
                    tag_container.setAdapter(tagInfoAdapter);
                } else showMessage(getString(R.string.cash_no_available_msg));
                final BigDecimal currencyBalance = balancesCash.get(currencyCode);
                TextView currency_text = (TextView) accountView.findViewById(R.id.currency_text);
                currency_text.setText(currencyBalance + " " + currencyCode);
                Button request_button = (Button)accountView.findViewById(R.id.cash_button);
                request_button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), CurrencyRequesActivity.class);
                        intent.putExtra(Constants.MAX_VALUE_KEY, currencyBalance);
                        intent.putExtra(Constants.CURRENCY_KEY, currencyCode);
                        intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.cash_dialog_msg,
                                currencyBalance, currencyCode));
                        startActivityForResult(intent, CURRENCY_REQUEST);
                    }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        @Override public int getItemCount() {
            return currencyCodes.size();
        }
    }


    public class TagInfoAdapter extends ArrayAdapter {

        private final Context context;
        private Map<String, BigDecimal> balancesCash;
        private List<String> currencyList;

        public TagInfoAdapter(Context context, Map<String, BigDecimal> balancesCash) {
            super(context, R.layout.currency_item_card, new ArrayList<>(balancesCash.keySet()));
            this.context = context;
            this.balancesCash = balancesCash;
            currencyList = new ArrayList<>(balancesCash.keySet());
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            String currencyCode = currencyList.get(position);
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View cardView = inflater.inflate(R.layout.currency_item_card, null);
            TextView total_amount = (TextView)cardView.findViewById(R.id.total_amount);
            total_amount.setText(balancesCash.get(currencyCode).setScale(2, BigDecimal.ROUND_DOWN).toString());
            TextView currency_symbol = (TextView)cardView.findViewById(R.id.currency_symbol);
            currency_symbol.setText(StringUtils.getCurrencySymbol(currencyCode));
            return cardView;
        }

    }

}