package ru.kontur.vostok.hercules.routing.engine.tree;

import ru.kontur.vostok.hercules.protocol.hpath.HPath;

import java.util.List;

/**
 * Decision tree index.
 *
 * @author Aleksandr Yuferov
 */
class Index {
    private final List<HPath> allowedTags;
    private final Node root;

    Index(List<HPath> allowedTags, Node root) {
        this.allowedTags = allowedTags;
        this.root = root;
    }

    /**
     * Allowed tags from engine configuration.
     *
     * @return Allowed tags paths.
     * @see DecisionTreeEngineConfig#allowedTags()
     */
    List<HPath> allowedTags() {
        return allowedTags;
    }

    /**
     * Root of decision tree.
     *
     * @return Root of decision tree.
     */
    Node root() {
        return root;
    }
}
