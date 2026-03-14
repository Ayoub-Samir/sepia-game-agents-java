package edu.cwru.sepia.agent.planner;

/**
 * Single grounded action in the produced plan.
 */
public final class PlanStep {

    public enum StepType {
        MOVE,
        HARVEST_GOLD,
        HARVEST_WOOD,
        DEPOSIT_GOLD,
        DEPOSIT_WOOD
    }

    private final StepType type;
    private final Location target;
    private final int cost;

    public PlanStep(StepType type, Location target, int cost) {
        this.type = type;
        this.target = target;
        this.cost = cost;
    }

    public StepType getType() {
        return type;
    }

    public Location getTarget() {
        return target;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        return type + " " + target + " (cost=" + cost + ")";
    }
}
