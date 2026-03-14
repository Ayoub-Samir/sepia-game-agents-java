package edu.cwru.sepia.agent.planner;

/**
 * Harvests 100 resources from the designated node, provided there is capacity.
 */
public final class HarvestAction implements GroundedAction {
    private static final int COST = 1;

    private final Location resourceLocation;

    public HarvestAction(Location resourceLocation) {
        if (!resourceLocation.isResourceSite()) {
            throw new IllegalArgumentException("Harvest target must be a resource location.");
        }
        this.resourceLocation = resourceLocation;
    }

    @Override
    public boolean isApplicable(PlanningState state) {
        return state.getLocation() == resourceLocation
                && state.getCargoType() == CargoType.NONE
                && state.getResourceAmount(resourceLocation) >= PlanningState.HARVEST_AMOUNT;
    }

    @Override
    public PlanningState apply(PlanningState state) {
        return state.harvest(resourceLocation);
    }

    @Override
    public int getCost(PlanningState state) {
        return COST;
    }

    @Override
    public PlanStep toPlanStep(PlanningState priorState) {
        PlanStep.StepType stepType =
                resourceLocation.getResource() == Location.Resource.GOLD
                        ? PlanStep.StepType.HARVEST_GOLD
                        : PlanStep.StepType.HARVEST_WOOD;
        return new PlanStep(stepType, resourceLocation, COST);
    }

    @Override
    public String toString() {
        return "Harvest " + resourceLocation;
    }
}
