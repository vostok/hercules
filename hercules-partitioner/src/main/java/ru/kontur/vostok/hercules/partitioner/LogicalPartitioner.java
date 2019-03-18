package ru.kontur.vostok.hercules.partitioner;


import ru.kontur.vostok.hercules.meta.stream.Stream;
import ru.kontur.vostok.hercules.meta.timeline.Timeline;

public class LogicalPartitioner {

    public static int[] getPartitionsForLogicalSharding(Stream stream, int k, int n) {
        return getPartitionsForLogicalSharding(stream.getPartitions(), k, n);
    }

    public static int[] getPartitionsForLogicalSharding(Timeline timeline, int k, int n) {
        return getPartitionsForLogicalSharding(timeline.getSlices(), k, n);
    }

    public static int[] getPartitionsForLogicalSharding(int totalPartitions, int k, int n) {
        if (totalPartitions == n) {
            return new int[]{k};
        } else if (totalPartitions < n) {
            if (k < totalPartitions) {
                return new int[]{k};
            } else {
                return new int[]{};
            }
        } else {
            int[] res = new int[(totalPartitions - k - 1) / n + 1];
            for (int i = 0; i < res.length; ++i ) {
                res[i] = k + i * n;
            }
            return res;
        }
    }
}
