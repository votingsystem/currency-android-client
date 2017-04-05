package org.currency.http;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RecordingCookieJar implements CookieJar {

    private final Deque<List<Cookie>> requestCookies = new ArrayDeque<>();
    private final Deque<List<Cookie>> responseCookies = new ArrayDeque<>();

    public void enqueueRequestCookies(Cookie... cookies) {
        requestCookies.add(Arrays.asList(cookies));
    }

    public List<Cookie> takeResponseCookies() {
        return responseCookies.removeFirst();
    }

    @Override public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        responseCookies.add(cookies);
    }

    @Override public List<Cookie> loadForRequest(HttpUrl url) {
        if (requestCookies.isEmpty()) return Collections.emptyList();
        return requestCookies.removeFirst();
    }
}
