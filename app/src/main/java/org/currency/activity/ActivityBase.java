package org.currency.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.currency.App;
import org.currency.android.R;
import org.currency.debug.DebugActionRunnerFragment;
import org.currency.dto.ResponseDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.fragment.CurrencyAccountsPagerFragment;
import org.currency.fragment.MessageDialogFragment;
import org.currency.fragment.MessagesGridFragment;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.fragment.QRActionsFragment;
import org.currency.fragment.ReceiptGridFragment;
import org.currency.fragment.WalletFragment;
import org.currency.ui.DialogButton;
import org.currency.util.ActivityResult;
import org.currency.util.Constants;
import org.currency.util.UIUtils;

import java.lang.ref.WeakReference;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ActivityBase extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = ActivityBase.class.getSimpleName();
    public static final String MENU_KEY = "MENU_KEY";

    private WeakReference<Fragment> currentFragment;

    private App app = null;
    private NavigationView navigationView;
    private Menu menu;
    private Integer menuType;
    private ActivityResult activityResult;

    public static final String BROADCAST_ID = ActivityBase.class.getSimpleName();
    public static final String CHILD_FRAGMENT = "CHILD_FRAGMENT";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            if (responseDto != null) {
                if (ResponseDto.SC_ERROR == responseDto.getStatusCode()) {
                    MessageDialogFragment.showDialog(responseDto.getStatusCode(),
                            getString(R.string.error_lbl), responseDto.getMessage(),
                            getSupportFragmentManager());
                } else if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                    if(responseDto.getServiceCaller() == CHILD_FRAGMENT) {
                        selectedContentFragment(Integer.valueOf(responseDto.getMessage()));
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);
        Toolbar toolbar = UIUtils.setSupportActionBar(this);
        app = (App) getApplicationContext();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        int selectedFragmentMenuId = getIntent().getIntExtra(Constants.FRAGMENT_KEY, -1);
        if (selectedFragmentMenuId > 0) selectedContentFragment(selectedFragmentMenuId);
        if (savedInstanceState == null) {
            selectedContentFragment(R.id.fa_qrcode);
            setMenu(R.menu.drawer_currency);
        } else {
            setMenu(savedInstanceState.getInt(MENU_KEY));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (currentFragment != null && currentFragment.get() != null) {
            currentFragment.get().onActivityResult(requestCode, resultCode, data);
        } else this.activityResult = new ActivityResult(requestCode, resultCode, data);
    }

    public ActivityResult getActivityResult() {
        ActivityResult result = this.activityResult;
        this.activityResult = null;
        return result;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (!app.isHistoryEmpty()) {
                selectedContentFragment(app.getHistoryItem());
            } else super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (!Constants.IS_DEBUG_SESSION) {
            MenuItem debugMenuItem = menu.findItem(R.id.menu_debug);
            if (debugMenuItem != null) debugMenuItem.setVisible(false);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent = null;
        switch (id) {
            case R.id.menu_debug:
                if (Constants.IS_DEBUG_SESSION) {
                    intent = new Intent(getBaseContext(), FragmentContainerActivity.class);
                    intent.putExtra(Constants.FRAGMENT_KEY, DebugActionRunnerFragment.class.getName());
                    startActivity(intent);
                }
                return true;
            case R.id.close_app:
                app.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        app.addHistoryItem(item.getItemId());
        return selectedContentFragment(item.getItemId());
    }

    public void setMenu(int menuType) {
        this.menuType = menuType;
        navigationView.getMenu().clear();
        navigationView.inflateMenu(menuType);
    }

    private boolean selectedContentFragment(int menuId) {
        //this is to remove actionbar spinners
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setSubtitle(null);
        switch (menuId) {
            case R.id.currency_accounts:
                replaceFragment(new CurrencyAccountsPagerFragment(), CurrencyAccountsPagerFragment.TAG);
                setMenu(R.menu.drawer_currency);
                break;
            case R.id.fa_qrcode:
                replaceFragment(new QRActionsFragment(), QRActionsFragment.TAG);
                break;
            case R.id.receipts:
                replaceFragment(new ReceiptGridFragment(), ReceiptGridFragment.TAG);
                break;
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.wallet:
                replaceFragment(new WalletFragment(), WalletFragment.TAG);
                break;
            case R.id.contacts:
                intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                break;
            case R.id.messages:
                replaceFragment(new MessagesGridFragment(), MessagesGridFragment.TAG);
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void replaceFragment(Fragment newFragment, String tag) {
        if (currentFragment != null)
            getSupportFragmentManager().beginTransaction().remove(currentFragment.get()).commit();
        currentFragment = new WeakReference<>(newFragment);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, currentFragment.get(), tag).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        while(app.getAppState().process(this));


        /*if (app.isRootedPhone()) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.non_rooted_phones_required_msg), positiveButton, null, this);
            return;
        }
        if(PrefUtils.getOperationPassword() == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startActivity(new Intent(ActivityBase.this,
                                    PasswordTypeSelectorActivity.class));
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.access_mode_passw_required_msg), positiveButton, null, this);
            return;
        }*/

        if(App.getInstance().getCurrencyService() == null) {
            new EntityLoader().execute();
        } /*else if(PrefUtils.getOperationPassword() == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startActivity(new Intent(ActivityBase.this,
                                    PasswordTypeSelectorActivity.class));
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.access_mode_passw_required_msg), positiveButton, null, this);
            return;
        }*/
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(BROADCAST_ID));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(MENU_KEY, menuType);
    }

    public class EntityLoader extends AsyncTask<String, String, MetadataDto> {

        public EntityLoader() { }

        @Override protected void onPreExecute() {
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getSupportFragmentManager());
        }

        @Override protected MetadataDto doInBackground(String... urls) {
            MetadataDto metadataDto = App.getInstance().getSystemEntity(
                    Constants.DEFAULT_CURRENCY_SERVER, true);
            return metadataDto;
        }

        @Override protected void onPostExecute(MetadataDto result) {
            ProgressDialogFragment.hide(getSupportFragmentManager());
            if(result != null) {
                App.getInstance().setCurrencyService(result);
            } else {
                DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new EntityLoader().execute();
                            }
                        });
                UIUtils.showMessageDialog(getString(R.string.error_lbl), getString(
                        R.string.missing_server_connection), positiveButton, null, ActivityBase.this);
            }
        }
    }

}