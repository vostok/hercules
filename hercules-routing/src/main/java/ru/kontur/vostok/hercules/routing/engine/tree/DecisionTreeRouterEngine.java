package ru.kontur.vostok.hercules.routing.engine.tree;

import com.google.common.base.Preconditions;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.util.routing.Destination;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.RouterEngine;
import ru.kontur.vostok.hercules.util.routing.interpolation.Interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import ru.kontur.vostok.hercules.util.text.IgnoreCaseWrapper;

/**
 * Router engine implemented using decision tree.
 *
 * @author Aleksandr Yuferov
 */
public class DecisionTreeRouterEngine implements RouterEngine<Event> {
    private final Interpolator interpolator;
    private final Indexer indexer;
    private final AtomicReference<Index> index = new AtomicReference<>();

    public DecisionTreeRouterEngine(DecisionTreeEngineConfig defaultConfig, Destination<?> defaultDestination) {
        this.interpolator = new Interpolator();
        this.indexer = new Indexer(defaultConfig, defaultDestination);
    }

    @Override
    public Destination<?> route(Event query) {
        Index index = this.index.get();
        Preconditions.checkState(index != null, "a try given to route using not initialized engine");

        Container payload = query.getPayload();
        List<HPath> allowedTags = index.allowedTags();
        var tagValues = new ArrayList<IgnoreCaseWrapper<TinyString>>(allowedTags.size());
        Interpolator.Context interpolationContext = interpolator.createContext();
        for (HPath tagPath : allowedTags) {
            Variant rawTagValue = tagPath.extract(payload);
            TinyString tagValue = rawTagValue != null
                    ? TinyString.of((byte[]) rawTagValue.getValue())
                    : null;
            tagValues.add(tagValue != null
                    ? new IgnoreCaseWrapper<>(tagValue)
                    : null);
            interpolationContext.add("tag", tagPath.getPath(), tagValue);
        }

        Destination<?> foundDestination = index.root().visit(tagValues);
        return foundDestination.interpolate(interpolator, interpolationContext);
    }

    @Override
    public void init(EngineConfig configRaw, List<? extends Route> rulesRaw) {
        @SuppressWarnings("unchecked")
        List<DecisionTreeEngineRoute<?>> routes = (List<DecisionTreeEngineRoute<?>>) rulesRaw;
        Index newIndex = indexer.init((DecisionTreeEngineConfig) configRaw, routes);
        index.set(newIndex);
    }

    @Override
    public void onRouteCreated(Route route) {
        Index newIndex = indexer.onRuleCreated((DecisionTreeEngineRoute<?>) route);
        index.set(newIndex);
    }

    @Override
    public void onRouteChanged(Route route) {
        Index newIndex = indexer.onRuleChanged((DecisionTreeEngineRoute<?>) route);
        index.set(newIndex);
    }

    @Override
    public void onRouteRemoved(UUID routeId) {
        Index newIndex = indexer.onRuleRemoved(routeId);
        index.set(newIndex);
    }

    @Override
    public void onEngineConfigChanged(EngineConfig configRaw) {
        Index newIndex = indexer.onEngineConfigChanged((DecisionTreeEngineConfig) configRaw);
        index.set(newIndex);
    }
}
