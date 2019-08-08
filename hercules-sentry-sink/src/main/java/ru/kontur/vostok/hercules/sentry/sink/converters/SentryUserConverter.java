package ru.kontur.vostok.hercules.sentry.sink.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.event.interfaces.UserInterface;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.tags.UserTags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

        Map<String, Object> data = new HashMap<>();
        writeExtraData(data, user);

        return new UserInterface(id, username, ipAddress, email, data);
    }

    private static void writeExtraData( Map<String, Object> data, final Container user) {
        for (Map.Entry<String, Variant> entry : user) {
            String key = entry.getKey();
            if (!STANDARD_USER_FIELDS.contains(key)) {
                Optional<String> valueOptional = VariantUtil.extractAsString(entry.getValue());
                if (!valueOptional.isPresent()) {
                    try {
                        valueOptional = Optional.of((new ObjectMapper()).writeValueAsString(entry.getValue()));
                    } catch (JsonProcessingException e) {
                        continue;
                    }
                }
                data.put(key, valueOptional.get());
            }
        }
    }

    private SentryUserConverter() {
        //static class
    }
}
