package ru.kontur.vostok.hercules.meta;

/**
 * @author Gregory Koshelev
 */
public class ZnodeValidator {
    /**
     * See http://zookeeper.apache.org/doc/r3.4.13/zookeeperProgrammers.html#ch_zkDataModel
     * <p>
     * ZooKeeper has a hierarchal name space, much like a distributed file system.
     * The only difference is that each node in the namespace can have data associated with it as well as children.
     * It is like having a file system that allows a file to also be a directory.
     * Paths to nodes are always expressed as canonical, absolute, slash-separated paths; there are no relative reference.
     * Any unicode character can be used in a path subject to the following constraints:                            <br>
     * - The null character (u0000) cannot be part of a path name. (This causes problems with the C binding.)       <br>
     * - The following characters can't be used because they don't display well, or render in confusing ways:
     *   u0001 - u0019 and u007F - u009F.<br>
     * - The following characters are not allowed: ud800 - uF8FF, uFFF0 - uFFFF.                                    <br>
     * <p>
     * Following restrictions are not checked:                                                                      <br>
     * - The "." character can be used as part of another name, but "." and ".." cannot alone be used to indicate a
     * node along a path, because ZooKeeper doesn't use relative paths.
     * The following would be invalid: "/a/b/./c" or "/a/b/../c".                                                   <br>
     * - The token "zookeeper" is reserved.
     * <p>
     * Also, check if value doesn't contains slash ('/') since slash is used to indicate node hierarchy
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
