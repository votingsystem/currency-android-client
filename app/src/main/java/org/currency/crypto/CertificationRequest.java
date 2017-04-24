package org.currency.crypto;

import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.DERBoolean;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.DERUTF8String;
import org.bouncycastle2.asn1.pkcs.Attribute;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.currency.App;
import org.currency.cms.CMSGenerator;
import org.currency.cms.CMSSignedMessage;
import org.currency.cms.CMSUtils;
import org.currency.dto.CertExtensionDto;
import org.currency.dto.DeviceDto;
import org.currency.dto.currency.CurrencyCertExtensionDto;
import org.currency.http.MediaType;
import org.currency.util.Constants;
import org.currency.util.JSON;
import org.currency.xades.SignatureAlgorithm;
import org.currency.xades.SignatureBuilder;
import org.currency.xades.XAdESUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import static org.currency.util.Constants.ANDROID_PROVIDER;
import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificationRequest implements java.io.Serializable {

    public static final String TAG = CertificationRequest.class.getSimpleName();

    private static final long serialVersionUID = 1L;

    private transient PKCS10CertificationRequest csr;
    private transient CMSGenerator signedMailGenerator;
    private transient KeyPair keyPair;
    private String signatureMechanism;
    private X509Certificate certificate;
    private byte[] signedCsr;

    private CertificationRequest(KeyPair keyPair, PKCS10CertificationRequest csr, String signatureMechanism) {
        this.keyPair = keyPair;
        this.csr = csr;
        this.signatureMechanism = signatureMechanism;
    }

    public static CertificationRequest getCurrencyRequest(
            String signatureMechanism, String provider, String currencyServerURL, String revocationHash,
            BigDecimal amount, String currencyCode)
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException,
            SignatureException, IOException {
        KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
        X500Principal subject = new X500Principal("CN=currencyEntity:" + currencyServerURL +
                ", OU=CURRENCY_VALUE:" + amount + ", OU=CURRENCY_CODE:" + currencyCode +
                ", OU=DigitalCurrency");
        CurrencyCertExtensionDto dto = new CurrencyCertExtensionDto(amount, currencyCode, revocationHash,
                currencyServerURL);
        ASN1EncodableVector asn1EncodableVector = new ASN1EncodableVector();
        asn1EncodableVector.add(new Attribute(new ASN1ObjectIdentifier(Constants.CURRENCY_OID),
                new DERSet(new DERUTF8String(JSON.writeValueAsString(dto)))));
        asn1EncodableVector.add(new Attribute(new ASN1ObjectIdentifier(Constants.ANON_CERT_OID),
                new DERSet(new DERBoolean(true))));
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(signatureMechanism, subject,
                keyPair.getPublic(), new DERSet(asn1EncodableVector), keyPair.getPrivate(), provider);
        return new CertificationRequest(keyPair, csr, signatureMechanism);
    }

    public CMSSignedMessage signData(byte[] contentToSign, String timeStampServiceURL) throws Exception {
        TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(contentToSign, timeStampServiceURL);
        return getSignedMailGenerator().signData(contentToSign, timeStampToken);
    }

    private CMSGenerator getSignedMailGenerator() throws Exception {
        if (signedMailGenerator == null) {
            Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(signedCsr);
            LOGD(TAG + "getSignedMailGenerator()", "Num certs: " + certificates.size());
            if(certificates.isEmpty()) throw new Exception (" --- missing certs --- ");
            certificate = certificates.iterator().next();
            X509Certificate[] arrayCerts = new X509Certificate[certificates.size()];
            certificates.toArray(arrayCerts);
            signedMailGenerator = new CMSGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
            signedMailGenerator = new CMSGenerator(keyPair.getPrivate(), arrayCerts,
                    signatureMechanism, ANDROID_PROVIDER);
        }
        return signedMailGenerator;
    }

    public static CertificationRequest getUserRequest(String numId, String email, String phone,
            String deviceName, String deviceUUID, String givenName, String surName,
            DeviceDto.Type deviceType) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, SignatureException, IOException {
        KeyPair keyPair = KeyGenerator.INSTANCE.genKeyPair();
        String principal = "SERIALNUMBER=" + numId + ", GIVENNAME=" + givenName + ", SURNAME=" + surName;
        CertExtensionDto dto = new CertExtensionDto(deviceName, deviceUUID,
                email, phone, deviceType).setNumId(numId).setGivenname(givenName).setSurname(surName);
        Attribute attribute = new Attribute(new ASN1ObjectIdentifier(Constants.DEVICE_OID),
                new DERSet(new DERUTF8String(JSON.writeValueAsString(dto))));
        X500Principal subject = new X500Principal(principal);
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Constants.SIGNATURE_ALGORITHM,
                subject, keyPair.getPublic(), new DERSet(attribute), keyPair.getPrivate(),
                Constants.PROVIDER);
        return new CertificationRequest(keyPair, csr, Constants.SIGNATURE_ALGORITHM);
    }

    public byte[] signDataWithTimeStamp(byte[] cotentToSign, String contentType) throws Exception {
        LOGD(TAG + ".signCMSDataWithTimeStamp", "contentType: " + contentType);
        switch (contentType) {
            case MediaType.JSON:
                Collection<X509Certificate> certCollection = PEMUtils.fromPEMToX509CertCollection(signedCsr);
                List<Certificate> certList = new ArrayList(certCollection);
                CMSGenerator cmsGenerator = new CMSGenerator(keyPair.getPrivate(), certList,
                        Constants.SIGNATURE_ALGORITHM, Constants.ANDROID_PROVIDER);
                TimeStampToken timeStampToken = CMSUtils.getTimeStampToken(cotentToSign,
                        App.getInstance().getTimeStampServiceURL());
                return cmsGenerator.signData(cotentToSign, timeStampToken).toPEM();
            default:
                List<X509Certificate> certificates = new ArrayList<>(
                        PEMUtils.fromPEMToX509CertCollection(signedCsr));
                X509Certificate issuedCert = PEMUtils.fromPEMToX509Cert(signedCsr);
                return new SignatureBuilder(cotentToSign, XAdESUtils.XML_MIME_TYPE,
                        SignatureAlgorithm.RSA_SHA_256.getName(), keyPair.getPrivate(), issuedCert,
                        certificates, App.getInstance().getTimeStampServiceURL()).build();
        }
    }

    public X509Certificate getCertificate() throws Exception {
        if(certificate == null)
            return PEMUtils.fromPEMToX509Cert(signedCsr);
        return certificate;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() throws Exception {
        return keyPair.getPrivate();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public byte[] getCsrPEM() throws Exception {
        return PEMUtils.getPEMEncoded(csr);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(certificate != null) s.writeObject(certificate.getEncoded());
            else s.writeObject(null);
            if(keyPair != null) {//this is to deserialize private keys outside android environments
                s.writeObject(keyPair.getPublic().getEncoded());
                s.writeObject(keyPair.getPrivate().getEncoded());
            } else {
                s.writeObject(null);
                s.writeObject(null);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        s.defaultReadObject();
        byte[] certificateBytes = (byte[]) s.readObject();
        if(certificateBytes != null) {
            try {
                certificate = CertUtils.loadCertificate(certificateBytes);
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            byte[] publicKeyBytes = (byte[]) s.readObject();
            PublicKey publicKey =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            byte[] privateKeyBytes = (byte[]) s.readObject();
            PrivateKey privateKey =  KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            if(privateKey != null && publicKey != null) keyPair = new KeyPair(publicKey, privateKey);
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public byte[] getSignedCsr() {
        return signedCsr;
    }

    public CertificationRequest setSignedCsr(byte[] signedCsr) {
        this.signedCsr = signedCsr;
        return this;
    }

}