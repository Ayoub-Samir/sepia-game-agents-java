package edu.cwru.sepia.agent.planner;

import java.util.Optional;

/**
 * Command-line entry point for generating plans for the resource collection scenario.
 */
public final class ResourceCollectionPlannerCli {

    private ResourceCollectionPlannerCli() {
    }

    public static void main(String[] args) {
        int goalGold = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        int goalWood = args.length > 1 ? Integer.parseInt(args[1]) : 200;

        Goal goal = new Goal(goalGold, goalWood);
        PlanningState initialState = InitialStateFactory.fromDefaults();

        ForwardPlanner planner = new ForwardPlanner();
        Optional<Plan> planResult = planner.plan(initialState, goal);

        if (!planResult.isPresent()) {
            System.err.println("Unable to find a plan for goal " + goalGold + " gold, " + goalWood + " wood.");
            System.exit(1);
            return;
        }

        Plan plan = planResult.get();
        System.out.println("=== Resource Plan ===");
        System.out.println("Goal: " + goalGold + " gold, " + goalWood + " wood");
        System.out.println("Plan cost (estimated makespan): " + plan.getTotalCost());
        System.out.println();

        int stepIndex = 1;
        for (PlanStep step : plan.getSteps()) {
            System.out.println(stepIndex++ + ". " + describe(step));
        }
    }

    private static String describe(PlanStep step) {
        switch (step.getType()) {
            case MOVE:
                return String.format("Move x%d %s from %s to %s", step.getUnitCount(), step.getCargoType(), step.getSource(), step.getTarget());
            case HARVEST_GOLD:
                return String.format("Harvest gold x%d at %s", step.getUnitCount(), step.getTarget());
            case HARVEST_WOOD:
                return String.format("Harvest wood x%d at %s", step.getUnitCount(), step.getTarget());
            case DEPOSIT_GOLD:
                return String.format("Deposit gold x%d at town hall", step.getUnitCount());
            case DEPOSIT_WOOD:
                return String.format("Deposit wood x%d at town hall", step.getUnitCount());
            case BUILD_PEASANT:
                return "Build peasant at town hall";
            default:
                return step.toString();
        }
    }
}
