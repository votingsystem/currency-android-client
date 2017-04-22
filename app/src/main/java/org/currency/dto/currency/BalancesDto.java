package org.currency.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.currency.dto.UserDto;
import org.currency.throwable.ValidationException;
import org.currency.util.TimePeriod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancesDto {

    public static final String TAG = BalancesDto.class.getSimpleName();

    private UserDto user;
    private TimePeriod timePeriod;
    private List<TransactionDto> transactionFromList;
    private List<TransactionDto> transactionToList;
    private Map<String, Map> balances;
    private Map<String, BigDecimal> balancesFrom = new HashMap<>();
    private Map<String, BigDecimal> balancesTo = new HashMap<>();
    private Map<String, BigDecimal> balancesCash = new HashMap<>();

    public BalancesDto() {}


    public static BalancesDto TO(List<TransactionDto> transactionList, Map<String, BigDecimal> balancesTo) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionToList(transactionList);
        dto.setBalancesTo(balancesTo);
        return dto;
    }

    public static BalancesDto FROM(List<TransactionDto> transactionList, Map<String, BigDecimal> balancesFrom) {
        BalancesDto dto = new BalancesDto();
        dto.setTransactionFromList(transactionList);
        dto.setBalancesFrom(balancesFrom);
        return dto;
    }

    public void setTo(BalancesDto balancesToDto) {
        setTransactionToList(balancesToDto.getTransactionToList());
        setBalancesTo(balancesToDto.getBalancesTo());
    }


    public void setFrom(BalancesDto balancesFromDto) {
        setTransactionFromList(balancesFromDto.getTransactionFromList());
        setBalancesFrom(balancesFromDto.balancesFrom);
    }

    public void calculateCash() {
        balancesCash = new HashMap<>(balancesTo);
        for(String currencyCode: balancesFrom.keySet()) {
            if(balancesCash.containsKey(currencyCode)) {
                balancesCash.put(currencyCode, balancesCash.get(currencyCode).subtract(
                        balancesFrom.get(currencyCode)));
            } else {
                balancesCash.put(currencyCode, balancesFrom.get(currencyCode).negate());
            }
        }
    }

    public BigDecimal getCurrencyCodeAvailable(String currencyCode) throws ValidationException {
        return balancesCash.get(currencyCode);
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public List<TransactionDto> getTransactionList() {
        List<TransactionDto> result = new ArrayList<>(transactionToList);
        result.addAll(transactionFromList);
        return result;
    }

    public Map<String, Map> getBalances() {
        return balances;
    }

    public void setBalances(Map<String, Map> balances) {
        this.balances = balances;
    }

    public List<TransactionDto> getTransactionFromList() {
        return transactionFromList;
    }

    public void setTransactionFromList(List<TransactionDto> transactionFromList) {
        this.transactionFromList = transactionFromList;
    }

    public List<TransactionDto> getTransactionToList() {
        return transactionToList;
    }

    public void setTransactionToList(List<TransactionDto> transactionToList) {
        this.transactionToList = transactionToList;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<String, BigDecimal> getBalancesCash() {
        if(balancesCash == null) calculateCash();
        return balancesCash;
    }

    public void setBalancesCash(Map<String, BigDecimal> balancesCash) {
        this.balancesCash = balancesCash;
    }

    public Map<String, BigDecimal> getBalancesFrom() {
        return balancesFrom;
    }

    public void setBalancesFrom(Map<String, BigDecimal> balancesFrom) {
        this.balancesFrom = balancesFrom;
    }

    public Map<String, BigDecimal> getBalancesTo() {
        return balancesTo;
    }

    public void setBalancesTo(Map<String, BigDecimal> balancesTo) {
        this.balancesTo = balancesTo;
    }

    @JsonIgnore
    public Set<String> getCurrencyCodes() {
        if(this.balancesCash != null) {
            return balancesCash.keySet();
        } else return Collections.emptySet();
    }


}