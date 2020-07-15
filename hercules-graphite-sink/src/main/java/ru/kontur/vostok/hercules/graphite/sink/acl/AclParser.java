package ru.kontur.vostok.hercules.graphite.sink.acl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AclParser is used for parse ACL file.<br>
 * After parsing each line in ACL file will be converted to {@link AccessControlEntry}.
 *
 * @author Vladimir Tsypaev
 */
public final class AclParser {
    public static List<AccessControlEntry> parse(InputStream in) {
        List<AccessControlEntry> acl = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String ace;
            while ((ace = reader.readLine()) != null) {
                acl.add(new AccessControlEntry(ace));
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
        return acl;
    }

    private AclParser() {
        /* static class */
    }
}
