package ru.kontur.vostok.hercules.management.api.routing.sentry;

import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfig;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineRoute;
import ru.kontur.vostok.hercules.routing.interpolation.InterpolationExpression;
import ru.kontur.vostok.hercules.routing.interpolation.Interpolator;
import ru.kontur.vostok.hercules.routing.sentry.SentryDestination;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Validator of routes for Sentry.
 *
 * @author Aleksandr Yuferov
 */
public class SentryRouteValidator implements Validator<Route> {
    private final ZookeeperReadRepository repository;
    private final DecisionTreeEngineConfig defaultConfig;
    private final Interpolator interpolator = new Interpolator();

    public SentryRouteValidator(ZookeeperReadRepository repository, DecisionTreeEngineConfig defaultConfig) {
        this.repository = repository;
        this.defaultConfig = defaultConfig;
    }

    @Override
    public ValidationResult validate(Route routeRaw) {
        @SuppressWarnings("unchecked")
        var verifiableRoute = (DecisionTreeEngineRoute<SentryDestination>) routeRaw;
        Map<String, String> conditions = verifiableRoute.conditions();
        {
            ValidationResult error = findRouteWithSimilarConditions(conditions)
                    .map(id -> "a route " + id + " exists with identical conditions")
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        {
            ValidationResult error = findProhibitedTags(conditions)
                    .map(paths -> "tag paths found in conditions that are not allowed: " + paths)
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        SentryDestination destination = verifiableRoute.destination();
        {
            if (destination == null || destination.isNowhere()) {
                return ValidationResult.error("destination cannot be null or have null fields or have " +
                        "empty strings in fields");
            }
        }
        {
            ValidationResult error = findConstantInterpolations(conditions, destination.organization())
                    .map(desc -> "constant interpolations found in 'organization' field:\n" + desc)
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        {
            ValidationResult error = findConstantInterpolations(conditions, destination.project())
                    .map(desc -> "constant interpolations found in 'project' field:\n" + desc)
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        return ValidationResult.ok();
    }

    private Optional<String> findProhibitedTags(Map<String, String> conditions) {
        var engineConfig = Optional
                .ofNullable((DecisionTreeEngineConfig) repository.fetchEngineConfig(null))
                .orElse(defaultConfig);
        List<String> allowedTagsPaths = engineConfig.allowedTags().stream()
                .map(HPath::getPath)
                .collect(Collectors.toList());
        String prohibitedTagsDesc = conditions.keySet().stream()
                .filter(condition -> !allowedTagsPaths.contains(condition))
                .collect(Collectors.joining(", "));
        return prohibitedTagsDesc.isEmpty() ? Optional.empty() : Optional.of(prohibitedTagsDesc);
    }

    private Optional<String> findConstantInterpolations(Map<String, String> conditions, String templatedField) {
        String replacementDesc = interpolator.extractInterpolations(templatedField).stream()
                .filter(interpolation -> interpolation.namespace().equals("tag"))
                .map(InterpolationExpression::variable)
                .filter(conditions::containsKey)
                .distinct()
                .map(path -> String.format("* '{tag:%s}' can be replaced with '%s'", path, conditions.get(path)))
                .collect(Collectors.joining("\n", "", ""));
        return replacementDesc.isEmpty() ? Optional.empty() : Optional.of(replacementDesc);
    }

    @SuppressWarnings("unchecked")
    private Optional<UUID> findRouteWithSimilarConditions(Map<String, String> conditions) {
        return repository.fetchAllRoutesFilesNames(null).stream()
                .map(relativePath -> repository.fetchRouteByRelativePath(relativePath, null))
                .map(route -> (DecisionTreeEngineRoute<SentryDestination>) route)
                .filter(route -> route.conditions().equals(conditions))
                .map(DecisionTreeEngineRoute::id)
                .findFirst();
    }
}
