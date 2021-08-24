package ru.kontur.vostok.hercules.cassandra.util;

import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Describes cassandra authorization credentials.
 *
 * @author Evgeniy Zatoloka
 */
public class CassandraAuthProvider {

    private final String classname;
    private final String username;
    private final String password;

    public CassandraAuthProvider(Properties properties) {
        this.classname = PropertiesUtil.get(Props.CLASS, properties).get();
        this.username = PropertiesUtil.get(Props.USERNAME, properties).get();
        this.password = PropertiesUtil.get(Props.PASSWORD, properties).get();
    }

    public String getClassname() {
        return classname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private static class Props {
        static final Parameter<String> CLASS =
                Parameter.stringParameter("class").
                        withDefault(CassandraDefaults.DEFAULT_AUTH_CLASSNAME).
                        build();

        static final Parameter<String> USERNAME =
                Parameter.stringParameter("username").
                        required().
                        build();

        static final Parameter<String> PASSWORD =
                Parameter.stringParameter("password").
                        required().
                        build();
    }
}
