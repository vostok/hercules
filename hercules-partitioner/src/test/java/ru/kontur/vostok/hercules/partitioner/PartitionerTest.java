package ru.kontur.vostok.hercules.partitioner;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class PartitionerTest {
    @Test
    public void roundRobinPartitionerTest() {
        final int partitions = 3;
        // Specify seed to avoid flaky test since int overflow and negative to positive transition as well
        Partitioner partitioner = new RoundRobinPartitioner(0);

        Event event = buildTestEvent();

        int[] values = new int[partitions];
        for (int i = 0; i < partitions; i++) {
            int partition = partitioner.partition(event, null, partitions);
            assertRange(0, partitions, partition);
            values[partition]++;
        }
        assertArrayValuesEqual(1, values);
    }

    @Test
    public void batchedPerThreadPartitionerTest() {
        final int partitions = 3;
        final int batchSize = 10;
        // Specify seed to avoid flaky test since int overflow and negative to positive transition as well
        Partitioner partitioner = new BatchedPerThreadPartitioner(batchSize, () -> new AtomicInteger(0));

        Event event = buildTestEvent();

        int partition = partitioner.partition(event, null, partitions);
        assertRange(0, partitions, partition);

        for (int i = 1; i < batchSize; i++) {
            assertEquals(
                    "Events from the same batch must belong to same partition",
                    partition,
                    partitioner.partition(event, null, partitions));
        }

        int secondPartition = partitioner.partition(event, null, partitions);
        assertRange(0, partitions, secondPartition);
        assertNotEquals(partition, secondPartition);
    }

    Event buildTestEvent() {
        TimeSource time = new MockTimeSource();
        return EventBuilder
                .create(TimeUtil.millisToTicks(time.milliseconds()), UUID.randomUUID())
                .build();
    }

    void assertRange(int minBoundInclusive, int maxBoundExclusive, int actualValue) {
        if (minBoundInclusive >= maxBoundExclusive) {
            throw new IllegalArgumentException("min bound should be lesser than max bound");
        }

        assertTrue(minBoundInclusive <= actualValue && actualValue < maxBoundExclusive);
    }

    void assertArrayValuesEqual(int expectedValue, int[] actualValues) {
        for (int actualValue : actualValues) {
            assertEquals(expectedValue, actualValue);
        }
    }
}
