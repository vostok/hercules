package ru.kontur.vostok.hercules.tags;

import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.TagDescriptionBuilder;

import java.util.Optional;

/**
 * StackFrameTags
 *
 * @author Kirill Sulim
 */
public final class StackFrameTags {

    /**
     * Function name
     */
    public static final TagDescription<Optional<String>> FUNCTION_TAG = TagDescriptionBuilder.string("function")
        .optional()
        .build();

    /**
     * Type where function is declared
     */
    public static final TagDescription<Optional<String>> TYPE_TAG = TagDescriptionBuilder.string("type")
        .optional()
        .build();

    /**
     * File name
     */
    public static final TagDescription<Optional<String>> FILE_TAG = TagDescriptionBuilder.string("file")
        .optional()
        .build();

    /**
     * Line number
     */
    public static final TagDescription<Optional<Integer>> LINE_NUMBER_TAG = TagDescriptionBuilder.integer("line")
        .optional()
        .build();

    /**
     * Column number
     */
    public static final TagDescription<Optional<Integer>> COLUMN_NUMBER_TAG = TagDescriptionBuilder.integer("column")
        .optional()
        .build();

    private StackFrameTags() {
        /* static class */
    }
}
