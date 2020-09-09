package ru.kontur.vostok.hercules.elastic.adapter.format;

import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.protocol.hpath.HTree;

/**
 * Mutable tree-like structure to build payload of the {@link ru.kontur.vostok.hercules.protocol.Event}.
 *
 * @author Gregory Koshelev
 */
public class ProtoContainer {
    private final HTree<Variant> content = new HTree<>();

    public void put(HPath path, Variant variant) {
        content.put(path, variant);
    }

    public ProtoContainer() {
    }

    /**
     * Fill event's payload.
     *
     * @param eventBuilder the event builder
     */
    public void fill(EventBuilder eventBuilder) {
        HTree<Variant>.Navigator navigator = content.navigator();

        for (TinyString tag : navigator.children()) {
            navigator.navigateToChild(tag);
            eventBuilder.tag(tag, nodeToVariant(navigator));
            navigator.navigateToParent();
        }
    }

    private Variant nodeToVariant(HTree<Variant>.Navigator navigator) {
        if (!navigator.hasChildren()) {
            return navigator.getValue();
        }
        return Variant.ofContainer(subtreeToContainer(navigator));
    }

    private Container subtreeToContainer(HTree<Variant>.Navigator navigator) {
        Container.ContainerBuilder builder = Container.builder();
        for (TinyString tag : navigator.children()) {
            navigator.navigateToChild(tag);
            builder.tag(tag, nodeToVariant(navigator));
            navigator.navigateToParent();
        }
        return builder.build();
    }
}
