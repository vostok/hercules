package ru.kontur.vostok.hercules.management.api.routing.sentry;

import org.jetbrains.annotations.Nullable;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;
import ru.kontur.vostok.hercules.routing.engine.tree.DecisionTreeEngineConfig;
import ru.kontur.vostok.hercules.routing.sentry.SentryDestination;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.util.List;

/**
 * Decision tree routing engine config validator for Sentry.
 * <p>
 * Validates that allowed tags is not null and contains {@code properties/project} and {@code properties/subproject}.
 * Tags {@code properties/project} and {@code properties/subproject} are required because of implementation of
 * default routing (see {@link SentryDestination#byDefault()}).
 *
 * @author Aleksandr Yuferov
 */
public class SentryEngineConfigValidator implements Validator<EngineConfig> {
    private static final String PROJECT_TAG_PATH = "properties/project";
    private static final String SUBPROJECT_TAG_PATH = "properties/subproject";

    @Override
    public ValidationResult validate(EngineConfig configRaw) {
        DecisionTreeEngineConfig config = (DecisionTreeEngineConfig) configRaw;
        List<HPath> allowedTags = config.allowedTags();
        if (allowedTags == null) {
            return ValidationResult.error("allowedTags cannot be null");
        }
        ValidationResult error = validateHasRequiredTags(allowedTags);
        if (error != null) {
            return error;
        }
        return ValidationResult.ok();
    }

    @Nullable
    private ValidationResult validateHasRequiredTags(List<HPath> allowedTags) {
        boolean containsProjectTag = false;
        boolean containsSubprojectTag = false;
        for (HPath path : allowedTags) {
            if (PROJECT_TAG_PATH.equals(path.getPath())) {
                containsProjectTag = true;
            }
            if (SUBPROJECT_TAG_PATH.equals(path.getPath())) {
                containsSubprojectTag = true;
            }
        }
        if (!containsProjectTag || !containsSubprojectTag) {
            return ValidationResult.error("sentry routing algorithm requires tags properties/project, " +
                    "properties/subproject listed in allowedTags");
        }
        return null;
    }
}
