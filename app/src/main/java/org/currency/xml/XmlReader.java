package org.currency.xml;

import org.currency.dto.AddressDto;
import org.currency.dto.QRResponseDto;
import org.currency.dto.ResponseDto;
import org.currency.dto.identity.IdentityRequestDto;
import org.currency.dto.metadata.ContactPersonDto;
import org.currency.dto.metadata.CountryDto;
import org.currency.dto.metadata.KeyDto;
import org.currency.dto.metadata.LocationDto;
import org.currency.dto.metadata.MetadataDto;
import org.currency.dto.metadata.OrganizationDto;
import org.currency.dto.metadata.SystemEntityDto;
import org.currency.dto.metadata.TrustedEntitiesDto;
import org.currency.http.SystemEntityType;
import org.currency.util.DateUtils;
import org.currency.util.OperationType;
import org.kxml2.kdom.Element;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlReader {

    public static final String TAG = XmlReader.class.getSimpleName();


    public static QRResponseDto readQRResponse(byte[] xmlBytes) throws IOException, XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        QRResponseDto qrResponse = new QRResponseDto();
        mainElement.getAttributeValue(null, "QRResponse");
        qrResponse.setOperationType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        qrResponse.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return qrResponse;
    }

    public static ResponseDto readResponse(byte[] xmlBytes) throws IOException, XmlPullParserException {
        ResponseDto responseDto = new ResponseDto();
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        if(mainElement.getAttributeValue(null, "StatusCode") != null) {
            responseDto.setStatusCode(Integer.valueOf(mainElement.getAttributeValue(null, "StatusCode")));
        }
        responseDto.setMessage(XMLUtils.getTextChild(mainElement, "Message"));
        responseDto.setBase64Data(XMLUtils.getTextChild(mainElement, "Base64Data"));
        return responseDto;
    }

    public static MetadataDto readMetadata(byte[] xmlBytes) throws IOException, XmlPullParserException {
        MetadataDto metadata = new MetadataDto();
        SystemEntityDto systemEntityDto = new SystemEntityDto();
        metadata.setEntity(systemEntityDto);
        Element metadataElement = XMLUtils.parse(xmlBytes).getRootElement();
        metadata.setLanguage(metadataElement.getAttributeValue(null, "Language"));
        metadata.setTimeZone(metadataElement.getAttributeValue(null, "TimeZone"));
        metadata.setValidUntil(metadataElement.getAttributeValue(null, "ValidUntil"));

        Element entityElement = metadataElement.getElement(null, "Entity");
        systemEntityDto.setId(entityElement.getAttributeValue(null, "Id"));
        if (entityElement.getAttributeValue(null, "Type") != null)
            systemEntityDto.setEntityType(SystemEntityType.getByName(entityElement.getAttributeValue(null, "Type")));

        Element organizationElement = entityElement.getElement(null, "Organization");
        if (organizationElement != null) {
            OrganizationDto organization = new OrganizationDto();
            organization.setOrganizationName(XMLUtils.getTextChild(organizationElement, "OrganizationName"));
            organization.setOrganizationUnit(XMLUtils.getTextChild(organizationElement, "OrganizationUnit"));
            organization.setOrganizationURL(XMLUtils.getTextChild(organizationElement, "OrganizationURL"));
            systemEntityDto.setOrganization(organization);
        }


        Element locationElement = entityElement.getElement(null, "Location");
        if (locationElement != null) {
            LocationDto location = new LocationDto();
            location.setCity(XMLUtils.getTextChild(locationElement, "City"));

            Element countryElement = locationElement.getElement(null, "Country");
            if (countryElement != null) {
                CountryDto country = new CountryDto();
                country.setCode(countryElement.getAttributeValue(null, "Code"));
                country.setDisplayName(countryElement.getAttributeValue(null, "DisplayName"));
                country.setLanguage(countryElement.getAttributeValue(null, "Language"));
                location.setCountry(country);
            }

            Element addressElement = locationElement.getElement(null, "Address");
            if (addressElement != null) {
                AddressDto address = new AddressDto();
                address.setPostalCode(addressElement.getAttributeValue(null, "PostalCode"));
                if (addressElement.getChild(0) != null) {
                    String addressStr = new String(org.bouncycastle2.util.encoders.Base64.decode(
                            ((String) addressElement.getChild(0)).getBytes()));
                    address.setAddress(addressStr);
                }
                location.setAddress(address);
            }
            systemEntityDto.setLocation(location);
        }

        Element contactPersonElement = entityElement.getElement(null, "ContactPerson");
        if (contactPersonElement != null) {
            ContactPersonDto contactPerson = new ContactPersonDto();
            if (contactPersonElement.getAttributeValue(null, "ContactType") != null)
                contactPerson.setContactType(ContactPersonDto.Type.valueOf(
                        contactPersonElement.getAttributeValue(null, "ContactType").toUpperCase()));
            contactPerson.setCompany(XMLUtils.getTextChild(contactPersonElement, "Company"));
            contactPerson.setGivenName(XMLUtils.getTextChild(contactPersonElement, "GivenName"));
            contactPerson.setSurName(XMLUtils.getTextChild(contactPersonElement, "SurName"));
            contactPerson.setEmailAddress(XMLUtils.getTextChild(contactPersonElement, "Phone"));
            contactPerson.setTelephoneNumber(XMLUtils.getTextChild(contactPersonElement, "Email"));
            systemEntityDto.setContactPerson(contactPerson);
        }

        Element keysElement = metadataElement.getElement(null, "Keys");
        if (keysElement != null) {
            Set<KeyDto> keyDescriptorSet = new HashSet<>();
            for (int i = 0; i < keysElement.getChildCount(); i++) {
                Element keyElement = keysElement.getElement(i);
                KeyDto keyDto = new KeyDto();
                if (keyElement.getAttributeValue(null, "Type") != null)
                    keyDto.setType(KeyDto.Type.valueOf(keyElement.getAttributeValue(null, "Type").toUpperCase()));
                if (keyElement.getAttributeValue(null, "Use") != null)
                    keyDto.setUse(KeyDto.Use.valueOf(keyElement.getAttributeValue(null, "Use").toUpperCase()));
                keyDto.setX509CertificateBase64((String) keyElement.getChild(0));
                keyDescriptorSet.add(keyDto);
            }
            metadata.setKeyDescriptorSet(keyDescriptorSet);
        }
        Element trustedEntitiesElement = metadataElement.getElement(null, "TrustedEntities");
        if (trustedEntitiesElement != null) {
            TrustedEntitiesDto trustedEntities = new TrustedEntitiesDto();
            Set<TrustedEntitiesDto.EntityDto> entities = new HashSet<>();
            trustedEntities.setEntities(entities);
            for (int i = 0; i < trustedEntitiesElement.getChildCount(); i++) {
                Element trustedEntityElement = trustedEntitiesElement.getElement(i);
                TrustedEntitiesDto.EntityDto entity = new TrustedEntitiesDto.EntityDto();
                entity.setCountryCode(trustedEntityElement.getAttributeValue(null, "CountryCode"));
                entity.setId(trustedEntityElement.getAttributeValue(null, "Id"));
                if (trustedEntityElement.getAttributeValue(null, "Type") != null)
                    entity.setType(SystemEntityType.getByName(trustedEntityElement.getAttributeValue(null, "Type")));
                entities.add(entity);
            }
            metadata.setTrustedEntities(trustedEntities);
        }
        return metadata;
    }

    public static IdentityRequestDto readIdentityRequest(byte[] xmlBytes) throws IOException,
            XmlPullParserException {
        Element mainElement = XMLUtils.parse(xmlBytes).getRootElement();
        IdentityRequestDto request = new IdentityRequestDto();
        if(mainElement.getAttributeValue(null, "Date") != null) {
            request.setDate(DateUtils.getXmlDate(mainElement.getAttributeValue(null, "Date")));
        }
        request.setType(OperationType.valueOf(mainElement.getAttributeValue(null, "Type")));
        Element identityServiceElement = mainElement.getElement(null, "IndentityServiceEntity");
        if(identityServiceElement != null) {
            SystemEntityDto identityService = new SystemEntityDto(identityServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(identityServiceElement.getAttributeValue(null, "Type")));
            request.setIndentityServiceEntity(identityService.getId());
        }
        Element callbackServiceElement = mainElement.getElement(null, "CallbackServiceEntity");
        if(callbackServiceElement != null) {
            SystemEntityDto callbackService = new SystemEntityDto(callbackServiceElement.getAttributeValue(null, "Id"),
                    SystemEntityType.getByName(callbackServiceElement.getAttributeValue(null, "Type")));
            request.setCallbackServiceEntityId(callbackService.getId());
        }
        request.setRevocationHashBase64(XMLUtils.getTextChild(mainElement, "RevocationHashBase64"));
        request.setUUID(XMLUtils.getTextChild(mainElement, "UUID"));
        return request;
    }

}
