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
        return new PlanningState(Location.TOWN_HALL, CargoType.NONE, 0, 0, 0, resources, false);
    }

    public static PlanningState fromState(StateView state, int playerId) {
        int[] resources = defaultResourceArray();
        // Overwrite with live values for safety.
        for (ResourceNode.ResourceView resourceView : state.getAllResourceNodes()) {
            Optional<Location> location = LocationLookup.byCoordinates(resourceView.getXPosition(), resourceView.getYPosition());
            if (!location.isPresent()) {
                continue;
            }
            resources[location.get().ordinal()] = resourceView.getAmountRemaining();
        }

        Location initialLocation = inferPeasantLocation(state, playerId).orElse(Location.TOWN_HALL);
        UnitView peasant = findSinglePeasant(state, playerId);

        CargoType cargoType = CargoType.NONE;
        int cargoAmount = 0;
        if (peasant.getCargoAmount() > 0) {
            cargoAmount = peasant.getCargoAmount();
            ResourceType cargo = peasant.getCargoType();
            cargoType = cargo == ResourceType.GOLD ? CargoType.GOLD
                    : cargo == ResourceType.WOOD ? CargoType.WOOD : CargoType.NONE;
        }

        int gold = state.getResourceAmount(playerId, ResourceType.GOLD);
        int wood = state.getResourceAmount(playerId, ResourceType.WOOD);

        return new PlanningState(initialLocation, cargoType, cargoAmount, gold, wood, resources, false);
    }

    private static UnitView findSinglePeasant(StateView state, int playerId) {
        List<Integer> units = state.getUnitIds(playerId);
        for (Integer id : units) {
            UnitView unit = state.getUnit(id);
            if ("Peasant".equals(unit.getTemplateView().getName())) {
                return unit;
            }
        }
        throw new IllegalStateException("Expected a peasant for player " + playerId);
    }

    private static Optional<Location> inferPeasantLocation(StateView state, int playerId) {
        UnitView peasant = findSinglePeasant(state, playerId);
        return LocationLookup.byCoordinates(peasant.getXPosition(), peasant.getYPosition());
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
