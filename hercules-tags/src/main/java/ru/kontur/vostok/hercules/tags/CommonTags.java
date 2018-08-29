package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * CommonTags collection of common tags
 *
 * @author Kirill Sulim
 */
public final class CommonTags {

    /**
     * Environment tag
     */
    public static final TagDescription ENVIRONMENT_TAG = TagDescription.create("env", Type.STRING);

    /**
     * Project tag
     */
    public static final TagDescription PROJECT_TAG = TagDescription.create("proj", Type.STRING);

    private CommonTags() {
    }
}
