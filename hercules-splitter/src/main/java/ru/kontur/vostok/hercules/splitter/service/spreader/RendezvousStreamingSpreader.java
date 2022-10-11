package ru.kontur.vostok.hercules.splitter.service.spreader;

import ru.kontur.vostok.hercules.splitter.service.NodeStreamingSpreader;
import ru.kontur.vostok.hercules.splitter.service.StreamingHasher;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Streaming spreader that implements Rendezvous hashing strategy.
 *
 * @author Aleksandr Yuferov
 */
@NotThreadSafe
public class RendezvousStreamingSpreader implements NodeStreamingSpreader {

    private final Node[] nodes;

    /**
     * Constructor.
     *
     * @param nodes         Nodes weights by names.
     * @param hasherFactory Factory of streaming hash functions.
     */
    public RendezvousStreamingSpreader(Map<String, Double> nodes, Supplier<StreamingHasher> hasherFactory) {
        this.nodes = new Node[nodes.size()];
        int i = 0;
        for (Map.Entry<String, Double> node : nodes.entrySet()) {
            String name = node.getKey();
            double weight = node.getValue();
            this.nodes[i++] = new Node(name, weight, hasherFactory.get());
        }
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        for (Node node : nodes) {
            node.update(buffer, offset, length);
        }
    }

    @Override
    public String destination() {
        Node result = nodes[0];
        double maxScore = result.score();
        for (int i = 1; i < nodes.length; i++) {
            Node node = nodes[i];
            double currScore = node.score();
            if (currScore > maxScore) {
                maxScore = currScore;
                result = node;
            }
        }
        return result.name();
    }

    @Override
    public void reset() {
        for (Node node : nodes) {
            node.reset();
        }
    }

    /**
     * Physical node representation in destination storage system.
     */
    private static class Node {
        private static final long LONG_MAX_HASH_VALUE = 0xFFFFFFFFL;
        private static final double DOUBLE_MAX_HASH_VALUE = LONG_MAX_HASH_VALUE;

        private final String name;
        private final byte[] nameBytes;
        private final double weight;
        private final StreamingHasher hasher;

        Node(String name, double weight, StreamingHasher hasher) {
            this.name = name;
            this.nameBytes = name.getBytes(StandardCharsets.UTF_8);
            this.weight = weight;
            this.hasher = hasher;
        }

        void update(byte[] data, int offset, int size) {
            hasher.update(data, offset, size);
        }

        void reset() {
            hasher.reset();
            hasher.update(nameBytes);
        }

        double score() {
            long unsignedHashValue = hasher.hash() & LONG_MAX_HASH_VALUE;
            return -weight / Math.log(unsignedHashValue / DOUBLE_MAX_HASH_VALUE);
        }

        String name() {
            return name;
        }
    }
}
