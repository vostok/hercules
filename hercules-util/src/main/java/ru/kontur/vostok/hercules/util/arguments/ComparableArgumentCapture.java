package ru.kontur.vostok.hercules.util.arguments;

/**
 * ComparableArgumentCapture
 *
 * @author Kirill Sulim
 */
public class ComparableArgumentCapture<C, T extends Comparable<C>> extends ArgumentCapture<T> {

    public ComparableArgumentCapture(T argument) {
        super(argument);
    }

    public void isGreaterThan(C exclusiveLowerBond) {
        if (argument.compareTo(exclusiveLowerBond) <= 0) {
            throw new IllegalArgumentException(String.format("%s is lesser or equals than %s", argument, exclusiveLowerBond));
        }
    }
}
