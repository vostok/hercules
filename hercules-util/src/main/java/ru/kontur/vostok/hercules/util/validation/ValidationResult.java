package ru.kontur.vostok.hercules.util.validation;

import org.jetbrains.annotations.NotNull;

/**
 * Validation result
 *
 * @author Gregory Koshelev
 */
public final class ValidationResult {
    private static final ValidationResult OK = new ValidationResult();
    private static final ValidationResult MISSED = new ValidationResult("Value is missing");

    private final String errorReason;

    private ValidationResult() {
        this.errorReason = null;
    }

    private ValidationResult(String reason) {
        this.errorReason = reason;
    }

    public boolean isOk() {
        return errorReason == null;
    }

    public boolean isError() {
        return errorReason != null;
    }

    public String error() {
        if (isError()) {
            return errorReason;
        }
        throw new IllegalStateException("No error reason in successful validation result");
    }

    /**
     * Successful validation result
     *
     * @return successful validation result
     */
    public static ValidationResult ok() {
        return OK;
    }

    /**
     * Unsuccessful validation result with error reason
     *
     * @param reason the error reason
     * @return unsuccessful validation result
     */
    public static ValidationResult error(@NotNull String reason) {
        return new ValidationResult(reason);
    }

    /**
     * Unsuccessful validation result of missed value.
     *
     * @return unsuccessful validation result
     */
    public static ValidationResult missed() {
        return MISSED;
    }
}
