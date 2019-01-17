package ru.kontur.vostok.hercules.partitioner;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.bytes.ByteUtil;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author Gregory Koshelev
 */
public class RandomPartitioner implements Partitioner {
    private static SecureRandom root = new SecureRandom();
    private final ThreadLocal<Random> cachedRandom = new ThreadLocal<>();

    private Random getRandom() {
        Random random = cachedRandom.get();
        if (random == null) {
            random = new Random(ByteUtil.toLong(root.generateSeed(8)));
            cachedRandom.set(random);
        }
        return random;
    }
    @Override
    public int partition(Event event, ShardingKey shardingKey, int partitions) {
        return getRandom().nextInt(partitions);
    }
}
