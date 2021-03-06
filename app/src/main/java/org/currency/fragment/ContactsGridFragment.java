package org.currency.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.fasterxml.jackson.core.type.TypeReference;

import org.currency.App;
import org.currency.activity.ContactPagerActivity;
import org.currency.android.R;
import org.currency.contentprovider.UserContentProvider;
import org.currency.dto.ResponseDto;
import org.currency.dto.ResultListDto;
import org.currency.dto.UserDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.ObjectUtils;
import org.currency.util.OperationType;
import org.currency.util.UIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ContactsGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = ContactsGridFragment.class.getSimpleName();

    public static final int CONTACT_PICKER = 1;

    public enum Mode {SEARCH, CONTACT}

    private View rootView;
    private GridView gridView;
    private String queryStr = null;
    private Mode mode = Mode.CONTACT;
    private App app = null;
    private String broadCastId = ContactsGridFragment.class.getSimpleName();
    private static final int loaderId = 0;
    private AtomicBoolean isProgressDialogVisible = new AtomicBoolean(false);
    private UserDto contactUser;
    private List<UserDto> userList = null;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            ResponseDto responseDto = intent.getParcelableExtra(Constants.RESPONSE_KEY);
            setProgressDialogVisible(false);
            if(ResponseDto.SC_CONNECTION_TIMEOUT == responseDto.getStatusCode()) {
                if(gridView.getAdapter().getCount() == 0)
                    rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
            }
            if(ResponseDto.SC_OK != responseDto.getStatusCode()) {
                MessageDialogFragment.showDialog(responseDto, getFragmentManager());
            }
        }
    };


    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (App) getActivity().getApplicationContext();
        Bundle data = getArguments();
        if (data != null && data.containsKey(SearchManager.QUERY)) {
            queryStr = data.getString(SearchManager.QUERY);
        }
        if(savedInstanceState != null) {
            mode = (Mode) savedInstanceState.getSerializable(Constants.STATE_KEY);
        }
        LOGD(TAG + ".onCreate", "args: " + getArguments() + " - loaderId: " + loaderId +
                " - queryStr: " + queryStr + " - mode: " + mode);
        if(queryStr != null && mode != Mode.SEARCH) {
            mode = Mode.SEARCH;
            new ContactsFetcher(queryStr).execute();
        }
        setHasOptionsMenu(true);
    };

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        if(Mode.CONTACT == mode) {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.contacts_lbl));
        } else {
            ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(
                    getString(R.string.search_result));
        }
        rootView = inflater.inflate(R.layout.contacts_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        if(savedInstanceState != null) {
            String dtoStr = savedInstanceState.getString(Constants.DTO_KEY);
            if(dtoStr != null) {
                try {
                    userList = JSON.readValue(dtoStr, new TypeReference<List<UserDto>>(){});
                    ContactListAdapter adapter = new ContactListAdapter(userList, app);
                    gridView.setAdapter(adapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                contactUser = (UserDto) savedInstanceState.getSerializable(Constants.USER_KEY);
                Parcelable gridState = savedInstanceState.getParcelable(Constants.LIST_STATE_KEY);
                gridView.onRestoreInstanceState(gridState);
            }
        } else {
            ContactDBListAdapter adapter = new ContactDBListAdapter(getActivity(), null,false);
            gridView.setAdapter(adapter);
        }
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        getLoaderManager().initLoader(loaderId, null, this);
        return rootView;
    }

    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult(..)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode + " - data: " + data);
        if (resultCode != Activity.RESULT_OK) return;
        if (data == null) return;
        switch (requestCode) {
            case CONTACT_PICKER:
                final UserDto user = extractInfoFromContactPickerIntent(data, getActivity());
                if(user != null && user.getId() == null) {
                    AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                        getString(R.string.error_lbl),
                        getString(R.string.contactvs_not_found_msg, user.getGivenName()),
                        getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                fetchUser(user);
                            }
                        });
                    UIUtils.showMessageDialog(builder);
                } else launchPager(null, user);
                break;
        }
    }

    private void fetchUser(UserDto user) {
        this.contactUser = user;
        new ContactsFetcher(user).execute("");
    }

    @Override public void onScrollStateChanged(AbsListView view, int scrollState) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) { }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        return true;
    }

    private void setProgressDialogVisible(final boolean isVisible) {
        if(isVisible && isProgressDialogVisible.get()) return;
        isProgressDialogVisible.set(isVisible);
        if(!isAdded()) return;
        //bug, without Handler triggers 'Can not perform this action inside of onLoadFinished'
        new Handler(){
            @Override public void handleMessage(Message msg) {
                if (isVisible) {
                    ProgressDialogFragment.showDialog(
                            getString(R.string.wait_msg),
                            getString(R.string.loading_data_msg),
                            getFragmentManager());
                } else ProgressDialogFragment.hide(getFragmentManager());
            }
        }.sendEmptyMessage(UIUtils.EMPTY_MESSAGE);
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(Constants.LIST_STATE_KEY, gridState);
        outState.putSerializable(Constants.STATE_KEY, mode);
        outState.putSerializable(Constants.USER_KEY, contactUser);
        if(userList != null) {
            try {
                outState.putString(Constants.DTO_KEY, JSON.writeValueAsString(userList));
            } catch (Exception ex) { ex.printStackTrace();}
        }
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.contacts_grid_fragment, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.open_contacts:
                startActivityForResult(new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI), CONTACT_PICKER);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG + ".onListItemClick", "Clicked item - position:" + position + " - id: " + id);
        if(gridView.getAdapter() instanceof CursorAdapter) {
            launchPager(position, null);
        } else {
            launchPager(null, userList.get(position));
        }
    }

    private void launchPager(Integer position, UserDto user) {
        Intent intent = new Intent(getActivity(), ContactPagerActivity.class);
        intent.putExtra(Constants.CURSOR_POSITION_KEY, position);
        intent.putExtra(Constants.STATE_KEY, mode);
        try {
            intent.putExtra(Constants.DTO_KEY, JSON.writeValueAsString(userList));
        } catch (Exception ex) { ex.printStackTrace();}
        intent.putExtra(Constants.USER_KEY, user);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        LOGD(TAG + ".onCreateLoader", "onCreateLoader");
        return new CursorLoader(this.getActivity(),
                UserContentProvider.CONTENT_URI, null, UserContentProvider.TYPE_COL + " =? ",
                new String[]{UserDto.Type.CONTACT.toString()}, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount());
        if(!(gridView.getAdapter() instanceof CursorAdapter)) {
            ContactDBListAdapter adapter = new ContactDBListAdapter(getActivity(), null,false);
            gridView.setAdapter(adapter);
        }
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        if(gridView.getAdapter() instanceof CursorAdapter)
            ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    public class ContactDBListAdapter extends CursorAdapter {

        public ContactDBListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return LayoutInflater.from(context).inflate(R.layout.contact_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                String fullName = cursor.getString(cursor.getColumnIndex(
                        UserContentProvider.FULL_NAME_COL));
                String nif = cursor.getString(cursor.getColumnIndex(UserContentProvider.NIF_COL));
                ((TextView)view.findViewById(R.id.fullname)).setText(fullName);
                ((TextView) view.findViewById(R.id.nif)).setText(nif);
            }
        }
    }

    public class ContactListAdapter extends BaseAdapter {

        private List<UserDto> itemList;
        private Context context;

        public ContactListAdapter(List<UserDto> itemList, Context ctx) {
            this.itemList = itemList;
            this.context = ctx;
        }

        @Override public int getCount() {
            return itemList.size();
        }

        @Override public Object getItem(int position) {
            return itemList.get(position);
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            UserDto user = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.contact_card, null);
            }
            ((TextView)itemView.findViewById(R.id.fullname)).setText(user.getGivenName());
            ((TextView) itemView.findViewById(R.id.nif)).setText(user.getNumId());
            return itemView;
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

    public class ContactsFetcher extends AsyncTask<String, String, ResponseDto> {

        private String phone, email;
        private String serviceURL;

        public ContactsFetcher(String searchQuery) {
            this.serviceURL = OperationType.getSearchURL(app.getCurrencyService()
                    .getEntity().getId(), searchQuery);
        }

        public ContactsFetcher(UserDto user) {
            this.phone = user.getPhone();
            this.email = user.getEmail();
            this.serviceURL = OperationType.getSearchServiceURL(app.getCurrencyService()
                    .getEntity().getId(), phone, email);
        }

        @Override protected void onPreExecute() { setProgressDialogVisible(true); }

        @Override protected ResponseDto doInBackground(String... params) {
            return HttpConn.getInstance().doGetRequest(serviceURL, ContentType.JSON);
        }

        @Override protected void onProgressUpdate(String... progress) { }

        @Override protected void onPostExecute(ResponseDto responseDto) {
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                try {
                    if(phone != null || email != null) {
                        userList = null;
                        UserDto user = JSON.getMapper().readValue(responseDto.getMessageBytes(), UserDto.class);
                        if(contactUser != null)
                            user.setContactURI(contactUser.getContactURI());
                        launchPager(null, user);
                    } else {
                        ResultListDto<UserDto> resultList = JSON.getMapper().readValue(
                                responseDto.getMessageBytes(), new TypeReference<ResultListDto<UserDto>>() {});
                        userList = new ArrayList<>(resultList.getResultList());
                        if(userList.size() > 0)
                            rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
                        ContactListAdapter adapter = new ContactListAdapter(userList, app);
                        gridView.setAdapter(adapter);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MessageDialogFragment.showDialog(ResponseDto.SC_ERROR,
                            getString(R.string.exception_lbl), ex.getMessage(), getFragmentManager());
                }
            } else {
                MessageDialogFragment.showDialog(ResponseDto.SC_ERROR, getString(R.string.error_lbl),
                        responseDto.getMessage(), getFragmentManager());
            }
            setProgressDialogVisible(false);
        }
    }

    public static UserDto extractInfoFromContactPickerIntent(final Intent intent, Context mContext) {
        Cursor cursor = null;
        try {
            Uri contactURI = intent.getData();
            String id = contactURI.getLastPathSegment();// Get the contact id from the Uri
            String phone = null;
            String email = null;
            String name = null;
            UserDto user = null;
            cursor = mContext.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0) {
                    phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                }
            }
            cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + id, null, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0)  email = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
            String[] args = { contactURI.toString() };
            cursor = mContext.getContentResolver().query(UserContentProvider.CONTENT_URI, null,
                    UserContentProvider.CONTACT_URI_COL + " = ?" , args, null);
            if(cursor != null) {
                cursor.moveToFirst();
                if(cursor.getCount() > 0) {
                    user = (UserDto) ObjectUtils.deSerializeObject(cursor.getBlob(
                            cursor.getColumnIndex(UserContentProvider.SERIALIZED_OBJECT_COL)));
                }
            }
            if(user == null) {
                user = new UserDto();
                user.setGivenName(name);
                user.setPhone(phone);
                user.setEmail(email);
            }
            user.setContactURI(contactURI);
            LOGD(TAG, "email: " + email + " - phone: " + phone + " - displayName: " + name);
            return user;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }
}