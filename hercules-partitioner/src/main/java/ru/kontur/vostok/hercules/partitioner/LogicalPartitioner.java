package ru.kontur.vostok.hercules.partitioner;


import ru.kontur.vostok.hercules.meta.stream.Stream;

public class LogicalPartitioner {

    public static int[] getPartitionsForLogicalSharding(Stream stream, int k, int n) {
        if (stream.getPartitions() == n) {
            return new int[]{k};
        } else if (stream.getPartitions() < n) {
            if (k < stream.getPartitions()) {
                return new int[]{k};
            } else {
                return new int[]{};
            }
        } else {
            int[] res = new int[(stream.getPartitions() - k - 1)/ n + 1];
            for (int i = 0; i < res.length; ++i ) {
                res[i] = k + i * n;
            }
            return res;
        }
    }
}
