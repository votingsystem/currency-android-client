package org.currency.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class JSON {

    private static Logger log = Logger.getLogger(JSON.class.getSimpleName());

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    static {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final ObjectMapper mapper = getMapper();

    public static class HTMLCharacterEscapes extends CharacterEscapes {
        private final int[] asciiEscapes;

        public HTMLCharacterEscapes() {
            // start with set of characters known to require escaping (double-quote, backslash etc)
            int[] esc = CharacterEscapes.standardAsciiEscapesForJSON();
            // and force escaping of a few others:
            //esc['<'] = CharacterEscapes.ESCAPE_STANDARD;
            //esc['>'] = CharacterEscapes.ESCAPE_STANDARD;
            esc['&'] = CharacterEscapes.ESCAPE_STANDARD;
            esc['\''] = CharacterEscapes.ESCAPE_STANDARD;
            asciiEscapes = esc;
        }
        // this method gets called for character codes 0 - 127
        @Override public int[] getEscapeCodesForAscii() {
            return asciiEscapes;
        }
        // and this for others; we don't need anything special here
        @Override public SerializableString getEscapeSequence(int ch) {
            // no further escaping (beyond ASCII chars) needed:
            return null;
        }
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.setDateFormat(dateFormat);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
        mapper.getJsonFactory().setCharacterEscapes(new HTMLCharacterEscapes());
        //mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS,true);
        //mapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,true);
        return mapper;
    }

    public static <T> T readValue(String content, Class<T> valueType)
            throws IOException, JsonParseException, JsonMappingException {
        return mapper.readValue(content, valueType);
    }

    public static <T> T readValue(byte[] src, TypeReference valueTypeRef)
            throws IOException, JsonParseException, JsonMappingException {
        return mapper.readValue(src, valueTypeRef);
    }

    public static <T> T readValue(String src, TypeReference valueTypeRef)
            throws IOException, JsonParseException, JsonMappingException {
        return mapper.readValue(src, valueTypeRef);
    }

    public static <T> T readValue(byte[] src, Class<T> valueType) throws IOException {
        return mapper.readValue(src, valueType);
    }

    public static byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
        return mapper.writeValueAsBytes(value);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return mapper.writeValueAsString(value);
    }
}
