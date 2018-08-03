package ru.kontur.vostok.hercules.meta;

/**
 * @author Gregory Koshelev
 */
public class ZnodeValidator {
    /**
     * See http://zookeeper.apache.org/doc/r3.4.13/zookeeperProgrammers.html#ch_zkDataModel
     * @param value is possible name for znode
     * @return true if value is allowed by ZK Data Model, false otherwise
     */
    public static boolean validate(String value) {
        return value.chars().noneMatch(ch -> (ch >= 0x00 && ch <= 0x19) || (ch >= 0x7F && ch <= 0x9F) || (ch >= 0xD800 & ch <= 0xF8FF) || (ch >= 0xFFF0 && ch <= 0xFFFF));
    }
}
