package org.currency.cms;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.ASN1ObjectIdentifier;
import org.bouncycastle2.asn1.ASN1OctetString;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.CMSAttributes;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.currency.dto.ResponseDto;
import org.currency.http.ContentType;
import org.currency.http.HttpConn;
import org.currency.util.Constants;
import org.currency.util.HashUtils;
import org.currency.xades.DigestAlgorithm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import static org.currency.util.LogUtils.LOGD;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class CMSUtils {

    public static final String TAG = CMSUtils.class.getSimpleName();

    public static DERObject toDERObject(byte[] data) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        ASN1InputStream asnInputStream = new ASN1InputStream(inStream);
        return asnInputStream.readObject();
    }

    public static CMSSignedData addTimeStampToUnsignedAttributes(CMSSignedData cmsdata,
                                                                 TimeStampToken timeStampToken) throws Exception {
        DERObject derObject = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
        DERSet derset = new DERSet(derObject);
        Attribute timeStampAsAttribute = new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, derset);
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        byte[] timeStampTokenHash = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
        Iterator<SignerInformation> it = cmsdata.getSignerInfos().getSigners().iterator();
        List<SignerInformation> newSigners = new ArrayList<>();
        while (it.hasNext()) {
            SignerInformation signer = it.next();
            byte[] digestBytes = CMSUtils.getSignerDigest(signer);
            if(Arrays.equals(timeStampTokenHash, digestBytes)) {
                LOGD(TAG + ".onCreate", "setTimeStampToken - found signer");
                AttributeTable attributeTable = signer.getUnsignedAttributes();
                SignerInformation updatedSigner = null;
                if(attributeTable != null) {
                    LOGD(TAG + ".onCreate", "setTimeStampToken - signer with UnsignedAttributes");
                    hashTable = attributeTable.toHashtable();
                    if(!hashTable.contains(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken)) {
                        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
                    }
                    timeStampAsAttributeTable = new AttributeTable(hashTable);
                }
                updatedSigner = signer.replaceUnsignedAttributes(signer, timeStampAsAttributeTable);
                newSigners.add(updatedSigner);
            } else newSigners.add(signer);
        }
        SignerInformationStore newSignersStore = new SignerInformationStore(newSigners);
        return  CMSSignedData.replaceSigners(cmsdata, newSignersStore);
    }

    public static byte[] getSignerDigest(SignerInformation signer) throws CMSException {
        Attribute hash = signer.getSignedAttributes().get(CMSAttributes.messageDigest);
        return ((ASN1OctetString)hash.getAttrValues().getObjectAt(0)).getOctets();
    }

    public static DERObject getSingleValuedSignedAttribute(AttributeTable signedAttrTable,
                           DERObjectIdentifier attrOID, String printableName) throws CMSException {
        if (signedAttrTable == null) return null;
        ASN1EncodableVector vector = signedAttrTable.getAll(attrOID);
        switch (vector.size()) {
            case 0:
                return null;
            case 1:
                Attribute t = (Attribute)vector.get(0);
                ASN1Set attrValues = t.getAttrValues();
                if (attrValues.size() != 1) throw new CMSException("A " + printableName +
                        " attribute MUST have a single attribute value");
                return attrValues.getObjectAt(0).getDERObject();
            default: throw new CMSException(
                    "The SignedAttributes in a signerInfo MUST NOT include multiple instances of the "
                            + printableName + " attribute");
        }
    }

    //method with http connections, if invoked from main thread -> android.os.NetworkOnMainThreadException
    public static TimeStampToken getTimeStampToken(byte[] contentToSign, String timeStampServiceURL)
            throws Exception {
        byte[] digest = HashUtils.getHash(contentToSign, Constants.DATA_DIGEST_ALGORITHM);
        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        reqgen.setCertReq(true);
        TimeStampRequest timeStampRequest = reqgen.generate(
                new ASN1ObjectIdentifier(DigestAlgorithm.SHA256.getOid()).toString(), digest);
        ResponseDto response = HttpConn.getInstance().doPostRequest(
                timeStampRequest.getEncoded(), ContentType.TIMESTAMP_QUERY, timeStampServiceURL);
        if (ResponseDto.SC_OK == response.getStatusCode()) {
            byte[] bytesToken = response.getMessageBytes();
            TimeStampResponse timeStampResponse = new TimeStampResponse(bytesToken);
            TimeStampToken timeStampToken = timeStampResponse.getTimeStampToken();
            return timeStampToken;
        } else throw new Exception(response.getMessage());
    }

    public static CMSAttributeTableGenerator getSignedAttributeTableGenerator(
            TimeStampToken timeStampToken) throws IOException {
        DERObject derObject = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
        DERSet derset = new DERSet(derObject);
        Attribute timeStampAsAttribute = new Attribute(
                PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, derset);
        Hashtable hashTable = new Hashtable();
        hashTable.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, timeStampAsAttribute);
        AttributeTable timeStampAsAttributeTable = new AttributeTable(hashTable);
        DefaultSignedAttributeTableGenerator signedAttributeGenerator =
                new DefaultSignedAttributeTableGenerator(timeStampAsAttributeTable);
        return signedAttributeGenerator;
    }

}
