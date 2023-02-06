package ru.kontur.vostok.hercules.radamant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.radamant.configurator.RuleFlatConfigurator;
import ru.kontur.vostok.hercules.radamant.rules.RuleFlat;
import ru.kontur.vostok.hercules.radamant.rules.RuleTableFlat;

/**
 * @author Tatyana Tokmyanina
 */
public class RuleFlatConfiguratorTest {
    private static RuleFlatConfigurator configurator;

    private RuleTableFlat ruleTable;

    @Before
    public void init() {
        MetricsCollector metricsCollector = Mockito.mock(MetricsCollector.class);
        ruleTable = new RuleTableFlat(metricsCollector);
    }

    @Test
    public void shouldReadRulesFromFile() {
        configurator = new RuleFlatConfigurator("resource://rules_flat.json");
        configurator.init(ruleTable);
        RuleFlat[] rules = ruleTable.getRules();
        assertNotNull(rules);
        assertEquals(1, rules.length);
        RuleFlat first = rules[0];
        assertEquals("description", first.getDescription());
        assertEquals("test_rule", first.getName());

        assertNotNull(first.getEnrichment());
        Map<TinyString, Variant> enrichment = first.getEnrichment();
        assertEquals(5, enrichment.size());
        TinyString[] keys = new TinyString[]{
                TinyString.of("name_pattern"),
                TinyString.of("period"),
                TinyString.of("expire after"),
                TinyString.of("timestamp"),
                TinyString.of("function")};
        String[] values = new String[]{"new.metric.name.{1}", "60", "90", "start of bucket", "sum"};
        for (int i = 0; i < 4; i++) {
            assertEquals(values[i], new String((byte[]) enrichment.get(keys[i]).getValue(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void shouldThrowsExceptionWhenRulesHasSameNames() {
        configurator = new RuleFlatConfigurator("resource://rules_flat_same_names.json");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> configurator.init(ruleTable));
        assertEquals("Rules with same names: test_rule", ex.getMessage());
    }
}
