# Alpha-Beta Combat Agent

SEPIA combat agent for the footmen-vs-archers assignment.

Official SEPIA documentation:
[SEPIA API docs](http://engr.case.edu/ray_soumya/Sepia/html/)

## Contents

- [src](./src/)
  Java source for `AlphaBetaCombatAgent`.
- [scenarios](./scenarios/)
  Runner configs and map states for `2v1` and `2v2` combat setups.
- [opponent_agents](./opponent_agents/)
  Bundled compiled archer opponent used by the original assignment.

## Agent Summary

The agent uses:

- fixed-depth minimax
- alpha-beta pruning
- action ordering heuristics
- a hand-tuned evaluation function
- collision filtering for simultaneous moves

Design notes are in:

- [alpha_beta_action_ordering.md](../docs/design_notes/alpha_beta_action_ordering.md)
- [alpha_beta_evaluation_function.md](../docs/design_notes/alpha_beta_evaluation_function.md)

## Build

Run the commands from the `alpha_beta_combat_agent` directory.

```powershell
New-Item -ItemType Directory -Force build | Out-Null
javac -cp "..\\third_party\\Sepia.jar" -d build (Get-ChildItem -Recurse "src\\*.java" | ForEach-Object { $_.FullName })
```

## Run

Two footmen vs one archer:

```powershell
java -cp "build;..\\third_party\\Sepia.jar;opponent_agents\\edu\\cwru\\sepia\\agent" edu.cwru.sepia.Main2 scenarios\\run_two_footmen_vs_one_archer.xml
```

Two footmen vs two archers:

```powershell
java -cp "build;..\\third_party\\Sepia.jar;opponent_agents\\edu\\cwru\\sepia\\agent" edu.cwru.sepia.Main2 scenarios\\run_two_footmen_vs_two_archers.xml
```
