package ru.kontur.vostok.hercules.auth;

/**
 * @author Gregory Koshelev
 */
public class AuthManager {
    public AuthResult authStream(String apiKey, String stream, Action action) {
        return AuthResult.ok();//TODO: auth apiKey
    }

    public AuthResult authTimeline(String apiKey, String timeline, Action action) {
        return AuthResult.ok();//TODO: auth apiKey
    }
}
