package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.interfaces.UserInterface;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.tags.UserTags;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SentryUserConverter {

    private static final Set<String> STANDARD_USER_FIELDS = Stream.of(
            UserTags.ID_TAG,
            UserTags.USERNAME_TAG,
            UserTags.IP_ADDRESS_TAG,
            UserTags.EMAIL_TAG)
            .map(TagDescription::getName).collect(Collectors.toSet());

    public static UserInterface convert(final Container user) {

        String id = ContainerUtil.extract(user, UserTags.ID_TAG).orElse(null);

        String username = ContainerUtil.extract(user, UserTags.USERNAME_TAG).orElse(null);

        String ipAddress = ContainerUtil.extract(user, UserTags.IP_ADDRESS_TAG).orElse(null);

        String email = ContainerUtil.extract(user, UserTags.EMAIL_TAG).orElse(null);

        Map<String, Object> additionalData = SentryToMapConverter.containerToMap(user, STANDARD_USER_FIELDS);

        return new UserInterface(id, username, ipAddress, email, additionalData);
    }
}
