package ru.kontur.vostok.hercules.meta.auth.blacklist;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.curator.exception.CuratorException;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class BlacklistRepository {
    private final CuratorClient curatorClient;

    public BlacklistRepository(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public List<String> list() throws CuratorException {
        List<String> entries = curatorClient.children(zPrefix);
        return entries;
    }

    public void add(String key) throws CuratorException {
        curatorClient.createIfAbsent(zPrefix + "/" + key);
    }

    public void remove(String key) throws CuratorException {
        curatorClient.delete(zPrefix + "/" + key);
    }

    private static String zPrefix = "/hercules/auth/blacklist";
}
