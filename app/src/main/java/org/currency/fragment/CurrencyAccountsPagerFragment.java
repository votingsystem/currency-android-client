package org.currency.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.dto.ResponseDto;
import org.currency.dto.identity.SessionCertificationDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.Utils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyAccountsPagerFragment extends Fragment {

    public static final String TAG = CurrencyAccountsPagerFragment.class.getSimpleName();

    public static final int RC_IDENTIFICATE_CLIENT          = 0;

    //screens order
    private static final int USER_ACCOUNTS_POS       = 0;
    private static final int TRANSANCTIONVS_LIST_POS = 1;

    private CurrencyPagerAdapter pagerAdapter;
    private ViewPager mViewPager;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "onCreateView");
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.currency_accounts_main, container, false);
        mViewPager = (ViewPager) rootView.findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) { }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            public void onPageSelected(int position) {
                LOGD(TAG + ".onCreate", "onPageSelected: " + position);
                switch (position) {
                    case USER_ACCOUNTS_POS:
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(null);
                        break;
                    case TRANSANCTIONVS_LIST_POS:
                        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                                getString(R.string.movements_lbl));
                        break;
                }

            }
        });
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        pagerAdapter = new CurrencyPagerAdapter(getFragmentManager(),
                getActivity().getIntent().getExtras());
        mViewPager.setAdapter(pagerAdapter);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.currency_accounts_lbl));
        setHasOptionsMenu(true);
        if(App.getInstance().getSessionInfo() == null) {
            Utils.launchSessionIndentificationActivity(getActivity(), RC_IDENTIFICATE_CLIENT);
        }
        return rootView;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        Fragment selectedFragment = pagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        if(selectedFragment != null)
            selectedFragment.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_IDENTIFICATE_CLIENT:
                if(Activity.RESULT_OK == resultCode) {
                    ResponseDto responseDto = data.getParcelableExtra(Constants.RESPONSE_KEY);
                    new InitSessionTask(responseDto.getCmsMessage()).execute();
                }
                break;
        }
    }

    class CurrencyPagerAdapter extends FragmentStatePagerAdapter {

        final String TAG = CurrencyPagerAdapter.class.getSimpleName();

        SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

        private String searchQuery = null;
        private Bundle args;

        public CurrencyPagerAdapter(FragmentManager fragmentManager, Bundle args) {
            super(fragmentManager);
            this.args = (args != null)? args:new Bundle();
        }

        @Override public Fragment getItem(int position) {
            Fragment selectedFragment = null;
            switch(position) {
                case USER_ACCOUNTS_POS:
                    selectedFragment = new CurrencyAccountsFragment();
                    break;
                case TRANSANCTIONVS_LIST_POS:
                    selectedFragment = new TransactionGridFragment();
                    break;
            }
            args.putString(SearchManager.QUERY, searchQuery);
            selectedFragment.setArguments(args);
            LOGD(TAG + ".getItem", "position:" + position + " - args: " + args +
                    " - selectedFragment.getClass(): " + ((Object)selectedFragment).getClass());
            registeredFragments.put(position, selectedFragment);
            return selectedFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        @Override public int getCount() {
            return 2;
        } //CURRENCY_ACCOUNTS and TRANSANCTION_LIST

    }

    public class InitSessionTask extends AsyncTask<String, String, ResponseDto> {

        private CMSSignedMessage signedMessage;

        public InitSessionTask(CMSSignedMessage signedMessage) {
            this.signedMessage = signedMessage;
        }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.sending_data_lbl),
                    getString(R.string.init_session_msg), getFragmentManager());
        }

        @Override protected ResponseDto doInBackground(String... urls) {
            String serviceURL = OperationType.SESSION_CERTIFICATION.getUrl(
                    App.getInstance().getCurrencyService().getFirstIdentityProvider());
            ResponseDto response = null;
            try {
                response = HttpConn.getInstance().doPostRequest(signedMessage.toPEM(),
                        ContentType.PKCS7_SIGNED, serviceURL);
                SessionCertificationDto certificationDto = JSON.getMapper().readValue(response.getMessageBytes(),
                        SessionCertificationDto.class);
                App.getInstance().getSessionInfo().loadIssuedCerts(certificationDto);
                response = HttpConn.getInstance().doPostRequest(response.getMessageBytes(),
                        ContentType.XML, OperationType.INIT_SESSION.getUrl(
                        App.getInstance().getCurrencyService().getEntity().getId()));
            } catch (Exception ex) {
                ex.printStackTrace();
                response = ResponseDto.EXCEPTION(ex, getActivity());
            }
            return response;
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {


            } else {
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.error_lbl),
                        responseDto.getMessage(), getFragmentManager());
            }
            ProgressDialogFragment.hide(getFragmentManager());
        }
    }

}