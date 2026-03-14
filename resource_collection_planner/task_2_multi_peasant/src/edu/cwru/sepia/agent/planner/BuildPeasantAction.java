package edu.cwru.sepia.agent.planner;

import java.util.Collections;
import java.util.List;

/**
 * Builds an additional peasant at the town hall.
 */
public final class BuildPeasantAction implements GroundedAction {
    private static final int COST = 1;

    @Override
    public boolean isApplicable(PlanningState state) {
        return state.canBuildPeasant();
    }

    @Override
    public PlanningState apply(PlanningState state) {
        return state.buildPeasant();
    }

    @Override
    public int getCost(PlanningState state) {
        return COST;
    }

    @Override
    public List<PlanStep> toPlanSteps(PlanningState priorState) {
        PlanStep step = new PlanStep(
                PlanStep.StepType.BUILD_PEASANT,
                Location.TOWN_HALL,
                Location.TOWN_HALL,
                CargoType.NONE,
                1,
                COST);
        return Collections.singletonList(step);
    }
}
