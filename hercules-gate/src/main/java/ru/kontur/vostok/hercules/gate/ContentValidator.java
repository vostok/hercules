package ru.kontur.vostok.hercules.gate;

import ru.kontur.vostok.hercules.meta.auth.validation.Validation;
import ru.kontur.vostok.hercules.meta.filter.Filter;
import ru.kontur.vostok.hercules.protocol.Event;

/**
 * @author Gregory Koshelev
 */
public class ContentValidator {
    private final Validation validation;

    public ContentValidator(Validation validation) {
        this.validation = validation;
    }

    public boolean validate(Event event) {
        for (Filter filter : validation.getFilters()) {
            if (!filter.test(event.getPayload())) {
                return false;
            }
        }
        return true;
    }
}
