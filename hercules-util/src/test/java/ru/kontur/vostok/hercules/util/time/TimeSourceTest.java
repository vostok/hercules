package ru.kontur.vostok.hercules.util.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.util.functional.ThrowableCallable;
import ru.kontur.vostok.hercules.util.functional.ThrowableRunnable;
import ru.kontur.vostok.hercules.util.time.TimeSource.Result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;

/**
 * {@link TimeSource} unit tests.
 *
 * @author Aleksandr Yuferov
 */
@DisplayName("TimeSource unit tests")
@ExtendWith(MockitoExtension.class)
class TimeSourceTest {
    @Mock
    TimeSource timer;

    @Test
    @DisplayName("should measure time of execution for Runnable objects")
    void shouldMeasureTimeOfExecutionForRunnableObjects() {
        doReturn(100L, 150L).when(timer).milliseconds();
        doCallRealMethod().when(timer).measureMs(any(ThrowableRunnable.class));

        long actual = timer.measureMs(() -> {
        });

        assertEquals(50, actual);
    }

    @Test
    @DisplayName("should measure time of execution for Callable objects")
    void shouldMeasureTimeOfExecutionForCallableObjects() {
        doReturn(100L, 150L).when(timer).milliseconds();
        doCallRealMethod().when(timer).measureMs(any(ThrowableCallable.class));

        Result<Boolean> result = timer.measureMs(() -> true);

        assertEquals(true, result.getValue());
        assertEquals(50, result.getDurationMs());
    }
}