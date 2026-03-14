# SEPIA Game Agents in Java

Java implementations of game-playing agents built for the SEPIA RTS environment.

This repository contains two main projects:

- `resource_collection_planner`
  A forward-search planner for SEPIA resource collection scenarios, with separate single-peasant and multi-peasant task variants.
- `alpha_beta_combat_agent`
  A minimax agent with alpha-beta pruning for footmen-vs-archers combat scenarios.


## Repository Structure

```text
.
|-- README.md
|-- alpha_beta_combat_agent
|-- docs
|-- resource_collection_planner
|-- third_party
|   `-- Sepia.jar
`-- local_artifacts
```

## Projects

### Resource Collection Planner

Located in [resource_collection_planner](./resource_collection_planner/).

- `task_1_single_peasant`
  A single-worker A* planner over high-level movement, harvest, and deposit states.
- `task_2_multi_peasant`
  A higher-level planner that batches workers, models build-peasant actions, and reduces makespan through parallel collection.

### Alpha-Beta Combat Agent

Located in [alpha_beta_combat_agent](./alpha_beta_combat_agent/).

- `AlphaBetaCombatAgent`
  Uses fixed-depth minimax with alpha-beta pruning, heuristic move ordering, collision filtering, and a hand-tuned evaluation function.

## Documentation

Supporting material is organized under [docs](./docs/):

- reference SEPIA documentation
- preserved report files from the assignment submissions
- short design notes for the combat agent's evaluation and ordering heuristics
- official online SEPIA documentation: [SEPIA API docs](http://engr.case.edu/ray_soumya/Sepia/html/)

## Dependency

The SEPIA library is bundled at [third_party/Sepia.jar](./third_party/Sepia.jar).
The official online documentation is available at [http://engr.case.edu/ray_soumya/Sepia/html/](http://engr.case.edu/ray_soumya/Sepia/html/).

## Notes

