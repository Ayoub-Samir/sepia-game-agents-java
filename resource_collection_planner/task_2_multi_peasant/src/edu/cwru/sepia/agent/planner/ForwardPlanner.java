package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Forward planner using A* over aggregated resource states.
 */
public final class ForwardPlanner {

    private final PlannerHeuristic heuristic = new PlannerHeuristic();
    private final List<CollectAction> collectActions;
    private final BuildPeasantAction buildAction = new BuildPeasantAction();

    public ForwardPlanner() {
        collectActions = buildCollectActions();
    }

    public Optional<Plan> plan(PlanningState initialState, Goal goal) {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator
                .comparingInt(Node::fCost)
                .thenComparingInt(Node::getInsertionOrder));
        Map<PlanningState, Integer> bestCost = new HashMap<>();
        int insertionCounter = 0;

        int initialHeuristic = heuristic.estimate(initialState, goal);
        openList.add(new Node(initialState, null, null, 0, initialHeuristic, insertionCounter++));
        bestCost.put(initialState, 0);

        while (!openList.isEmpty()) {
            Node current = openList.poll();

            if (current.state.goalSatisfied(goal)) {
                return Optional.of(reconstructPlan(current));
            }

            int recordedCost = bestCost.getOrDefault(current.state, Integer.MAX_VALUE);
            if (current.gCost > recordedCost) {
                continue;
            }

            for (GroundedAction action : applicableActions(current.state, goal)) {
                if (!action.isApplicable(current.state)) {
                    continue;
                }
                PlanningState successor = action.apply(current.state);
                int actionCost = Math.max(1, action.getCost(current.state));
                int tentativeCost = current.gCost + actionCost;

                if (tentativeCost < bestCost.getOrDefault(successor, Integer.MAX_VALUE)) {
                    bestCost.put(successor, tentativeCost);
                    int heuristicEstimate = heuristic.estimate(successor, goal);
                    openList.add(new Node(successor, current, action, tentativeCost, heuristicEstimate, insertionCounter++));
                }
            }
        }

        return Optional.empty();
    }

    private List<GroundedAction> applicableActions(PlanningState state, Goal goal) {
        List<GroundedAction> actions = new ArrayList<>();

        int remainingGold = Math.max(0, goal.getRequiredGold() - state.getTownHallGold());
        int remainingWood = Math.max(0, goal.getRequiredWood() - state.getTownHallWood());
        int goldLoads = (int) Math.ceil(remainingGold / (double) PlanningState.HARVEST_AMOUNT);
        int woodLoads = (int) Math.ceil(remainingWood / (double) PlanningState.HARVEST_AMOUNT);

        Location preferredGold = nextAvailableSite(state, Location.Resource.GOLD);
        Location preferredWood = nextAvailableSite(state, Location.Resource.WOOD);
        int totalPeasants = state.getTotalPeasants();

        if (remainingGold > 0 && preferredGold != null) {
            int desiredCount = Math.min(totalPeasants, Math.max(1, goldLoads));
            addCollectAction(actions, preferredGold, CargoType.GOLD, desiredCount);
        }
        if (remainingWood > 0 && preferredWood != null) {
            int desiredCount = Math.min(totalPeasants, Math.max(1, woodLoads));
            addCollectAction(actions, preferredWood, CargoType.WOOD, desiredCount);
        }

        if (buildAction.isApplicable(state) && (remainingGold + remainingWood) >= PlanningState.HARVEST_AMOUNT * 4) {
            actions.add(buildAction);
        }

        return actions;
    }

    private void addCollectAction(List<GroundedAction> actions, Location site, CargoType cargoType, int desiredCount) {
        for (CollectAction action : collectActions) {
            if (action.getResourceLocation() != site || action.getCargoType() != cargoType) {
                continue;
            }
            if (action.getCount() == desiredCount) {
                actions.add(action);
                break;
            }
        }
    }

    private Location nextAvailableSite(PlanningState state, Location.Resource resourceType) {
        Location[] order = resourceType == Location.Resource.GOLD ? GOLD_PRIORITY : WOOD_PRIORITY;
        for (Location location : order) {
            if (state.getResourceAmount(location) >= PlanningState.HARVEST_AMOUNT) {
                return location;
            }
        }
        return null;
    }

    private Plan reconstructPlan(Node goalNode) {
        List<PlanStep> steps = new ArrayList<>();
        Node node = goalNode;
        while (node.parent != null && node.action != null) {
            List<PlanStep> actionSteps = node.action.toPlanSteps(node.parent.state);
            steps.addAll(0, actionSteps);
            node = node.parent;
        }
        int totalCost = goalNode.gCost;
        return new Plan(steps, totalCost);
    }

    private List<CollectAction> buildCollectActions() {
        List<CollectAction> actions = new ArrayList<>();
        for (Location location : Location.values()) {
            if (!location.isResourceSite()) {
                continue;
            }
            CargoType cargo = location.getResource() == Location.Resource.GOLD ? CargoType.GOLD : CargoType.WOOD;
            for (int count = 1; count <= PlanningState.MAX_WORKERS; count++) {
                actions.add(new CollectAction(location, cargo, count));
            }
        }
        return actions;
    }

    private static final class Node {
        private final PlanningState state;
        private final Node parent;
        private final GroundedAction action;
        private final int gCost;
        private final int hCost;
        private final int insertionOrder;

        Node(PlanningState state, Node parent, GroundedAction action, int gCost, int hCost, int insertionOrder) {
            this.state = state;
            this.parent = parent;
            this.action = action;
            this.gCost = gCost;
            this.hCost = hCost;
            this.insertionOrder = insertionOrder;
        }

        int fCost() {
            return gCost + hCost;
        }

        int getInsertionOrder() {
            return insertionOrder;
        }
    }

    private static final Location[] GOLD_PRIORITY = {
            Location.GOLD_MINE_NEAR,
            Location.GOLD_MINE_MID,
            Location.GOLD_MINE_FAR
    };

    private static final Location[] WOOD_PRIORITY = {
            Location.FOREST_WEST,
            Location.FOREST_SOUTH,
            Location.FOREST_EAST,
            Location.FOREST_NW,
            Location.FOREST_NE
    };
}
