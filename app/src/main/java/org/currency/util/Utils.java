package org.currency.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.google.zxing.integration.android.IntentIntegrator;

import org.currency.App;
import org.currency.activity.IdCardNFCReaderActivity;
import org.currency.activity.PatternLockInputActivity;
import org.currency.activity.PinInputActivity;
import org.currency.android.R;
import org.currency.dto.OperationPassword;
import org.currency.dto.ResponseDto;
import org.currency.service.PaymentService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static String localeToLanguageTag () {
        return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
    }

    public static void launchQRScanner(Activity activity) {
        IntentIntegrator integrator = new IntentIntegrator(activity);
        integrator.addExtra("SCAN_WIDTH", 500);
        integrator.addExtra("SCAN_HEIGHT", 500);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", activity.getString(R.string.set_focus_on_qrcode));
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES, activity);
    }

    public static void launchQRScanner(android.support.v4.app.Fragment fragment) {
        IntentIntegrator integrator = new IntentIntegrator(fragment);
        integrator.addExtra("SCAN_WIDTH", 500);
        integrator.addExtra("SCAN_HEIGHT", 500);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", fragment.getString(R.string.set_focus_on_qrcode));
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
    }

    protected void sendEmail(Context context, List<String> recipients,
                             String toUser, String content) {

        //String[] recipients = {recipient.getText().toString()};
        Intent email = new Intent(Intent.ACTION_SEND, Uri.parse("mailto:"));
        // prompts email clients only
        email.setType("message/rfc822");

        email.putExtra(Intent.EXTRA_EMAIL, recipients.toArray());
        email.putExtra(Intent.EXTRA_SUBJECT, toUser);
        email.putExtra(Intent.EXTRA_TEXT, content);

        try {
            // the user can choose the email client
            context.startActivity(Intent.createChooser(email, "Choose an email client from..."));
        } catch (android.content.ActivityNotFoundException ex) {
            //Toast.makeText(context, "No email client installed.", Toast.LENGTH_LONG).show();
        }
    }

    public static <T> Set<T> asSet(T... args) {
        Set<T> result = new HashSet<>();
        for (T arg : args) {
            result.add(arg);
        }
        return result;
    }

    //fileName must be at least 3 characters
    public static Uri createTempFile(String fileName, byte[] fileBytes, Context context) throws IOException {
        Uri result = null;
        File tempFile = File.createTempFile(fileName, ".xml", context.getExternalCacheDir());
        try {
            tempFile.createNewFile();
            FileOutputStream fo = new FileOutputStream(tempFile);
            fo.write(fileBytes);
            fo.close();
            result = Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ResponseDto getBroadcastResponse(OperationType operation, String serviceCaller,
                                                   ResponseDto responseDto, Context context) {
        if (ResponseDto.SC_OK == responseDto.getStatusCode()) {
            if (responseDto.getCaption() == null)
                responseDto.setCaption(context.getString(R.string.ok_lbl));

        } else {
            if (responseDto.getCaption() == null)
                responseDto.setCaption(context.getString(R.string.error_lbl));
        }
        responseDto.setOperationType(operation).setServiceCaller(serviceCaller);
        return responseDto;
    }

    public static void printKeyStoreInfo() throws CertificateException, NoSuchAlgorithmException,
            IOException, KeyStoreException, UnrecoverableEntryException {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.KeyStore.PrivateKeyEntry keyEntry = (java.security.KeyStore.PrivateKeyEntry)
                keyStore.getEntry("USER_CERT_ALIAS", null);
        Enumeration aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            LOGD(TAG, "Subject DN: " + cert.getSubjectX500Principal().toString());
            LOGD(TAG, "Issuer DN: " + cert.getIssuerDN().getName());
        }
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) return arguments;
        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }
        return arguments;
    }

    public static void launchPasswordInputActivity(String message, String operationCode,
                               PasswordInputStep step, Integer requestCode, Activity context) {
        OperationPassword operationPassword = PrefUtils.getOperationPassword();
        Intent intent = null;
        if (operationPassword != null) {
            switch (operationPassword.getInputType()) {
                case PATTER_LOCK:
                    intent = new Intent(context, PatternLockInputActivity.class);
                    break;
                case PIN:
                    intent = new Intent(context, PinInputActivity.class);
                    break;
            }
            if(message != null)
                intent.putExtra(Constants.MESSAGE_KEY, message);
            if(operationCode != null)
                intent.putExtra(Constants.OPERATION_CODE_KEY, operationCode);
            intent.putExtra(Constants.STEP_KEY, step);
            context.startActivityForResult(intent, requestCode);
        }
    }

    //http://stackoverflow.com/questions/1995439/get-android-phone-model-programmatically
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model.toLowerCase();
        } else {
            return (manufacturer + " " + model);
        }
    }

    //http://www.pocketmagic.net/android-unique-device-id/
    //requires adding a permission in AndroidManifest.xml -> android.permission.READ_PHONE_STATE
    public static String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) App.getInstance().getSystemService(
                Context.TELEPHONY_SERVICE);
        // phone = telephonyManager.getLine1Number(); -> operator dependent
        //IMSI
        //phone = telephonyManager.getSubscriberId();
        //the IMEI for GSM and the MEID or ESN for CDMA phones. Null if device ID is not available,
        //only for Android devices with Phone use.
        String deviceId = telephonyManager.getDeviceId();
        if (deviceId == null || deviceId.trim().isEmpty()) {
            deviceId = android.os.Build.SERIAL;
            if (deviceId == null || deviceId.trim().isEmpty()) {
                deviceId = UUID.randomUUID().toString();
            }
        }
        return deviceId;
    }

    public static void launchCurrencyStatusCheck(String broadCastId, String revocationHash) {
        Intent startIntent = new Intent(App.getInstance(), PaymentService.class);
        startIntent.putExtra(Constants.OPERATION_KEY, OperationType.CURRENCY_STATE);
        startIntent.putExtra(Constants.CALLER_KEY, broadCastId);
        startIntent.putExtra(Constants.REVOCATION_HASH, revocationHash);
        App.getInstance().startService(startIntent);
    }

    public static void launchSessionIndentificationActivity(Activity  activity, int activityResultCode){
        /*Intent intent = new Intent(activity, IdCardNFCReaderActivity.class);
        intent.putExtra(Constants.MESSAGE_KEY, activity.getString(R.string.enter_password_msg));
        intent.putExtra(Constants.MODE_KEY, IdCardNFCReaderActivity.MODE_INIT_SESSION);
        intent.putExtra(Constants.CONTENT_TYPE_KEY, MediaType.JSON);
        activity.startActivityForResult(intent, activityResultCode);*/
    }

    public static void launchIdCardSignatureActivity(byte[] documentToSign, String messagePrefix,
            String contentType, IdCardNFCReaderActivity.Step step, Integer activityResultCode,
            String operationUUID, Activity context) throws Exception {
        Intent intent = new Intent(context, IdCardNFCReaderActivity.class);
        intent.putExtra(Constants.MESSAGE_KEY, messagePrefix);
        intent.putExtra(Constants.MESSAGE_CONTENT_KEY, documentToSign);
        intent.putExtra(Constants.STEP_KEY, step);
        intent.putExtra(Constants.CONTENT_TYPE_KEY, contentType);
        if (operationUUID != null) {
            intent.putExtra(Constants.QR_CODE_KEY, operationUUID.substring(0, 4));
        }
        context.startActivityForResult(intent, activityResultCode);
    }

}