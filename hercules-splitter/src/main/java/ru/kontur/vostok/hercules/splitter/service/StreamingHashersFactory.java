package ru.kontur.vostok.hercules.splitter.service;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Factory of {@link StreamingHasher} implementations.
 * <p>
 * Available algorithms:
 * <ul>
 * <li>xxhash32_fastest_instance</li>
 * <li>xxhash32_native_instance</li>
 * <li>xxhash32_safe_instance</li>
 * <li>xxhash32_unsafe_instance</li>
 * <li>xxhash32_fastest_java_instance</li>
 * <li>adler32 (Guava)</li>
 * <li>murmur3_32 (Guava)</li>
 * <li>murmur3_128 (Guava)</li>
 * <li>crc32 (Guava)</li>
 * <li>crc32c (Guava)</li>
 * <li>sha256 (Guava)</li>
 * <li>sha384 (Guava)</li>
 * <li>sha512 (Guava)</li>
 * <li>sip24 (Guava)</li>
 * <li>md5 (Guava)</li>
 * <li>sha1 (Guava)</li>
 * </ul>
 *
 * @author Aleksandr Yuferov
 */
@SuppressWarnings("UnstableApiUsage")
public class StreamingHashersFactory {

    @SuppressWarnings("deprecation")
    private static final Map<String, Supplier<StreamingHasher>> FACTORIES = Map.ofEntries(
            Map.entry("xxhash32_fastest_instance", wrapXxHashFactory(XXHashFactory.fastestInstance()::newStreamingHash32)),
            Map.entry("xxhash32_native_instance", wrapXxHashFactory(XXHashFactory.nativeInstance()::newStreamingHash32)),
            Map.entry("xxhash32_safe_instance", wrapXxHashFactory(XXHashFactory.safeInstance()::newStreamingHash32)),
            Map.entry("xxhash32_unsafe_instance", wrapXxHashFactory(XXHashFactory.unsafeInstance()::newStreamingHash32)),
            Map.entry("xxhash32_fastest_java_instance", wrapXxHashFactory(XXHashFactory.fastestJavaInstance()::newStreamingHash32)),
            Map.entry("adler32", wrapGuavaFactory(Hashing.adler32()::newHasher)),
            Map.entry("murmur3_32", wrapGuavaFactory(Hashing.murmur3_32()::newHasher)),
            Map.entry("murmur3_128", wrapGuavaFactory(Hashing.murmur3_128()::newHasher)),
            Map.entry("crc32", wrapGuavaFactory(Hashing.crc32()::newHasher)),
            Map.entry("crc32c", wrapGuavaFactory(Hashing.crc32c()::newHasher)),
            Map.entry("sha256", wrapGuavaFactory(Hashing.sha256()::newHasher)),
            Map.entry("sha384", wrapGuavaFactory(Hashing.sha384()::newHasher)),
            Map.entry("sha512", wrapGuavaFactory(Hashing.sha512()::newHasher)),
            Map.entry("sip24", wrapGuavaFactory(Hashing.sipHash24()::newHasher)),
            Map.entry("md5", wrapGuavaFactory(Hashing.md5()::newHasher)),
            Map.entry("sha1", wrapGuavaFactory(Hashing.sha1()::newHasher))
    );


    /**
     * Get the factory of specified algorithm.
     *
     * @param algorithmName Name of the algorithm. Names are listed in class description.
     * @return Factory.
     */
    public static Supplier<StreamingHasher> factoryForAlgorithm(String algorithmName) {
        return FACTORIES.get(algorithmName.trim().toLowerCase());
    }

    private static Supplier<StreamingHasher> wrapXxHashFactory(IntFunction<StreamingXXHash32> factory) {
        return () -> new StreamingHasher() {
            private final StreamingXXHash32 hasher = factory.apply(0);

            @Override
            public void update(byte[] buffer, int offset, int length) {
                hasher.update(buffer, offset, length);
            }

            @Override
            public int hash() {
                return hasher.getValue();
            }

            @Override
            public void reset() {
                hasher.reset();
            }
        };
    }

    private static Supplier<StreamingHasher> wrapGuavaFactory(Supplier<Hasher> factory) {
        return () -> new StreamingHasher() {
            private Hasher hasher = factory.get();

            @Override
            public void update(byte[] buffer, int offset, int length) {
                hasher.putBytes(buffer, offset, length);
            }

            @Override
            public int hash() {
                return hasher.hash().asInt();
            }

            @Override
            public void reset() {
                hasher = factory.get();
            }
        };
    }
}
