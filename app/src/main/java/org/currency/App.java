package org.currency;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.bouncycastle.tsp.TimeStampToken;
import org.currency.activity.MessageActivity;
import org.currency.android.R;
import org.currency.cms.CMSGenerator;
import org.currency.cms.CMSSignedMessage;
import org.currency.cms.CMSUtils;
import org.currency.crypto.Encryptor;
import org.currency.crypto.KeyGenerator;
import org.currency.dto.QRMessageDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.dto.metadata.TrustedEntitiesDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.http.SessionInfo;
import org.currency.http.SystemEntityType;
import org.currency.util.Constants;
import org.currency.util.DateUtils;
import org.currency.util.OperationType;
import org.currency.util.PrefUtils;
import org.currency.util.RootUtils;
import org.currency.util.UIUtils;
import org.currency.util.WebSocketSession;
import org.currency.xades.SignatureValidator;
import org.currency.xades.XmlSignature;
import org.currency.xml.XmlReader;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.currency.util.Constants.ALGORITHM_RNG;
import static org.currency.util.Constants.KEY_SIZE;
import static org.currency.util.Constants.PROVIDER;
import static org.currency.util.Constants.SIG_NAME;
import static org.currency.util.Constants.USER_CERT_ALIAS;
import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class App extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = App.class.getSimpleName();

    private static final Map<String, WebSocketSession> sessionsMap = new HashMap<>();
    private static final Map<String, MetadataDto> systemEntityMap = new HashMap<>();
    private static final Map<String, QRMessageDto> qrMessagesMap = new HashMap<>();
    private AtomicInteger notificationId = new AtomicInteger(1);
    private boolean isRootedPhone = false;
    private List<Integer> historyList;
    private char[] token;
    private int defaultMainView = R.id.currency_accounts;
    private MetadataDto currencyService;
    private MetadataDto idProvider;
    private SessionInfo sessionInfo;
    private PublicKey browserPublicKey;
    private AppStateImpl appState;
    private String dnieCertPEM;

    private static App INSTANCE;

    public static App getInstance() {
        return INSTANCE;
    }

    public void putSystemEntity(MetadataDto systemEntity) {
        systemEntityMap.put(systemEntity.getEntity().getId(), systemEntity);
    }

    public String getCurrentWeekLapseId() {
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        return DateUtils.getPath(currentLapseCalendar.getTime());
    }

    public boolean isSocketConnectionEnabled() {
        LOGD(TAG, "isSocketConnectionEnabled ======= TODO");
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            appState = AppStateImpl.START;
            INSTANCE = this;
            KeyGenerator.INSTANCE.init(SIG_NAME, PROVIDER, KEY_SIZE, ALGORITHM_RNG);
            /*if (!PrefUtils.isEulaAccepted(this)) {//Check if the EULA has been accepted; if not, show it.
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();}*/
            PrefUtils.registerPreferenceChangeListener(this);
            if (!Constants.ALLOW_ROOTED_PHONES && RootUtils.isDeviceRooted()) {
                isRootedPhone = true;
            }
            historyList = new ArrayList();
            historyList.add(defaultMainView);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public AppStateImpl getAppState() {
        return appState;
    }

    public void setAppState (AppStateImpl appState) {
        this.appState = appState;
    }

    public void finish() {
        LOGD(TAG, "finish");
        HttpConn.getInstance().shutdown();
        UIUtils.killApp(true);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        PrefUtils.unregisterPreferenceChangeListener(this);
    }

    public Integer getNotificationId() {
        return notificationId.get();
    }

    public KeyStore.PrivateKeyEntry getUserPrivateKey() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(
                USER_CERT_ALIAS, null);
        return keyEntry;
    }

    public void putSocketSession(String sessionUUID, WebSocketSession session) {
        sessionsMap.put(sessionUUID, session.setUUID(sessionUUID));
    }

    public WebSocketSession getSocketSession(String sessionUUID) {
        return sessionsMap.get(sessionUUID);
    }

    public WebSocketSession getSocketSessionByDevice(String deviceUUID) {
        for(WebSocketSession socketSession : sessionsMap.values()) {
            if(socketSession.getDevice() != null && socketSession.getDevice().getUUID()
                    .equals(deviceUUID)) {
                return socketSession;
            }
        }
        return null;
    }

    public X509Certificate getX509UserCert() throws CertificateException, UnrecoverableEntryException,
            NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        return (X509Certificate) keyEntry.getCertificateChain()[0];
    }

    public byte[] decryptMessage(byte[] encryptedPEM) throws Exception {
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        //X509Certificate cryptoTokenCert = (X509Certificate) keyEntry.getCertificateChain()[0];
        PrivateKey privateKey = keyEntry.getPrivateKey();
        return Encryptor.decryptCMS(encryptedPEM, privateKey);
    }

    public MetadataDto getSystemEntity(final String systemEntityId, boolean forceHTTPLoad) {
        MetadataDto result = systemEntityMap.get(systemEntityId);
        if(result == null && forceHTTPLoad) {
            try {
                result = getSystemEntityFromURL(OperationType.GET_METADATA.getUrl(systemEntityId));
            }catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private MetadataDto getSystemEntityFromURL(String entityURL) throws Exception {
        /*if(Looper.getMainLooper().getThread() != Thread.currentThread()) { //not in main thread,
            //if invoked from main thread -> android.os.NetworkOnMainThreadException
        } else {  }*/
        ResponseDto responseDto = HttpConn.getInstance().doGetRequest(entityURL, ContentType.XML);
        Set<XmlSignature> signatures = new SignatureValidator(responseDto.getMessageBytes()).validate();
        X509Certificate systemEntityCert = signatures.iterator().next().getSigningCertificate();
        MetadataDto systemEntity = XmlReader.readMetadata(responseDto.getMessageBytes());
        systemEntity.setSigningCertificate(systemEntityCert);
        putSystemEntity(systemEntity);
        return systemEntity;
    }

    public void putQRMessage(QRMessageDto messageDto) {
        qrMessagesMap.put(messageDto.getUUID(), messageDto);
    }

    public QRMessageDto getQRMessage(String uuid) {
        return qrMessagesMap.get(uuid);
    }

    public QRMessageDto removeQRMessage(String uuid) {
        return qrMessagesMap.remove(uuid);
    }

    public void showNotification(ResponseDto responseDto) {
        final NotificationManager mgr = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent clickIntent = new Intent(this, MessageActivity.class);
        clickIntent.putExtra(Constants.RESPONSE_KEY, responseDto);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                notificationId.getAndIncrement(), clickIntent, PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentIntent(pendingIntent).setWhen(System.currentTimeMillis())
                .setAutoCancel(true).setContentTitle(responseDto.getCaption())
                .setContentText(responseDto.getNotificationMessage()).setSound(soundUri);
        if (responseDto.getStatusCode() == ResponseDto.SC_ERROR)
            builder.setSmallIcon(R.drawable.cancel_22);
        else if (responseDto.getStatusCode() == ResponseDto.SC_OK)
            builder.setSmallIcon(R.drawable.fa_check_32);
        else builder.setSmallIcon(R.drawable.bank_cards_32);
        mgr.notify(notificationId.getAndIncrement(), builder.build());
    }

    public void broadcastResponse(ResponseDto responseDto) {
        LOGD(TAG + ".broadcastResponse", "statusCode: " + responseDto.getStatusCode() +
                " - type: " + responseDto.getOperationType() + " - serviceCaller: " +
                responseDto.getServiceCaller());
        Intent intent = new Intent(responseDto.getServiceCaller());
        intent.putExtra(Constants.RESPONSE_KEY, responseDto);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { }

    public boolean isRootedPhone() {
        return isRootedPhone;
    }

    public boolean isHistoryEmpty() {
        return historyList.isEmpty();
    }

    public char[] getToken() {
        return token;
    }

    public void setToken(char[] token) {
        this.token = token;
    }

    public Integer getHistoryItem() {
        if (historyList.isEmpty()) return null;
        else if (historyList.size() == 1) return historyList.remove(0);
        else return historyList.remove(historyList.size() - 2);
    }

    public void addHistoryItem(Integer item) {
        historyList.add(item);
    }

    public String getTimeStampServiceURL() {
        return OperationType.TIMESTAMP_REQUEST.getUrl(currencyService.getFirstTimeStampEntityId());
    }

    public MetadataDto getCurrencyService() {
        return currencyService;
    }

    public List<TrustedEntitiesDto.EntityDto> getTrustedEntityList(
            String entityId, SystemEntityType entityType){
        List<TrustedEntitiesDto.EntityDto> result = new ArrayList<>();
        MetadataDto metadata = systemEntityMap.get(entityId);
        if(metadata != null) {
            for(TrustedEntitiesDto.EntityDto trustedEntity: metadata.getTrustedEntities().getEntities()) {
                if(trustedEntity.getType() == entityType)
                    result.add(trustedEntity);
            }
        }
        return result;
    }

    public void setCurrencyService(MetadataDto currencyService) {
        LOGD(TAG + ".setCurrencyService", "currency-service: " + currencyService.getEntity());
        systemEntityMap.put(currencyService.getEntity().getId(), currencyService);
        this.currencyService = currencyService;
    }

    public MetadataDto getIdProvider() {
        return idProvider;
    }

    public void setIdProvider(MetadataDto idProvider) {
        this.idProvider = idProvider;
    }

    //method with http connections, if invoked from main thread -> android.os.NetworkOnMainThreadException
    public CMSSignedMessage signCMSMessage(byte[] contentToSign) throws Exception {
        LOGD(TAG + ".signCMSMessage", "signCMSMessage");
        KeyStore.PrivateKeyEntry keyEntry = getUserPrivateKey();
        CMSGenerator cmsGenerator = new CMSGenerator(keyEntry.getPrivateKey(),
                Arrays.asList(keyEntry.getCertificateChain()[0]),
                Constants.SIGNATURE_ALGORITHM, Constants.ANDROID_PROVIDER);
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(contentToSign,
                OperationType.TIMESTAMP_REQUEST.getUrl(currencyService.getFirstTimeStampEntityId()));
        return cmsGenerator.signData(contentToSign, timeStampToken);
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public void setSessionInfo(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public PublicKey getBrowserPublicKey() {
        return browserPublicKey;
    }

    public void setBrowserPublicKey(PublicKey browserPublicKey) {
        this.browserPublicKey = browserPublicKey;
    }

    public String getDnieCertPEM() {
        if(dnieCertPEM == null)
            dnieCertPEM = PrefUtils.getDNIeCert();
        return dnieCertPEM;
    }

}