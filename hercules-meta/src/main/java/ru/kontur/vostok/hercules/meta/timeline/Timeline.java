package ru.kontur.vostok.hercules.meta.timeline;

import ru.kontur.vostok.hercules.meta.filter.Filter;

/**
 * @author Gregory Koshelev
 */
public class Timeline {
    private String name;
    private int slices;
    private String[] shardingKey;
    private long ttl;
    private long timetrapSize;
    private String[] streams;
    private Filter[] filters;
    private String description;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getSlices() {
        return slices;
    }
    public void setSlices(int slices) {
        this.slices = slices;
    }

    public String[] getShardingKey() {
        return shardingKey;
    }
    public void setShardingKey(String[] shardingKey) {
        this.shardingKey = shardingKey;
    }

    public long getTtl() {
        return ttl;
    }
    public void setTtl(long ttl) {
        this.ttl = ttl;
    }

    public long getTimetrapSize() {
        return timetrapSize;
    }
    public void setTimetrapSize(long timetrapSize) {
        this.timetrapSize = timetrapSize;
    }

    public String[] getStreams() {
        return streams;
    }
    public void setStreams(String[] streams) {
        this.streams = streams;
    }

    public Filter[] getFilters() {
        return filters;
    }
    public void setFilters(Filter[] filters) {
        this.filters = filters;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
}
