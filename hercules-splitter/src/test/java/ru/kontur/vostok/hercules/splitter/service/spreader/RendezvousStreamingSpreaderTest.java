package ru.kontur.vostok.hercules.splitter.service.spreader;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.splitter.service.StreamingHasher;
import ru.kontur.vostok.hercules.splitter.service.StreamingHashersFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Test class of the Rendezvous spreader.
 *
 * @author Aleksandr Yuferov
 */
public class RendezvousStreamingSpreaderTest {
    private Supplier<StreamingHasher> hasherFactory;
    private int sampleSize;

    /**
     * Preparation for tests.
     */
    @Before
    public void prepare() {
        hasherFactory = StreamingHashersFactory
                .factoryForAlgorithm("xxhash32_fastest_java_instance");

        sampleSize = 1_000_000;
    }

    @Test
    public void shouldDistributeDataUniformlyBetweenNodes() {
        Map<String, Double> nodes = new HashMap<>();
        nodes.put("node-1", 1.0);
        nodes.put("node-2", 1.0);

        Map<String, Set<Integer>> distribution = spread(nodes, hasherFactory);

        Assert.assertEquals(0.50, regardingSamplingSize(distribution.get("node-1")), 0.01);
        Assert.assertEquals(0.50, regardingSamplingSize(distribution.get("node-2")), 0.01);
    }

    @Test
    public void shouldDistributeDataByWeightsBetweenNodes() {
        Map<String, Double> nodes = new HashMap<>();
        nodes.put("node-1", 1.0);
        nodes.put("node-2", 4.0);

        Map<String, Set<Integer>> distribution = spread(nodes, hasherFactory);

        Assert.assertEquals(0.20, regardingSamplingSize(distribution.get("node-1")), 0.01);
        Assert.assertEquals(0.80, regardingSamplingSize(distribution.get("node-2")), 0.01);
    }

    @Test
    public void dataShouldBeMovedAccordingToTheProbabilityAfterNodeAdd() {
        Map<String, Double> nodes = new HashMap<>();
        nodes.put("node-1", 1.0);
        nodes.put("node-2", 1.0);
        Map<String, Set<Integer>> beforeNodeAdded = spread(nodes, hasherFactory);

        nodes.put("node-3", 1.0);
        Map<String, Set<Integer>> afterNodeAdded = spread(nodes, hasherFactory);

        Assert.assertEquals(0.66, remainingProportions("node-1", beforeNodeAdded, afterNodeAdded), 0.01);
        Assert.assertEquals(0.66, remainingProportions("node-2", beforeNodeAdded, afterNodeAdded), 0.01);
    }

    private double regardingSamplingSize(Set<Integer> nodeData) {
        return 1.0 * nodeData.size() / sampleSize;
    }

    private double remainingProportions(String nodeName, Map<String, Set<Integer>> before, Map<String, Set<Integer>> after) {
        Set<Integer> intersection = intersection(before.get(nodeName), after.get(nodeName));
        return 1.0 * intersection.size() / before.get(nodeName).size();
    }

    private <T> Set<T> intersection(Set<T> lhs, Set<T> rhs) {
        HashSet<T> result = new HashSet<>(lhs);
        result.retainAll(rhs);
        return result;
    }

    @NotNull
    private Map<String, Set<Integer>> spread(Map<String, Double> nodes, Supplier<StreamingHasher> hasherFactory) {
        RendezvousStreamingSpreader spreader = new RendezvousStreamingSpreader(nodes, hasherFactory);
        byte[] buffer = new byte[4];
        Map<String, Set<Integer>> counters = new HashMap<>();
        for (int i = 0; i < sampleSize; i++) {
            buffer[0] = (byte) (i & 0xFF);
            buffer[1] = (byte) ((i >> 4) & 0xFF);
            buffer[2] = (byte) ((i >> 8) & 0xFF);
            buffer[3] = (byte) ((i >> 16) & 0xFF);

            spreader.update(buffer);
            counters.computeIfAbsent(spreader.destination(), (key) -> new HashSet<>()).add(i);
            spreader.reset();
        }
        return counters;
    }
}
