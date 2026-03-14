package edu.cwru.sepia.agent.planner;

/**
 * Deposits cargo at the town hall.
 */
public final class DepositAction implements GroundedAction {
    private static final int COST = 1;

    @Override
    public boolean isApplicable(PlanningState state) {
        if (state.getLocation() != Location.TOWN_HALL) {
            return false;
        }
        return state.getCargoType() == CargoType.GOLD || state.getCargoType() == CargoType.WOOD;
    }

    @Override
    public PlanningState apply(PlanningState state) {
        return state.depositCargo();
    }

    @Override
    public int getCost(PlanningState state) {
        return COST;
    }

    @Override
    public PlanStep toPlanStep(PlanningState priorState) {
        PlanStep.StepType stepType;
        if (priorState.getCargoType() == CargoType.GOLD) {
            stepType = PlanStep.StepType.DEPOSIT_GOLD;
        } else if (priorState.getCargoType() == CargoType.WOOD) {
            stepType = PlanStep.StepType.DEPOSIT_WOOD;
        } else {
            throw new IllegalStateException("Deposit action requires cargo.");
        }
        return new PlanStep(stepType, Location.TOWN_HALL, COST);
    }

    @Override
    public String toString() {
        return "Deposit";
    }
}
