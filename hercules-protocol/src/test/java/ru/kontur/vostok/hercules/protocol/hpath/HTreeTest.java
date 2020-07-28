package ru.kontur.vostok.hercules.protocol.hpath;

import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.TinyString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gregory Koshelev
 */
public class HTreeTest {
    @Test
    public void testNavigator() {
        HTree<Integer> tree = new HTree<>();
        HTree<Integer>.Navigator navigator = tree.navigator();

        assertNull(navigator.getValue());

        assertFalse(navigator.navigateToChild(TinyString.of("two")));
        assertNull(navigator.getValue());

        assertNull(tree.put(HPath.fromPath("one"), 1));
        assertTrue(navigator.navigateToChild(TinyString.of("one")));
        assertEquals(1, (int) navigator.getValue());

        assertNull(tree.put(HPath.fromPath("one/two"), 2));
        assertTrue(navigator.navigateToChild(TinyString.of("two")));
        assertEquals(2, (int) navigator.getValue());
    }
}
