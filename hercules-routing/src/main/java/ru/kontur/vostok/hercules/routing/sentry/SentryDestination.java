package ru.kontur.vostok.hercules.routing.sentry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.kontur.vostok.hercules.routing.Destination;
import ru.kontur.vostok.hercules.routing.interpolation.InterpolationExpression;
import ru.kontur.vostok.hercules.routing.interpolation.Interpolator;
import ru.kontur.vostok.hercules.util.text.StringUtil;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Implementation of {@link Destination} interface for full identification of Sentry project.
 *
 * @author Aleksandr Yuferov
 */
@JsonPropertyOrder({"organization", "project"})
public class SentryDestination implements Destination<SentryDestination> {
    private static final SentryDestination NOWHERE = new SentryDestination(null, null);
    private static final InterpolationExpression PROJECT_INTERPOLATION = InterpolationExpression.of("tag", "properties/project");
    private static final InterpolationExpression SUBPROJECT_INTERPOLATION = InterpolationExpression.of("tag", "properties/subproject");
    private static final Pattern FORBIDDEN_CHARS_PATTERN = Pattern.compile("[^-_a-z0-9]");

    private final String organization;
    private final String project;

    /**
     * Destination to nowhere.
     * <p>
     * Object produced by this factory will:
     * <li>return {@code null} from {@link #organization()} and {@link #project()};</li>
     * <li>return {@code true} from {@link #isNowhere()};</li>
     * <li>{@link #sanitize()} and {@link #interpolate} methods will return the same object.</li>
     *
     * @return Nowhere destination.
     */
    public static SentryDestination toNowhere() {
        return NOWHERE;
    }

    /**
     * Default destination.
     * <p>
     * Destination created by this factory will override interpolation logic as follows.
     * <li>If project and subproject tags are presented in interpolation context then destination after interpolation
     * will be: organization - {@code <project tag value>}, project - {@code <subproject tag value>}.</li>
     * <li>If project tag is presented only then destination after interpolation will be:
     * organization - {@code <project tag value>}, project - {@code <project tag value>}.</li>
     * <li>If project not presented in event then destination after interpolation will be {@link #toNowhere()}.</li>
     *
     * @return Default destination.
     */
    public static SentryDestination byDefault() {
        return new SentryDestination("{tag:properties/project}", "{tag:properties/subproject}") {
            @Override
            public SentryDestination interpolate(Interpolator interpolator, Interpolator.Context context) {
                CharSequence project = context.stringValueOf(PROJECT_INTERPOLATION);
                CharSequence subproject = context.stringValueOf(SUBPROJECT_INTERPOLATION);
                return of(project, subproject == null ? project : subproject);
            }
        };
    }

    @JsonCreator
    public static SentryDestination of(
            @JsonProperty("organization") CharSequence organization,
            @JsonProperty("project") CharSequence project
    ) {
        if (StringUtil.isNullOrEmpty(organization) || StringUtil.isNullOrEmpty(project)) {
            return toNowhere();
        }
        return new SentryDestination(organization.toString(), project.toString());
    }

    protected SentryDestination(String organization, String project) {
        this.organization = organization;
        this.project = project;
    }

    @JsonIgnore
    public boolean isNowhere() {
        return organization == null || project == null;
    }

    @JsonProperty("organization")
    public String organization() {
        return organization;
    }

    @JsonProperty("project")
    public String project() {
        return project;
    }

    /**
     * Returns sanitized copy of the object.
     *
     * @return Object with sanitized fields.
     */
    public SentryDestination sanitize() {
        if (isNowhere()) {
            return this;
        }
        return new SentryDestination(sanitizeName(organization), sanitizeName(project));
    }

    /**
     * Perform interpolations into organization and project fields.
     *
     * @param interpolator Interpolator
     * @param context      Interpolation context
     * @return Copy of the object with interpolated fields.
     */
    @Override
    public SentryDestination interpolate(Interpolator interpolator, Interpolator.Context context) {
        if (isNowhere()) {
            return this;
        }
        return SentryDestination.of(
                interpolator.interpolate(organization, context),
                interpolator.interpolate(project, context)
        );
    }

    @Override
    public boolean equals(Object otherRaw) {
        if (this == otherRaw) {
            return true;
        }
        if (otherRaw == null || getClass() != otherRaw.getClass()) {
            return false;
        }
        SentryDestination other = (SentryDestination) otherRaw;
        return Objects.equals(organization, other.organization) &&
                Objects.equals(project, other.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, project);
    }

    private static String sanitizeName(String name) {
        return FORBIDDEN_CHARS_PATTERN.matcher(name.toLowerCase()).replaceAll("_");
    }
}
