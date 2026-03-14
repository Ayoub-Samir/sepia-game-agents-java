package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.util.List;
import java.util.Optional;

/**
 * Builds planning states by reading SEPIA state information or by using the known defaults for rc_3m5t.
 */
public final class InitialStateFactory {
    private InitialStateFactory() {
    }

    public static PlanningState fromDefaults() {
        int[] resources = defaultResourceArray();
        return new PlanningState(1, 0, 0, resources);
    }

    public static PlanningState fromState(StateView state, int playerId) {
        int[] resources = defaultResourceArray();
        for (ResourceNode.ResourceView resourceView : state.getAllResourceNodes()) {
            Optional<Location> location = LocationLookup.byCoordinates(resourceView.getXPosition(), resourceView.getYPosition());
            if (!location.isPresent()) {
                continue;
            }
            resources[location.get().ordinal()] = resourceView.getAmountRemaining();
        }

        int peasants = countPeasants(state, playerId);
        int gold = state.getResourceAmount(playerId, ResourceType.GOLD);
        int wood = state.getResourceAmount(playerId, ResourceType.WOOD);

        return new PlanningState(peasants, gold, wood, resources);
    }

    private static int countPeasants(StateView state, int playerId) {
        int count = 0;
        List<Integer> units = state.getUnitIds(playerId);
        for (Integer id : units) {
            UnitView unit = state.getUnit(id);
            if ("Peasant".equals(unit.getTemplateView().getName())) {
                count++;
            }
        }
        return count;
    }

    private static int[] defaultResourceArray() {
        int[] resources = new int[Location.values().length];
        for (Location location : Location.values()) {
            if (location.isResourceSite()) {
                resources[location.ordinal()] = location.getInitialCapacity();
            }
        }
        return resources;
    }
}
