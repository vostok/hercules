package ru.kontur.vostok.hercules.util.arguments;

/**
 * ArgumentChecker set of static function to begin checking of argument
 *
 * @author Kirill Sulim
 */
public class ArgumentChecker {

    public static <C, T extends Comparable<C>> ComparableArgumentCapture<C, T> check(T arg) {
        return new ComparableArgumentCapture<>(arg);
    }

    public static <T> ArgumentCapture<T> check(T arg) {
        return new ArgumentCapture<T>(arg);
    }

    public static StringCapture check(String s) {
        return new StringCapture(s);
    }
}
