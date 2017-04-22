package org.currency.fragment;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.currency.App;
import org.currency.android.R;
import org.currency.dto.MessageDto;
import org.currency.dto.QRMessageDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.ui.DialogButton;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.MsgUtils;
import org.currency.util.OperationType;
import org.currency.util.QRUtils;
import org.currency.util.UIUtils;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class  QRFragment extends Fragment {

    public static final String TAG = QRFragment.class.getSimpleName();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            LOGD(TAG + ".broadcastReceiver", "extras: " + intent.getExtras());
            MessageDto socketMsg = (MessageDto) intent.getSerializableExtra(Constants.MESSAGE_KEY);
            if(socketMsg != null) {
                switch (socketMsg.getOperation().getType()) {
                    case PAYMENT_CONFIRM:
                        if(ResponseDto.SC_OK == socketMsg.getStatusCode()) {
                            DialogButton dialogButton = new DialogButton(getString(R.string.accept_lbl),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            getActivity().onBackPressed();
                                        }
                                    });
                            UIUtils.showMessageDialog(getString(R.string.payment_ok_caption),
                                    intent.getStringExtra(Constants.MESSAGE_KEY), dialogButton, null,
                                    getActivity());
                        }
                        break;
                }
            }
        }
    };

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        View rootView = inflater.inflate(R.layout.qr_fragment, container, false);
        Intent intent = getActivity().getIntent();
        TransactionDto dto = (TransactionDto) intent.getSerializableExtra(
                Constants.TRANSACTION_KEY);
        QRMessageDto qrDto = new QRMessageDto(App.getInstance().getSessionInfo().getSessionDevice(),
                OperationType.TRANSACTION_INFO);
        qrDto.setData(dto);
        Bitmap bitmap = null;
        try {
            bitmap = QRUtils.encodeAsBitmap(JSON.writeValueAsString(qrDto), getActivity());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        ImageView view = (ImageView) rootView.findViewById(R.id.image_view);
        view.setImageBitmap(bitmap);
        setHasOptionsMenu(true);
        App.getInstance().putQRMessage(qrDto);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_code_lbl));
        ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(
                getString(R.string.qr_transaction_request_msg, dto.getAmount() + " " + dto.getCurrencyCode()));
        if(!App.getInstance().isSocketConnectionEnabled()) {
            AlertDialog.Builder builder = UIUtils.getMessageDialogBuilder(
                    getString(R.string.qr_code_lbl), getString(R.string.qr_connection_required_msg),
                    getActivity()).setPositiveButton(getString(R.string.accept_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            getActivity().finish();
                        }
                    });
            UIUtils.showMessageDialog(builder);
        }
        return rootView;
    }

    @Override public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(Constants.WEB_SOCKET_BROADCAST_ID));
    }


}