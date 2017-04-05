package org.currency.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.util.Store;
import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.cms.CMSUtils;
import org.currency.cms.DNIeContentSigner;
import org.currency.crypto.CertificationRequest;
import org.currency.crypto.PEMUtils;
import org.currency.dto.DeviceDto;
import org.currency.dto.OperationTypeDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.UserDto;
import org.currency.dto.identity.RegisterDto;
import org.currency.dto.identity.SessionCertificationDto;
import org.currency.fragment.MessageDialogFragment;
import org.currency.fragment.ProgressDialogFragment;
import org.currency.http.MediaType;
import org.currency.http.SessionInfo;
import org.currency.ui.DNIePasswordDialog;
import org.currency.ui.DialogButton;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.UIUtils;
import org.currency.xades.SignatureAlgorithm;
import org.currency.xades.SignatureBuilder;
import org.currency.xades.XAdESUtils;
import org.currency.xml.XMLUtils;

import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import es.gob.jmulticard.jse.provider.DnieProvider;
import es.gob.jmulticard.ui.passwordcallback.CancelledOperationException;
import es.gob.jmulticard.ui.passwordcallback.DNIeDialogManager;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class IdCardNFCReaderActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    public static final String TAG = IdCardNFCReaderActivity.class.getSimpleName();

    public static final String CERT_AUTENTICATION = "CertAutenticacion";
    public static final String CERT_SIGN          = "CertFirmaDigital";

    public enum Step {
        REGISTER_DEVICE, INIT_SESSION, SIGN_DOCUMENT
    }

    //public static final int MODE_REQUEST_PASSWORD = 0;
    //public static final int MODE_INIT_SESSION     = 1;

    private char[] idCardPassword = null;

    private byte[] contentToSign;
    private String contentType;
    private String dnieCAN;
    private NfcAdapter myNfcAdapter;
    private Tag tagFromIntent;
    private CertificationRequest csrRequest;

    //private int activityRequestMode;
    private Step step;

    private Handler myHandler = new Handler();

    final Runnable newRead = new Runnable() {
        public void run() {
            ((TextView) findViewById(R.id.msg)).setText( R.string.process_msg_reading);
            ((ImageView) findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_peq);
        }
    };

    final Runnable askForRead = new Runnable() {
        public void run() {
            ((ImageView) findViewById(R.id.result_img)).setImageResource(R.drawable.dni30_grey_peq);
            ((TextView) findViewById(R.id.msg)).setText(R.string.op_dgtinit);
        }
    };

    private void showMessageDialog(String caption, String message) {
        ProgressDialogFragment.hide(getSupportFragmentManager());
        MessageDialogFragment.showDialog(caption, message, getSupportFragmentManager());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.idcard_nfc_reader_activity);
        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (myNfcAdapter == null) {
            DialogButton positiveButton = new DialogButton(getString(R.string.ok_lbl),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            IdCardNFCReaderActivity.this.finish();
                        }
                    });
            UIUtils.showMessageDialog(getString(R.string.msg_lbl), getString(
                    R.string.nfc_required_msg), positiveButton, null, this);
        } else {
            dnieCAN = PrefUtils.getDNIeCAN();
            if (dnieCAN == null)
                launchUserDataFormActivity(getString(R.string.dnie_can_missing_msg));
            myNfcAdapter.setNdefPushMessage(null, this);
            myNfcAdapter.setNdefPushMessageCallback(null, this);
            contentToSign = getIntent().getExtras().getByteArray(Constants.MESSAGE_CONTENT_KEY);
            contentType = getIntent().getExtras().getString(Constants.CONTENT_TYPE_KEY, MediaType.XML);
            String qrMessage = getIntent().getExtras().getString(Constants.QR_CODE_KEY);
            if (qrMessage != null) {
                ((TextView) findViewById(R.id.qrCodeMsg)).setText(qrMessage.toUpperCase());
                findViewById(R.id.qrCodeMsg).setVisibility(View.VISIBLE);
            } else findViewById(R.id.qrCodeMsg).setVisibility(View.GONE);
            String message = getIntent().getExtras().getString(Constants.MESSAGE_KEY);
            if (message != null) {
                ((TextView) findViewById(R.id.topMsg)).setText(message);
            } else findViewById(R.id.topMsg).setVisibility(View.GONE);
        }
        step = (Step) getIntent().getExtras().getSerializable(Constants.STEP_KEY);
    }

    private void launchUserDataFormActivity(String message) {
        Intent intent = new Intent(this, UserDataFormActivity.class);
        if(message != null)
            intent.putExtra(Constants.MESSAGE_KEY, getString(R.string.dnie_can_missing_msg));
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (myNfcAdapter != null)
            enableNFCReaderMode();
    }

    @Override
    public void onPause() {
        super.onPause();
        LOGD(TAG, "disableNFCReaderMode");
        if (myNfcAdapter != null)
            disableNFCReaderMode();
    }

    private void enableNFCReaderMode() {
        LOGD(TAG, "enableNFCReaderMode");
        Bundle options = new Bundle();
        //30 secs to check NFC reader
        //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
        myNfcAdapter.enableReaderMode(this, this,
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B,
                options);
    }

    private void disableNFCReaderMode() {
        LOGD(TAG, "disableNFCReaderMode");
        myNfcAdapter.disableReaderMode(this);
    }

    Runnable cardOperation = new Runnable() {
        @Override
        public void run() {
            ResponseDto responseDto = null;
            DnieProvider dnieProvider = new DnieProvider();
            LOGD(TAG, "is dnieProvider loaded: " + (Security.getProvider(dnieProvider.getName()) != null));
            try {
                dnieProvider.setProviderTag(tagFromIntent);
                dnieProvider.setProviderCan(dnieCAN);
                Security.insertProviderAt(dnieProvider, 1);
                //Deactivate fastmode
                System.setProperty("es.gob.jmulticard.fastmode", "false");
                DNIePasswordDialog passwordDialog = new DNIePasswordDialog(
                        IdCardNFCReaderActivity.this, idCardPassword, true);
                DNIeDialogManager.setDialogUIHandler(passwordDialog);

                KeyStore ksUserDNIe = KeyStore.getInstance("MRTD");
                ksUserDNIe.load(null, null);
                //force load real certs
                //ksUserDNIe.getKey(CERT_SIGN, null);
                //X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);
                //LOGD(TAG, "userCert: " + userCert.toString());

                PrivateKey privateKey = (PrivateKey) ksUserDNIe.getKey(CERT_SIGN, null);
                X509Certificate userCert = (X509Certificate) ksUserDNIe.getCertificate(CERT_SIGN);
                Certificate[] chain = ksUserDNIe.getCertificateChain(CERT_SIGN);
                Store cerStore = new JcaCertStore(Arrays.asList(chain));
                List<X509Certificate> certChain = new ArrayList<>();
                for (Certificate certificate : chain) {
                    certChain.add((X509Certificate) certificate);
                }
                LOGD(TAG, "userCert: " + userCert.getIssuerX500Principal().toString());
                switch (step) {
                    case REGISTER_DEVICE:
                        contentToSign = buildRegistrationRequest(userCert);
                        break;
                    case INIT_SESSION:
                        contentToSign = buildInitSessionRequest(userCert);
                        break;
                }
                switch (contentType) {
                    case MediaType.XML:
                        String contentToSign = XMLUtils.prepareRequestToSign(
                                IdCardNFCReaderActivity.this.contentToSign);
                        byte[] signatureBytes = new SignatureBuilder(contentToSign.getBytes(),
                                XAdESUtils.XML_MIME_TYPE,
                                SignatureAlgorithm.RSA_SHA_256.getName(), privateKey, userCert,
                                certChain, App.getInstance().getTimeStampServiceURL()).build();
                        responseDto = new ResponseDto(ResponseDto.SC_OK, null, signatureBytes);
                        break;
                    default:
                        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(
                                IdCardNFCReaderActivity.this.contentToSign,
                                App.getInstance().getTimeStampServiceURL());
                        CMSSignedData cmsSignedData = DNIeContentSigner.signData(privateKey, userCert,
                                cerStore, IdCardNFCReaderActivity.this.contentToSign, timeStampToken);
                        responseDto = new ResponseDto(ResponseDto.SC_OK,
                                new CMSSignedMessage(cmsSignedData).toPEM());
                        break;
                }
                myHandler.post(askForRead);
                ProgressDialogFragment.hide(getSupportFragmentManager());
                if (responseDto != null)
                    finishOk(responseDto, userCert);
            } catch (CancelledOperationException ex) {
                showMessageDialog(getString(R.string.error_lbl), ex.getMessage());
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                myHandler.post(askForRead);
                showMessageDialog(getString(R.string.error_lbl),
                        getString(R.string.dnie_connection_null_error_msg));
            } catch (ProviderException ex) {
                ex.printStackTrace();
                myHandler.post(askForRead);
                showMessageDialog(getString(R.string.error_lbl), ex.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                String exMsg = "";
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("pace. can"))
                    exMsg = " - " + ex.getMessage();
                myHandler.post(askForRead);
                if(exMsg.contains("PACE. CAN incorrecto"))
                    launchUserDataFormActivity(getString(R.string.dnie_connection_error_msg) + exMsg);
                else
                    showMessageDialog(getString(R.string.error_lbl),
                            getString(R.string.dnie_connection_error_msg) + exMsg);
            } finally {
                //this seems a bad idea
                //LOGD(TAG, "removing provider: " + dnieProvider.getName());
                //Security.removeProvider(dnieProvider.getName());
            }
        }
    };

    private byte[] buildInitSessionRequest(X509Certificate x509Certificate) throws Exception {
        UserDto user = UserDto.getUser(x509Certificate);
        String mobileUUID = UUID.randomUUID().toString();
        CertificationRequest mobileCsrReq = CertificationRequest.getUserRequest(
                user.getNumId(), user.getEmail(), user.getPhone(), "currency-mobile-session",
                mobileUUID, user.getGivenName(), user.getSurName(), DeviceDto.Type.MOBILE);

        String browserUUID = UUID.randomUUID().toString();
        CertificationRequest browserCsrReq = CertificationRequest.getUserRequest(
                user.getNumId(), user.getEmail(), user.getPhone(), "currency-pc-session",
                browserUUID, user.getGivenName(), user.getSurName(), DeviceDto.Type.BROWSER);

        SessionCertificationDto sessionCertDto = new SessionCertificationDto(user,
                new String(mobileCsrReq.getCsrPEM()), mobileUUID,
                new String(browserCsrReq.getCsrPEM()), browserUUID).setOperation(
                new OperationTypeDto(OperationType.SESSION_CERTIFICATION,
                App.getInstance().getCurrencyService().getEntity().getId()));

        SessionInfo sessionInfo = new SessionInfo(mobileCsrReq, browserCsrReq);
        App.getInstance().setSessionInfo(sessionInfo);

        return JSON.getMapper().writeValueAsBytes(sessionCertDto);
    }

    private byte[] buildRegistrationRequest(X509Certificate userCert) throws Exception {
        UserDto user = UserDto.getUser(userCert);
        String deviceId = PrefUtils.getDeviceId();
        csrRequest = CertificationRequest.getUserRequest(
                user.getNumId(), user.getEmail(), user.getPhone(), "currency-mobile-application",
                deviceId, user.getGivenName(), user.getSurName(), DeviceDto.Type.MOBILE);
        RegisterDto registerDto = RegisterDto.build(user, deviceId, new String(csrRequest.getCsrPEM()));
        return JSON.getMapper().writeValueAsBytes(registerDto);
    }

    private void finishOk(ResponseDto response, X509Certificate userCert) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.RESPONSE_KEY, response);
        try {
            resultIntent.putExtra(Constants.CERT_REQUEST_KEY, csrRequest);
            resultIntent.putExtra(Constants.USER_KEY, new String(PEMUtils.getPEMEncoded(userCert)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        LOGD(TAG, "onTagDiscovered");
        tagFromIntent = tag;
        NfcA exNfcA = NfcA.get(tagFromIntent);
        NfcB exNfcB = NfcB.get(tagFromIntent);
        IsoDep exIsoDep = IsoDep.get(tagFromIntent);
        if ((exNfcA != null) || (exNfcB != null) || (exIsoDep != null)) {
            myHandler.post(newRead);
            MessageDialogFragment.hide(getSupportFragmentManager());
            ProgressDialogFragment.showDialog(getString(R.string.dnie_sign_progress_caption),
                    getString(R.string.dnie_sign_connecting_msg),
                    getSupportFragmentManager());
            new Thread(cardOperation).start();
        }
    }

}