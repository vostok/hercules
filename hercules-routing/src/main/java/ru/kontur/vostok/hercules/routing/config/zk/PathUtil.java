package ru.kontur.vostok.hercules.routing.config.zk;

import java.util.UUID;

/**
 * Utilities for ZooKeeper paths related with routing configurations.
 *
 * @author Aleksandr Yuferov
 */
class PathUtil {
    private static final String ROUTE_FILE_EXTENSION = ".json";

    static UUID extractRouteIdFromRelativePath(String relativePath) {
        return UUID.fromString(relativePath.substring(0, relativePath.length() - ROUTE_FILE_EXTENSION.length()));
    }

    static UUID extractRouteIdFromAbsolutePath(String rootPath, String absolutePath) {
        return UUID.fromString(
                absolutePath.substring(
                        rootPath.length() + 1,
                        absolutePath.length() - ROUTE_FILE_EXTENSION.length()
                )
        );
    }

    static String createAbsolutePathFromRouteId(String rootPath, UUID routeId) {
        return rootPath + '/' + routeId + ROUTE_FILE_EXTENSION;
    }

    private PathUtil() {
    }
}
