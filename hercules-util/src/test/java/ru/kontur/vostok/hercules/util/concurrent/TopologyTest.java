package ru.kontur.vostok.hercules.util.concurrent;

import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.util.collection.CollectionUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Gregory Koshelev
 */
public class TopologyTest {
    @Test(expected = Topology.TopologyIsEmptyException.class)
    public void shouldThrowExceptionOnEmptyTopology() {
        Topology<String> topology = new Topology<>(new String[0]);

        Assert.assertTrue(topology.isEmpty());
        topology.next();
    }

    @Test
    public void shouldIterateOverSingleElementTopology() {
        Topology<String> topology = new Topology<>(new String[]{"single-value"});

        Assert.assertFalse(topology.isEmpty());
        Assert.assertEquals("single-value", topology.next());
        Assert.assertEquals("single-value", topology.next());
        Assert.assertEquals("single-value", topology.next());
        Assert.assertEquals("single-value", topology.next());
    }

    @Test
    public void shouldGetEmptyTopologyWhenRemoveLastElement() {
        Topology<String> topology = new Topology<>(new String[]{"single-value"});

        Assert.assertEquals("single-value", topology.next());

        Assert.assertTrue(topology.remove("single-value"));
        Assert.assertTrue(topology.isEmpty());
    }

    @Test
    public void shouldGetEmptyTopologyAfterRemovingAllElements() {
        Topology<String> topology = new Topology<>(new String[]{"first", "second"});

        Assert.assertTrue(topology.remove("first"));
        Assert.assertTrue(topology.remove("second"));
        Assert.assertTrue(topology.isEmpty());
    }

    @Test
    public void shouldRemoveExactlyOnceOnMultipleRemoving() {
        Topology<String> topology = new Topology<>(new String[]{"first", "second"});

        Assert.assertTrue(topology.remove("second"));
        Assert.assertFalse(topology.remove("second"));
        Assert.assertFalse(topology.isEmpty());
        Assert.assertEquals("first", topology.next());
    }

    @Test
    public void shouldProcessNormallyMutualRemovingAndAddition() {
        Topology<String> topology = new Topology<>(new String[]{"first", "second"});

        Assert.assertTrue(topology.remove("first"));
        topology.add("first");
        Assert.assertTrue(topology.remove("second"));
        Assert.assertEquals("first", topology.next());
    }

    @Test
    public void shouldBreakIterationWhenTopologyIsEmpty() {
        String[] elements = new String[]{"first", "second", "third", "fourth", "fifth"};
        Topology<String> topology = new Topology<>(elements);

        Set<String> actualElements = new HashSet<>();
        try {
            for (String element : topology) {
                actualElements.add(element);
                Assert.assertTrue(topology.remove(element));
            }
        } catch (Topology.TopologyIsEmptyException ex) {
            /* it is not possible in single-threaded environment */
            Assert.fail();
        }

        Assert.assertEquals(CollectionUtil.setOf(elements), actualElements);
    }

    @Test
    public void shouldIterateWithForEach() {
        String[] elements = new String[]{"first", "second", "third", "fourth", "fifth"};
        Topology<String> topology = new Topology<>(elements);

        int i = 10;
        Set<String> actualElements = new HashSet<>();
        Iterator<String> it = topology.iterator();
        while (i-- > 0 && it.hasNext()) {
            actualElements.add(it.next());
        }
        Assert.assertEquals(CollectionUtil.setOf(elements), actualElements);
    }
}
