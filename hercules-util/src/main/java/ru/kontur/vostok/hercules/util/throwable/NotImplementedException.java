package ru.kontur.vostok.hercules.util.throwable;

/**
 * NotImplementedException - exception to mark not implemented logic. Usual cases: in switch-default block to avoid
 * loss of implementation, in interface implementation drafts etc.
 *
 * @author Kirill Sulim
 */
public class NotImplementedException extends RuntimeException {

    public NotImplementedException() {
    }

    public NotImplementedException(String message) {
        super(message);
    }

    public <T extends Enum> NotImplementedException(T value) {
        super(String.format(
                "Not implemented for '%s' value of '%s' enum",
                value.name(),
                value.getClass().getCanonicalName()
        ));
    }
}
