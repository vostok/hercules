package ru.kontur.vostok.hercules.sink.filter;

import org.junit.jupiter.api.Test;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Anton Akkuzin
 */
public class PatternTreeTest {

    @Test
    public void shouldNotInitializeWithNonPrimitiveTypes() {
        Exception exception = null;

        try {
            new PatternTree(List.of(Type.STRING, Type.VECTOR));
        } catch (IllegalArgumentException ex) {
            exception = ex;
        }

        assert exception != null;
        assertEquals("Type 'VECTOR' is not primitive.", exception.getMessage());
    }

    @Test
    public void shouldThrowWhenPatternSizeIsDifferentFromTypesSize() {
        PatternTree patternTree = new PatternTree(List.of(Type.STRING, Type.INTEGER, Type.FLOAT));

        Exception exception = null;
        try {
            patternTree.put("some_str:1234");
        } catch (IllegalArgumentException ex) {
            exception = ex;
        }

        assert exception != null;
        assertEquals("Pattern size should be equal to paths size.", exception.getMessage());
    }

    @Test
    public void shouldThrowWhenWrongType() {
        PatternTree patternTree = new PatternTree(List.of(Type.INTEGER));

        boolean thrown = false;
        try {
            patternTree.put("not_a_number");
        } catch (Exception ex) {
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void shouldMatchCorrectPatterns() {
        PatternTree patternTree = new PatternTree(List.of(Type.STRING, Type.INTEGER, Type.FLOAT));

        String[] patterns = {
                "a:99:105.9",
                "a:*:999.9",
                "b:99:*",
                "*:1234:*",
        };

        for (String pattern : patterns) {
            patternTree.put(pattern);
        }

        assertTrue(patternTree.matches(List.of(
                Variant.ofString("a"),
                Variant.ofInteger(99),
                Variant.ofFloat(105.9f)
        )));
        assertTrue(patternTree.matches(List.of(
                Variant.ofString("a"),
                Variant.ofInteger(99),
                Variant.ofFloat(999.9f)
        )));
        assertTrue(patternTree.matches(List.of(
                Variant.ofString("a"),
                Variant.ofInteger(99),
                Variant.ofFloat(105.9f)
        )));
        assertTrue(patternTree.matches(List.of(
                Variant.ofString("b"),
                Variant.ofInteger(99),
                Variant.ofFloat(3.14f)
        )));
        assertTrue(patternTree.matches(List.of(
                Variant.ofString("b"),
                Variant.ofInteger(99),
                Variant.ofFloat(1)
        )));
        assertTrue(patternTree.matches(List.of(
                Variant.ofString("c"),
                Variant.ofInteger(1234),
                Variant.ofFloat(999.9f)
        )));
        assertFalse(patternTree.matches(List.of(
                Variant.ofString("a"),
                Variant.ofInteger(99),
                Variant.ofFloat(666)
        )));
    }

    @Test
    public void shouldMatchStarWhenVariantIsMissing() {
        PatternTree patternTree = new PatternTree(List.of(Type.STRING, Type.INTEGER, Type.FLOAT));

        String[] patterns = {
                "b:99:*",
                "*:1234:*",
        };

        for (String pattern : patterns) {
            patternTree.put(pattern);
        }

        ArrayList<Variant> variants = new ArrayList<>();
        variants.add(null);
        variants.add(Variant.ofInteger(1234));
        variants.add(null);

        assertTrue(patternTree.matches(variants));
    }

    @Test
    public void shouldMatchToAllPrimitiveTypes() {
        assertTrue(matchesToPrimitiveType(Type.BYTE, "123", Variant.ofByte((byte) 123)));
        assertTrue(matchesToPrimitiveType(Type.SHORT, "123", Variant.ofShort((short) 123)));
        assertTrue(matchesToPrimitiveType(Type.INTEGER, "1234", Variant.ofInteger(1234)));
        assertTrue(matchesToPrimitiveType(Type.LONG, "123456789", Variant.ofLong(123456789)));
        assertTrue(matchesToPrimitiveType(Type.FLAG, "true", Variant.ofFlag(true)));
        assertTrue(matchesToPrimitiveType(Type.FLAG, "false", Variant.ofFlag(false)));
        assertTrue(matchesToPrimitiveType(Type.FLOAT, "3.14", Variant.ofFloat(3.14f)));
        assertTrue(matchesToPrimitiveType(Type.DOUBLE, "3.14", Variant.ofDouble(3.14)));
        assertTrue(matchesToPrimitiveType(Type.STRING, "some_str", Variant.ofString("some_str")));
        assertTrue(matchesToPrimitiveType(Type.NULL, "null", Variant.ofNull()));

        UUID uuid = UUID.randomUUID();
        assertTrue(matchesToPrimitiveType(Type.UUID, uuid.toString(), Variant.ofUuid(uuid)));
    }

    private boolean matchesToPrimitiveType(Type type, String pattern, Variant variant) {
        PatternTree patternTree = new PatternTree(List.of(type));
        patternTree.put(pattern);
        return patternTree.matches(List.of(variant));
    }
}
