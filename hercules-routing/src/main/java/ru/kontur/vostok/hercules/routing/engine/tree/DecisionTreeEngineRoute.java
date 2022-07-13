package ru.kontur.vostok.hercules.routing.engine.tree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.kontur.vostok.hercules.routing.Destination;
import ru.kontur.vostok.hercules.routing.engine.Route;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Decision tree router engine route.
 *
 * @author Aleksandr Yuferov
 */
@JsonPropertyOrder({"id", "conditions", "destination", "description"})
public class DecisionTreeEngineRoute<D extends Destination<D>> implements Route {
    private final UUID id;
    private final Map<String, String> conditions;
    private final D destination;
    private final String description;

    /**
     * Builder factory.
     *
     * @return Builder object.
     * @param <D> Destination type.
     */
    public static <D extends Destination<D>> Builder<D> builder() {
        return new Builder<>();
    }

    protected DecisionTreeEngineRoute(UUID id, Map<String, String> conditions, D destination, String description) {
        this.id = id;
        this.conditions = conditions;
        this.destination = destination;
        this.description = description;
    }

    @JsonProperty("id")
    public UUID id() {
        return id;
    }

    /**
     * Conditions.
     * <p>
     * Map holds tags values that must be presented in the corresponding event tags in order for this route to be
     * selected as the result of engine calculations.
     * Map indexes should contain only allowed tags that are listed in {@link DecisionTreeEngineConfig#allowedTags()}.
     *
     * @return Condition map.
     */
    @JsonProperty("conditions")
    public Map<String, String> conditions() {
        return conditions;
    }

    /**
     * Destination point of this route.
     *
     * @return Destination.
     */
    @JsonProperty("destination")
    public D destination() {
        return destination;
    }

    /**
     * Human-readable description of this route.
     *
     * @return Description.
     */
    @JsonProperty("description")
    public String description() {
        return description;
    }

    public static class Builder<D extends Destination<D>> {
        private UUID id;
        private Map<String, String> conditions;
        private D destination;
        private String description;

        public Builder<D> setId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder<D> setConditions(Map<String, String> conditions) {
            this.conditions = conditions;
            return this;
        }

        public Builder<D> setDestination(D destination) {
            this.destination = destination;
            return this;
        }

        public Builder<D> setDescription(String description) {
            this.description = description;
            return this;
        }

        public DecisionTreeEngineRoute<D> build() {
            Objects.requireNonNull(conditions, "conditions");
            return new DecisionTreeEngineRoute<>(id, conditions, destination, description);
        }
    }
}
