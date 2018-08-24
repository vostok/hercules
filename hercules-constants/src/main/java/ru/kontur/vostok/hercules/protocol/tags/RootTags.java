package ru.kontur.vostok.hercules.protocol.tags;

import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.Type;

/**
 * RootTags
 *
 * @author Kirill Sulim
 */
public final class RootTags {

    public static final FieldDescription ENVIRONMENT_TAG = FieldDescription.create("env", Type.STRING);

    private RootTags() {
    }
}
