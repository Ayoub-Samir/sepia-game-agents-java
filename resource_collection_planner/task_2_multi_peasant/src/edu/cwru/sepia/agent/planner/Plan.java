package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable collection of plan steps along with the aggregate cost.
 */
public final class Plan {
    private final List<PlanStep> steps;
    private final int totalCost;

    public Plan(List<PlanStep> steps, int totalCost) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        this.totalCost = totalCost;
    }

    public List<PlanStep> getSteps() {
        return steps;
    }

    public int getTotalCost() {
        return totalCost;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Plan cost: ").append(totalCost).append(System.lineSeparator());
        for (int i = 0; i < steps.size(); i++) {
            sb.append(i).append(": ").append(steps.get(i)).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
