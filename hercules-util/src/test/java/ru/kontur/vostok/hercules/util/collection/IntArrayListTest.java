package ru.kontur.vostok.hercules.util.collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Aleksandr Yuferov
 */
public class IntArrayListTest {
    @Test
    public void defaultStateTest() {
        IntArrayList list = new IntArrayList();

        Assert.assertEquals(0, list.size());
        Assert.assertEquals(0, list.capacity());
    }

    @Test
    public void shouldSetGivenCapacity() {
        IntArrayList list = new IntArrayList(5);

        Assert.assertEquals(0, list.size());
        Assert.assertEquals(5, list.capacity());
    }

    @Test
    public void shouldBeDynamic() {
        IntArrayList list = new IntArrayList();

        list.add(1);

        Assert.assertEquals(1, list.size());
        Assert.assertEquals(4, list.capacity());
    }

    @Test
    public void cleanShouldSetSizeToZero() {
        IntArrayList list = new IntArrayList();
        list.add(1);

        list.clean();

        Assert.assertEquals(0, list.size());
        Assert.assertEquals(4, list.capacity());
    }

    @Test
    public void getShouldReturnElementByIndex() {
        IntArrayList list = new IntArrayList();
        list.add(123);

        int element = list.get(0);

        Assert.assertEquals(123, element);
    }

    @Test
    public void setShouldChangeElementByIndex() {
        IntArrayList list = new IntArrayList();
        list.add(123);

        list.set(0, 321);

        Assert.assertEquals(321, list.get(0));
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(4, list.capacity());
    }
}
