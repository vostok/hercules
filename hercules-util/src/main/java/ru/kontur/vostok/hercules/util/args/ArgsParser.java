package ru.kontur.vostok.hercules.util.args;

import java.util.HashMap;
import java.util.Map;

/**
 * Util class provides methods to parse input arguments
 * <br>
 * @author Gregory Koshelev
 */
public class ArgsParser {
    /**
     * Parse arguments into pairs key=value
     * @param args is arguments to be parsed
     * @return map with parsed arguments
     */
    public static Map<String, String> parse(String[] args) {
        Map<String, String> parameters = new HashMap<>(args.length);
        for (String arg : args) {
            int indexOfEqual = arg.indexOf('=');
            if (indexOfEqual == -1 || indexOfEqual == 0 || indexOfEqual == arg.length() - 1) {
                continue;
            }
            parameters.put(arg.substring(0, indexOfEqual), arg.substring(indexOfEqual + 1));
        }
        return parameters;
    }

}
