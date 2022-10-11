package ru.kontur.vostok.hercules.splitter.serialization;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.splitter.service.NodeStreamingSpreader;

import static org.mockito.Mockito.never;

/**
 * {@link DestinationReader} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@RunWith(MockitoJUnitRunner.class)
public class DestinationReaderTest {

    @Mock
    private Reader<Event> eventReader;

    @Mock
    private NodeStreamingSpreader spreader;

    @InjectMocks
    private DestinationReader destinationReader;

    @Mock
    private Decoder decoder;

    @Test
    public void shouldWorkCorrectly() {
        Mockito.when(spreader.destination()).thenReturn("destination-node");

        String result = destinationReader.read(decoder);

        InOrder inOrder = Mockito.inOrder(eventReader, spreader);
        inOrder.verify(eventReader).read(decoder);
        inOrder.verify(spreader).destination();
        inOrder.verify(spreader).reset();
        Assert.assertEquals("destination-node", result);
    }

    @Test
    public void shouldResetSpreaderIfReaderThrowsException() {
        Mockito.when(eventReader.read(Mockito.any())).thenThrow(new RuntimeException("something going wrong"));

        Assert.assertThrows(RuntimeException.class, () -> destinationReader.read(decoder));

        InOrder inOrder = Mockito.inOrder(eventReader, spreader);
        inOrder.verify(eventReader).read(decoder);
        inOrder.verify(spreader).reset();
        Mockito.verify(spreader, never()).destination();
    }

    @Test
    public void shouldResetSpreaderIfDestinationThrowsException() {
        Mockito.when(spreader.destination()).thenThrow(new RuntimeException("something going wrong"));

        Assert.assertThrows(RuntimeException.class, () -> destinationReader.read(decoder));

        InOrder inOrder = Mockito.inOrder(spreader);
        inOrder.verify(spreader).destination();
        inOrder.verify(spreader).reset();
    }
}
