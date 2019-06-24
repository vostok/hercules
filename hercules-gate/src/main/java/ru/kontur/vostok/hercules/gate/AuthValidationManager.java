package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.curator.CuratorClient;
import ru.kontur.vostok.hercules.meta.auth.validation.Validation;
import ru.kontur.vostok.hercules.meta.auth.validation.ValidationSerializer;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.util.Maps;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gregory Koshelev
 */
public class AuthValidationManager {
    private final CuratorClient curatorClient;
    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
    private final ValidationSerializer validationSerializer = new ValidationSerializer();
    private final AtomicReference<Map<String, Map<String, ContentValidator>>> validators = new AtomicReference<>(new HashMap<>());
    private final AtomicReference<Map<String, Map<String, Set<String>>>> tags = new AtomicReference<>(new HashMap<>());

    private static final ContentValidator EMPTY_VALIDATOR = new ContentValidator(new Validation(null, null, new Filter[0]));

    public AuthValidationManager(CuratorClient curatorClient) {
        this.curatorClient = curatorClient;
    }

    public void start() throws Exception {
        if (!state.compareAndSet(State.INIT, State.STARTING)) {
            throw new IllegalStateException("Invalid state of content validator");
        }

        update();

        state.set(State.RUNNING);
    }

    private void update() throws Exception {
        if (state.get() == State.STOPPED) {
            return;
        }

        List<String> children = curatorClient.children("/hercules/auth/validations");

        Map<String, Map<String, ContentValidator>> newValidators = new HashMap<>();
        Map<String, Map<String, Set<String>>> newTags = new HashMap<>();
        for (String value : children) {
            Validation validation = validationSerializer.deserialize(value);

            Map<String, ContentValidator> streamToValidatorMap = newValidators.computeIfAbsent(validation.getApiKey(), key -> new HashMap<>());
            streamToValidatorMap.put(validation.getStream(), new ContentValidator(validation));

            Map<String, Set<String>> streamToTagsMap = newTags.computeIfAbsent(validation.getApiKey(), key -> new HashMap<>());
            streamToTagsMap.put(validation.getStream(), extractTags(validation));
        }
        validators.set(newValidators);
        tags.set(newTags);
    }

    public void stop() {
        state.set(State.STOPPED);
    }

    public Set<String> getTags(String apiKey, String stream) {
        return tags.get().getOrDefault(apiKey, Collections.emptyMap()).getOrDefault(stream, Collections.emptySet());
    }

    public ContentValidator validator(String apiKey, String stream) {
        return validators.get().getOrDefault(apiKey, Collections.emptyMap()).getOrDefault(stream, EMPTY_VALIDATOR);
    }

    private static Set<String> extractTags(Validation validation) {
        Filter[] filters = validation.getFilters();

        if (filters == null || filters.length == 0) {
            return Collections.emptySet();
        }

        Set<String> tags = new HashSet<>(Maps.effectiveHashMapCapacity(filters.length));
        for (Filter filter : filters) {
            tags.add(filter.getHPath().getRootTag());//TODO: Should be revised (do not parse all the tag tree if the only tag chain is needed)
        }
        return tags;
    }

    private enum State {
        INIT,
        STARTING,
        RUNNING,
        STOPPED;
    }
}
