package org.currency.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.currency.App;
import org.currency.activity.CurrencyActivity;
import org.currency.android.R;
import org.currency.dto.ResponseDto;
import org.currency.model.Currency;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.MsgUtils;
import org.currency.util.PasswordInputStep;
import org.currency.util.Utils;
import org.currency.wallet.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletFragment extends Fragment {

    public static final String TAG = WalletFragment.class.getSimpleName();

    public static final int RC_OPEN_WALLET          = 0;

    private View rootView;
    private GridView gridView;
    private CurrencyListAdapter adapter = null;
    private List<Currency> currencyList;
    private String broadCastId = WalletFragment.class.getSimpleName();
    private Menu menu;
    private boolean walletLoaded = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        LOGD(TAG + ".broadcastReceiver", "extras:" + intent.getExtras());
        ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
        switch(responseDto.getOperationType()) {
            case CURRENCY_STATE:
                currencyList = new ArrayList<>(Wallet.getCurrencySet());
                printSummary();
                if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.error_lbl), responseDto.getMessage(),
                            getFragmentManager());
                }
                break;
            case CURRENCY_ACCOUNTS_INFO:
                break;
        }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getActivity().setTitle(getString(R.string.wallet_lbl));
        rootView = inflater.inflate(R.layout.wallet_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent intent = new Intent(getActivity(), CurrencyActivity.class);
                intent.putExtra(Constants.CURRENCY_KEY, currencyList.get(position));
                startActivity(intent);
            }
        });
        if(Wallet.getCurrencySet() == null) {
            currencyList = new ArrayList<>();
            Utils.launchPasswordInputActivity(
                    getString(R.string.enter_wallet_password_msg), null,
                    PasswordInputStep.PIN_REQUEST, RC_OPEN_WALLET, (AppCompatActivity)getActivity());
        } else {
            currencyList = new ArrayList<>(Wallet.getCurrencySet());
            printSummary();
        }
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.wallet_lbl));
        setHasOptionsMenu(true);
        return rootView;
    }

    private void printSummary() {
        adapter = new CurrencyListAdapter(currencyList, getActivity());
        gridView.setAdapter(adapter);
        adapter.setItemList(currencyList);
        adapter.notifyDataSetChanged();
        if(menu != null) menu.removeItem(R.id.open_wallet);
        //Map<String, Map<String, IncomesDto>> currencyMap = Wallet.getCurrencyCodeMap();
        Map<String, BigDecimal> currencyMap = Wallet.getCurrencyCodeMap();
        ((LinearLayout)rootView.findViewById(R.id.summary)).removeAllViews();
        for(String currencyCode : currencyMap.keySet()) {
            LinearLayout currencyDataLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(
                    R.layout.wallet_currency_summary, null);
            BigDecimal currencyCodeAmount = currencyMap.get(currencyCode);
            String contentFormatted = currencyCodeAmount.toPlainString()+ " " + currencyCode;
            TextView amountTextView = new TextView(getActivity());
            amountTextView.setGravity(Gravity.CENTER);
            amountTextView.setText(Html.fromHtml(contentFormatted));
            ((LinearLayout)currencyDataLayout.findViewById(R.id.tag_info)).addView(amountTextView);
            ((LinearLayout)rootView.findViewById(R.id.summary)).addView(currencyDataLayout);
        }
        walletLoaded = true;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        switch (requestCode) {
            case RC_OPEN_WALLET:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    try {
                        currencyList = new ArrayList<>(Wallet.getCurrencySet(
                                new String(responseDto.getMessageBytes()).toCharArray()));
                        if(App.getInstance().getSessionInfo() != null)
                            Utils.launchCurrencyStatusCheck(broadCastId, null);
                        printSummary();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                                getString(R.string.error_lbl), ex.getMessage(), getFragmentManager());
                    }
                }
                break;
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.clear();
        if(!walletLoaded)
            menuInflater.inflate(R.menu.wallet, menu);
        this.menu = menu;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_wallet:
                Utils.launchPasswordInputActivity(getString(R.string.enter_wallet_password_msg), null,
                        PasswordInputStep.PIN_REQUEST,RC_OPEN_WALLET, (AppCompatActivity)getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void isProgressDialogVisible(boolean isVisible) {
        if(isVisible) ProgressDialogFragment.showDialog(
                getString(R.string.unlocking_wallet_msg), getString(R.string.wait_msg), getFragmentManager());
        else ProgressDialogFragment.hide(getFragmentManager());
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }


    public class CurrencyListAdapter  extends ArrayAdapter<Currency> {

        private List<Currency> itemList;
        private Context context;

        public CurrencyListAdapter(List<Currency> itemList, Context ctx) {
            super(ctx, R.layout.currency_card, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public Currency getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        @Override public View getView(int position, View view, ViewGroup parent) {
            Currency currency = itemList.get(position);
            if (view == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.currency_card, null);
            }
            //Date weekLapse = DateUtils.getDateFromPath(weekLapseStr);
            //Calendar weekLapseCalendar = Calendar.getInstance();
            //weekLapseCalendar.setTime(weekLapse);
            LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            linearLayout.setBackgroundColor(Color.WHITE);
            TextView date_data = (TextView)view.findViewById(R.id.date_data);
            date_data.setText(DateUtils.getDayWeekDateSimpleStr(currency.getDateFrom()));

            TextView currency_state = (TextView) view.findViewById(R.id.currency_state);
            currency_state.setText(currency.getStateMsg(getActivity()));
            currency_state.setTextColor(currency.getStateColor(getActivity()));

            TextView amount = (TextView) view.findViewById(R.id.amount);
            amount.setText(currency.getAmount().toPlainString());
            amount.setTextColor(currency.getStateColor(getActivity()));
            TextView currencyTextView = (TextView) view.findViewById(R.id.currencyCode);
            currencyTextView.setText(currency.getCurrencyCode().toString());
            currencyTextView.setTextColor(currency.getStateColor(getActivity()));

            if(DateUtils.getCurrentWeekPeriod().inRange(currency.getDateTo())) {
                TextView time_limited_msg = (TextView) view.findViewById(R.id.time_limited_msg);
                time_limited_msg.setText(getString(R.string.lapse_lbl,
                        DateUtils.getDayWeekDateStr(currency.getDateTo(), "HH:mm")));
            }
            return view;
        }

        public List<Currency> getItemList() {
            return itemList;
        }

        public void setItemList(List<Currency> itemList) {
            this.itemList = itemList;
        }
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

}