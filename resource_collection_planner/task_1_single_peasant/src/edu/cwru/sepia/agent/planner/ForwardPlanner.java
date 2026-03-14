package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Forward state-space planner that uses A* search to minimise makespan (approximated as sum of action costs).
 */
public final class ForwardPlanner {

    private final PlannerHeuristic heuristic = new PlannerHeuristic();
    private final EnumMap<Location, MoveAction> moveActions = new EnumMap<>(Location.class);
    private final EnumMap<Location, HarvestAction> harvestActions = new EnumMap<>(Location.class);
    private final DepositAction depositAction = new DepositAction();

    public ForwardPlanner() {
        for (Location location : Location.values()) {
            moveActions.put(location, new MoveAction(location));
            if (location.isResourceSite()) {
                harvestActions.put(location, new HarvestAction(location));
            }
        }
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

            for (GroundedAction action : applicableActions(current.state)) {
                if (!action.isApplicable(current.state)) {
                    continue;
                }
                int actionCost = Math.max(1, action.getCost(current.state));
                PlanningState successor = action.apply(current.state);
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

    private List<GroundedAction> applicableActions(PlanningState state) {
        List<GroundedAction> actions = new ArrayList<>();

        for (MoveAction moveAction : moveActions.values()) {
            if (moveAction.isApplicable(state)) {
                actions.add(moveAction);
            }
        }

        for (HarvestAction harvest : harvestActions.values()) {
            if (harvest.isApplicable(state)) {
                actions.add(harvest);
            }
        }

        if (depositAction.isApplicable(state)) {
            actions.add(depositAction);
        }

        return actions;
    }

    private Plan reconstructPlan(Node goalNode) {
        List<PlanStep> steps = new ArrayList<>();
        Node node = goalNode;
        while (node.parent != null && node.action != null) {
            steps.add(0, node.action.toPlanStep(node.parent.state));
            node = node.parent;
        }
        return new Plan(steps, goalNode.gCost);
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
}
