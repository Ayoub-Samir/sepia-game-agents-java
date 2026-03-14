package edu.cwru.sepia.agent.planner;

/**
 * Simple admissible heuristic that estimates the remaining makespan in number of turns.
 */
public final class PlannerHeuristic {
    private static final int HARVEST_COST = 1;
    private static final int DEPOSIT_COST = 1;

    private final int minGoldTripCost;
    private final int minWoodTripCost;

    public PlannerHeuristic() {
        this.minGoldTripCost = computeMinTripCost(Location.Resource.GOLD);
        this.minWoodTripCost = computeMinTripCost(Location.Resource.WOOD);
    }

    public int estimate(PlanningState state, Goal goal) {
        int remainingGold = Math.max(0, goal.getRequiredGold() - state.getTownHallGold());
        int remainingWood = Math.max(0, goal.getRequiredWood() - state.getTownHallWood());

        int goldLoads = (int) Math.ceil(remainingGold / (double) PlanningState.HARVEST_AMOUNT);
        int woodLoads = (int) Math.ceil(remainingWood / (double) PlanningState.HARVEST_AMOUNT);

        int workers = Math.max(1, state.getTotalPeasants());

        int goldDuration = goldLoads == 0 ? 0
                : (int) Math.ceil(goldLoads / (double) workers) * minGoldTripCost;
        int woodDuration = woodLoads == 0 ? 0
                : (int) Math.ceil(woodLoads / (double) workers) * minWoodTripCost;

        return Math.max(goldDuration, woodDuration);
    }

    private int computeMinTripCost(Location.Resource resourceType) {
        int min = Integer.MAX_VALUE;
        for (Location location : Location.values()) {
            if (!location.isResourceSite() || location.getResource() != resourceType) {
                continue;
            }
            int roundTrip = 2 * Location.TOWN_HALL.distanceTo(location) + HARVEST_COST + DEPOSIT_COST;
            if (roundTrip < min) {
                min = roundTrip;
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}
