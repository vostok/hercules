package ru.kontur.vostok.hercules.kafka.util.consumer;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class SubscriptionTest {
    @Test
    public void singleExactMatching() {
        Subscription subscription = Subscription.builder().include(new String[]{"exact_matching"}).build();
        Pattern pattern = subscription.toPattern();
        assertEquals("^exact_matching$", pattern.pattern());
    }

    @Test
    public void singlePatternMatching() {
        Subscription subscription = Subscription.builder().include(new String[]{"pattern_*"}).build();
        Pattern pattern = subscription.toPattern();
        assertEquals("^pattern_[a-z0-9_]*$", pattern.pattern());
    }

    @Test
    public void multipleMatching() {
        Subscription subscription = Subscription.builder().include(new String[]{"exact_matching", "pattern_*"}).build();
        Pattern pattern = subscription.toPattern();
        assertEquals("^exact_matching$|^pattern_[a-z0-9_]*$", pattern.pattern());
    }

    @Test
    public void matchingWithExclusion() {
        Subscription subscription =
                Subscription.builder().
                        include(new String[]{"pattern_*", "a_*_c"}).
                        exclude(new String[]{"pattern_exclusion_*", "a_b_c"}).
                        build();
        Pattern pattern = subscription.toPattern();
        assertEquals("(?!(^pattern_exclusion_[a-z0-9_]*$|^a_b_c$))(^pattern_[a-z0-9_]*$|^a_[a-z0-9_]*_c$)", pattern.pattern());
        assertTrue(pattern.matcher("pattern_include").matches());
        assertFalse(pattern.matcher("pattern_exclusion_1").matches());
        assertTrue(pattern.matcher("a__c").matches());
        assertFalse(pattern.matcher("a_b_c").matches());
    }
}
