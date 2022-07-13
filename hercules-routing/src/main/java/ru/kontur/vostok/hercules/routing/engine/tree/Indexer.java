package ru.kontur.vostok.hercules.routing.engine.tree;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.routing.Destination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Decision tree indexer.
 * <p>
 * Creates decision tree index using given configuration and rebuild index at any configuration change event.
 *
 * @author Aleksandr Yuferov
 */
class Indexer {
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
        List<HPath> allowedTags = config.allowedTags();
        var rootBuilder = new TreeBuilder(defaultDestination);
        var conditions = new TinyString[allowedTags.size()];
        for (DecisionTreeEngineRoute<?> route : routes) {
            Map<String, String> routeConditions = route.conditions();
            for (int tagIndex = 0; tagIndex < conditions.length; tagIndex++) {
                String tagKey = allowedTags.get(tagIndex).getPath();
                String tagConditionValue = routeConditions.get(tagKey);
                conditions[tagIndex] = tagConditionValue != null
                        ? TinyString.of(tagConditionValue)
                        : null;
            }
            rootBuilder.addRoute(conditions, route.destination());
        }
        return new Index(allowedTags, rootBuilder.build());
    }

    private int findRuleById(UUID id) {
        return IntStream.range(0, routes.size())
                .filter(index -> id.equals(routes.get(index).id()))
                .findFirst()
                .orElse(-1);
    }

    private static class TreeBuilder {
        private final NodeBuilder root;

        TreeBuilder(Destination<?> defaultDestination) {
            root = new NodeBuilder(0, null);
            root.setDestination(defaultDestination);
        }

        /**
         * Add route into the tree.
         * <p>
         * Finds node responsible for the condition in the tree and sets the destination of this node as given
         * in argument.
         *
         * @param conditions  Condition of the route.
         * @param destination Destination of the route.
         */
        void addRoute(TinyString[] conditions, Destination<?> destination) {
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

        private NodeBuilder findResponsibleNode(TinyString[] conditions) {
            NodeBuilder current = root;
            int end = findLastNonNullElement(conditions);
            for (int i = 0; i <= end; i++) {
                TinyString condition = conditions[i];
                current = current.get(condition);
            }
            return current;
        }

        private static <T> int findLastNonNullElement(T[] array) {
            for (int index = array.length - 1; index >= 0; index--) {
                if (array[index] != null) {
                    return index;
                }
            }
            return -1;
        }

        private static class NodeBuilder {
            private final int index;
            private final NodeBuilder parent;
            private final Map<TinyString, NodeBuilder> children = new HashMap<>();
            private Destination<?> destination;

            NodeBuilder(int index, NodeBuilder parent) {
                this.index = index;
                this.parent = parent;
            }

            /**
             * Get child node-builder by condition value.
             *
             * @param condition Condition value.
             * @return Child node-builder.
             */
            NodeBuilder get(TinyString condition) {
                return children.computeIfAbsent(condition, (key) -> new NodeBuilder(index + 1, this));
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
             * Get destination for current node.
             * <p>
             * If destination is not set for current node then should find it in one of the parents nodes.
             *
             * @return Destination for current node.
             */
            Destination<?> destination() {
                NodeBuilder node = this;
                while (node != null && node.destination == null) {
                    node = node.parent;
                }
                return node != null ? node.destination : null;
            }

            /**
             * Create node.
             * <p>
             * If there is no children in this builder-node then creates a leaf node with the result of
             * {@link #destination()} method.
             * If there are some children in this builder-node then creates a decision node in witch as else-node
             * creates a leaf with the result of {@link #destination()} method.
             *
             * @return Created node.
             */
            Node build() {
                if (!children.isEmpty()) {
                    Map<TinyString, Node> constructedChildren = children.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
                    return new Node.DecisionNode(index, constructedChildren, destination());
                }
                return new Node.LeafNode(destination());
            }
        }
    }
}
