package ru.kontur.vostok.hercules.management.api;

import ru.kontur.vostok.hercules.auth.AuthResult;

import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class AdminManager {
    private final Set<String> adminKeys;

    public AdminManager(Set<String> adminKeys) {
        this.adminKeys = adminKeys;
    }

    public AuthResult auth(String apiKey) {
        return adminKeys.contains(apiKey) ? AuthResult.ok() : AuthResult.denied();
    }
}
