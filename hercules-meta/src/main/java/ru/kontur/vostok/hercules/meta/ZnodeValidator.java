package ru.kontur.vostok.hercules.meta;

/**
 * @author Gregory Koshelev
 */
public class ZnodeValidator {
    /**
     * See http://zookeeper.apache.org/doc/r3.4.13/zookeeperProgrammers.html#ch_zkDataModel
     *
     * @param value is possible name for znode
     * @return true if value is allowed by ZK Data Model, false otherwise
     */
    public static boolean validate(String value) {
        return value.chars().noneMatch(
                ch -> /**/ (0x0000 <= ch && ch <= 0x0019)
                        || (0x007F <= ch && ch <= 0x009F)
                        || (0xD800 <= ch && ch <= 0xF8FF)
                        || (0xFFF0 <= ch && ch <= 0xFFFF)
                        || ch == '/');
    }
}
