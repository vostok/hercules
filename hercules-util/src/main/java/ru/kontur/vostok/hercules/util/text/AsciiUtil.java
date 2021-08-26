package ru.kontur.vostok.hercules.util.text;

/**
 * @author Gregory Koshelev
 */
public class AsciiUtil {
    public static final byte ASCII_SPACE = ' ';
    public static final byte ASCII_UNDERSCORE = '_';
    public static final byte ASCII_DOT = '.';
    public static final byte ASCII_SEMICOLON = ';';
    public static final byte ASCII_EQUAL_SIGN = '=';

    public static boolean isAlphaNumeric(byte c) {
        return isLatin(c) || isDigit(c);
    }

    public static boolean isLatin(byte c) {
        return isUpperCaseLatin(c) || isLowerCaseLatin(c);
    }

    public static boolean isUpperCaseLatin(byte c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLowerCaseLatin(byte c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isDigit(byte c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isUnderscore(byte c) {
        return c == ASCII_UNDERSCORE;
    }

    public static boolean isDot(byte c) {
        return c == ASCII_DOT;
    }
}
