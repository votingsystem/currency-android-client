package org.currency.activity;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.util.HelpUtils;
import org.currency.util.PrefUtils;

import java.lang.ref.WeakReference;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    public static final int RC_USER_DATA                  = 0;
    public static final int RC_SELECT_PASSWORD_INPUT_TYPE = 1;

    private WeakReference<SettingsFragment> settingsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRef = new WeakReference<>(new SettingsFragment());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsRef.get()).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        settingsRef.get().onActivityResult(requestCode, resultCode, data);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private Preference dnieButton;
        private Preference cryptoAccessButton;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            setHasOptionsMenu(true);
            dnieButton = findPreference("dnieButton");
            dnieButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), UserDataFormActivity.class);
                    getActivity().startActivityForResult(intent, RC_USER_DATA);
                    return true;
                }
            });
            //PreferenceScreen preference_screen = getPreferenceScreen();
            Preference aboutButton = findPreference("aboutAppButton");
            aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    HelpUtils.showAbout(getActivity());
                    return true;
                }
            });
            cryptoAccessButton = findPreference("cryptoAccessButton");
            cryptoAccessButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Intent intent = new Intent(getActivity(), PasswordTypeSelectorActivity.class);
                    getActivity().startActivityForResult(intent, RC_SELECT_PASSWORD_INPUT_TYPE);
                    return true;
                }
            });
            updateView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.settings_activity, container, false);
            Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar_vs);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });
            toolbar.setTitle(R.string.navdrawer_item_settings);
            return rootView;
        }

        private void updateView() {
            OperationPassword passwAccessMode = PrefUtils.getOperationPassword();
            if (passwAccessMode != null) {
                switch (passwAccessMode.getInputType()) {
                    case PATTER_LOCK:
                        cryptoAccessButton.setSummary(getString(R.string.pattern_lock_lbl));
                        break;
                    case PIN:
                        cryptoAccessButton.setSummary(getString(R.string.pin_lbl));
                        break;
                    case DNIE_PASSW:
                        cryptoAccessButton.setSummary(getString(R.string.id_card_passw_lbl));
                        break;
                }
            } else cryptoAccessButton.setSummary(getString(R.string.id_card_passw_lbl));
            String dnieCAN = PrefUtils.getDNIeCAN();
            if (dnieCAN != null) {
                dnieButton.setSummary("CAN: " + dnieCAN);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
            updateView();
        }
    }

}