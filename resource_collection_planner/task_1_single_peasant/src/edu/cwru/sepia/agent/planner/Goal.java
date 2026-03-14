package edu.cwru.sepia.agent.planner;

/**
 * Required town hall resource totals that the planner must achieve.
 */
public final class Goal {
    private final int requiredGold;
    private final int requiredWood;

    public Goal(int requiredGold, int requiredWood) {
        if (requiredGold < 0 || requiredWood < 0) {
            throw new IllegalArgumentException("Goal resource requirements must be non-negative.");
        }
        this.requiredGold = requiredGold;
        this.requiredWood = requiredWood;
    }

    public int getRequiredGold() {
        return requiredGold;
    }

    public int getRequiredWood() {
        return requiredWood;
    }

    @Override
    public String toString() {
        return "Goal{gold=" + requiredGold + ", wood=" + requiredWood + '}';
    }
}
