# Alpha-Beta Evaluation Function

This note summarizes the feature ideas behind the combat agent's state evaluation.

## Features

1. `totalDistFootmenToArcher`
   Sum of all footman-to-archer Manhattan distances. Smaller values are better for melee units.

2. `minDistFootmanToArcher`
   Closest footman-to-archer distance. A single nearby footman creates immediate tactical pressure.

3. `totalArcherHealth`
   Sum of enemy hit points. Lower enemy health means the maximizing player is closer to winning.

4. `totalFootmanHealth`
   Sum of friendly hit points. Healthy footmen can absorb counter-attacks while closing distance.

5. `numArchersAlive`
   The count of surviving archers. This is one of the strongest swing features in the state.

6. `numFootmenInAttackRangeNextTurn`
   Number of footmen that can threaten an attack after one move.

7. `numFootmenAdjacentRightNow`
   Number of footmen already in melee range.

8. `archerMobility`
   Number of safe neighboring tiles available to the archers.

9. `footmenPressure`
   Number of footmen within distance `<= 2` of an archer.

10. `distToArcherCentroid`
    Distance from the footmen to the average archer position, useful when chasing multiple targets.

## Sketch

```java
double evaluateState(GameState state) {
    double totalDist = computeTotalFootmenToArcherDistance(state);
    double totalArcherHP = computeTotalArcherHealth(state);
    double totalFootmanHP = computeTotalFootmanHealth(state);
    double archersAlive = computeNumArchersAlive(state);
    double adjacentNow = computeFootmenAdjacentNow(state);

    return
        -200 * totalDist +
        -35 * totalArcherHP +
         20 * totalFootmanHP +
        -500 * archersAlive +
        +220 * adjacentNow;
}
```
