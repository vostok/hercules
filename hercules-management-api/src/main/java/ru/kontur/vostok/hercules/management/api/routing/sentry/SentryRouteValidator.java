package ru.kontur.vostok.hercules.management.api.routing.sentry;

import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.routing.config.zk.ZookeeperReadRepository;
import ru.kontur.vostok.hercules.routing.engine.Route;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfig;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineRoute;
import ru.kontur.vostok.hercules.util.routing.interpolation.InterpolationExpression;
import ru.kontur.vostok.hercules.util.routing.interpolation.Interpolator;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
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
        var engineConfig = Optional
                .ofNullable((DecisionTreeEngineConfig) repository.fetchEngineConfig(null))
                .orElse(defaultConfig);
        {
            ValidationResult error = findProhibitedTags(engineConfig, conditions)
                    .map(paths -> "tag paths found in conditions that are not allowed: " + paths)
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        {
            ValidationResult error = checkAllTagsListed(engineConfig, conditions)
                    .map(paths -> "all tag paths should be listed, but paths " + paths + " not found")
                    .map(ValidationResult::error)
                    .orElse(null);
            if (error != null) {
                return error;
            }
        }
        SentryDestination destination = verifiableRoute.destination();
        {
            if (destination == null) {
                return ValidationResult.error("destination cannot be null (if you want to create 'nowhere' destination use "
                        + "'{ \"organization\": null, \"project\": \"null\" }' destination)");
            }
        }
        if (!destination.isNowhere()) {
            {
                List<InterpolationExpression> expressions = interpolator.extractInterpolations(destination.organization());
                ValidationResult error = findConstantInterpolations(conditions, expressions)
                        .map(desc -> "constant interpolations found in 'organization' field:\n" + desc)
                        .or(() -> findNullInterpolations(conditions, expressions)
                                .map(desc -> "null interpolations found in 'organization' field:\n" + desc))
                        .map(ValidationResult::error)
                        .orElse(null);
                if (error != null) {
                    return error;
                }
            }
            {
                List<InterpolationExpression> expressions = interpolator.extractInterpolations(destination.project());
                ValidationResult error = findConstantInterpolations(conditions, expressions)
                        .map(desc -> "constant interpolations found in 'project' field:\n" + desc)
                        .or(() -> findNullInterpolations(conditions, expressions)
                                .map(desc -> "null interpolations found in 'project' field:\n" + desc))
                        .map(ValidationResult::error)
                        .orElse(null);
                if (error != null) {
                    return error;
                }
            }
        }
        return ValidationResult.ok();
    }

    private Optional<String> checkAllTagsListed(DecisionTreeEngineConfig engineConfig, Map<String, String> conditions) {
        String result = engineConfig.allowedTags().stream()
                .map(HPath::getPath)
                .filter(path -> !conditions.containsKey(path))
                .collect(Collectors.joining(", "));
        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }

    private Optional<String> findProhibitedTags(DecisionTreeEngineConfig config, Map<String, String> conditions) {
        List<String> allowedTagsPaths = config.allowedTags().stream()
                .map(HPath::getPath)
                .collect(Collectors.toList());
        String prohibitedTagsDesc = conditions.keySet().stream()
                .filter(condition -> !allowedTagsPaths.contains(condition))
                .collect(Collectors.joining(", "));
        return prohibitedTagsDesc.isEmpty() ? Optional.empty() : Optional.of(prohibitedTagsDesc);
    }

    private Optional<String> findConstantInterpolations(Map<String, String> conditions, List<InterpolationExpression> expressions) {
        String replacementDesc = expressions.stream()
                .filter(interpolation -> interpolation.namespace().equals("tag"))
                .map(InterpolationExpression::variable)
                .filter(variable -> {
                    String condition = conditions.get(variable);
                    return condition != null && !"*".equals(condition);
                })
                .distinct()
                .map(path -> String.format("* '{tag:%s}' can be replaced with '%s'", path, conditions.get(path)))
                .collect(Collectors.joining("\n", "", ""));
        return replacementDesc.isEmpty() ? Optional.empty() : Optional.of(replacementDesc);
    }

    private Optional<String> findNullInterpolations(Map<String, String> conditions, List<InterpolationExpression> expressions) {
        String description = expressions.stream()
                .filter(interpolation -> interpolation.namespace().equals("tag"))
                .map(InterpolationExpression::variable)
                .filter(path -> conditions.get(path) == null)
                .distinct()
                .map(path -> String.format("* '{tag:%s}' will always be null, i.e. messages for this rule will be always filtered. "
                        + "Use nowhere destination ('{ \"organization\": null, \"project\": \"null\" }') if you want filter data using this conditions", path))
                .collect(Collectors.joining("\n", "", ""));
        return description.isEmpty() ? Optional.empty() : Optional.of(description);
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
