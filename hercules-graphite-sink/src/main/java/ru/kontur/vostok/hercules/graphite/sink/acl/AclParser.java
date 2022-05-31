package ru.kontur.vostok.hercules.graphite.sink.acl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AclParser is used for parse ACL file.<br>
 * After parsing each line in ACL file will be converted to {@link AccessControlEntry}.
 *
 * @author Vladimir Tsypaev
 */
public final class AclParser {
    private static final Pattern BLANK_OR_COMMENT_LINE_PATTERN = Pattern.compile("^\\s*$|^\\s*#.*");

    public static List<AccessControlEntry> parse(BufferedReader reader) throws IOException {
        List<AccessControlEntry> acl = new ArrayList<>();
        String ace;
        while ((ace = reader.readLine()) != null) {
            if (BLANK_OR_COMMENT_LINE_PATTERN.matcher(ace).matches()) {
                continue;
            }
            acl.add(AccessControlEntry.fromString(ace));
        }
        return acl;
    }

    private AclParser() {
        /* static class */
    }
}
