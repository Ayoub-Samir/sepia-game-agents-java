package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a full gather-deposit cycle executed by a group of peasants.
 */
public final class CollectAction implements GroundedAction {
    private static final int HARVEST_COST = 1;
    private static final int DEPOSIT_COST = 1;

    private final Location resourceLocation;
    private final CargoType cargoType;
    private final int count;

    public CollectAction(Location resourceLocation, CargoType cargoType, int count) {
        if (!resourceLocation.isResourceSite()) {
            throw new IllegalArgumentException("Collect target must be a resource location.");
        }
        if (cargoType == CargoType.NONE) {
            throw new IllegalArgumentException("Collect action must specify a cargo type.");
        }
        this.resourceLocation = resourceLocation;
        this.cargoType = cargoType;
        this.count = count;
    }

    @Override
    public boolean isApplicable(PlanningState state) {
        if (count <= 0 || count > state.getTotalPeasants()) {
            return false;
        }
        int available = state.getResourceAmount(resourceLocation);
        return available >= PlanningState.HARVEST_AMOUNT * count;
    }

    @Override
    public PlanningState apply(PlanningState state) {
        return state.collect(resourceLocation, count);
    }

    @Override
    public int getCost(PlanningState state) {
        int travel = 2 * Location.TOWN_HALL.distanceTo(resourceLocation);
        return travel + HARVEST_COST + DEPOSIT_COST;
    }

    @Override
    public List<PlanStep> toPlanSteps(PlanningState priorState) {
        List<PlanStep> steps = new ArrayList<>(4);
        steps.add(new PlanStep(
                PlanStep.StepType.MOVE,
                Location.TOWN_HALL,
                resourceLocation,
                CargoType.NONE,
                count,
                Location.TOWN_HALL.distanceTo(resourceLocation)));

        PlanStep.StepType harvestType = cargoType == CargoType.GOLD
                ? PlanStep.StepType.HARVEST_GOLD
                : PlanStep.StepType.HARVEST_WOOD;
        steps.add(new PlanStep(
                harvestType,
                resourceLocation,
                resourceLocation,
                CargoType.NONE,
                count,
                HARVEST_COST));

        steps.add(new PlanStep(
                PlanStep.StepType.MOVE,
                resourceLocation,
                Location.TOWN_HALL,
                cargoType,
                count,
                resourceLocation.distanceTo(Location.TOWN_HALL)));

        PlanStep.StepType depositType = cargoType == CargoType.GOLD
                ? PlanStep.StepType.DEPOSIT_GOLD
                : PlanStep.StepType.DEPOSIT_WOOD;
        steps.add(new PlanStep(
                depositType,
                Location.TOWN_HALL,
                Location.TOWN_HALL,
                cargoType,
                count,
                DEPOSIT_COST));

        return steps;
    }

    public Location getResourceLocation() {
        return resourceLocation;
    }

    public CargoType getCargoType() {
        return cargoType;
    }

    public int getCount() {
        return count;
    }
}
