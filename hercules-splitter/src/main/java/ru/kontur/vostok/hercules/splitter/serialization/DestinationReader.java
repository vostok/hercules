package ru.kontur.vostok.hercules.splitter.serialization;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.splitter.service.NodeStreamingSpreader;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Perform {@link Reader#read(Decoder)} operation on given event reader and returns destination node name from given spreader using
 * {@link NodeStreamingSpreader#destination()} method.
 * <p>
 * Expected that event reader implementation performs some actions that will change spreader state.
 * <p>
 * Spreader will be reset after destination is received from it even if exception was thrown.
 * <p>
 * Objects of this class is not thread safe and cannot be shared between threads because spreader can change state between methods calls
 * {@link Reader#read(Decoder)} and {@link NodeStreamingSpreader#destination()} (see {@link #read(Decoder)} method implementation).
 *
 * @author Aleksandr Yuferov
 */
@NotThreadSafe
public class DestinationReader implements Reader<String> {

    private final Reader<Event> eventReader;
    private final NodeStreamingSpreader spreader;

    /**
     * Constructor.
     *
     * @param eventReader Event reader.
     * @param spreader    Node spreader.
     */
    public DestinationReader(Reader<Event> eventReader, NodeStreamingSpreader spreader) {
        this.eventReader = eventReader;
        this.spreader = spreader;
    }

    /**
     * Perform reading data.
     * <p>
     * Performs reading delegating it to given event reader and after that returns destination that spreader returns. Expected that event reader implementation
     * performs some actions that will change spreader state. After performing all actions reset the spreader.
     *
     * @param decoder Decoder.
     * @return Result of {@link NodeStreamingSpreader#destination()} method.
     */
    @Override
    public String read(Decoder decoder) {
        try {
            eventReader.read(decoder);
            return spreader.destination();
        } finally {
            spreader.reset();
        }
    }
}
