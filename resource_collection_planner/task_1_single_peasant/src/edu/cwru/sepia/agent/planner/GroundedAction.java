package edu.cwru.sepia.agent.planner;

/**
 * Abstraction of a grounded STRIPS-like action used by the A* planner.
 */
public interface GroundedAction {

    boolean isApplicable(PlanningState state);

    PlanningState apply(PlanningState state);

    int getCost(PlanningState state);

    PlanStep toPlanStep(PlanningState priorState);
}
