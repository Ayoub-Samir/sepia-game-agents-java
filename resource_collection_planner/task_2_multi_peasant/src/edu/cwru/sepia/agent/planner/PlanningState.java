package edu.cwru.sepia.agent.planner;

import java.util.Arrays;

/**
 * Simplified planning state that assumes all peasants begin and end each action at the town hall.
 */
public final class PlanningState {
    public static final int HARVEST_AMOUNT = 100;
    public static final int MAX_WORKERS = 3;
    private static final int MAX_SUPPLY = 3;
    private static final int BUILD_COST = 400;

    private final int totalPeasants;
    private final int townHallGold;
    private final int townHallWood;
    private final int[] resourceAmounts;
    private final int hashCode;

    public PlanningState(int totalPeasants,
                         int townHallGold,
                         int townHallWood,
                         int[] resourceAmounts) {
        this.totalPeasants = totalPeasants;
        this.townHallGold = townHallGold;
        this.townHallWood = townHallWood;
        this.resourceAmounts = resourceAmounts.clone();
        this.hashCode = computeHashCode();
    }

    public int getTotalPeasants() {
        return totalPeasants;
    }

    public int getTownHallGold() {
        return townHallGold;
    }

    public int getTownHallWood() {
        return townHallWood;
    }

    public int getResourceAmount(Location location) {
        if (!location.isResourceSite()) {
            return 0;
        }
        return resourceAmounts[location.ordinal()];
    }

    public boolean goalSatisfied(Goal goal) {
        return townHallGold >= goal.getRequiredGold()
                && townHallWood >= goal.getRequiredWood();
    }

    public boolean canBuildPeasant() {
        return totalPeasants < MAX_SUPPLY && townHallGold >= BUILD_COST;
    }

    public PlanningState buildPeasant() {
        return new PlanningState(totalPeasants + 1, townHallGold - BUILD_COST, townHallWood, resourceAmounts);
    }

    public PlanningState collect(Location resourceLocation, int count) {
        int[] updatedResources = resourceAmounts.clone();
        int index = resourceLocation.ordinal();
        updatedResources[index] -= HARVEST_AMOUNT * count;
        int newGold = townHallGold;
        int newWood = townHallWood;
        if (resourceLocation.getResource() == Location.Resource.GOLD) {
            newGold += HARVEST_AMOUNT * count;
        } else {
            newWood += HARVEST_AMOUNT * count;
        }
        return new PlanningState(totalPeasants, newGold, newWood, updatedResources);
    }

    public int[] getResourceSnapshot() {
        return resourceAmounts.clone();
    }

    private int computeHashCode() {
        int result = totalPeasants;
        result = 31 * result + townHallGold;
        result = 31 * result + townHallWood;
        result = 31 * result + Arrays.hashCode(resourceAmounts);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlanningState)) {
            return false;
        }
        PlanningState other = (PlanningState) obj;
        return totalPeasants == other.totalPeasants
                && townHallGold == other.townHallGold
                && townHallWood == other.townHallWood
                && Arrays.equals(resourceAmounts, other.resourceAmounts);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "PlanningState{" +
                "peasants=" + totalPeasants +
                ", gold=" + townHallGold +
                ", wood=" + townHallWood +
                '}';
    }
}
