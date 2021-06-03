package ru.kontur.vostok.hercules.elastic.sink;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Describes Elastic authentication.
 *
 * @author Petr Demenev
 */
public class ElasticAuth {

    private final CredentialsProvider credentialsProvider;

    public ElasticAuth(Properties properties) {
        AuthType type = PropertiesUtil.get(Props.TYPE, properties).get();

        if (type == AuthType.BASIC) {
            this.credentialsProvider = new BasicCredentialsProvider();
            final String username = PropertiesUtil.get(Props.BASIC_USERNAME, properties).get();
            final String password = PropertiesUtil.get(Props.BASIC_PASSWORD, properties).get();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        } else {
            this.credentialsProvider = null;
        }
    }

    @Nullable
    public CredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    private enum AuthType {
        NONE,
        BASIC;
    }

    private static class Props {
        static final Parameter<AuthType> TYPE =
                Parameter.enumParameter("type", AuthType.class).
                        withDefault(AuthType.NONE).
                        build();

        static final Parameter<String> BASIC_USERNAME =
                Parameter.stringParameter("basic.username").
                        required().
                        build();

        static final Parameter<String> BASIC_PASSWORD =
                Parameter.stringParameter("basic.password").
                        required().
                        build();
    }
}
