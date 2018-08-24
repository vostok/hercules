package ru.kontur.vostok.hercules.util.arguments;

/**
 * StringCapture
 *
 * @author Kirill Sulim
 */
public class StringCapture extends ComparableArgumentCapture<String, String> {

    public StringCapture(String argument) {
        super(argument);
    }

    public void isNotEmpty() {
        isNotNull();
        if (argument.isEmpty()) {
            throw new IllegalArgumentException("String is empty");
        }
    }
}
