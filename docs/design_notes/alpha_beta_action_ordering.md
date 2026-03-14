# Alpha-Beta Action Ordering

This note summarizes the move-ordering ideas used by the combat agent to improve alpha-beta pruning.

## Features

1. `attackBonus`
   Whether the action is an attack. Attacks are usually the strongest footman actions and should be explored first.

2. `distanceDecrease`
   How much closer the unit gets to the nearest opponent after acting. Closing distance is good for footmen.

3. `distanceIncrease`
   How much farther the acting unit ends up from the nearest opponent. Retreating footmen and over-aggressive archers are both undesirable.

4. `neutralMove`
   Whether the action preserves the current distance. Neutral repositioning is acceptable, but lower priority than attacks or pressure-building moves.

5. `emergencyEscape`
   Whether an archer ends the action with a footman at distance `<= 2`. In those states, escape moves should dominate the move ordering.

6. `runAwayPenalty`
   Footman-only feature that penalizes any action that increases distance to the nearest archer.

7. `mobilityGain` / `mobilityLoss`
   Archer-only features that estimate how many legal neighboring tiles remain available after the move. Higher mobility makes kiting easier.

## Sketch

```java
int scoreAction(Action action, GameState state, boolean isFootmanTurn) {
    int score = 0;
    int oldDist = distanceBeforeAction(state, action);
    int newDist = distanceAfterAction(state, action);

    int distanceDecrease = Math.max(0, oldDist - newDist);
    int distanceIncrease = Math.max(0, newDist - oldDist);
    boolean isAttack = action.getType().equals(ActionType.PRIMITIVEATTACK);

    if (isFootmanTurn) {
        score += 2000 * (isAttack ? 1 : 0);
        score += 300 * distanceDecrease;
        score -= 300 * distanceIncrease;
        return score;
    }

    int oldMobility = archerMobilityBefore(state);
    int newMobility = archerMobilityAfter(state, action);
    score += 50 * Math.max(0, newMobility - oldMobility);
    score -= 50 * Math.max(0, oldMobility - newMobility);
    return score;
}
```
