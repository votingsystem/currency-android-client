package org.currency.model;

import org.currency.App;
import org.currency.android.R;
import org.currency.cms.CMSSignedMessage;
import org.currency.dto.TagDto;
import org.currency.dto.currency.CurrencyBatchDto;
import org.currency.dto.currency.TransactionDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.throwable.ValidationException;
import org.currency.util.JSON;
import org.currency.util.OperationType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyBundle {
    
    private List<Currency> tagTimeUnlimitedCurrencyList;
    private List<Currency> tagTimeLimitedCurrencyList;
    private List<Currency> wildTagTimeUnlimitedCurrencyList;
    private List<Currency> wildTagTimeLimitedCurrencyList;
    private String currencyCode;
    private String tag;

    public CurrencyBundle(String currencyCode, String tag) {
        this.currencyCode = currencyCode;
        this.tag = tag;
        tagTimeUnlimitedCurrencyList = new ArrayList<>();
        tagTimeLimitedCurrencyList = new ArrayList<>();
        wildTagTimeUnlimitedCurrencyList = new ArrayList<>();
        wildTagTimeLimitedCurrencyList = new ArrayList<>();
    }

    public void addCurrency(Currency currency) throws ValidationException {
        if(!currency.getTag().equals(tag) && !TagDto.WILDTAG.equals(currency.getTag()))
            throw new ValidationException("CurrencyBundle for Tag: " + tag + " - can't have " +
                    "items with tag: " + currency.getTag());
        if(!currency.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("CurrencyBundle for currencyCode: " + currencyCode +
                    " - can't have items with currencyCode: " + currency.getCurrencyCode());
        if(currency.getTag().equals(tag)) {
            if(currency.isTimeLimited()) tagTimeLimitedCurrencyList.add(currency);
            else tagTimeUnlimitedCurrencyList.add(currency);
        } else {
            if(currency.isTimeLimited()) wildTagTimeLimitedCurrencyList.add(currency);
            else wildTagTimeUnlimitedCurrencyList.add(currency);
        }
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public static CurrencyBundle load(Collection<Currency> currencyList) throws Exception {
        CurrencyBundle currencyBundle = null;
        for(Currency currency : currencyList) {
            if(currencyBundle == null)  {
                currencyBundle = new CurrencyBundle(currency.getCurrencyCode(), currency.getTag());
            }
            currencyBundle.addCurrency(currency);
        }
        return currencyBundle;
    }


    public CurrencyBatchDto getCurrencyBatchDto(TransactionDto transactionDto) throws Exception {
        String toUserIBAN = transactionDto.getToUserIBAN() == null ? null:
                transactionDto.getToUserIBAN().iterator().next();
        return getCurrencyBatchDto(transactionDto.getOperation(),
                transactionDto.getSubject(), toUserIBAN,
                transactionDto.getAmount(), transactionDto.getCurrencyCode(),
                transactionDto.getTag().getName(), transactionDto.isTimeLimited(),
                transactionDto.getUUID());
    }
    
    public CurrencyBatchDto getCurrencyBatchDto(OperationType operation, String subject,
                String toUserIBAN, BigDecimal batchAmount, String currencyCode, String tag,
                Boolean timeLimited, String uuid) throws Exception {
        if(!currencyCode.equals(this.currencyCode)) throw new ValidationException(
                "Bundle with currencyCode: " + this.currencyCode + " can't handle currencyCode: " +
                currencyCode);
        if(!tag.equals(this.tag)) throw new ValidationException(
                "Bundle with tag: " + this.tag + " can't handle tag: " + tag);
        CurrencyBatchDto dto = new CurrencyBatchDto();
        dto.setOperation(operation);
        dto.setSubject(subject);
        dto.setToUserIBAN(toUserIBAN);
        dto.setBatchAmount(batchAmount);
        dto.setCurrencyCode(currencyCode);
        dto.setTag(tag);
        dto.setTimeLimited(timeLimited);
        if(uuid == null) dto.setBatchUUID(UUID.randomUUID().toString());
        else dto.setBatchUUID(uuid);
        Set<String> currencySetSignatures = new HashSet<>();
        Set<Currency> currencySet = new HashSet<>();

        List<Currency> wildTagTimeLimitedCurrencyList = new ArrayList<>(this.wildTagTimeLimitedCurrencyList);
        List<Currency> wildTagTimeUnlimitedCurrencyList = new ArrayList<>(this.wildTagTimeUnlimitedCurrencyList);
        List<Currency> tagTimeLimitedCurrencyList = new ArrayList<>(this.tagTimeLimitedCurrencyList);
        List<Currency> tagTimeUnlimitedCurrencyList = new ArrayList<>(this.tagTimeUnlimitedCurrencyList);

        Collections.sort(wildTagTimeLimitedCurrencyList, currencyComparator);
        Collections.sort(wildTagTimeUnlimitedCurrencyList, currencyComparator);
        Collections.sort(tagTimeLimitedCurrencyList, currencyComparator);
        Collections.sort(tagTimeUnlimitedCurrencyList, currencyComparator);
        BigDecimal wildTagTimeLimitedAmount = getTotalAmount(wildTagTimeLimitedCurrencyList);
        BigDecimal wildTagTimeUnlimitedAmount = getTotalAmount(wildTagTimeUnlimitedCurrencyList);
        BigDecimal wildTagAmount = wildTagTimeLimitedAmount.add(wildTagTimeUnlimitedAmount);
        BigDecimal tagTimeLimitedAmount = getTotalAmount(tagTimeLimitedCurrencyList);
        BigDecimal tagTimeUnlimitedAmount = getTotalAmount(tagTimeUnlimitedCurrencyList);
        BigDecimal tagAmount = tagTimeLimitedAmount.add(tagTimeUnlimitedAmount);
        BigDecimal tagAmountPlusWiltagLimited = tagAmount.add(wildTagTimeLimitedAmount);
        BigDecimal totalAmount = tagAmount.add(wildTagAmount);
        BigDecimal bundleAccumulated = BigDecimal.ZERO;
        Currency lastCurrencyAdded = null;
        if(batchAmount.compareTo(tagTimeLimitedAmount) < 0) {
            while(bundleAccumulated.compareTo(batchAmount) < 0) {
                lastCurrencyAdded = tagTimeLimitedCurrencyList.remove(0);
                currencySet.add(lastCurrencyAdded);
                bundleAccumulated = bundleAccumulated.add(lastCurrencyAdded.getAmount());
            }
        } else if(batchAmount.compareTo(tagAmount) < 0) {
            List<Currency> tagCurrencyList = new ArrayList<>(tagTimeLimitedCurrencyList);
            tagCurrencyList.addAll(tagTimeUnlimitedCurrencyList);
            Collections.sort(tagCurrencyList, currencyComparator);
            while(bundleAccumulated.compareTo(batchAmount) < 0) {
                lastCurrencyAdded = tagCurrencyList.remove(0);
                currencySet.add(lastCurrencyAdded);
                bundleAccumulated = bundleAccumulated.add(lastCurrencyAdded.getAmount());
            }
        } else if(batchAmount.compareTo(tagAmountPlusWiltagLimited) < 0) {
            List<Currency> tagCurrencyList = new ArrayList<>(tagTimeLimitedCurrencyList);
            tagCurrencyList.addAll(tagTimeUnlimitedCurrencyList);
            tagCurrencyList.addAll(wildTagTimeLimitedCurrencyList);
            Collections.sort(tagCurrencyList, currencyComparator);
            while(bundleAccumulated.compareTo(batchAmount) < 0) {
                lastCurrencyAdded = tagCurrencyList.remove(0);
                currencySet.add(lastCurrencyAdded);
                bundleAccumulated = bundleAccumulated.add(lastCurrencyAdded.getAmount());
            }
        } else if(batchAmount.compareTo(totalAmount) <= 0) {
            List<Currency> tagCurrencyList = new ArrayList<>(tagTimeLimitedCurrencyList);
            tagCurrencyList.addAll(tagTimeUnlimitedCurrencyList);
            tagCurrencyList.addAll(wildTagTimeLimitedCurrencyList);
            tagCurrencyList.addAll(wildTagTimeUnlimitedCurrencyList);
            Collections.sort(tagCurrencyList, currencyComparator);
            while(bundleAccumulated.compareTo(batchAmount) < 0) {
                lastCurrencyAdded = tagCurrencyList.remove(0);
                currencySet.add(lastCurrencyAdded);
                bundleAccumulated = bundleAccumulated.add(lastCurrencyAdded.getAmount());
            }
        } else throw new ValidationException(App.getInstance().getString(R.string.tagAmountErrorMsg,
                batchAmount + " " + currencyCode, tag, totalAmount  + " " + currencyCode ));
        if(bundleAccumulated.compareTo(batchAmount) > 0) {
            BigDecimal leftOverAmount = bundleAccumulated.subtract(batchAmount);
            dto.setLeftOverCurrency(new Currency(
                    App.getInstance().getCurrencyService().getEntity().getId(),
                    leftOverAmount, lastCurrencyAdded.getCurrencyCode(),
                    lastCurrencyAdded.isTimeLimited(), lastCurrencyAdded.getTag()));
        }
        for(Currency currency : currencySet) {
            MetadataDto metadataDto = App.getInstance().getCurrencyService();
            CMSSignedMessage cmsMessage = currency.getCertificationRequest().signData(
                    JSON.writeValueAsBytes(dto),
                    OperationType.TIMESTAMP_REQUEST.getUrl(metadataDto.getFirstTimeStampEntityId()));
            currency.setCMS(cmsMessage);
            currencySetSignatures.add(currency.getCMS().toPEMStr());
        }
        dto.setCurrencySet(currencySetSignatures);
        dto.setCurrencyCollection(currencySet);
        return dto;
    }

    public static BigDecimal getTotalAmount(Collection<Currency> currencyCollection) {
        BigDecimal result = BigDecimal.ZERO;
        for(Currency currency : currencyCollection) {
            result = result.add(currency.getAmount());
        }
        return result;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = getTotalAmount(tagTimeUnlimitedCurrencyList);
        result = result.add(getTotalAmount(tagTimeLimitedCurrencyList));
        result = result.add(getTotalAmount(wildTagTimeUnlimitedCurrencyList));
        result = result.add(getTotalAmount(wildTagTimeLimitedCurrencyList));
        return result;
    }

    private static Comparator<Currency> currencyComparator = new Comparator<Currency>() {
        public int compare(Currency c1, Currency c2) {
            return c1.getAmount().compareTo(c2.getAmount());
        }
    };

}