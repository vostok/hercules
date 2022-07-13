package ru.kontur.vostok.hercules.routing.engine.tree;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.routing.Destination;

import java.util.Map;

/**
 * Decision tree node interface.
 *
 * @author Aleksandr Yuferov
 */
interface Node {
    /**
     * Visit node.
     *
     * @param input Input array.
     * @return Result destination.
     */
    Destination<?> visit(TinyString[] input);

    /**
     * Node that makes decision relying on input array.
     */
    class DecisionNode implements Node {
        private final int index;
        private final Map<TinyString, Node> nodes;
        private final Destination<?> destination;

        /**
         * Constructor.
         *
         * @param index           The index of the value to check in the input array.
         * @param nodes           Map holds nodes by the value that should be visited next.
         * @param destination Destination will be returned if nothing found in nodes map.
         */
        DecisionNode(int index, Map<TinyString, Node> nodes, Destination<?> destination) {
            this.index = index;
            this.nodes = nodes;
            this.destination = destination;
        }

        @Override
        public Destination<?> visit(TinyString[] input) {
            TinyString value = input[index];
            Node child = nodes.get(value);
            if (child != null) {
                return child.visit(input);
            }
            return destination;
        }
    }

    /**
     * Leaf node that holds result destination.
     */
    class LeafNode implements Node {
        private final Destination<?> result;

        LeafNode(Destination<?> result) {
            this.result = result;
        }

        @Override
        public Destination<?> visit(TinyString[] input) {
            return result;
        }
    }
}
