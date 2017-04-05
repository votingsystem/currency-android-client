package org.currency.http;


public class MediaType {

    public static final String CURRENCY = "application/currency;";
    public static final String PKCS7_SIGNED = "application/pkcs7-signature";
    public static final String PKCS7_ENCRYPTED = "application/pkcs7-encrypted";
    public static final String PKCS7_SIGNED_ENCRYPTED = "application/pkcs7-signature;application/pkcs7-encrypted";
    public static final String ENCRYPTED = "application/pkcs7-encrypted";
    public static final String JSON_ENCRYPTED = "application/json;application/pkcs7-encrypted";
    public static final String JSON = "application/json";
    public static final String XML = "application/xml";
    public static final String VOTE = "application/vote;application/pkcs7-signature;";
    public static final String PEM = "application/pem-file";
    public static final String ZIP = "application/zip";
    public static final String MESSAGEVS = "application/pkcs7-messagevs";
    public static final String BACKUP = "application/backup";
    public static final String MULTIPART_ENCRYPTED = "multipart/encrypted";

}
