package org.currency.debug;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.currency.App;
import org.currency.android.R;
import org.currency.debug.actions.BrowserAction;
import org.currency.debug.actions.ForceSyncNowAction;
import org.currency.debug.actions.NFCActivityAction;
import org.currency.debug.actions.PatternLockAction;
import org.currency.debug.actions.PrefsAction;
import org.currency.debug.actions.RegisterAction;
import org.currency.debug.actions.SignDocumentAndSendAction;

import static org.currency.util.LogUtils.LOGD;

public class DebugActionRunnerFragment extends Fragment {

    private static final String TAG = DebugActionRunnerFragment.class.getSimpleName();

    private TextView mLogArea;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.debug_action_runner, null);
        mLogArea = (TextView) rootView.findViewById(R.id.logArea);
        ViewGroup tests = (ViewGroup) rootView.findViewById(R.id.debug_action_list);
        tests.addView(createTestAction(new ForceSyncNowAction(
                (App) getActivity().getApplicationContext())));
        tests.addView(createTestAction(new PatternLockAction()));
        tests.addView(createTestAction(new PrefsAction()));
        tests.addView(createTestAction(new BrowserAction()));
        tests.addView(createTestAction(new RegisterAction()));
        tests.addView(createTestAction(new SignDocumentAndSendAction()));
        tests.addView(createTestAction(new NFCActivityAction((App) getActivity().getApplicationContext())));
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(
                getString(R.string.debug_tests));
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected View createTestAction(final DebugAction test) {
        Button testButton = new Button(this.getActivity());
        testButton.setText(test.getLabel());
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final long start = System.currentTimeMillis();
                mLogArea.setText("");
                test.run(view.getContext(), new DebugAction.Callback() {
                    @Override
                    public void done(boolean success, String message) {
                        logTimed((System.currentTimeMillis() - start),
                                (success ? "[OK] " : "[FAIL] ") + message);
                    }
                });
            }
        });
        return testButton;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        LOGD(TAG, "onActivityResult - requestCode: " + requestCode + " - resultCode: " + resultCode);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result != null) {
            String contents = result.getContents();
            LOGD(TAG + ".onActivityResult", "contents: " + contents);
        }
    }

    protected void logTimed(long time, String message) {
        message = "[" + time + "ms] " + message;
        LOGD(TAG, message);
        mLogArea.append(message + "\n");
    }

}
