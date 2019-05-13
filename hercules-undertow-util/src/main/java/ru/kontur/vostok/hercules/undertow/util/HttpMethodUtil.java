package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.util.HttpString;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;
import ru.kontur.vostok.hercules.util.Maps;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Gregory Koshelev
 */
public class HttpMethodUtil {
    private static final Map<HttpString, HttpMethod> methods;

    static {
        Map<HttpString, HttpMethod> map = new HashMap<>(Maps.effectiveHashMapCapacity(HttpMethod.values().length));

        for (HttpMethod method : HttpMethod.values()) {
            map.put(HttpString.tryFromString(method.toString()), method);
        }

        methods = map;
    }

    public static HttpMethod of(HttpString value) throws NotSupportedHttpMethodException {
        HttpMethod method = methods.get(value);
        if (method == null) {
            throw new NotSupportedHttpMethodException("Unsupported method " + value);
        }
        return method;
    }
}
