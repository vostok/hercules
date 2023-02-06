package ru.kontur.vostok.hercules.radamant.configurator;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import ru.kontur.vostok.hercules.configuration.Sources;
import ru.kontur.vostok.hercules.radamant.rules.RuleFlat;

/**
 * Read rules for flat metrics from file.
 * <p>
 * Transfer rules to {@link RuleConfiguratorWatcher} object that should pass into {@link #init}
 * method.
 *
 * @author Tatyana Tokmyanina
 */
public class RuleFlatConfigurator implements RuleConfigurator {
    private final String fileName;
    private final ObjectReader rulesReader;

    /**
     * Create RuleFlatConfigurator
     *
     * @param fileName File with rules.
     */
    public RuleFlatConfigurator(String fileName) {
        this.fileName = fileName;
        rulesReader = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readerFor(RuleFlat[].class);
    }

    /**
     * Read rules and reports about changes to
     * {@link RuleConfiguratorWatcher}
     *
     * @param watcher Observer, that will receive rules.
     */
    @Override
    public void init(RuleConfiguratorWatcher watcher) {
        RuleFlat[] rules = readRules(fileName);
        watcher.updateRules(rules);
    }

    private RuleFlat[] readRules(String fileName) {
        Set<String> ruleNames = new HashSet<>();
        try (InputStream in = Sources.load(fileName)) {
            RuleFlat[] result = rulesReader.readValue(in);
            for (RuleFlat rule: result) {
                if (ruleNames.contains(rule.getName())) {
                    throw new IllegalArgumentException("Rules with same names: " + rule.getName());
                }
                else {
                    ruleNames.add(rule.getName());
                }
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
