package ru.kontur.vostok.hercules.meta.auth.validation;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.ZnodeValidator;

import java.util.List;

/**
 * @author Gregory Koshelev
 */
public class ValidationRepository {
    private final ValidationSerializer serializer;
    private final CuratorClient curatorClient;

    public ValidationRepository(ValidationSerializer serializer, CuratorClient curatorClient) {
        this.serializer = serializer;
        this.curatorClient = curatorClient;
    }

    public List<String> list() throws Exception {
        List<String> validations = curatorClient.children(zPrefix);
        return validations;
    }

    public void create(Validation validation) throws Exception {
        String znode = serializer.serialize(validation);
        if (!ZnodeValidator.validate(znode)) {
            throw new IllegalArgumentException("Invalid znode name");
        }
        curatorClient.createIfAbsent(zPrefix + "/" + znode);
    }

    public void delete(Validation validation) throws Exception {
        String znode = serializer.serialize(validation);
        curatorClient.delete(zPrefix + "/" + znode);
    }

    private static String zPrefix = "/hercules/auth/validations";
}
