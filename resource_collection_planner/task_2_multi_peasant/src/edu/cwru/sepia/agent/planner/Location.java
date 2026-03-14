package edu.cwru.sepia.agent.planner;

/**
 * Represents the high-level locations that the planner reasons about.
 * <p>
 * Each location is associated with map coordinates from the rc_3m5t map and,
 * for resource-bearing sites, their initial capacity and resource type.
 */
public enum Location {
    TOWN_HALL("TownHall", Kind.DEPOT, 8, 10, Resource.NONE, 0),
    GOLD_MINE_NEAR("GoldMineNear", Kind.GOLD_MINE, 4, 7, Resource.GOLD, 100),
    GOLD_MINE_MID("GoldMineMiddle", Kind.GOLD_MINE, 15, 3, Resource.GOLD, 500),
    GOLD_MINE_FAR("GoldMineFar", Kind.GOLD_MINE, 22, 16, Resource.GOLD, 5000),
    FOREST_WEST("ForestWest", Kind.FOREST, 4, 12, Resource.WOOD, 400),
    FOREST_SOUTH("ForestSouth", Kind.FOREST, 12, 13, Resource.WOOD, 400),
    FOREST_EAST("ForestEast", Kind.FOREST, 17, 9, Resource.WOOD, 400),
    FOREST_NW("ForestNorthWest", Kind.FOREST, 4, 4, Resource.WOOD, 400),
    FOREST_NE("ForestNorthEast", Kind.FOREST, 12, 4, Resource.WOOD, 400);

    public enum Kind {
        DEPOT,
        GOLD_MINE,
        FOREST
    }

    public enum Resource {
        NONE,
        GOLD,
        WOOD
    }

    private final String label;
    private final Kind kind;
    private final int x;
    private final int y;
    private final Resource resource;
    private final int initialCapacity;

    Location(String label, Kind kind, int x, int y, Resource resource, int initialCapacity) {
        this.label = label;
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.resource = resource;
        this.initialCapacity = initialCapacity;
    }

    public String getLabel() {
        return label;
    }

    public Kind getKind() {
        return kind;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isResourceSite() {
        return resource != Resource.NONE;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public int distanceTo(Location other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public String toString() {
        return label;
    }
}
