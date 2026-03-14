package edu.cwru.sepia.agent.planner;

/**
 * Abstraction of a grounded STRIPS-like action used by the A* planner.
 */
public interface GroundedAction {

    boolean isApplicable(PlanningState state);

    PlanningState apply(PlanningState state);

    int getCost(PlanningState state);

    java.util.List<PlanStep> toPlanSteps(PlanningState priorState);
}
