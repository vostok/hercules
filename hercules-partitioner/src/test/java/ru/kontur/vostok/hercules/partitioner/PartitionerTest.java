package ru.kontur.vostok.hercules.partitioner;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.util.time.MockTimeSource;
import ru.kontur.vostok.hercules.util.time.TimeSource;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class PartitionerTest {
    @Test
    public void roundRobinPartitionerTest() {
        final int partitions = 4;
        Partitioner partitioner = new RoundRobinPartitioner();

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
        final int partitions = 4;
        final int batchSize = 8;
        Partitioner partitioner = new BatchedPerThreadPartitioner(batchSize);

        Event event = buildTestEvent();

        int firstPartition = partitioner.partition(event, null, partitions);
        assertRange(0, partitions, firstPartition);

        // There is no guarantee that a size of the first batch equals batchSize due to random seed...
        int secondPartition;
        do {
            secondPartition = partitioner.partition(event, null, partitions);
            assertRange(0, partitions, secondPartition);
        } while (firstPartition == secondPartition);

        // ...That is why check the second one
        for (int i = 1; i < batchSize; i++) {
            int partition = partitioner.partition(event, null, partitions);
            assertRange(0, partitions, partition);
            assertEquals(
                    "Events from the same batch must belong to same partition",
                    secondPartition,
                    partition);
        }

        int thirdPartition = partitioner.partition(event, null, partitions);
        assertRange(0, partitions, thirdPartition);
        assertNotEquals(secondPartition, thirdPartition);
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
