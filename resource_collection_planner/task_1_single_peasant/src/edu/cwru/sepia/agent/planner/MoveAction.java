package edu.cwru.sepia.agent.planner;

/**
 * Move between high-level locations. Costs are based on Manhattan distance between sites.
 */
public final class MoveAction implements GroundedAction {
    private final Location destination;

    public MoveAction(Location destination) {
        this.destination = destination;
    }

    @Override
    public boolean isApplicable(PlanningState state) {
        if (state.getLocation() == destination) {
            return false;
        }
        if (state.hasCargo() && destination != Location.TOWN_HALL) {
            return false;
        }
        if (destination == Location.TOWN_HALL) {
            return true;
        }
        return state.getResourceAmount(destination) >= PlanningState.HARVEST_AMOUNT;
    }

    @Override
    public PlanningState apply(PlanningState state) {
        return state.moveTo(destination);
    }

    @Override
    public int getCost(PlanningState state) {
        return state.getLocation().distanceTo(destination);
    }

    @Override
    public PlanStep toPlanStep(PlanningState priorState) {
        return new PlanStep(PlanStep.StepType.MOVE, destination, getCost(priorState));
    }

    @Override
    public String toString() {
        return "Move -> " + destination;
    }
}
