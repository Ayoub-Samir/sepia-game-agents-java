package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate.UnitTemplateView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Alpha-beta agent for the two-player Footmen vs. Archer scenarios.
 * Uses ordering heuristics + evaluation function + fixed collision + fixed damage.
 */
public class AlphaBetaCombatAgent extends Agent {

    private final int maxDepth;
    private final int enemyId = 1;
    private long totalElapsedNanos = 0;
    private int stepCount = 0;

    private final Map<Integer, UnitStats> templateStats = new HashMap<>();

    public AlphaBetaCombatAgent(int playernum, String[] otherargs) {
        super(playernum);
        int depth = 3;
        if (otherargs != null && otherargs.length > 0) {
            try {
                depth = Integer.parseInt(otherargs[0]);
            } catch (NumberFormatException ignored) { }
        }
        this.maxDepth = Math.max(1, depth);
        System.out.println("AlphaBetaCombatAgent constructed. Depth = " + this.maxDepth);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate,
                                            edu.cwru.sepia.environment.model.history.History.HistoryView statehistory) {
        cacheTemplateStats(newstate);
        totalElapsedNanos = 0;
        stepCount = 0;
        return new HashMap<>();
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate,
                                           edu.cwru.sepia.environment.model.history.History.HistoryView statehistory) {

        long start = System.nanoTime();
        cacheTemplateStats(newstate);

        GameState root = GameState.fromStateView(newstate, playernum, enemyId, templateStats);
        if (root.isTerminal()) {
            stepCount++;
            return new HashMap<>();
        }

        JointAction best = chooseBestAction(root);
        if (best == null) {
            stepCount++;
            return new HashMap<>();
        }

        Map<Integer, Action> actions = new HashMap<>();
        for (AtomicAction aa : best.actions) {
            switch (aa.type) {
                case ATTACK:
                    actions.put(aa.unitId, Action.createPrimitiveAttack(aa.unitId, aa.targetId));
                    break;
                case MOVE:
                    Direction d = directionFromDelta(aa.dx, aa.dy);
                    actions.put(aa.unitId, Action.createPrimitiveMove(aa.unitId, d));
                    break;
                default:

                    break;
            }
        }
        long end = System.nanoTime();
        totalElapsedNanos += (end - start);
        stepCount++;
        System.out.println("AlphaBetaCombatAgent depth " + maxDepth + " middleStep took " + ((end - start) / 1_000_000.0) + " ms");
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate,
                             edu.cwru.sepia.environment.model.history.History.HistoryView statehistory) {
        double totalMs = totalElapsedNanos / 1_000_000.0;
        System.out.println("AlphaBetaCombatAgent finished episode. Steps: " + stepCount + " Total time: " + totalMs + " ms Avg: " + (stepCount == 0 ? 0 : totalMs / stepCount) + " ms");
    }

    @Override
    public void savePlayerData(OutputStream os) { }

    @Override
    public void loadPlayerData(InputStream is) { }

    private void cacheTemplateStats(State.StateView state) {
        for (int playerId : state.getPlayerNumbers()) {
            for (int unitId : state.getUnitIds(playerId)) {
                UnitView uv = state.getUnit(unitId);
                UnitTemplateView tv = (UnitTemplateView) uv.getTemplateView();
                int templateId = tv.getID();
                if (!templateStats.containsKey(templateId)) {
                    templateStats.put(templateId, new UnitStats(
                            tv.getRange(),
                            tv.getBasicAttack(),
                            tv.getPiercingAttack(),
                            tv.getArmor()));
                }
            }
        }
    }




    private JointAction chooseBestAction(GameState root) {

        double alpha = Double.NEGATIVE_INFINITY;
        double beta  = Double.POSITIVE_INFINITY;
        double bestScore = Double.NEGATIVE_INFINITY;
        JointAction best = null;


        List<ScoredJointAction> scored = new ArrayList<>();
        List<JointAction> actions = generateJointActions(root, true);

        for (JointAction ja : actions) {
            int s = scoreJointAction(ja, root, true);
            scored.add(new ScoredJointAction(ja, s));
        }


        scored.sort((a, b) -> {
            if (b.score != a.score)
                return b.score - a.score;
            return Boolean.compare(containsAttack(b.action), containsAttack(a.action));
        });

        for (ScoredJointAction sja : scored) {
            JointAction ja = sja.action;
            GameState next = applyJointAction(root, ja, true);

            double val = alphaBeta(next, maxDepth - 1, false, alpha, beta);

            if (val > bestScore) {
                bestScore = val;
                best = ja;
            }

            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) break;
        }
        return best;
    }

    private boolean containsAttack(JointAction ja) {
        for (AtomicAction aa : ja.actions)
            if (aa.type == AtomicActionType.ATTACK)
                return true;
        return false;
    }

    private double alphaBeta(GameState state, int depth, boolean maximizing, double alpha, double beta) {

        if (depth == 0 || state.isTerminal()) {
            return evaluate(state);
        }

        if (maximizing) {
            double value = Double.NEGATIVE_INFINITY;


            List<ScoredJointAction> scored = new ArrayList<>();
            List<JointAction> actions = generateJointActions(state, true);
            for (JointAction ja : actions)
                scored.add(new ScoredJointAction(ja, scoreJointAction(ja, state, true)));

            scored.sort((a, b) -> b.score - a.score);

            for (ScoredJointAction sja : scored) {
                GameState child = applyJointAction(state, sja.action, true);
                value = Math.max(value, alphaBeta(child, depth - 1, false, alpha, beta));

                alpha = Math.max(alpha, value);
                if (beta <= alpha) break;
            }
            return value;

        } else {
            double value = Double.POSITIVE_INFINITY;


            List<ScoredJointAction> scored = new ArrayList<>();
            List<JointAction> actions = generateJointActions(state, false);
            for (JointAction ja : actions)
                scored.add(new ScoredJointAction(ja, scoreJointAction(ja, state, false)));

            scored.sort((a, b) -> b.score - a.score);

            for (ScoredJointAction sja : scored) {
                GameState child = applyJointAction(state, sja.action, false);
                value = Math.min(value, alphaBeta(child, depth - 1, true, alpha, beta));

                beta = Math.min(beta, value);
                if (beta <= alpha) break;
            }
            return value;
        }
    }





    private double evaluate(GameState state) {

        double minDist        = state.minDistanceFootmanToArcher();
        double totalDist      = state.totalDistanceFootmenToArchers();
        double totalArcherHP  = state.totalArcherHealth();
        double totalFootHP    = state.totalFootmanHealth();
        double archersAlive   = state.archers.size();
        double nextTurnThreat = state.footmenInAttackRangeNextTurn();
        double adjacentNow    = state.footmenAdjacentNow();
        double mobility       = state.archerMobility();
        double spread         = state.footmenSpread();



        return
                -500 * archersAlive +
                -35  * totalArcherHP +
                 20  * totalFootHP +
                // -45  * minDist +  // dont use this
                -200 * totalDist +
                +220 * adjacentNow;
                // +120 * nextTurnThreat +  // dont use this
                // -25  * mobility + // dont use this
                // -30  * spread;  // dont use this
    }





    private List<JointAction> generateJointActions(GameState state, boolean footmenTurn) {

        List<UnitState> movers    = footmenTurn ? state.footmen : state.archers;
        List<UnitState> opponents = footmenTurn ? state.archers : state.footmen;

        List<List<AtomicAction>> perUnit = new ArrayList<>();
        Set<Position> occupied = state.occupiedPositions();

        for (UnitState u : movers) {


            if (u.hp <= 0) continue;

            List<AtomicAction> actions = new ArrayList<>();
            actions.addAll(generateAttackActions(u, opponents));
            actions.addAll(generateMoveActions(u, occupied, state.xExtent, state.yExtent));


            if (actions.isEmpty()) {
                actions.add(AtomicAction.wait(u));
            }
            perUnit.add(actions);
        }


        List<JointAction> all = new ArrayList<>();
        buildJointActions(perUnit, 0, new ArrayList<>(), all);


        List<JointAction> filtered = new ArrayList<>();
        for (JointAction ja : all) {
            if (!collides(ja)) filtered.add(ja);
        }
        return filtered;
    }



    private void buildJointActions(List<List<AtomicAction>> perUnit,
                                   int idx,
                                   List<AtomicAction> current,
                                   List<JointAction> results) {
        if (idx == perUnit.size()) {
            results.add(new JointAction(new ArrayList<>(current)));
            return;
        }
        for (AtomicAction aa : perUnit.get(idx)) {
            current.add(aa);
            buildJointActions(perUnit, idx + 1, current, results);
            current.remove(current.size() - 1);
        }
    }





    private boolean collides(JointAction ja) {

        Map<Integer, Position> start = new HashMap<>();
        Map<Integer, Position> end   = new HashMap<>();

        for (AtomicAction aa : ja.actions) {

            start.put(aa.unitId, new Position(aa.startX, aa.startY));

            if (aa.type == AtomicActionType.MOVE)
                end.put(aa.unitId, new Position(aa.newX, aa.newY));
            else
                end.put(aa.unitId, new Position(aa.startX, aa.startY));
        }


        Set<Position> used = new HashSet<>();
        for (Position p : end.values()) {
            if (!used.add(p)) return true;
        }


        for (Integer id1 : start.keySet()) {
            for (Integer id2 : start.keySet()) {
                if (!id1.equals(id2)) {
                    Position s1 = start.get(id1);
                    Position e1 = end.get(id1);
                    Position s2 = start.get(id2);
                    Position e2 = end.get(id2);

                    if (s1.equals(e2) && s2.equals(e1))
                        return true;
                }
            }
        }
        return false;
    }








    private List<AtomicAction> generateAttackActions(UnitState unit, List<UnitState> opponents) {
        List<AtomicAction> actions = new ArrayList<>();

        for (UnitState opp : opponents) {
            if (opp.hp <= 0) continue;

            if (unit.distanceTo(opp) <= unit.range) {
                actions.add(AtomicAction.attack(unit, opp));
            }
        }
        return actions;
    }

    private List<AtomicAction> generateMoveActions(UnitState unit,
                                                   Set<Position> occupied,
                                                   int xExtent,
                                                   int yExtent) {

        List<AtomicAction> moves = new ArrayList<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};

        for (int[] d : dirs) {
            int nx = unit.x + d[0];
            int ny = unit.y + d[1];

            if (nx < 0 || ny < 0 || nx >= xExtent || ny >= yExtent)
                continue;

            Position np = new Position(nx, ny);
            if (!occupied.contains(np)) {
                moves.add(AtomicAction.move(unit, nx, ny, d[0], d[1]));
            }
        }
        return moves;
    }





    private GameState applyJointAction(GameState state, JointAction ja, boolean footmenTurn) {

        GameState next = state.copy();

        List<UnitState> movers   = footmenTurn ? next.footmen  : next.archers;
        List<UnitState> targets  = footmenTurn ? next.archers  : next.footmen;

        Map<Integer, UnitState> mob = new HashMap<>();
        for (UnitState u : movers)
            mob.put(u.id, u);


        for (AtomicAction aa : ja.actions) {
            if (aa.type == AtomicActionType.MOVE) {
                UnitState u = mob.get(aa.unitId);
                if (u != null) {
                    u.x = aa.newX;
                    u.y = aa.newY;
                }
            }
        }


        Map<Integer, UnitState> footmap = new HashMap<>();
        for (UnitState f : next.footmen) footmap.put(f.id, f);

        Map<Integer, UnitState> archmap = new HashMap<>();
        for (UnitState a : next.archers) archmap.put(a.id, a);


        for (AtomicAction aa : ja.actions) {
            if (aa.type == AtomicActionType.ATTACK) {

                UnitState attacker = mob.get(aa.unitId);

                UnitState target = footmenTurn
                        ? archmap.get(aa.targetId)
                        : footmap.get(aa.targetId);

                if (attacker != null && target != null && target.hp > 0) {


                    int dmg = Math.max(0, attacker.basic - target.armor) + attacker.piercing;

                    target.hp -= dmg;
                }
            }
        }

        next.pruneDead();
        return next;
    }





    private int scoreJointAction(JointAction ja, GameState state, boolean footmenTurn) {
        int s = 0;
        for (AtomicAction aa : ja.actions) {
            if (footmenTurn)
                s += scoreFootmanAction(aa, state);
            else
                s += scoreArcherAction(aa, state);
        }
        return s;
    }

    private int scoreFootmanAction(AtomicAction aa, GameState state) {

        if (aa.type == AtomicActionType.ATTACK)
            return 2000;

        UnitState foot = state.findUnit(aa.unitId);
        if (foot == null) return 0;

        int oldD = state.distanceToClosestArcher(foot.x, foot.y);
        int newD = state.distanceToClosestArcher(aa.newX, aa.newY);

        int closer = Math.max(0, oldD - newD);
        int farther = Math.max(0, newD - oldD);

        boolean retreat = newD > oldD;

        int score = 0;
        score += 600 * closer;
        score -= 600 * farther;
        if (oldD == newD) score += 20;
        if (retreat) score -= 3000;

        return score;
    }








    private int scoreArcherAction(AtomicAction aa, GameState state) {

        UnitState archer = state.findUnit(aa.unitId);
        if (archer == null) return 0;

        int oldDist = state.distanceToClosestFootman(archer.x, archer.y);

        int newX = (aa.type == AtomicActionType.MOVE) ? aa.newX : archer.x;
        int newY = (aa.type == AtomicActionType.MOVE) ? aa.newY : archer.y;

        int newDist = state.distanceToClosestFootman(newX, newY);

        int farther = Math.max(0, newDist - oldDist);
        int closer  = Math.max(0, oldDist - newDist);
        boolean danger = newDist <= 2;
        boolean neutral = newDist == oldDist;

        int oldMob = state.mobilityAt(archer.x, archer.y, state.occupiedPositionsExcluding(archer.id));
        int newMob = state.mobilityAt(newX, newY, state.occupiedPositionsExcluding(archer.id));

        int mobGain = Math.max(0, newMob - oldMob);
        int mobLoss = Math.max(0, oldMob - newMob);

        int score = 0;

        if (danger) {

            score += 2000;
            score += 60 * mobGain;
            score -= 60 * mobLoss;
            score += 600 * farther;
            score -= 700 * closer;

            if (aa.type == AtomicActionType.ATTACK)
                score += 200;

        } else {

            if (aa.type == AtomicActionType.ATTACK && newDist >= 3)
                score += 2000;

            score += 50 * mobGain;
            score -= 50 * mobLoss;

            if (neutral) score += 10;
            score += 400 * farther;
            score -= 250 * closer;
        }

        return score;
    }

    private Direction directionFromDelta(int dx, int dy) {
        if (dx == 1 && dy == 0) return Direction.EAST;
        if (dx == -1 && dy == 0) return Direction.WEST;
        if (dx == 0 && dy == 1) return Direction.SOUTH;
        return Direction.NORTH;
    }





    private static class GameState {

        final List<UnitState> footmen;
        final List<UnitState> archers;
        final int xExtent;
        final int yExtent;

        GameState(List<UnitState> footmen, List<UnitState> archers, int xExtent, int yExtent) {
            this.footmen = footmen;
            this.archers = archers;
            this.xExtent = xExtent;
            this.yExtent = yExtent;
        }

        static GameState fromStateView(State.StateView sv,
                                       int footPlayer,
                                       int archerPlayer,
                                       Map<Integer, UnitStats> stats) {

            List<UnitState> f = new ArrayList<>();
            for (int id : sv.getUnitIds(footPlayer)) {
                UnitView uv = sv.getUnit(id);
                UnitStats s = stats.get(uv.getTemplateView().getID());
                f.add(UnitState.fromView(uv, s, true));
            }

            List<UnitState> a = new ArrayList<>();
            for (int id : sv.getUnitIds(archerPlayer)) {
                UnitView uv = sv.getUnit(id);
                UnitStats s = stats.get(uv.getTemplateView().getID());
                a.add(UnitState.fromView(uv, s, false));
            }

            return new GameState(f, a, sv.getXExtent(), sv.getYExtent());
        }

        boolean isTerminal() {
            return footmen.isEmpty() || archers.isEmpty();
        }

        GameState copy() {
            List<UnitState> f = new ArrayList<>();
            for (UnitState u : footmen) f.add(u.copy());
            List<UnitState> a = new ArrayList<>();
            for (UnitState u : archers) a.add(u.copy());
            return new GameState(f, a, xExtent, yExtent);
        }

        void pruneDead() {
            footmen.removeIf(u -> u.hp <= 0);
            archers.removeIf(u -> u.hp <= 0);
        }

        UnitState findUnit(int id) {
            for (UnitState u : footmen) if (u.id == id) return u;
            for (UnitState u : archers) if (u.id == id) return u;
            return null;
        }

        Set<Position> occupiedPositions() {
            Set<Position> s = new HashSet<>();
            for (UnitState f : footmen) s.add(new Position(f.x, f.y));
            for (UnitState a : archers) s.add(new Position(a.x, a.y));
            return s;
        }

        Set<Position> occupiedPositionsExcluding(int id) {
            Set<Position> s = new HashSet<>();
            for (UnitState f : footmen) if (f.id != id) s.add(new Position(f.x, f.y));
            for (UnitState a : archers) if (a.id != id) s.add(new Position(a.x, a.y));
            return s;
        }





        double totalDistanceFootmenToArchers() {
            double sum = 0;
            for (UnitState f : footmen)
                for (UnitState a : archers) {
                    sum += Math.abs(f.x - a.x) + Math.abs(f.y - a.y);
                }
            return sum;
        }

        double minDistanceFootmanToArcher() {
            double best = Double.POSITIVE_INFINITY;
            for (UnitState f : footmen)
                for (UnitState a : archers) {
                    best = Math.min(best, Math.abs(f.x - a.x) + Math.abs(f.y - a.y));
                }
            return (footmen.isEmpty() || archers.isEmpty()) ? 0 : best;
        }

        double totalArcherHealth() {
            double hp = 0;
            for (UnitState a : archers) hp += a.hp;
            return hp;
        }

        double totalFootmanHealth() {
            double hp = 0;
            for (UnitState f : footmen) hp += f.hp;
            return hp;
        }

        double footmenInAttackRangeNextTurn() {
            int cnt = 0;
            for (UnitState f : footmen) {
                for (UnitState a : archers) {
                    if (f.distanceTo(a) <= 2) {
                        cnt++;
                        break;
                    }
                }
            }
            return cnt;
        }

        double footmenAdjacentNow() {
            int cnt = 0;
            for (UnitState f : footmen)
                for (UnitState a : archers)
                    if (f.distanceTo(a) <= 1) { cnt++; break; }
            return cnt;
        }

        double archerMobility() {
            double sum = 0;
            Set<Position> occ = occupiedPositions();
            for (UnitState a : archers)
                sum += mobilityAt(a.x, a.y, occ);
            return sum;
        }

        double footmenPressure() {
            int cnt = 0;
            for (UnitState f : footmen)
                for (UnitState a : archers)
                    if (f.distanceTo(a) <= 2) { cnt++; break; }
            return cnt;
        }

        double distanceToArcherCentroid() {
            if (archers.isEmpty() || footmen.isEmpty())
                return 0;

            double cx = 0, cy = 0;
            for (UnitState a : archers) {
                cx += a.x;
                cy += a.y;
            }
            cx /= archers.size();
            cy /= archers.size();

            double sum = 0;
            for (UnitState f : footmen) {
                sum += Math.abs(f.x - cx) + Math.abs(f.y - cy);
            }

            return sum;
        }

        double footmenSpread() {
            if (footmen.size() <= 1) {
                return 0;
            }
            double sum = 0;
            for (int i = 0; i < footmen.size(); i++) {
                for (int j = i + 1; j < footmen.size(); j++) {
                    sum += footmen.get(i).distanceTo(footmen.get(j));
                }
            }
            return sum;
        }

        int distanceToClosestArcher(int x, int y) {
            int best = Integer.MAX_VALUE;
            for (UnitState a : archers) {
                int d = Math.abs(a.x - x) + Math.abs(a.y - y);
                if (d < best) best = d;
            }
            return archers.isEmpty() ? 0 : best;
        }

        int distanceToClosestFootman(int x, int y) {
            int best = Integer.MAX_VALUE;
            for (UnitState f : footmen) {
                int d = Math.abs(f.x - x) + Math.abs(f.y - y);
                if (d < best) best = d;
            }
            return footmen.isEmpty() ? 0 : best;
        }

        int mobilityAt(int x, int y, Set<Position> occupied) {
            int moves = 0;
            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx >= 0 && ny >= 0 && nx < xExtent && ny < yExtent) {
                    if (!occupied.contains(new Position(nx, ny))) {
                        moves++;
                    }
                }
            }
            return moves;
        }
    }





    private static class UnitState {

        final int id;
        int x, y;
        int hp;
        final int range;
        final int basic;
        final int piercing;
        final int armor;
        final boolean isFootman;

        UnitState(int id, int x, int y, int hp,
                  int range, int basic, int piercing, int armor,
                  boolean isFootman) {

            this.id = id;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.range = range;
            this.basic = basic;
            this.piercing = piercing;
            this.armor = armor;
            this.isFootman = isFootman;
        }

        static UnitState fromView(Unit.UnitView uv, UnitStats stats, boolean isFootman) {
            return new UnitState(
                    uv.getID(),
                    uv.getXPosition(),
                    uv.getYPosition(),
                    uv.getHP(),
                    stats.range,
                    stats.basicAttack,
                    stats.piercingAttack,
                    stats.armor,
                    isFootman
            );
        }

        UnitState copy() {
            return new UnitState(id, x, y, hp, range, basic, piercing, armor, isFootman);
        }

        int distanceTo(UnitState other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y);
        }
    }

    private static class Position {
        final int x, y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Position)) return false;
            Position p = (Position)o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return (31 * x) ^ y;
        }
    }

    private static class JointAction {
        final List<AtomicAction> actions;
        JointAction(List<AtomicAction> actions) {
            this.actions = actions;
        }
    }

    private enum AtomicActionType { MOVE, ATTACK, WAIT }

    private static class AtomicAction {

        final AtomicActionType type;
        final int unitId;
        final int targetId;

        final int startX, startY;
        final int newX, newY;
        final int dx, dy;

        private AtomicAction(AtomicActionType type,
                             int unitId, int targetId,
                             int startX, int startY,
                             int newX, int newY,
                             int dx, int dy) {

            this.type = type;
            this.unitId = unitId;
            this.targetId = targetId;
            this.startX = startX;
            this.startY = startY;
            this.newX = newX;
            this.newY = newY;
            this.dx = dx;
            this.dy = dy;
        }

        static AtomicAction move(UnitState u, int nx, int ny, int dx, int dy) {
            return new AtomicAction(
                    AtomicActionType.MOVE,
                    u.id, -1,
                    u.x, u.y,
                    nx, ny,
                    dx, dy
            );
        }

        static AtomicAction attack(UnitState attacker, UnitState target) {
            return new AtomicAction(
                    AtomicActionType.ATTACK,
                    attacker.id,
                    target.id,
                    attacker.x, attacker.y,
                    attacker.x, attacker.y,
                    0, 0
            );
        }

        static AtomicAction wait(UnitState u) {
            return new AtomicAction(
                    AtomicActionType.WAIT,
                    u.id,
                    -1,
                    u.x, u.y,
                    u.x, u.y,
                    0, 0
            );
        }
    }

    private static class UnitStats {
        final int range;
        final int basicAttack;
        final int piercingAttack;
        final int armor;

        UnitStats(int range, int basicAttack, int piercingAttack, int armor) {
            this.range = range;
            this.basicAttack = basicAttack;
            this.piercingAttack = piercingAttack;
            this.armor = armor;
        }
    }





    private static class ScoredJointAction {
        final JointAction action;
        final int score;
        ScoredJointAction(JointAction a, int s) { this.action = a; this.score = s; }
    }

}
