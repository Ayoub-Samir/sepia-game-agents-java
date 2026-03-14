package edu.cwru.sepia.agent.planner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Helper for translating between map coordinates and planner locations.
 */
public final class LocationLookup {
    private static final Map<String, Location> COORDINATE_TO_LOCATION = new HashMap<>();

    static {
        for (Location location : Location.values()) {
            String key = key(location.getX(), location.getY());
            COORDINATE_TO_LOCATION.put(key, location);
        }
    }

    private LocationLookup() {
    }

    public static Optional<Location> byCoordinates(int x, int y) {
        return Optional.ofNullable(COORDINATE_TO_LOCATION.get(key(x, y)));
    }

    private static String key(int x, int y) {
        return x + "," + y;
    }
}
