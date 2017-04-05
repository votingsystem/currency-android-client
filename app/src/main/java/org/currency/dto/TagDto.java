package org.currency.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String WILDTAG = "WILDTAG";

    private Long id;
    private String name;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal timeLimited = BigDecimal.ZERO;
    private Long frequency;
    private Date dateCreated;
    private Date lastUpdated;

    public TagDto() { }

    public TagDto(String name) {
        this.name = name;
    }

    public TagDto(String name, BigDecimal total, BigDecimal timeLimited) {
        this.name = name;
        this.total = total;
        this.timeLimited = timeLimited;
    }
   
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        if(name != null) return name.trim();
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setFrequency(Long frequency) {
        this.frequency = frequency;
    }

    public Long getFrequency() {
        return frequency;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(BigDecimal timeLimited) {
        this.timeLimited = timeLimited;
    }

}