package org.currency.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.core.type.TypeReference;

import org.currency.App;
import org.currency.crypto.CertificationRequest;
import org.currency.crypto.Encryptor;
import org.currency.dto.EncryptedBundleDto;
import org.currency.dto.OperationPassword;
import org.currency.dto.UserDto;
import org.currency.dto.currency.BalancesDto;
import org.currency.dto.currency.CurrencyDto;
import org.currency.model.Currency;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.currency.util.Constants.APPLICATION_ID_KEY;
import static org.currency.util.Constants.PRIVATE_PREFS;
import static org.currency.util.LogUtils.LOGD;

public class PrefUtils {

    private static final String TAG = PrefUtils.class.getSimpleName();

    public static final String OPERATION_PASSWORD_KEY = "OPERATION_PASSWORD_KEY";


    public static TimeZone getDisplayTimeZone() {
        return TimeZone.getDefault();
    }

    private static BalancesDto userBalances;
    private static OperationPassword operationPassword;


    public static String getDeviceId() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(Constants.APPLICATION_ID_KEY, null);
        if (applicationId == null) {
            applicationId = UUID.randomUUID().toString();
            saveStringToPrefs(APPLICATION_ID_KEY, applicationId);
            LOGD(TAG, ".getDeviceId - new applicationId: " + applicationId);
        }
        return applicationId;
    }

    public static void putDNIeCert(String dnieCertPEM) {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Constants.DNIE_KEY, dnieCertPEM);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getDNIeCert() {
        SharedPreferences pref = App.getInstance().getSharedPreferences(Constants.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        return pref.getString(Constants.DNIE_KEY, null);
    }

    public static void putDNIeCAN(String CAN) {
        saveStringToPrefs(Constants.CAN_KEY, CAN);
    }

    public static String getDNIeCAN() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getString(Constants.CAN_KEY, null);
    }

    public static Calendar getLastPendingOperationCheckedTime() {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        Calendar lastCheckedTime = Calendar.getInstance();
        lastCheckedTime.setTimeInMillis(sp.getLong(Constants.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0L));
        return lastCheckedTime;
    }

    public static void registerPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void resetPasswordRetries() {
        saveIntToPrefs(Constants.RETRIES_KEY, 0);
    }

    public static int incrementPasswordRetries() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        int numRetries = settings.getInt(Constants.RETRIES_KEY, 0) + 1;
        if (numRetries >= Constants.NUM_MAX_PASSW_RETRIES) {
            LOGD(TAG, "NUM. MAX RETRIES EXCEEDED (3). Resseting OperationPassword");
            putOperationPassword(null);
            saveStringToPrefs(Constants.PROTECTED_PASSWORD_KEY, null);
            resetPasswordRetries();
        } else {
            saveIntToPrefs(Constants.RETRIES_KEY, numRetries);
        }
        return numRetries;
    }

    public static void putProtectedPassword(OperationPassword.InputType accessInputType,
                                            char[] passw, char[] passwordToEncrypt) {
        try {
            EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                    new String(passw), new String(passwordToEncrypt).getBytes()).toDto();
            saveStringToPrefs(Constants.PROTECTED_PASSWORD_KEY, new String(ObjectUtils.serializeObject(ebDto)));
            putOperationPassword(new OperationPassword(accessInputType, passw));
        } catch (Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
    }

    private static void saveStringToPrefs(String key, String value) {
        try {
            if (value != null)
                value = value.trim();
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(key.trim(), value);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void saveIntToPrefs(String key, Integer value) {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(key.trim(), value);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static char[] getProtectedPassword(char[] passw) {
        char[] password = null;
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(Constants.PROTECTED_PASSWORD_KEY, null);
            if (dtoStr != null) {
                EncryptedBundleDto ebDto = (EncryptedBundleDto) ObjectUtils.deSerializeObject(dtoStr.getBytes());
                byte[] resultBytes = Encryptor.pbeAES_Decrypt(new String(passw),
                        ebDto.getEncryptedBundle());
                password = new String(resultBytes, "UTF-8").toCharArray();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            PrefUtils.incrementPasswordRetries();
        }
        return password;
    }

    public static String getCsrRequest() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(Constants.CSR_KEY, null);
    }

    public static void putCsrRequest(CertificationRequest certificationRequest) {
        try {
            byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
            saveStringToPrefs(Constants.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void putOperationPassword(OperationPassword operationPassword) {
        String passwAccessModeStr = null;
        if (operationPassword != null) {
            try {
                byte[] serialized = ObjectUtils.serializeObject(operationPassword);
                passwAccessModeStr = new String(serialized, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        saveStringToPrefs(OPERATION_PASSWORD_KEY, passwAccessModeStr);
        PrefUtils.operationPassword = operationPassword;
    }

    public static OperationPassword getOperationPassword() {
        if(operationPassword != null)
            return operationPassword;
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serialized = settings.getString(OPERATION_PASSWORD_KEY, null);
        if (serialized != null) {
            operationPassword = (OperationPassword) ObjectUtils.deSerializeObject(serialized.getBytes());
        }
        return operationPassword;
    }

    public static Date getCurrencyAccountsLastCheckDate() {
        SharedPreferences pref = App.getInstance().getSharedPreferences(Constants.PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(Constants.USER_ACCOUNT_LAST_CHECKED_KEY, 0));
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        if (lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public static BalancesDto getBalances() throws Exception {
        if (userBalances != null) return userBalances;
        SharedPreferences pref = App.getInstance().getSharedPreferences(
                Constants.PRIVATE_PREFS, Context.MODE_PRIVATE);
        String balancesStr = pref.getString(Constants.BALANCE_KEY, null);
        if (balancesStr == null) return new BalancesDto();
        try {
            userBalances = JSON.readValue(balancesStr, BalancesDto.class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return userBalances;
    }

    public static void putWallet(Collection<Currency> currencyCollection, char[] passw,
            char[] token) throws Exception {
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String encryptedWallet = null;
            if (currencyCollection != null) {
                Set<CurrencyDto> currencyDtoSet = CurrencyDto.serializeCollection(currencyCollection);
                byte[] walletBytes = JSON.writeValueAsBytes(currencyDtoSet);
                EncryptedBundleDto ebDto = Encryptor.pbeAES_Encrypt(
                        new String(passw) + new String(token), walletBytes).toDto();
                encryptedWallet = JSON.writeValueAsString(ebDto);
            }
            editor.putString(Constants.WALLET_FILE_NAME, encryptedWallet);
            editor.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Set<Currency> getWallet(char[] passw, char[] token) {
        Set<Currency> result = null;
        try {
            SharedPreferences settings = App.getInstance().getSharedPreferences(
                    PRIVATE_PREFS, Context.MODE_PRIVATE);
            String dtoStr = settings.getString(Constants.WALLET_FILE_NAME, null);
            if(dtoStr != null) {
                EncryptedBundleDto ebDto = JSON.readValue(dtoStr, EncryptedBundleDto.class);
                byte[] walletBytes = Encryptor.pbeAES_Decrypt(
                        new String(passw) + new String(token), ebDto.getEncryptedBundle());
                Set<CurrencyDto> currencyDtoSet = JSON.readValue(
                        walletBytes, new TypeReference<Set<CurrencyDto>>(){});
                result = CurrencyDto.deSerializeCollection(currencyDtoSet);
            }
        } catch(Exception ex) {ex.printStackTrace();}
        if(result == null) return new HashSet<>();
        return result;
    }

    public static void putBalances(BalancesDto balancesDto) throws Exception {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(Constants.USER_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        try {
            editor.putString(Constants.BALANCE_KEY, JSON.writeValueAsString(balancesDto));
            editor.commit();
            userBalances = balancesDto;
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static UserDto getAppUser() {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUser = settings.getString(Constants.USER_KEY, null);
        if(serializedUser != null) {
            UserDto user = (UserDto) ObjectUtils.deSerializeObject(serializedUser.getBytes());
            return user;
        }
        return null;
    }

    public static void putAppUser(UserDto user) {
        SharedPreferences settings = App.getInstance().getSharedPreferences(
                PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUser = ObjectUtils.serializeObject(user);
        try {
            editor.putString(Constants.USER_KEY, new String(serializedUser, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

}