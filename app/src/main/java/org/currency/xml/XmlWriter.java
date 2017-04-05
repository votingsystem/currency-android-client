package org.currency.xml;

import org.currency.dto.identity.IdentityRequestDto;
import org.currency.http.SystemEntityType;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XmlWriter {

    public static byte[] write(IdentityRequestDto identityRequest) throws Exception {
        Document doc = new Document();
        Element identityRequestElement = doc.createElement("", "IdentityRequest");
        identityRequestElement.setAttribute(null, "Type", identityRequest.getType().name());

        if(identityRequest.getIndentityServiceEntity() != null) {
            Element indentityServiceElement = doc.createElement("", "IndentityServiceEntity");
            indentityServiceElement.setAttribute(null, "Id", identityRequest.getIndentityServiceEntity());
            indentityServiceElement.setAttribute(null, "Type", SystemEntityType.ID_PROVIDER.getName());
            indentityServiceElement.addChild(Node.TEXT, "");
            identityRequestElement.addChild(Node.ELEMENT, indentityServiceElement);
        }

        if(identityRequest.getCallbackServiceEntityId() != null) {
            Element indentityServiceElement = doc.createElement("", "CallbackServiceEntity");
            indentityServiceElement.setAttribute(null, "Id", identityRequest.getCallbackServiceEntityId());
            indentityServiceElement.setAttribute(null, "Type", SystemEntityType.VOTING_SERVICE_PROVIDER.getName());
            indentityServiceElement.addChild(Node.TEXT, "");
            identityRequestElement.addChild(Node.ELEMENT, indentityServiceElement);
        }

        Element revocationHashElement = doc.createElement("", "RevocationHashBase64");
        revocationHashElement.addChild(Node.TEXT, identityRequest.getRevocationHashBase64());
        identityRequestElement.addChild(Node.ELEMENT, revocationHashElement);

        Element uuidElement = doc.createElement("", "UUID");
        uuidElement.addChild(Node.TEXT, identityRequest.getUUID());
        identityRequestElement.addChild(Node.ELEMENT, uuidElement);

        doc.addChild(Node.ELEMENT, identityRequestElement);
        return XMLUtils.serialize(doc);
    }

}