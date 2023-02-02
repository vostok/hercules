package ru.kontur.vostok.hercules.application;

/**
 * Application runner.
 * <p>
 * Implementations of this interface starting concrete Hercules applications.
 * </p>
 *
 * @author Aleksandr Yuferov
 */
public interface ApplicationRunner {

    /**
     * Application id.
     * <p>
     * Similar to an {@link #getApplicationName() application name} but used in places where special characters forbidden, e.g metrics paths. Also expected to
     * be short.
     * </p>
     *
     * @return Application id.
     */
    String getApplicationId();

    /**
     * Application name.
     * <p>
     * Human readable name of the application. It is used in answer of {@code GET /about} request.
     * </p>
     *
     * @return Application name.
     */
    String getApplicationName();

    /**
     * Initialization phase of the application.
     * <p>
     * In this phase all classes of the application context should be created. To control the lifetime {@link Container} is used. To get the instance of
     * container object use {@link Application#getContainer()}.
     * </p>
     *
     * @param application {@link Application} instance.
     * @throws Exception Will be caught by {@link Application} class and stop the process with code {@code 1}.
     */
    void init(Application application) throws Exception;

}
