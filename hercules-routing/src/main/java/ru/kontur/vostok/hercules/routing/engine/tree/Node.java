package ru.kontur.vostok.hercules.routing.engine.tree;

import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.routing.Destination;
import ru.kontur.vostok.hercules.util.text.IgnoreCaseWrapper;

import java.util.List;
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
    Destination<?> visit(List<IgnoreCaseWrapper<TinyString>> input);

    /**
     * Node that makes decision relying on input array.
     */
    class DecisionNode implements Node {

        private final int index;
        private final Map<IgnoreCaseWrapper<TinyString>, Node> concreteValuesNodes;
        private final Node anyOtherValuesNode;

        /**
         * Constructor.
         *
         * @param index    The index of the value to check in the input array.
         * @param concrete Map holds nodes by the value that should be visited next.
         * @param any      Special node for "any" mask rules.
         */
        DecisionNode(int index, Map<IgnoreCaseWrapper<TinyString>, Node> concrete, Node any) {
            this.index = index;
            this.concreteValuesNodes = concrete;
            this.anyOtherValuesNode = any;
        }

        @Override
        public Destination<?> visit(List<IgnoreCaseWrapper<TinyString>> input) {
            IgnoreCaseWrapper<TinyString> value = input.get(index);
            Node child = concreteValuesNodes.get(value);
            if (child != null) {
                return child.visit(input);
            }
            return anyOtherValuesNode.visit(input);
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
        public Destination<?> visit(List<IgnoreCaseWrapper<TinyString>> input) {
            return result;
        }
    }
}
