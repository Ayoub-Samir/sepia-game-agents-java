package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MultiPeasantPlannerAgent extends Agent {
    private static final long serialVersionUID = 1L;

    private final Goal goal;
    private final ForwardPlanner planner;

    private int townHallId = -1;
    private int peasantTemplateId = -1;
    private Map<Location, Integer> resourceIdLookup = new HashMap<>();
    private final Map<Integer, PeasantRecord> peasants = new HashMap<>();

    private Deque<PlanStep> pendingSteps = new ArrayDeque<>();
    private ActiveStep activeStep = null;
    private boolean planFailure = false;
    private int issuedSteps = 0;
    private int executedEnvironmentSteps = 0;
    private int planCompletionStep = -1;
    private int deliveredGold = 0;
    private int deliveredWood = 0;

    public MultiPeasantPlannerAgent(int playernum) {
        this(playernum, new String[0]);
    }

    public MultiPeasantPlannerAgent(int playernum, String[] otherArgs) {
        super(playernum);
        if (otherArgs != null && otherArgs.length >= 2) {
            this.goal = new Goal(Integer.parseInt(otherArgs[0]), Integer.parseInt(otherArgs[1]));
        } else {
            this.goal = new Goal(200, 200);
        }
        this.planner = new ForwardPlanner();
    }

    @Override
    public Map<Integer, Action> initialStep(StateView newState, HistoryView historyView) {
        bootstrapUnits(newState);
        executedEnvironmentSteps = 0;
        pendingSteps.clear();
        activeStep = null;
        planFailure = false;
        issuedSteps = 0;
        planCompletionStep = -1;
        deliveredGold = 0;
        deliveredWood = 0;

        PlanningState planningState = InitialStateFactory.fromState(newState, getPlayerNumber());
        Optional<Plan> plan = planner.plan(planningState, goal);
        if (!plan.isPresent()) {
            System.err.println("Planning failed for goal " + goal);
            planFailure = true;
            return new HashMap<>();
        }

        pendingSteps.addAll(plan.get().getSteps());
        return advancePlan(newState);
    }

    @Override
    public Map<Integer, Action> middleStep(StateView newState, HistoryView historyView) {
        executedEnvironmentSteps++;
        if (planFailure) {
            return new HashMap<>();
        }
        return advancePlan(newState);
    }

    @Override
    public void terminalStep(StateView newState, HistoryView historyView) {
        System.out.println("Episode complete. Issued " + issuedSteps + " plan steps across "
                + executedEnvironmentSteps + " environment steps.");
        if (planCompletionStep >= 0) {
            System.out.println("Plan finished after " + planCompletionStep + " environment steps.");
        }
        if (!planFailure && pendingSteps.isEmpty() && activeStep == null) {
            System.out.println("Resources delivered via plan: gold=" + deliveredGold + ", wood=" + deliveredWood);
        }
    }

    @Override
    public void savePlayerData(java.io.OutputStream os) {
        // No-op
    }

    @Override
    public void loadPlayerData(java.io.InputStream is) {
        // No-op
    }

    private Map<Integer, Action> advancePlan(StateView state) {
        Map<Integer, Action> actions = new HashMap<>();
        refreshPeasants(state);

        if (activeStep != null && isCurrentStepComplete()) {
            completeCurrentStep();
        }

        if (planFailure) {
            return actions;
        }

        if (activeStep == null) {
            if (pendingSteps.isEmpty()) {
                if (planCompletionStep < 0 && allPeasantsIdle()) {
                    planCompletionStep = executedEnvironmentSteps;
                }
                return actions;
            }
            activeStep = new ActiveStep(pendingSteps.peek(), peasants.size());
            issuedSteps++;
            System.out.println("Issuing step " + issuedSteps + ": " + describe(activeStep.step));
        }

        actions.putAll(issueCommandsForActiveStep(state));
        return actions;
    }

    private Map<Integer, Action> issueCommandsForActiveStep(StateView state) {
        Map<Integer, Action> actions = new HashMap<>();
        PlanStep step = activeStep.step;
        int needed = step.getUnitCount() - activeStep.assignedUnits.size();

        switch (step.getType()) {
            case MOVE:
                if (needed > 0) {
                    List<Integer> units = selectIdlePeasants(step.getSource(), step.getCargoType(), needed);
                    if (units.size() < needed) {
                        break;
                    }
                    for (Integer unitId : units) {
                        int[] dest = state.getClosestOpenPosition(step.getTarget().getX(), step.getTarget().getY());
                        if (dest == null) {
                            planFailure = true;
                            return new HashMap<>();
                        }
                        actions.put(unitId, Action.createCompoundMove(unitId, dest[0], dest[1]));
                        activeStep.assignedUnits.add(unitId);
                    }
                }
                break;
            case HARVEST_GOLD:
            case HARVEST_WOOD:
                if (needed > 0) {
                    List<Integer> units = selectIdlePeasants(step.getTarget(), CargoType.NONE, needed);
                    if (units.size() < needed) {
                        break;
                    }
                    Integer resourceId = resourceIdLookup.get(step.getTarget());
                    if (resourceId == null) {
                        planFailure = true;
                        return new HashMap<>();
                    }
                    for (Integer unitId : units) {
                        actions.put(unitId, Action.createCompoundGather(unitId, resourceId));
                        activeStep.assignedUnits.add(unitId);
                    }
                }
                break;
            case DEPOSIT_GOLD:
            case DEPOSIT_WOOD:
                if (needed > 0) {
                    CargoType cargo = step.getType() == PlanStep.StepType.DEPOSIT_GOLD ? CargoType.GOLD : CargoType.WOOD;
                    List<Integer> units = selectIdlePeasants(Location.TOWN_HALL, cargo, needed);
                    if (units.size() < needed) {
                        break;
                    }
                    for (Integer unitId : units) {
                        Action deposit = Action.createCompoundDeposit(unitId, townHallId);
                        if (deposit.getType() == ActionType.PRIMITIVEDEPOSIT) {
                            deposit = new TargetedAction(unitId, ActionType.COMPOUNDDEPOSIT, townHallId);
                        }
                        actions.put(unitId, deposit);
                        activeStep.assignedUnits.add(unitId);
                    }
                }
                break;
            case BUILD_PEASANT:
                if (!activeStep.buildIssued) {
                    Action build = Action.createCompoundProduction(townHallId, peasantTemplateId);
                    actions.put(townHallId, build);
                    activeStep.buildIssued = true;
                    activeStep.expectedPeasantCount = peasants.size() + 1;
                }
                break;
            default:
                break;
        }
        return actions;
    }

    private boolean isCurrentStepComplete() {
        PlanStep step = activeStep.step;
        switch (step.getType()) {
            case MOVE:
                return assignedUnitsIdleNear(step.getTarget());
            case HARVEST_GOLD:
                return assignedUnitsCarry(CargoType.GOLD);
            case HARVEST_WOOD:
                return assignedUnitsCarry(CargoType.WOOD);
            case DEPOSIT_GOLD:
            case DEPOSIT_WOOD:
                return assignedUnitsCarry(CargoType.NONE);
            case BUILD_PEASANT:
                return peasants.size() >= activeStep.expectedPeasantCount;
            default:
                return false;
        }
    }

    private void completeCurrentStep() {
        PlanStep step = activeStep.step;
        if (step.getType() == PlanStep.StepType.DEPOSIT_GOLD) {
            deliveredGold += step.getUnitCount() * PlanningState.HARVEST_AMOUNT;
        } else if (step.getType() == PlanStep.StepType.DEPOSIT_WOOD) {
            deliveredWood += step.getUnitCount() * PlanningState.HARVEST_AMOUNT;
        }
        pendingSteps.pop();
        activeStep = null;
    }

    private boolean assignedUnitsIdleNear(Location location) {
        for (Integer unitId : activeStep.assignedUnits) {
            PeasantRecord record = peasants.get(unitId);
            if (record == null || !record.idle || record.distanceTo(location) > 3) {
                return false;
            }
        }
        return true;
    }

    private boolean assignedUnitsCarry(CargoType cargoType) {
        for (Integer unitId : activeStep.assignedUnits) {
            PeasantRecord record = peasants.get(unitId);
            if (record == null || !record.idle || record.cargoType != cargoType) {
                return false;
            }
        }
        return true;
    }

    private boolean allPeasantsIdle() {
        for (PeasantRecord record : peasants.values()) {
            if (!record.idle) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> selectIdlePeasants(Location location, CargoType cargoType, int required) {
        List<Integer> selected = new ArrayList<>();
        for (PeasantRecord record : peasants.values()) {
            if (selected.size() >= required) {
                break;
            }
            if (!record.idle) {
                continue;
            }
            if (activeStep.assignedUnits.contains(record.unitId)) {
                continue;
            }
            if (record.cargoType != cargoType) {
                continue;
            }
            if (record.distanceTo(location) > 3) {
                continue;
            }
            selected.add(record.unitId);
        }
        return selected;
    }

    private void bootstrapUnits(StateView state) {
        peasants.clear();
        List<Integer> unitIds = state.getUnitIds(getPlayerNumber());
        for (Integer id : unitIds) {
            UnitView unit = state.getUnit(id);
            String name = unit.getTemplateView().getName();
            if ("Peasant".equals(name)) {
                peasants.put(id, PeasantRecord.fromUnit(unit));
            } else if ("TownHall".equals(name)) {
                townHallId = id;
            }
        }
        resourceIdLookup = indexResources(state.getAllResourceNodes());
        peasantTemplateId = state.getTemplate(getPlayerNumber(), "Peasant").getID();
    }

    private Map<Location, Integer> indexResources(List<ResourceNode.ResourceView> nodes) {
        Map<Location, Integer> mapping = new HashMap<>();
        for (ResourceNode.ResourceView view : nodes) {
            Optional<Location> location = LocationLookup.byCoordinates(view.getXPosition(), view.getYPosition());
            location.ifPresent(loc -> mapping.put(loc, view.getID()));
        }
        return mapping;
    }

    private void refreshPeasants(StateView state) {
        Set<Integer> seen = new HashSet<>();
        for (Integer id : state.getUnitIds(getPlayerNumber())) {
            UnitView unit = state.getUnit(id);
            String name = unit.getTemplateView().getName();
            if ("Peasant".equals(name)) {
                PeasantRecord record = peasants.get(id);
                if (record == null) {
                    record = PeasantRecord.fromUnit(unit);
                    peasants.put(id, record);
                } else {
                    record.updateFromUnit(unit);
                }
                seen.add(id);
            } else if ("TownHall".equals(name)) {
                townHallId = id;
            }
        }
        peasants.keySet().retainAll(seen);
    }

    private String describe(PlanStep step) {
        switch (step.getType()) {
            case MOVE:
                return String.format("Move x%d %s from %s to %s", step.getUnitCount(), step.getCargoType(), step.getSource(), step.getTarget());
            case HARVEST_GOLD:
                return String.format("Harvest gold x%d at %s", step.getUnitCount(), step.getTarget());
            case HARVEST_WOOD:
                return String.format("Harvest wood x%d at %s", step.getUnitCount(), step.getTarget());
            case DEPOSIT_GOLD:
                return String.format("Deposit gold x%d", step.getUnitCount());
            case DEPOSIT_WOOD:
                return String.format("Deposit wood x%d", step.getUnitCount());
            case BUILD_PEASANT:
                return "Build peasant";
            default:
                return step.toString();
        }
    }

    private static final class ActiveStep {
        final PlanStep step;
        final Set<Integer> assignedUnits = new HashSet<>();
        boolean buildIssued = false;
        int expectedPeasantCount;

        ActiveStep(PlanStep step, int currentPeasants) {
            this.step = step;
            this.expectedPeasantCount = currentPeasants;
        }
    }

    private static final class PeasantRecord {
        final int unitId;
        int x;
        int y;
        CargoType cargoType;
        boolean idle;

        PeasantRecord(int unitId, int x, int y, CargoType cargoType, boolean idle) {
            this.unitId = unitId;
            this.x = x;
            this.y = y;
            this.cargoType = cargoType;
            this.idle = idle;
        }

        static PeasantRecord fromUnit(UnitView unit) {
            return new PeasantRecord(unit.getID(), unit.getXPosition(), unit.getYPosition(), cargoType(unit), unit.getCurrentDurativeAction() == null);
        }

        void updateFromUnit(UnitView unit) {
            this.x = unit.getXPosition();
            this.y = unit.getYPosition();
            this.cargoType = cargoType(unit);
            this.idle = unit.getCurrentDurativeAction() == null;
        }

        int distanceTo(Location location) {
            return Math.abs(location.getX() - x) + Math.abs(location.getY() - y);
        }

        private static CargoType cargoType(UnitView unit) {
            if (unit.getCargoAmount() <= 0) {
                return CargoType.NONE;
            }
            ResourceType cargo = unit.getCargoType();
            if (cargo == ResourceType.GOLD) {
                return CargoType.GOLD;
            } else if (cargo == ResourceType.WOOD) {
                return CargoType.WOOD;
            }
            return CargoType.NONE;
        }
    }
}
