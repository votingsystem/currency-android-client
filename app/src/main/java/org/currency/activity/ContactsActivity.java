package org.currency.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.currency.android.R;
import org.currency.fragment.ContactsGridFragment;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.util.UIUtils;

import java.lang.ref.WeakReference;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactsActivity extends AppCompatActivity {

	public static final String TAG = ContactsActivity.class.getSimpleName();

    private WeakReference<ContactsGridFragment> contactsGridRef;

    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        UIUtils.setSupportActionBar(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        Bundle arguments = new Bundle();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            LOGD(TAG + ".ACTION_SEARCH: ", "query: " + query);
            arguments.putString(SearchManager.QUERY, query);
        }
        ContactsGridFragment fragment = new ContactsGridFragment();
        fragment.setArguments(arguments);
        contactsGridRef = new WeakReference<>(fragment);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, contactsGridRef.get(),
                ((Object) fragment).getClass().getSimpleName()).commit();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(contactsGridRef.get() != null)
            contactsGridRef.get().onActivityResult(requestCode, resultCode, intent);
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if (isVisible) {
            ProgressDialogFragment.showDialog(getString(R.string.wait_msg),
                    getString(R.string.loading_data_msg), getSupportFragmentManager());
        } else ProgressDialogFragment.hide(getSupportFragmentManager());
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.search_item:
                onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    };

}