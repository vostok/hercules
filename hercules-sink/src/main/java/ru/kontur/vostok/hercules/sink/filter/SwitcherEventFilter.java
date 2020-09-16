package ru.kontur.vostok.hercules.sink.filter;

import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.properties.PropertiesUtil;

import java.util.Properties;

/**
 * Switcher filter is a mock filter for unit testing.
 * @author Gregory Koshelev
 */
public class SwitcherEventFilter extends EventFilter {
    /**
     * If {@code true} the filter pass all events, otherwise all events will be filtered out.
     */
    private volatile boolean on;

    /**
     * Inheritors must implement constructor with the same signature.
     *
     * @param properties properties for the filter initialization
     */
    public SwitcherEventFilter(Properties properties) {
        super(properties);

        on = PropertiesUtil.get(Props.ON, properties).get();
    }

    @Override
    public boolean test(Event event) {
        return on;
    }

    public void permitAll() {
        on = true;
    }

    public void denyAll() {
        on = false;
    }

    private static class Props {
        private static final Parameter<Boolean> ON = Parameter.booleanParameter("on").
                withDefault(true).
                build();
    }
}
