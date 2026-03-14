package edu.cwru.sepia.agent.planner;

import java.util.Arrays;

/**
 * Immutable STRIPS-like state description tailored to the rc_3m5t resource collection scenario.
 */
public final class PlanningState {
    public static final int HARVEST_AMOUNT = 100;

    private final Location location;
    private final CargoType cargoType;
    private final int cargoAmount;
    private final int townHallGold;
    private final int townHallWood;
    private final int[] resourceAmounts;
    private final int hashCode;

    public PlanningState(Location location,
                         CargoType cargoType,
                         int cargoAmount,
                         int townHallGold,
                         int townHallWood,
                         int[] resourceAmounts) {
        this(location, cargoType, cargoAmount, townHallGold, townHallWood, resourceAmounts, true);
    }

    PlanningState(Location location,
                  CargoType cargoType,
                  int cargoAmount,
                  int townHallGold,
                  int townHallWood,
                  int[] resourceAmounts,
                  boolean defensiveCopy) {
        this.location = location;
        this.cargoType = cargoType;
        this.cargoAmount = cargoAmount;
        this.townHallGold = townHallGold;
        this.townHallWood = townHallWood;
        this.resourceAmounts = defensiveCopy ? resourceAmounts.clone() : resourceAmounts;
        this.hashCode = computeHashCode();
    }

    public Location getLocation() {
        return location;
    }

    public CargoType getCargoType() {
        return cargoType;
    }

    public int getCargoAmount() {
        return cargoAmount;
    }

    public int getTownHallGold() {
        return townHallGold;
    }

    public int getTownHallWood() {
        return townHallWood;
    }

    public int getResourceAmount(Location resourceLocation) {
        if (!resourceLocation.isResourceSite()) {
            return 0;
        }
        return resourceAmounts[resourceLocation.ordinal()];
    }

    public boolean hasCargo() {
        return cargoType != CargoType.NONE && cargoAmount > 0;
    }

    public boolean goalSatisfied(Goal goal) {
        return townHallGold >= goal.getRequiredGold() && townHallWood >= goal.getRequiredWood();
    }

    public PlanningState moveTo(Location destination) {
        if (destination == location) {
            return this;
        }
        return new PlanningState(destination, cargoType, cargoAmount, townHallGold, townHallWood, resourceAmounts, false);
    }

    public PlanningState harvest(Location resourceLocation) {
        if (!resourceLocation.isResourceSite()) {
            throw new IllegalArgumentException("Cannot harvest from non-resource location: " + resourceLocation);
        }
        int[] updatedResources = resourceAmounts.clone();
        int index = resourceLocation.ordinal();
        updatedResources[index] = updatedResources[index] - HARVEST_AMOUNT;
        CargoType newCargo = resourceLocation.getResource() == Location.Resource.GOLD ? CargoType.GOLD : CargoType.WOOD;
        return new PlanningState(resourceLocation, newCargo, HARVEST_AMOUNT, townHallGold, townHallWood, updatedResources, false);
    }

    public PlanningState depositCargo() {
        if (!hasCargo()) {
            return this;
        }
        int newGold = townHallGold;
        int newWood = townHallWood;
        if (cargoType == CargoType.GOLD) {
            newGold += cargoAmount;
        } else if (cargoType == CargoType.WOOD) {
            newWood += cargoAmount;
        }
        return new PlanningState(Location.TOWN_HALL, CargoType.NONE, 0, newGold, newWood, resourceAmounts, false);
    }

    public PlanningState dropCargo() {
        return new PlanningState(location, CargoType.NONE, 0, townHallGold, townHallWood, resourceAmounts, false);
    }

    public int[] getResourceSnapshot() {
        return resourceAmounts.clone();
    }

    private int computeHashCode() {
        int result = location.hashCode();
        result = 31 * result + cargoType.hashCode();
        result = 31 * result + cargoAmount;
        result = 31 * result + townHallGold;
        result = 31 * result + townHallWood;
        result = 31 * result + Arrays.hashCode(resourceAmounts);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlanningState)) {
            return false;
        }
        PlanningState other = (PlanningState) obj;
        return location == other.location
                && cargoType == other.cargoType
                && cargoAmount == other.cargoAmount
                && townHallGold == other.townHallGold
                && townHallWood == other.townHallWood
                && Arrays.equals(resourceAmounts, other.resourceAmounts);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "PlanningState{" +
                "location=" + location +
                ", cargoType=" + cargoType +
                ", cargoAmount=" + cargoAmount +
                ", townHallGold=" + townHallGold +
                ", townHallWood=" + townHallWood +
                '}';
    }
}
