package ru.kontur.vostok.hercules.splitter.serialization;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.kontur.vostok.hercules.protocol.decoder.Decoder;
import ru.kontur.vostok.hercules.protocol.decoder.Reader;
import ru.kontur.vostok.hercules.splitter.service.StreamingAlgorithm;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;

/**
 * {@link StreamingAlgorithmReader} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@RunWith(MockitoJUnitRunner.class)
public class StreamingAlgorithmReaderTest {

    @Mock
    private Reader<SomeType> originalReader;

    @Mock
    private SomeType returnValue;

    @Mock
    private StreamingAlgorithm algorithm;

    @Mock
    private Decoder decoder;

    private StreamingAlgorithmReader<SomeType> reader;

    @Before
    public void prepare() {
        reader = new StreamingAlgorithmReader<>(originalReader, returnValue, algorithm);
    }

    @Test
    public void shouldDelegateWorkToStreamingAlgorithmInReadMethod() {
        byte[] array = new byte[0];
        Mockito.doReturn(1).when(decoder).position();
        Mockito.when(decoder.array()).thenReturn(array);
        Mockito.doReturn(10).when(originalReader).skip(decoder);

        SomeType result = reader.read(decoder);

        Assert.assertSame(returnValue, result);
        Mockito.verify(algorithm).update(refEq(array), eq(1), eq(10));
    }

    @Test
    public void shouldDelegateSkipToOriginalReaderInSkipMethod() {
        Mockito.when(originalReader.skip(decoder)).thenReturn(10);

        int result = reader.skip(decoder);

        Mockito.verify(originalReader).skip(decoder);
        Assert.assertEquals(10, result);
    }

    private static class SomeType {}
}
