package org.currency.fragment;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.currency.App;
import org.currency.activity.FragmentContainerActivity;
import org.currency.android.R;
import org.currency.dto.UserDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.util.Constants;
import org.currency.util.OperationType;
import org.currency.util.Utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionFormFragment extends Fragment {

    public static final String TAG = TransactionFormFragment.class.getSimpleName();

    public enum Type {QR_FORM, TRANSACTION_FORM}

    private Spinner currencySpinner;
    private EditText amount_text;
    private TextView subject;
    private UserDto toUser;
    private String broadCastId = TransactionFormFragment.class.getSimpleName();
    private CheckBox from_user_checkbox, currency_send_checkbox, currency_change_checkbox;
    private Type formType;

    @Override public View onCreateView(LayoutInflater inflater,
               ViewGroup container, Bundle savedInstanceState) {
        LOGD(TAG + ".onCreateView", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        formType = (Type) getArguments().getSerializable(Constants.OPERATION_KEY);
        toUser = (UserDto) getArguments().getSerializable(Constants.USER_KEY);
        View rootView = inflater.inflate(R.layout.transaction_form_fragment, container, false);
        rootView.findViewById(R.id.request_button).setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        generateQR();
                    }
                });
        subject = (EditText) rootView.findViewById(R.id.subject);
        amount_text = (EditText) rootView.findViewById(R.id.amount);
        from_user_checkbox = (CheckBox) rootView.findViewById(R.id.from_user_checkbox);
        from_user_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                    checkBoxSelected(buttonView, isChecked);
                }
            });
        currency_send_checkbox = (CheckBox) rootView.findViewById(R.id.currency_send_checkbox);
        currency_send_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                checkBoxSelected(buttonView, isChecked);
            }
        });
        currency_change_checkbox = (CheckBox) rootView.findViewById(R.id.currency_change_checkbox);
        currency_change_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                checkBoxSelected(buttonView, isChecked);
            }
        });
        Button request_button = (Button) rootView.findViewById(R.id.request_button);
        currencySpinner = (Spinner) rootView.findViewById(R.id.currency_spinner);
        switch (formType) {
            case QR_FORM:
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.qr_create_lbl));
                request_button.setText(getString(R.string.qr_create_lbl));
                break;
            case TRANSACTION_FORM:
                if(toUser.getConnectedDevices() == null || toUser.getConnectedDevices().size() == 0) {
                    currency_change_checkbox.setVisibility(View.GONE);
                }
                ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(getString(R.string.send_money_lbl));
                ((AppCompatActivity)getActivity()).getSupportActionBar().setSubtitle(toUser.getFullName());
                request_button.setText(getString(R.string.send_money_lbl));
                break;
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return rootView;
    }

    private void checkBoxSelected(CompoundButton checkbox, boolean isChecked) {
        if(isChecked) {
            if(formType == Type.TRANSACTION_FORM) {
                from_user_checkbox.setChecked(false);
                currency_send_checkbox.setChecked(false);
                currency_change_checkbox.setChecked(false);
            }
            ((CheckBox) checkbox).setChecked(true);
        }
    }


    private void generateQR() {
        LOGD(TAG + ".generateQR", "generateQR");
        subject.setError(null);
        amount_text.setError(null);
        if(TextUtils.isEmpty(subject.getText().toString())){
            subject.setError(getString(R.string.subject_missing_msg));
            return ;
        }
        Integer selectedAmount = 0;
        try {
            selectedAmount = Integer.valueOf(amount_text.getText().toString());
        } catch (Exception ex) { LOGD(TAG + ".generateQR", "ERROR - amount_text:" + amount_text.getText());}
        if(selectedAmount <= 0) {
            amount_text.setError(getString(R.string.min_withdrawal_msg));
            return;
        }
        List<OperationType> paymentOptions = new ArrayList<>();
        if(from_user_checkbox.isChecked())
            paymentOptions.add(OperationType.TRANSACTION_FROM_USER);
        if(currency_send_checkbox.isChecked())
            paymentOptions.add(OperationType.CURRENCY_SEND);
        if(currency_change_checkbox.isChecked())
            paymentOptions.add(OperationType.CURRENCY_CHANGE);
        if(paymentOptions.isEmpty()) {
            MessageDialogFragment.showDialog(getString(R.string.error_lbl),
                    getString(R.string.min_payment_option_msg), getFragmentManager());
            return;
        }

        switch (formType) {
            case TRANSACTION_FORM:
                TransactionDto transactionDto = new TransactionDto();
                transactionDto.setAmount(new BigDecimal(amount_text.getText().toString()));
                transactionDto.setCurrencyCode(currencySpinner.getSelectedItem().toString());
                transactionDto.setPaymentOptions(paymentOptions);
                transactionDto.setSubject(subject.getText().toString());
                transactionDto.setToUserName(toUser.getFullName());
                transactionDto.setToUserIBAN(Utils.asSet(toUser.getIBAN()));
                transactionDto.setDateCreated(new Date());
                transactionDto.setUUID(UUID.randomUUID().toString());
                Intent resultIntent = new Intent(getActivity(), FragmentContainerActivity.class);
                resultIntent.putExtra(Constants.FRAGMENT_KEY, PaymentFragment.class.getName());
                resultIntent.putExtra(Constants.TRANSACTION_KEY, transactionDto);
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(resultIntent);
                break;
            case QR_FORM:
                TransactionDto dto = TransactionDto.PAYMENT_REQUEST(
                        App.getInstance().getSessionInfo().getUser().getFullName(), UserDto.Type.USER,
                        new BigDecimal(amount_text.getText().toString()),
                        currencySpinner.getSelectedItem().toString(),
                        App.getInstance().getSessionInfo().getUser().getIBAN(), subject.getText().toString());
                dto.setPaymentOptions(paymentOptions);
                Intent intent = new Intent(getActivity(), FragmentContainerActivity.class);
                intent.putExtra(Constants.TRANSACTION_KEY, dto);
                intent.putExtra(Constants.FRAGMENT_KEY, QRFragment.class.getName());
                startActivity(intent);
                break;
        }
    }

}