package ru.kontur.vostok.hercules.protocol;

import java.util.function.Function;

/**
 * @author Gregory Koshelev
 */
interface TypeDecoder extends Function<Decoder, Object> {
}
