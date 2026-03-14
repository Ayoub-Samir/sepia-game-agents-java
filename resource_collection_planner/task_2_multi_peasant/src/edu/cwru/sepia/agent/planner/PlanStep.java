package edu.cwru.sepia.agent.planner;

public final class PlanStep {

    public enum StepType {
        MOVE,
        HARVEST_GOLD,
        HARVEST_WOOD,
        DEPOSIT_GOLD,
        DEPOSIT_WOOD,
        BUILD_PEASANT
    }

    private final StepType type;
    private final Location source;
    private final Location target;
    private final CargoType cargoType;
    private final int unitCount;
    private final int cost;

    public PlanStep(StepType type,
                    Location source,
                    Location target,
                    CargoType cargoType,
                    int unitCount,
                    int cost) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.cargoType = cargoType;
        this.unitCount = unitCount;
        this.cost = cost;
    }

    public StepType getType() {
        return type;
    }

    public Location getSource() {
        return source;
    }

    public Location getTarget() {
        return target;
    }

    public CargoType getCargoType() {
        return cargoType;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public int getCost() {
        return cost;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type.name());
        if (unitCount > 0) {
            sb.append(" x").append(unitCount);
        }
        if (source != null) {
            sb.append(" from ").append(source);
        }
        if (target != null) {
            sb.append(" to ").append(target);
        }
        if (cargoType != null && cargoType != CargoType.NONE) {
            sb.append(" carrying ").append(cargoType);
        }
        sb.append(" (cost=").append(cost).append(")");
        return sb.toString();
    }
}
