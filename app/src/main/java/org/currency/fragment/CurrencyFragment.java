package org.currency.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.currency.android.R;
import org.currency.model.Currency;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.MsgUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyFragment extends Fragment {

    public static final String TAG = CurrencyFragment.class.getSimpleName();

    private TextView currency_amount, currency_state, date_info, hash_cert;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState +
                " - arguments: " + getArguments());
        View rootView = inflater.inflate(R.layout.currency, container, false);
        currency_amount = (TextView)rootView.findViewById(R.id.currency_amount);
        currency_state = (TextView)rootView.findViewById(R.id.currency_state);
        date_info = (TextView)rootView.findViewById(R.id.date_info);
        hash_cert = (TextView)rootView.findViewById(R.id.hash_cert);
        if(getArguments() != null && getArguments().containsKey(Constants.CURRENCY_KEY)) {
            Currency currency = (Currency) getArguments().getSerializable(Constants.CURRENCY_KEY);
            initCurrencyScreen(currency);
        }
        return rootView;
    }

    public void initCurrencyScreen(Currency currency) {
        try {
            hash_cert.setText(currency.getRevocationHash());
            currency_amount.setText(currency.getAmount().toPlainString() + " " +
                    currency.getCurrencyCode());
            getActivity().setTitle(currency.getAmount().toPlainString() + " " + currency.getCurrencyCode());
            date_info.setText(getString(R.string.lapse_info,
                    DateUtils.getDateStr(currency.getDateTo(), "dd MMM yyyy' 'HH:mm")));
            if(currency.getState() != null && Currency.State.OK != currency.getState()) {
                currency_state.setText(MsgUtils.getCurrencyStateMessage(currency, getActivity()));
                currency_state.setVisibility(View.VISIBLE);
                currency_amount.setTextColor(getResources().getColor(R.color.red_vs));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

}