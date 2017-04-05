package org.currency.dto;

import android.net.Uri;
import android.util.Base64;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle2.asn1.x500.RDN;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x500.style.BCStyle;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.jce.X509Principal;
import org.currency.crypto.CertUtils;
import org.currency.util.Constants;
import org.currency.util.Country;
import org.currency.util.IdDocument;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.currency.util.LogUtils.LOGD;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = UserDto.class.getSimpleName();

    public enum State {ACTIVE, PENDING, SUSPENDED, CANCELED}

    public enum Type {ENTITY, USER, CONTACT, ID_CARD_USER, TIMESTAMP_SERVER, ANON_ELECTOR, BANK,
        CURRENCY_SERVER, IDENTITY_SERVER, ANON_CURRENCY, BROWSER, MOBILE}

    @JsonIgnore
    private Long id;
    @JsonProperty("AppState")
    private State state;
    @JsonProperty("Type")
    private Type type;
    @JsonProperty("ConnectedDevices")
    private Set<DeviceDto> connectedDevices;
    @JsonProperty("Device")
    private DeviceDto device;
    @JsonProperty("Certificates")
    private Set<CertificateDto> certificates = new HashSet<>();
    @JsonProperty("GivenName")
    private String givenName;
    @JsonProperty("SurName")
    private String surName;
    @JsonProperty("Phone")
    private String phone;
    @JsonProperty("Email")
    private String email;
    @JsonProperty("Company")
    private String company;
    @JsonProperty("Country")
    private String country;
    @JsonProperty("Country")
    private String IBAN;
    @JsonProperty("NumId")
    private String numId;
    @JsonProperty("DocumentType")
    private IdDocument documentType;
    @JsonProperty("Details")
    private String details;
    @JsonProperty("UUID")
    private String UUID;

    private AddressDto address;
    @JsonIgnore
    private X509Certificate x509Certificate;
    @JsonIgnore
    private byte[] imageBytes;
    private transient Uri contactURI;

    @JsonIgnore
    private TimeStampToken timeStampToken;
    @JsonIgnore
    private SignerInformation signerInformation;

    public UserDto() { }

    public UserDto(String givenName, String surName, String phone, String email) {
        this.givenName = givenName;
        this.surName = surName;
        this.phone = phone;
        this.email = email;
    }

    public static UserDto getUser(X500Name subject) {
        UserDto result = new UserDto();
        for (RDN rdn : subject.getRDNs()) {
            AttributeTypeAndValue attributeTypeAndValue = rdn.getFirst();
            if (BCStyle.SERIALNUMBER.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setNumId(attributeTypeAndValue.getValue().toString());
            } else if (BCStyle.SURNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setSurName(attributeTypeAndValue.getValue().toString());
            } else if (BCStyle.GIVENNAME.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setGivenName(attributeTypeAndValue.getValue().toString());
            } else if (BCStyle.CN.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCountry(attributeTypeAndValue.getValue().toString());
            } else if (BCStyle.C.getId().equals(attributeTypeAndValue.getType().getId())) {
                result.setCountry(attributeTypeAndValue.getValue().toString());
            } else LOGD(TAG, "oid: " + attributeTypeAndValue.getType().getId() +
                    " - value: " + attributeTypeAndValue.getValue().toString());
        }
        return result;
    }

    public static UserDto getUser(X509Certificate x509Cert) throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(x509Cert).getSubject();
        UserDto user = getUser(x500name);
        user.setX509Certificate(x509Cert);
        user.setCertificates(new HashSet<>(Arrays.asList(new CertificateDto())));
        try {
            CertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(
                    CertExtensionDto.class, x509Cert, Constants.DEVICE_OID);
            if (certExtensionDto != null) {
                user.setEmail(certExtensionDto.getEmail());
                user.setPhone(certExtensionDto.getMobilePhone());
                user.setNumId(certExtensionDto.getNumId());
                user.setUUID(certExtensionDto.getUUID());
                user.setGivenName(certExtensionDto.getGivenname());
                user.setSurName(certExtensionDto.getSurname());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return user;
    }

    public static UserDto getUser(Principal principal) {
        return getUser(new X500Name(principal.getName()));
    }

    public static UserDto getUser(X509Principal principal) {
        UserDto user = new UserDto();
        user.setNumId((String) principal.getValues(new DERObjectIdentifier("2.5.4.5")).get(0));
        user.setSurName((String) principal.getValues(new DERObjectIdentifier("2.5.4.4")).get(0));
        user.setGivenName((String) principal.getValues(new DERObjectIdentifier("2.5.4.42")).get(0));
        user.setCountry((String) principal.getValues(new DERObjectIdentifier("2.5.4.6")).get(0));
        return user;
    }

    public Set<DeviceDto> getDevices() throws Exception {
        return connectedDevices;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }

    public void setTimeStampToken(TimeStampToken timeStampToken) {
        this.timeStampToken = timeStampToken;
    }

    public SignerInformation getSignerInformation() {
        return signerInformation;
    }

    public void setSignerInformation(SignerInformation signerInformation) {
        this.signerInformation = signerInformation;
    }

    public String getFullName() {
        return givenName + " " + surName;
    }

    @JsonIgnore
    public String getSignedContentDigestBase64() {
        if (signerInformation.getContentDigest() == null) return null;
        return Base64.encodeToString(signerInformation.getContentDigest(), Base64.NO_WRAP);
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @JsonIgnore
    public boolean checkUserFromCSR(X509Certificate x509CertificateToCheck)
            throws CertificateEncodingException {
        X500Name x500name = new JcaX509CertificateHolder(x509CertificateToCheck).getSubject();
        UserDto userToCheck = getUser(x500name);
        if (!numId.equals(userToCheck.getNumId())) return false;
        if (!givenName.equals(userToCheck.getGivenName())) return false;
        if (!surName.equals(userToCheck.getSurName())) return false;
        return true;
    }

    public Set<DeviceDto> getConnectedDevices() {
        return connectedDevices;
    }

    public void setConnectedDevices(Set<DeviceDto> connectedDevices) {
        this.connectedDevices = connectedDevices;
    }

    public Set<CertificateDto> getCertificates() {
        return certificates;
    }

    public DeviceDto getDevice() {
        return device;
    }

    public void setDevice(DeviceDto device) {
        this.device = device;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
        if (address != null) {
            try {
                address.setCountry(Country.valueOf(country));
            } catch (Exception ex) {
                LOGD(TAG, ex.getMessage());
            }
        }
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Uri getContactURI() {
        return contactURI;
    }

    public void setContactURI(Uri contactURI) {
        this.contactURI = contactURI;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (contactURI != null) s.writeObject(contactURI.toString());
        else s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            String contactURIStr = (String) s.readObject();
            if (contactURIStr != null) contactURI = Uri.parse(contactURIStr);
        } catch (Exception ex) {
            LOGD(TAG, "readObject EXCEPTION");
        }
    }

    public Type getType() {
        return type;
    }

    public UserDto setType(Type type) {
        this.type = type;
        return this;
    }

    public void setCertificates(Set<CertificateDto> certificates) {
        this.certificates = certificates;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getSurName() {
        return surName;
    }

    public void setSurName(String surName) {
        this.surName = surName;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getNumId() {
        return numId;
    }

    public void setNumId(String numId) {
        this.numId = numId;
    }

    public IdDocument getDocumentType() {
        return documentType;
    }

    public void setDocumentType(IdDocument documentType) {
        this.documentType = documentType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    @JsonIgnore
    public void setCertificates(Collection<CertificateDto> certificates) {
        if (certificates == null) this.certificates = null;
        else this.certificates = new HashSet<>(certificates);
    }

}
