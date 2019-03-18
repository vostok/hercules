package ru.kontur.vostok.hercules.util.properties;

/**
 * PropertyException - marks property loading errors
 *
 * @author Kirill Sulim
 */
public class PropertyException extends RuntimeException {

    /**
     * @param message error message
     */
    public PropertyException(String message) {
        super(message);
    }
}
