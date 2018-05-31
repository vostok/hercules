package ru.kontur.vostok.hercules.protocol.decoder;

/**
 * @author Gregory Koshelev
 */
@FunctionalInterface
interface TypeSkipper {
    int skip(Decoder decoder);
}
