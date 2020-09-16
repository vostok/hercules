package ru.kontur.vostok.hercules.util.text;

/**
 * @author Gregory Koshelev
 */
public final class CharUtil {
    public static boolean isAlphaNumeric(char c) {
        return isLatin(c) || isDigit(c);
    }

    public static boolean isLatin(char c) {
        return isUpperCaseLatin(c) || isLowerCaseLatin(c);
    }

    public static boolean isUpperCaseLatin(char c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowerCaseLatin(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isUnderscore(char c) {
        return c == '_';
    }

    public static boolean isDot(char c) {
        return c == '.';
    }

    public static boolean isMinusSign(char c) {
        return c == '-';
    }

    public static boolean isPlusSign(char c) {
        return c == '+';
    }

    private CharUtil() {
        /* static class */
    }
}
