package ru.kontur.vostok.hercules.routing.engine.tree;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.routing.Destination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ru.kontur.vostok.hercules.util.text.IgnoreCaseWrapper;

/**
 * Decision tree indexer.
 * <p>
 * Creates decision tree index using given configuration and rebuild index at any configuration change event.
 *
 * @author Aleksandr Yuferov
 */
class Indexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Indexer.class);

    private static final IgnoreCaseWrapper<TinyString> ANY_MASK = new IgnoreCaseWrapper<>(TinyString.of("*"));

    private final DecisionTreeEngineConfig defaultConfig;
    private final Destination<?> defaultDestination;
    private List<DecisionTreeEngineRoute<?>> routes;
    private DecisionTreeEngineConfig config;

    Indexer(DecisionTreeEngineConfig defaultConfig, Destination<?> defaultDestination) {
        this.defaultConfig = defaultConfig;
        this.defaultDestination = defaultDestination;
    }

    synchronized Index init(DecisionTreeEngineConfig config, List<DecisionTreeEngineRoute<?>> rules) {
        this.config = config != null ? config : defaultConfig;
        this.routes = new ArrayList<>(rules);
        return reindex();
    }

    synchronized Index onRuleCreated(DecisionTreeEngineRoute<?> rule) {
        this.routes.add(rule);
        return reindex();
    }

    synchronized Index onRuleChanged(DecisionTreeEngineRoute<?> rule) {
        int foundIndex = findRuleById(rule.id());
        if (foundIndex != -1) {
            routes.set(foundIndex, rule);
        } else {
            routes.add(rule);
        }
        return reindex();
    }

    synchronized Index onRuleRemoved(UUID ruleId) {
        int foundIndex = findRuleById(ruleId);
        if (foundIndex != -1) {
            routes.remove(foundIndex);
        }
        return reindex();
    }

    synchronized Index onEngineConfigChanged(DecisionTreeEngineConfig config) {
        this.config = config != null ? config : defaultConfig;
        return reindex();
    }

    private Index reindex() {
        LOGGER.debug("Reindexing started ...");
        List<IgnoreCaseWrapper<TinyString>> conditions = new ArrayList<>(treeHeight());
        var rootBuilder = new TreeBuilder(defaultDestination);
        for (DecisionTreeEngineRoute<?> route : routes) {
            Map<String, String> routeConditions = route.conditions();
            for (HPath tag : config.allowedTags()) {
                String tagKey = tag.getPath();
                String tagConditionValue = routeConditions.get(tagKey);
                conditions.add(tagConditionValue != null
                        ? new IgnoreCaseWrapper<>(TinyString.of(tagConditionValue))
                        : null
                );
            }
            rootBuilder.addRoute(conditions, route.destination());
            conditions.clear();
        }
        LOGGER.debug("Reindexing finished");
        return new Index(config.allowedTags(), rootBuilder.build());
    }

    private int treeHeight() {
        return config.allowedTags().size();
    }

    private int findRuleById(UUID id) {
        return IntStream.range(0, routes.size())
                .filter(index -> id.equals(routes.get(index).id()))
                .findFirst()
                .orElse(-1);
    }

    private class TreeBuilder {

        private final NodeBuilder root;

        TreeBuilder(Destination<?> defaultDestination) {
            root = new NodeBuilder(0);
            root.setDestination(defaultDestination);
        }

        /**
         * Add route into the tree.
         * <p>
         * Finds node responsible for the condition in the tree and sets the destination of this node as given in argument.
         *
         * @param conditions  Condition of the route.
         * @param destination Destination of the route.
         */
        void addRoute(List<IgnoreCaseWrapper<TinyString>> conditions, Destination<?> destination) {
            findResponsibleNode(conditions)
                    .setDestination(destination);
        }

        /**
         * Build the tree.
         *
         * @return Result tree.
         */
        Node build() {
            return root.build();
        }

        private NodeBuilder findResponsibleNode(List<IgnoreCaseWrapper<TinyString>> conditions) {
            NodeBuilder current = root;
            int treeHeight = treeHeight();
            for (int i = 0; i < treeHeight; i++) {
                IgnoreCaseWrapper<TinyString> condition = conditions.get(i);
                current = current.get(condition);
            }
            return current;
        }

        private class NodeBuilder {

            private final int index;
            private final Map<IgnoreCaseWrapper<TinyString>, NodeBuilder> children = new HashMap<>();
            private final NodeBuilder any;
            private Destination<?> destination;

            NodeBuilder(int index) {
                this.index = index;
                if (index <= treeHeight()) {
                    this.any = new NodeBuilder(index + 1);
                } else {
                    this.any = null;
                }
            }

            /**
             * Get child node-builder by condition value.
             *
             * @param condition Condition value.
             * @return Child node-builder.
             */
            NodeBuilder get(IgnoreCaseWrapper<TinyString> condition) {
                return ANY_MASK.equals(condition)
                        ? any
                        : children.computeIfAbsent(condition, (key) -> new NodeBuilder(index + 1));
            }

            /**
             * Destination.
             *
             * @param destination Destination of the node.
             */
            void setDestination(Destination<?> destination) {
                this.destination = destination;
            }

            /**
             * Create node.
             *
             * @return Created node.
             */
            Node build() {
                if (index < treeHeight()) {
                    Map<IgnoreCaseWrapper<TinyString>, Node> constructedChildren = children.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
                    return new Node.DecisionNode(index, Collections.unmodifiableMap(constructedChildren), any.build());
                }
                return new Node.LeafNode(destination == null ? defaultDestination : destination);
            }
        }
    }
}
