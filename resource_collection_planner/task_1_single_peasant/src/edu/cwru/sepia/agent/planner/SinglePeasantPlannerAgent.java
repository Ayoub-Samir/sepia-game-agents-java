package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plan execution agent that runs the A* planner once at the beginning of a scenario and
 * then replays the plan in SEPIA. Counts how many environment steps were required.
 */
public final class SinglePeasantPlannerAgent extends Agent {
    private static final long serialVersionUID = 1L;

    private final Goal goal;
    private final ForwardPlanner planner;

    private int peasantId = -1;
    private int townHallId = -1;
    private Map<Location, Integer> resourceIdLookup = new HashMap<>();
    private Deque<PlanStep> pendingSteps = new ArrayDeque<>();
    private int issuedSteps = 0;
    private int executedEnvironmentSteps = 0;
    private boolean planFailure = false;
    private int planCompletionStep = -1;
    private int deliveredGold = 0;
    private int deliveredWood = 0;

    public SinglePeasantPlannerAgent(int playernum) {
        this(playernum, new String[0]);
    }

    public SinglePeasantPlannerAgent(int playernum, String[] otherArgs) {
        super(playernum);
        if (otherArgs != null && otherArgs.length >= 2) {
            this.goal = new Goal(Integer.parseInt(otherArgs[0]), Integer.parseInt(otherArgs[1]));
        } else {
            this.goal = new Goal(200, 200);
        }
        this.planner = new ForwardPlanner();

        System.out.println("SinglePeasantPlannerAgent initialised with target " + goal);
    }

    @Override
    public Map<Integer, Action> initialStep(StateView newState, HistoryView historyView) {
        bootstrapUnitIds(newState);
        executedEnvironmentSteps = 0;
        pendingSteps.clear();
        issuedSteps = 0;
        planFailure = false;
        planCompletionStep = -1;
        deliveredGold = 0;
        deliveredWood = 0;

        PlanningState planningState = InitialStateFactory.fromState(newState, getPlayerNumber());
        Optional<Plan> planResult = planner.plan(planningState, goal);
        if (!planResult.isPresent()) {
            System.err.println("Planning failed for goal " + goal + ". Terminating.");
            planFailure = true;
            return new HashMap<>();
        }

        Plan plan = planResult.get();
        pendingSteps.addAll(plan.getSteps());

        System.out.println("Plan ready (" + plan.getSteps().size() + " steps, estimated cost "
                + plan.getTotalCost() + ")");

        return issueNextAction(newState);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView newState, HistoryView historyView) {
        executedEnvironmentSteps++;
        if (planFailure) {
            return new HashMap<>();
        }
        if (pendingSteps.isEmpty()) {
            if (planCompletionStep < 0 && peasantIsIdle(newState)) {
                planCompletionStep = executedEnvironmentSteps;
            }
            return new HashMap<>();
        }
        if (!peasantIsIdle(newState)) {
            return new HashMap<>();
        }
        return issueNextAction(newState);
    }

    @Override
    public void terminalStep(StateView newState, HistoryView historyView) {
        System.out.println("Episode complete. Issued " + issuedSteps + " plan steps across "
                + executedEnvironmentSteps + " environment steps.");
        if (planCompletionStep >= 0) {
            System.out.println("Plan finished after " + planCompletionStep + " environment steps.");
        } else {
            System.out.println("Plan never reached completion before episode ended.");
        }
        if (!planFailure && !pendingSteps.isEmpty()) {
            System.err.println("Warning: plan did not finish. Remaining steps: " + pendingSteps.size());
        } else if (!planFailure) {
            System.out.println("Resources delivered via plan: gold=" + deliveredGold + ", wood=" + deliveredWood);
            System.out.println("Plan executed successfully.");
        }
    }

    @Override
    public void savePlayerData(java.io.OutputStream os) {
        // No-op; this agent is not learning.
    }

    @Override
    public void loadPlayerData(java.io.InputStream is) {
        // No-op; this agent is not learning.
    }

    private void bootstrapUnitIds(StateView state) {
        List<Integer> unitIds = state.getUnitIds(getPlayerNumber());
        for (Integer id : unitIds) {
            UnitView unitView = state.getUnit(id);
            String typeName = unitView.getTemplateView().getName();
            if ("Peasant".equals(typeName)) {
                peasantId = id;
            } else if ("TownHall".equals(typeName)) {
                townHallId = id;
            }
        }
        if (peasantId < 0 || townHallId < 0) {
            throw new IllegalStateException("Unable to find both peasant and town hall for player " + getPlayerNumber());
        }
        resourceIdLookup = indexResources(state.getAllResourceNodes());
    }

    private Map<Location, Integer> indexResources(List<ResourceNode.ResourceView> resourceNodes) {
        Map<Location, Integer> mapping = new HashMap<>();
        for (ResourceNode.ResourceView resourceView : resourceNodes) {
            Optional<Location> location = LocationLookup.byCoordinates(resourceView.getXPosition(), resourceView.getYPosition());
            if (location.isPresent()) {
                mapping.put(location.get(), resourceView.getID());
            }
        }
        return mapping;
    }

    private boolean peasantIsIdle(StateView state) {
        UnitView peasant = state.getUnit(peasantId);
        return peasant.getCurrentDurativeAction() == null;
    }

    private Map<Integer, Action> issueNextAction(StateView state) {
        Map<Integer, Action> actions = new HashMap<>();
        if (pendingSteps.isEmpty()) {
            return actions;
        }

        PlanStep step = pendingSteps.peek();
        Action action = translateStep(step, state);
        if (action == null) {
            planFailure = true;
            pendingSteps.clear();
            return actions;
        }

        pendingSteps.pop();
        actions.put(peasantId, action);
        issuedSteps++;
        System.out.println("Issuing step " + issuedSteps + ": " + describe(step));
        if (step.getType() == PlanStep.StepType.DEPOSIT_GOLD) {
            deliveredGold += PlanningState.HARVEST_AMOUNT;
        } else if (step.getType() == PlanStep.StepType.DEPOSIT_WOOD) {
            deliveredWood += PlanningState.HARVEST_AMOUNT;
        }
        return actions;
    }

    private String describe(PlanStep step) {
        switch (step.getType()) {
            case MOVE:
                return "Move to " + step.getTarget();
            case HARVEST_GOLD:
                return "Harvest gold from " + step.getTarget();
            case HARVEST_WOOD:
                return "Harvest wood from " + step.getTarget();
            case DEPOSIT_GOLD:
                return "Deposit carried gold";
            case DEPOSIT_WOOD:
                return "Deposit carried wood";
            default:
                return step.toString();
        }
    }

    private Action translateStep(PlanStep step, StateView state) {
        switch (step.getType()) {
            case MOVE:
                int[] open = state.getClosestOpenPosition(step.getTarget().getX(), step.getTarget().getY());
                if (open == null) {
                    System.err.println("Unable to find open position near " + step.getTarget());
                    return null;
                }
                return Action.createCompoundMove(peasantId, open[0], open[1]);
            case HARVEST_GOLD:
            case HARVEST_WOOD:
                Integer resourceId = resourceIdLookup.get(step.getTarget());
                if (resourceId == null) {
                    System.err.println("No resource id found for location " + step.getTarget());
                    return null;
                }
                return Action.createCompoundGather(peasantId, resourceId);
            case DEPOSIT_GOLD:
            case DEPOSIT_WOOD:
                Action deposit = Action.createCompoundDeposit(peasantId, townHallId);
                // The API returns a more specific subtype for compound deposits.
                if (deposit.getType() == ActionType.PRIMITIVEDEPOSIT) {
                    // ensure the unit faces the town hall; fallback to targeted deposit
                    TargetedAction targeted = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townHallId);
                    return targeted;
                }
                return deposit;
            default:
                return null;
        }
    }
}
