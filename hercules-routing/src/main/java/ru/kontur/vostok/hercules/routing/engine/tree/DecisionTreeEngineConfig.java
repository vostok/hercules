package ru.kontur.vostok.hercules.routing.engine.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ru.kontur.vostok.hercules.protocol.hpath.HPath;
import ru.kontur.vostok.hercules.routing.engine.EngineConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decision tree router engine configuration.
 *
 * @author Aleksandr Yuferov
 */
public class DecisionTreeEngineConfig implements EngineConfig {
    @JsonSerialize(contentUsing = HPathSerializer.class)
    @JsonDeserialize(contentUsing = HPathDeserializer.class)
    private final List<HPath> allowedTags;

    public static Builder builder() {
        return new Builder();
    }

    @JsonCreator
    public DecisionTreeEngineConfig(
            @JsonProperty(value = "allowedTags", required = true) List<HPath> allowedTags
    ) {
        this.allowedTags = Collections.unmodifiableList(allowedTags);
    }

    /**
     * Allowed tags.
     * <p>
     * Tags that should be allowed to use in conditions and interpolations in destination.
     *
     * @return List of allowed tags paths.
     */
    @JsonProperty("allowedTags")
    public List<HPath> allowedTags() {
        return allowedTags;
    }

    private static class HPathDeserializer extends StdDeserializer<HPath> {
        public HPathDeserializer() {
            super(HPath.class);
        }

        @Override
        public HPath deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            return HPath.fromPath(parser.getValueAsString());
        }
    }

    private static class HPathSerializer extends StdSerializer<HPath> {
        public HPathSerializer() {
            super(HPath.class);
        }

        @Override
        public void serialize(HPath value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeString(value.getPath());
        }
    }

    public static class Builder {
        private final List<HPath> allowedTags = new ArrayList<>();

        public Builder addAllowedTag(String tagPath) {
            return addAllowedTag(HPath.fromPath(tagPath));
        }

        public Builder addAllowedTag(HPath tagPath) {
            allowedTags.add(tagPath);
            return this;
        }

        public DecisionTreeEngineConfig build() {
            return new DecisionTreeEngineConfig(allowedTags);
        }
    }
}
