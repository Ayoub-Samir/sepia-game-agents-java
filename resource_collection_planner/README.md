# Resource Collection Planner

This project contains two SEPIA planning milestones for the resource collection assignment.

Official SEPIA documentation:
[SEPIA API docs](http://engr.case.edu/ray_soumya/Sepia/html/)

## Variants

### Task 1: Single Peasant

Located in [task_1_single_peasant](./task_1_single_peasant/).

- `SinglePeasantPlannerAgent.java`
  Executes a plan in SEPIA with one peasant.
- `ResourceCollectionPlannerCli.java`
  Runs the planner offline and prints the generated plan.

### Task 2: Multi Peasant

Located in [task_2_multi_peasant](./task_2_multi_peasant/).

- `MultiPeasantPlannerAgent.java`
  Executes aggregated multi-peasant plans in SEPIA.
- `ResourceCollectionPlannerCli.java`
  Prints the generated task-level plan.

## Scenarios

Each task folder includes renamed SEPIA runner configs under `scenarios/`.

Examples:

- `run_task_1_200_gold_200_wood.xml`
- `run_task_1_1000_gold_1000_wood_visual.xml`
- `run_task_2_3000_gold_2000_wood.xml`

## Build

Run the commands from the `resource_collection_planner` directory.

Task 1:

```powershell
New-Item -ItemType Directory -Force build\task_1_single_peasant | Out-Null
javac -cp "..\\third_party\\Sepia.jar" -d build\task_1_single_peasant (Get-ChildItem -Recurse "task_1_single_peasant\\src\\*.java" | ForEach-Object { $_.FullName })
```

Task 2:

```powershell
New-Item -ItemType Directory -Force build\task_2_multi_peasant | Out-Null
javac -cp "..\\third_party\\Sepia.jar" -d build\task_2_multi_peasant (Get-ChildItem -Recurse "task_2_multi_peasant\\src\\*.java" | ForEach-Object { $_.FullName })
```

## Run

Offline planner CLI example:

```powershell
java -cp "build\\task_2_multi_peasant;..\\third_party\\Sepia.jar" edu.cwru.sepia.agent.planner.ResourceCollectionPlannerCli 1000 1000
```

SEPIA runner example:

```powershell
java -cp "build\\task_2_multi_peasant;..\\third_party\\Sepia.jar" edu.cwru.sepia.Main2 task_2_multi_peasant\\scenarios\\run_task_2_1000_gold_1000_wood.xml
```

## Results

Preserved run summaries are stored in [docs/reports](../docs/reports/).
