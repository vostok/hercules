package ru.kontur.vostok.hercules.meta.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.kontur.vostok.hercules.protocol.Variant;

/**
 * @author Gregory Koshelev
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Conditions.Negate.class, name = "negate"),
        @JsonSubTypes.Type(value = Conditions.Exist.class, name = "exist"),
        @JsonSubTypes.Type(value = Conditions.Range.class, name = "range"),
        @JsonSubTypes.Type(value = Conditions.NumericalEquality.class, name = "numerical_equality"),
        @JsonSubTypes.Type(value = Conditions.StringEquality.class, name = "string_equality"),
        @JsonSubTypes.Type(value = Conditions.StartsWith.class, name = "starts_with"),
})
public interface Condition {
    boolean test(Variant variant);

}
