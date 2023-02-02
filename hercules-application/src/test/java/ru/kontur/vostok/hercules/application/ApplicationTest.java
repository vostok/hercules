package ru.kontur.vostok.hercules.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.CollectionUtils;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.util.functional.ThrowableCallable;
import ru.kontur.vostok.hercules.util.functional.ThrowableRunnable;
import ru.kontur.vostok.hercules.util.test.InMemoryLoggerAppender;
import ru.kontur.vostok.hercules.util.time.TimeSource;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * {@link Application} class unit tests.
 *
 * @author Aleksandr Yuferov
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("Application class unit tests")
class ApplicationTest {

    @Mock
    ApplicationConfig config;

    @Mock
    ApplicationContext context;

    @Mock
    Container container;

    @Mock
    ApplicationRunner runner;

    InMemoryLoggerAppender inMemoryLoggerAppender;
    Logger logger = (Logger) LoggerFactory.getLogger(Application.class);

    @Mock
    TimeSource timeSource;

    @Mock(name = "ApplicationStateObserverMock")
    ApplicationStateObserver applicationStateObserver;

    Application application;

    @BeforeEach
    void prepare() {
        lenient().doCallRealMethod().when(timeSource).measureMs(any(ThrowableCallable.class));
        lenient().doCallRealMethod().when(timeSource).measureMs(any(ThrowableRunnable.class));
        lenient().doReturn("My-Cool-App").when(context).getApplicationName();
        doReturn(List.of(applicationStateObserver)).when(config).getStateObservers();

        inMemoryLoggerAppender = new InMemoryLoggerAppender();
        inMemoryLoggerAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.ALL);
        logger.addAppender(inMemoryLoggerAppender);

        application = new Application(config, context, container, logger, timeSource);
    }

    @Test
    @DisplayName("should log initialization time")
    void shouldLogStartupTime() {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            return 123L;
        }).when(timeSource).measureMs(any(ThrowableRunnable.class));

        application.start(runner);

        assertTrue(inMemoryLoggerAppender.contains("Application My-Cool-App initialized for 123 millis", Level.INFO));
    }

    @Test
    @DisplayName("should return true if no errors occurred during start")
    void shouldReturnTrueIfNoErrorsOccurredDuringStar() {
        boolean result = application.start(runner);

        assertTrue(result);
    }

    @Test
    @DisplayName("should return false if error occurred on initialization process")
    void shouldReturnFalseIfErrorOccurredOnInitializationProcess() throws Throwable {
        doThrow(new RuntimeException("some error")).when(runner).init(any());

        boolean result = application.start(runner);

        verify(runner).init(any());
        assertFalse(result);
    }

    @Test
    @DisplayName("should log error if it is occurred in initialization process")
    void shouldLogErrorIfItIsOccurredOnInitializationProcess() throws Throwable {
        doThrow(new RuntimeException("some error")).when(runner).init(any());

        application.start(runner);

        verify(runner).init(any());
        List<ILoggingEvent> events = inMemoryLoggerAppender.search("Fatal error occurred during application initialization", Level.ERROR);
        assertEquals(1, events.size());
        IThrowableProxy throwableProxy = CollectionUtils.getOnlyElement(events).getThrowableProxy();
        assertEquals(RuntimeException.class.getName(), throwableProxy.getClassName());
        assertEquals("some error", throwableProxy.getMessage());
    }

    @Test
    @DisplayName("should stop application if error occurred in initialization process")
    void shouldStopApplicationIfErrorOccurredOnInitializationProcess() throws Throwable {
        doThrow(new RuntimeException("some error")).when(runner).init(any());

        application.start(runner);

        var inOrder = Mockito.inOrder(applicationStateObserver, runner);
        inOrder.verify(applicationStateObserver).onApplicationStateChanged(refEq(application), eq(ApplicationState.STARTING));
        inOrder.verify(runner).init(refEq(application));
        inOrder.verify(applicationStateObserver).onApplicationStateChanged(refEq(application), eq(ApplicationState.STOPPING));
        inOrder.verify(applicationStateObserver).onApplicationStateChanged(refEq(application), eq(ApplicationState.STOPPED));
        verifyNoMoreInteractions(applicationStateObserver);
    }

    @Test
    @DisplayName("should log shutdown process duration")
    void shouldLogShutdownDuration() {
        doReturn("My-Cool-App").when(context).getApplicationName();
        assumeTrue(application.start(runner));
        doAnswer(invocation -> {
            invocation.callRealMethod();
            return new TimeSource.Result<>(true, 123);
        }).when(timeSource).measureMs(any(ThrowableCallable.class));

        application.stop();

        assertTrue(inMemoryLoggerAppender.contains("Finished My-Cool-App shutting down for 123 millis with status OK", Level.INFO));
    }

    @Test
    @DisplayName("should throw if try given to start twice")
    void shouldThrowIfTryGivenToStartTwice() {
        assumeTrue(application.start(runner));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> application.start(runner));
        assertEquals("can start application only in INIT or STOPPED but current state was RUNNING", exception.getMessage());
    }

    @Test
    @DisplayName("should support restart")
    void shouldSupportRestart() {
        assumeTrue(application.start(runner));
        application.stop();

        assertTrue(application.start(runner));
    }

    @Test
    @DisplayName("should log error thrown from ApplicationStateObserver and suppress it")
    void shouldLogErrorThrownFromApplicationStateObserverAndSuppressIt() throws Exception {
        doThrow(new Exception("some error"))
                .when(applicationStateObserver).onApplicationStateChanged(any(), any());

        assertTrue(application.start(runner));

        List<ILoggingEvent> foundEvents = inMemoryLoggerAppender
                .search("Application state observer " + applicationStateObserver + " thrown exception during state observers notification", Level.ERROR);
        assertEquals(2, foundEvents.size());
    }

    @Test
    @DisplayName("should have correct stop order")
    void shouldHaveCorrectStopOrder() throws Exception {
        doReturn(123).when(config).getShutdownGracePeriodMs();
        doReturn(321).when(config).getShutdownTimeoutMs();

        assumeTrue(application.start(runner));
        application.stop();

        InOrder inOrder = inOrder(applicationStateObserver, timeSource, container);
        inOrder.verify(applicationStateObserver).onApplicationStateChanged(refEq(application), eq(ApplicationState.STOPPING));
        inOrder.verify(timeSource).sleep(eq(123L));
        inOrder.verify(container).stop(eq(321L), eq(TimeUnit.MILLISECONDS));
    }
}
