package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.List;

/**
 * Trash
 *
 * @author Kirill Sulim
 */
public class Trash {

    private static SentryApiClient sentryApiClient;

    public static void main(String[] args) {
        sentryApiClient = new SentryApiClient(
                "http://localhost:9000",
                "5bcaa362cfbe403a8c34dc521e13acb5fcc69927248f43d5b45cb4b6fe76bd6d"
        );

        Result<Void, String> test = sentryApiClient.createOrganization("test");

        if (test.isOk()) {
            System.out.println("ok");
        }
        else {
            System.out.println(test.getError());
        }
    }
}
