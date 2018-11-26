package ru.kontur.vostok.hercules.client;

import ru.kontur.vostok.hercules.util.arguments.Preconditions;

/**
 * LogicalShardState
 *
 * @author Kirill Sulim
 */
public class LogicalShardState {

    private final int shardId;
    private final int shardCount;

    public LogicalShardState(int shardId, int shardCount) {
        Preconditions.check(0 <= shardId, "Shard if must be greater or equals 0");
        Preconditions.check(0 < shardCount, "Shard count must be greater than 0");
        Preconditions.check(shardId < shardCount, "Shard id must be lesser than shard count");

        this.shardId = shardId;
        this.shardCount = shardCount;
    }

    public int getShardId() {
        return shardId;
    }

    public int getShardCount() {
        return shardCount;
    }
}
