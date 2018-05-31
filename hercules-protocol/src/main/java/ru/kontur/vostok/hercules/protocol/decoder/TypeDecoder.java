package ru.kontur.vostok.hercules.protocol.decoder;

import java.util.function.Function;

/**
 * @author Gregory Koshelev
 */
@FunctionalInterface
interface TypeDecoder {
    Object decode(Decoder decoder);
}
