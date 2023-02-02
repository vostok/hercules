package ru.kontur.vostok.hercules.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kontur.vostok.hercules.application.Application.Starter;
import ru.kontur.vostok.hercules.util.lifecycle.Lifecycle;
import ru.kontur.vostok.hercules.util.lifecycle.Stoppable;

import java.io.Closeable;

import static com.github.stefanbirkner.systemlambda.SystemLambda.catchSystemExit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * {@link Application} class integration tests.
 *
 * @author Aleksandr Yuferov
 */
@DisplayName("Application class integration tests")
@ExtendWith(MockitoExtension.class)
class ApplicationIntegrationTest {

    @Mock
    ApplicationRunner applicationRunner;

    @BeforeEach
    void prepare() {
        lenient().doReturn("app-id").when(applicationRunner).getApplicationId();
        lenient().doReturn("My-Cool-App").when(applicationRunner).getApplicationName();
    }

    @Test
    @DisplayName("should invoke ApplicationRunner lifetime handlers")
    void shouldWarmupIfConfigSays() throws Exception {
        Application app = Application.run(applicationRunner,
                "application.properties=resource://minimal-application.properties",
                "application.warmup.enable=true",
                "application.shutdown.grace.period.ms=100"
        );
        app.stop();

        verify(applicationRunner).init(refEq(app));
    }

    @Test
    @DisplayName("should throw if try run Application multiple time")
    void shouldThrowIfTryRunApplicationMultipleTime() {
        Application application = null;
        try {
            application = Application.run(applicationRunner, "application.properties=resource://minimal-application.properties");

            var ex = assertThrows(IllegalStateException.class, () -> Application.run(applicationRunner,
                    "application.properties=resource://minimal-application.properties"));
            assertEquals("Expect state STOPPED, but was RUNNING", ex.getMessage());
        } finally {
            if (application != null) {
                application.stop();
            }
        }
    }

    @Test
    @DisplayName("should control lifecycle of components registered in container")
    void shouldControlLifecycleOfComponentsRegisteredInContainer() throws Exception {
        Closeable closeable = mock(Closeable.class);
        Stoppable stoppable = mock(Stoppable.class);
        Lifecycle lifecycle = mock(Lifecycle.class);
        doAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            Container container = application.getContainer();
            container.register(closeable);
            container.register(stoppable);
            container.register(lifecycle);
            return null;
        }).when(applicationRunner).init(any());

        Application app = Application.run(applicationRunner, "application.properties=resource://minimal-application.properties");

        verify(lifecycle).start();
        verifyNoMoreInteractions(lifecycle);
        verifyNoInteractions(stoppable, closeable);


        doReturn(true).when(stoppable).stop(anyLong(), any());
        doReturn(true).when(lifecycle).stop(anyLong(), any());

        app.stop();

        InOrder inOrder = inOrder(closeable, stoppable, lifecycle);
        inOrder.verify(lifecycle).stop(anyLong(), any());
        inOrder.verify(stoppable).stop(anyLong(), any());
        inOrder.verify(closeable).close();
    }

    @Test
    @DisplayName("should have singleton reference")
    void shouldHaveSingletonReference() {

        Application app = null;
        try {
            app = Application.run(applicationRunner, "application.properties=resource://minimal-application.properties");

            assertEquals(app, Application.application());
            assertEquals(app.getContext(), Application.context());
        } finally {
            if (app != null) {
                app.stop();
            }
        }
    }

    @Test
    @DisplayName("should exit with code 1 if startup is failed")
    void shouldExitWithCode1IfStartupIsFailed() throws Exception {
        doThrow(new Exception("error")).when(applicationRunner).init(any());

        int code = catchSystemExit(() -> Application.run(applicationRunner, "application.properties=resource://minimal-application.properties"));

        assertEquals(1, code);
    }

    @Test
    @DisplayName("should have possibility to create external application state observers")
    void shouldHavePossibilityToCreateExternalApplicationStateObservers() {
        Application app = null;
        try (var mocked = mockConstruction(NoopApplicationStateObserver.class)) {
            app = Application.run(
                    applicationRunner,
                    "application.properties=resource://minimal-application.properties",
                    "application.warmup.enable=true",
                    "application.shutdown.grace.period.ms=100",
                    "application.state.observers.classes=ru.kontur.vostok.hercules.application.NoopApplicationStateObserver"
            );
            app.stop();

            NoopApplicationStateObserver createdObserver = mocked.constructed().get(0);
            InOrder inOrder = inOrder(createdObserver);
            inOrder.verify(createdObserver).onApplicationStateChanged(app, ApplicationState.STARTING);
            inOrder.verify(createdObserver).onApplicationStateChanged(app, ApplicationState.RUNNING);
            inOrder.verify(createdObserver).onApplicationStateChanged(app, ApplicationState.STOPPING);
            inOrder.verify(createdObserver).onApplicationStateChanged(app, ApplicationState.STOPPED);
            inOrder.verifyNoMoreInteractions();
        } finally {
            if (app != null) {
                app.stop();
            }
        }
    }

    @Test
    @DisplayName("testing of old run() method (with starter)")
    void testingOfOldRunMethod() {
        try {
            Starter starter = mock(Starter.class);
            Application.run(
                    "My Application Name",
                    "application-id",
                    new String[]{"application.properties=resource://minimal-application.properties"},
                    starter
            );

            Application application = Application.application();
            verify(starter).start(refEq(application.getConfig().getAllProperties()), refEq(Application.application().getContainer()));
        } finally {
            Application.application().stop();
        }
    }

    @Test
    @DisplayName("testing of old run() method (without starter)")
    void testingOfOldRunMethod2() {
        try {
            Application.run(
                    "My Application Name",
                    "application-id",
                    new String[]{"application.properties=resource://minimal-application.properties"}
            );

            Application application = Application.application();
            assertEquals(application.getState(), ApplicationState.RUNNING);
        } finally {
            Application.application().stop();
        }
    }
}
