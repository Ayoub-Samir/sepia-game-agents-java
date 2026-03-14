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

        if (state.getCargoType() == CargoType.GOLD) {
            remainingGold = Math.max(0, remainingGold - state.getCargoAmount());
        } else if (state.getCargoType() == CargoType.WOOD) {
            remainingWood = Math.max(0, remainingWood - state.getCargoAmount());
        }

        int goldLoads = (int) Math.ceil(remainingGold / (double) PlanningState.HARVEST_AMOUNT);
        int woodLoads = (int) Math.ceil(remainingWood / (double) PlanningState.HARVEST_AMOUNT);

        int estimate = goldLoads * minGoldTripCost + woodLoads * minWoodTripCost;

        if (state.hasCargo()) {
            // At minimum the peasant must reach the town hall and deposit once.
            estimate += state.getLocation().distanceTo(Location.TOWN_HALL) + DEPOSIT_COST;
        }

        return estimate;
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
