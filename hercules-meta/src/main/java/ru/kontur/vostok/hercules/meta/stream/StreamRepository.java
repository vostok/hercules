package ru.kontur.vostok.hercules.meta.stream;

/**
 * @author Gregory Koshelev
 */
public class StreamRepository {
    public BaseStream getBaseStream(String name) {
        //TODO: Get BaseStream by name
        BaseStream stream = new BaseStream();
        stream.setName(name);
        stream.setPartitions(2);
        stream.setShardingKey(new String[0]);
        stream.setTtl(24 * 3600 * 1000);

        return stream;
    }
}
