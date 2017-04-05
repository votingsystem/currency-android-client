package org.currency.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.currency.App;
import org.currency.android.R;
import org.currency.dto.TagDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.model.Currency;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    public static String formatElectionSubtitle(Context context, String params) {
        return null;
    }

    public static String formatIntervalTimeString(long intervalStart, long intervalEnd,
                                                  StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, intervalStart, intervalEnd, TIME_FLAGS,
                PrefUtils.getDisplayTimeZone().getID()).toString();
    }

    public static String getHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }
        }
        return null;
    }

    public static String getCertInfoMessage(X509Certificate certificate, Context context) {
        return context.getString(R.string.cert_info_formated_msg,
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                org.currency.util.DateUtils.getDayWeekDateStr(certificate.getNotBefore(), "HH:mm"),
                org.currency.util.DateUtils.getDayWeekDateStr(certificate.getNotAfter(), "HH:mm"));
    }

    public static String getTagMessage(String tag) {
        if(TagDto.WILDTAG.equals(tag))
            return App.getInstance().getString(R.string.wildtag_lbl);
        else
            return tag.toLowerCase();
    }

    public static String getCurrencyDescriptionMessage(Currency currency, Context context) {
        return currency.getAmount().toPlainString() + " " + currency.getCurrencyCode() +
                " " + context.getString(R.string.for_lbl ) + " '" + getTagMessage(currency.getTag()) + "'";
    }

    public static String getCurrencyRequestMessage(TransactionDto transactionDto, Context context) {
        String tagMessage = getTagMessage(transactionDto.getTag().getName());
        return context.getString(R.string.currency_request_msg, transactionDto.getAmount().toPlainString(),
                transactionDto.getCurrencyCode(), tagMessage);
    }

    public static String getTransactionConfirmMessage(TransactionDto transactionDto, Context context) {
        return context.getString(R.string.transaction_request_confirm_msg,
                transactionDto.getDescription(context),
                transactionDto.getAmount().toString() + " " + transactionDto.getCurrencyCode(),
                transactionDto.getToUserName());
    }

    public static String getUpdateCurrencyWithErrorMsg(Collection<Currency> currencyWithErrors, Context context) {
        Map<String, Map<String, BigDecimal>> expendedMap = new HashMap<>();
        Map<String,  Map<String, BigDecimal>> lapsedMap = new HashMap<>();
        Map<String,  Map<String, BigDecimal>> unknownMap = new HashMap<>();
        for(Currency currency : currencyWithErrors) {
            switch (currency.getState()) {
                case LAPSED:
                    if(lapsedMap.containsKey(currency.getCurrencyCode())) {
                        Map<String, BigDecimal> tagInfo = lapsedMap.get(currency.getCurrencyCode());
                        if(tagInfo == null) {
                            tagInfo = new HashMap<>();
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        } else {
                            BigDecimal tagAccumulated = tagInfo.get(currency.getTag()).add(
                                    currency.getAmount());
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        }
                        lapsedMap.put(currency.getCurrencyCode(), tagInfo);
                    } else {
                        Map<String, BigDecimal> tagInfo = new HashMap<>();
                        tagInfo.put(currency.getTag(), currency.getAmount());
                        lapsedMap.put(currency.getCurrencyCode(), tagInfo);
                    }
                    break;
                case EXPENDED:
                    if(expendedMap.containsKey(currency.getCurrencyCode())) {
                        Map<String, BigDecimal> tagInfo = expendedMap.get(currency.getCurrencyCode());
                        if(tagInfo == null) {
                            tagInfo = new HashMap<>();
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        } else {
                            BigDecimal tagAccumulated = tagInfo.get(currency.getTag()).add(
                                    currency.getAmount());
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        }
                        expendedMap.put(currency.getCurrencyCode(), tagInfo);
                    } else {
                        Map<String, BigDecimal> tagInfo = new HashMap<>();
                        tagInfo.put(currency.getTag(), currency.getAmount());
                        expendedMap.put(currency.getCurrencyCode(), tagInfo);
                    }
                    break;
                case UNKNOWN:
                    if(unknownMap.containsKey(currency.getCurrencyCode())) {
                        Map<String, BigDecimal> tagInfo = unknownMap.get(currency.getCurrencyCode());
                        if(tagInfo == null) {
                            tagInfo = new HashMap<>();
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        } else {
                            BigDecimal tagAccumulated = tagInfo.get(currency.getTag()).add(
                                    currency.getAmount());
                            tagInfo.put(currency.getTag(), currency.getAmount());
                        }
                        unknownMap.put(currency.getCurrencyCode(), tagInfo);
                    } else {
                        Map<String, BigDecimal> tagInfo = new HashMap<>();
                        tagInfo.put(currency.getTag(), currency.getAmount());
                        unknownMap.put(currency.getCurrencyCode(), tagInfo);
                    }
                    break;
                default:
            }
        }
        StringBuilder sb = new StringBuilder();
        if(expendedMap.size() > 0) {
            for(String currency : expendedMap.keySet()) {
                Map<String, BigDecimal> tagInfo = expendedMap.get(currency);
                for(String tag: tagInfo.keySet()) {
                    sb.append(context.getString(R.string.currency_expended_msg, tagInfo.get(tag).
                            toString() + " " + currency, getTagMessage(tag)) + "<br/>");
                }

            }

        }
        if(lapsedMap.size() > 0) {
            for(String currency : lapsedMap.keySet()) {
                Map<String, BigDecimal> tagInfo = lapsedMap.get(currency);
                for(String tag: tagInfo.keySet()) {
                    sb.append(context.getString(R.string.currency_lapsed_msg, tagInfo.get(tag).
                            toString() + " " + currency, getTagMessage(tag)) + "<br/>");

                }
            }
        }
        if(unknownMap.size() > 0) {
            for(String currency : unknownMap.keySet()) {
                Map<String, BigDecimal> tagInfo = unknownMap.get(currency);
                for(String tag: tagInfo.keySet()) {
                    sb.append(context.getString(R.string.currency_unknown_msg, tagInfo.get(tag).
                            toString() + " " + currency, getTagMessage(tag)) + "<br/>");

                }
            }
        }
        String result = context.getString(R.string.updated_currency_with_error_msg) + ":<br/>" +
                sb.toString();
        return result;
    }

    public static String getCurrencyStateMessage(Currency currency, Context context) {
        switch(currency.getState()) {
            case EXPENDED: return context.getString(R.string.expended_lbl);
            case LAPSED: return context.getString(R.string.lapsed_lbl);
            default:return null;
        }
    }

}