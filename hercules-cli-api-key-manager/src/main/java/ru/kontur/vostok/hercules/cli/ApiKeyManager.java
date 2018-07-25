package ru.kontur.vostok.hercules.cli;

import ru.kontur.vostok.hercules.meta.curator.CuratorClient;

/**
 * ApiKeyManager
 */
public class ApiKeyManager {

    private static final String BLACKLIST_PATH = "/hercules/auth/blacklist";
    private static final String KEYS_PATH = "";

    private final CuratorClient curatorClient;

    public ApiKeyManager(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public void createKey(String key) throws Exception {
        curatorClient.create(BLACKLIST_PATH + "/" + key, null);
    }

    public void blockKey(String key) {

    }

    public void unblockKey(String key) {

    }

    public void removeKey(String key) {

    }

}
