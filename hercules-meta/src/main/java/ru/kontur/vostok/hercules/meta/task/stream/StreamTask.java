package ru.kontur.vostok.hercules.meta.task.stream;

import ru.kontur.vostok.hercules.meta.stream.Stream;

/**
 * @author Gregory Koshelev
 */
public class StreamTask {
    private Stream stream;
    private StreamTaskType type;

    public StreamTask(Stream stream, StreamTaskType type) {
        this.stream = stream;
        this.type = type;
    }

    public StreamTask() {
    }

    public Stream getStream() {
        return stream;
    }
    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public StreamTaskType getType() {
        return type;
    }
    public void setType(StreamTaskType type) {
        this.type = type;
    }
}
