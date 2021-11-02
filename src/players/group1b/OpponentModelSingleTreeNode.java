package players.group1b;

import core.GameState;
import objects.Bomb;
import objects.GameObject;
import players.heuristics.AdvancedHeuristic;
import players.heuristics.CustomHeuristic;
import players.heuristics.StateHeuristic;
import utils.ElapsedCpuTimer;
import utils.Types;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class OpponentModelSingleTreeNode
{
    public MCTSOpponentModelParams params;

    private OpponentModelSingleTreeNode parent;
    private OpponentModelSingleTreeNode[] children;
    private double totValue;
    private int nVisits;
    private Random m_rnd;
    private int m_depth;
    private double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    private int childIdx;
    private int fmCallsCount;

    private int num_actions;
    private Types.ACTIONS[] actions;

    private HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics;

    private ArrayList<Types.ACTIONS> badActions;

    private GameState rootState;
    private StateHeuristic rootStateHeuristic;

    OpponentModelSingleTreeNode(MCTSOpponentModelParams p, Random rnd, int num_actions, Types.ACTIONS[] actions, HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics) {
        this(p, null, -1, rnd, num_actions, actions, 0, null, parsedStatistics);
    }

    private OpponentModelSingleTreeNode(MCTSOpponentModelParams p, OpponentModelSingleTreeNode parent, int childIdx, Random rnd, int num_actions,
                           Types.ACTIONS[] actions, int fmCallsCount, StateHeuristic sh, HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics) {
        this.params = p;
        this.fmCallsCount = fmCallsCount;
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
        children = new OpponentModelSingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        this.parsedStatistics = parsedStatistics;
        this.badActions = new ArrayList<>();
        if(parent != null) {
            m_depth = parent.m_depth + 1;
            this.rootStateHeuristic = sh;
        }
        else
            m_depth = 0;
    }

    void setRootGameState(GameState gs)
    {
        this.rootState = gs;
        if (params.heuristic_method == params.CUSTOM_HEURISTIC)
            this.rootStateHeuristic = new CustomHeuristic(gs);
        else if (params.heuristic_method == params.ADVANCED_HEURISTIC) // New method: combined heuristics
            this.rootStateHeuristic = new AdvancedHeuristic(gs, m_rnd);
    }


    void mctsSearch(ElapsedCpuTimer elapsedTimer) {

        double avgTimeTaken;
        double acumTimeTaken = 0;
        long remaining;
        int numIters = 0;

        int remainingLimit = 5;
        boolean stop = false;

        while(!stop){

            GameState state = rootState.copy();
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            OpponentModelSingleTreeNode selected = treePolicy(state);
            double delta = selected.rollOut(state);
            backUp(selected, delta);

            //Stopping condition
//            if(params.stop_type == params.STOP_TIME) {
            if(true) {
                numIters++;
                acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
                avgTimeTaken  = acumTimeTaken/numIters;
                remaining = elapsedTimer.remainingTimeMillis();
                stop = remaining <= 2 * avgTimeTaken || remaining <= remainingLimit;
            }else if(params.stop_type == params.STOP_ITERATIONS) {
                numIters++;
                stop = numIters >= params.num_iterations;
            }else if(params.stop_type == params.STOP_FMCALLS)
            {
                fmCallsCount+=params.rollout_depth;
                stop = (fmCallsCount + params.rollout_depth) > params.num_fmcalls;
            }
        }
//        fmCallsCount+=params.rollout_depth;
////        fmCallsCount+=params.rollout_depth;
//        System.out.println("Our agent: " + fmCallsCount);
//        System.out.println("Our agent: " + numIters);

    }

    private OpponentModelSingleTreeNode treePolicy(GameState state) {

        OpponentModelSingleTreeNode cur = this;

        while (!state.isTerminal() && cur.m_depth < params.rollout_depth)
        {
            if (cur.notFullyExpanded()) {
                return cur.expand(state);

            } else {
                cur = cur.uct(state);
//                System.out.println("Our agent: " + cur.m_depth);
            }
        }

        return cur;
    }

    private ArrayList<Types.ACTIONS> actionsNotToConsider(GameState state){
        ArrayList<Types.ACTIONS> actions = new ArrayList<Types.ACTIONS>();
        ImmediatePlayerBoard playerBoard = new ImmediatePlayerBoard(state.getPosition(), state);
        Types.TILETYPE[][] board = playerBoard.getBoard();
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                if(board[y][x] == Types.TILETYPE.RIGID || board[y][x] == Types.TILETYPE.FLAMES || board[y][x] == Types.TILETYPE.WOOD){
                    if(y == 0 && x == 1){
                        actions.add(Types.ACTIONS.ACTION_UP);
                    }
                    if(y == 1 && x == 2){
                        actions.add(Types.ACTIONS.ACTION_RIGHT);
                    }
                    if(y == 2 && x == 2){
                        actions.add(Types.ACTIONS.ACTION_DOWN);
                    }
                    if(y == 1 && x == 0){
                        actions.add(Types.ACTIONS.ACTION_LEFT);
                    }
                }
            }
        }
        return actions;
    }

    private OpponentModelSingleTreeNode expand(GameState state) {
        if(badActions.size() == 0){
            badActions = actionsNotToConsider(state);
        }

        int bestAction = 1;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();

            if(badActions.contains(actions[i])){
            } else {
                if (x > bestValue && children[i] == null ) {
                    bestAction = i;
                    bestValue = x;
                }
            }

        }

        //Roll the state
        roll(state, actions[bestAction]);

        OpponentModelSingleTreeNode tn = new OpponentModelSingleTreeNode(params,this,bestAction,this.m_rnd,num_actions,
                actions, fmCallsCount, rootStateHeuristic, parsedStatistics);
        children[bestAction] = tn;
        return tn;
    }

    private void roll(GameState gs, Types.ACTIONS act)
    {
        //Simple, all random first, then my position.
        int nPlayers = 4;
        Types.ACTIONS[] actionsAll = new Types.ACTIONS[4];
        int playerId = gs.getPlayerId() - Types.TILETYPE.AGENT0.getKey();

//        ArrayList<Enemy> enemies = getEnemies(gs);
//        int enemyIndex = 0;
        for(int i = 0; i < nPlayers; ++i)
        {
            if(playerId == i)
            {
                actionsAll[i] = act;
            }else {
//                actionsAll[i] = enemies.get(enemyIndex).mostLikelyAction(gs, parsedStatistics);
//                System.out.println(actionsAll[i]);
//                enemyIndex += 1;
                // This is actually the only change in the whole MCTS tree search that we make
                // We assume that each enemy takes the action that is most likely from stats taken
                // from lots of plays of the game. It isn't perfect but it's better than random!
                // Below is what used to happen, it'd pick an action at random
                 int actionIdx = m_rnd.nextInt(gs.nActions());
                 actionsAll[i] = Types.ACTIONS.all().get(actionIdx);
            }
        }

        gs.next(actionsAll);

    }


//    private ArrayList<Enemy> getEnemies(GameState gs){
//        ArrayList<Enemy> enemies = new ArrayList<>();
//        Types.TILETYPE[][] board = gs.getBoard();
//        ArrayList<Types.TILETYPE> enemyIDs = gs.getAliveEnemyIDs();
//        int boardSizeX = board.length;
//        int boardSizeY = board[0].length;
//
//        for (int x = 0; x < boardSizeX; x++) {
//            for (int y = 0; y < boardSizeY; y++) {
//
//                if(Types.TILETYPE.getAgentTypes().contains(board[y][x]) &&
//                        board[y][x].getKey() != gs.getPlayerId()){ // May be an enemy
//                    if(enemyIDs.contains(board[y][x])) { // Is enemy
//                        // Create enemy object
//                        Enemy enemy = new Enemy(new Vector2d(x, y), m_rnd);
//                        enemies.add(enemy);
//                    }
//                }
//            }
//        }
//        // If an enemy is killed then they won't be on the board (so we'd have <3 in the array)
//        // thus, add them in but set them to be dead
//        while(enemies.size() < 3){
//            Enemy enemy = new Enemy(new Vector2d(0, 0), m_rnd);
//            enemy.setIsDead();
//            enemies.add(enemy);
//        }
//        return enemies;
//    }

    private OpponentModelSingleTreeNode uct(GameState state) {
        OpponentModelSingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (OpponentModelSingleTreeNode child : this.children)
        {
            if(child != null) {
                double hvVal = child.totValue;
                double childValue = hvVal / (child.nVisits + params.epsilon);

                childValue = Utils.normalise(childValue, bounds[0], bounds[1]);

                double uctValue = childValue +
                        params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + params.epsilon));

                uctValue = Utils.noise(uctValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly

                // small sampleRandom numbers: break ties in unexpanded nodes
                if (uctValue > bestValue) {
                    selected = child;
                    bestValue = uctValue;
                }
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
                    + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        roll(state, actions[selected.childIdx]);

        return selected;
    }

    private double rollOut(GameState state)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {
            int action = safeRandomAction(state);
            roll(state, actions[action]);
            thisDepth++;
        }

        return rootStateHeuristic.evaluateState(state);
    }

    private int safeRandomAction(GameState state)
    {
        Types.TILETYPE[][] board = state.getBoard();
        ArrayList<Types.ACTIONS> actionsToTry = Types.ACTIONS.all();
        int width = board.length;
        int height = board[0].length;

        while(actionsToTry.size() > 0) {

            int nAction = m_rnd.nextInt(actionsToTry.size());
            Types.ACTIONS act = actionsToTry.get(nAction);
            Vector2d dir = act.getDirection().toVec();

            Vector2d pos = state.getPosition();
            int x = pos.x + dir.x;
            int y = pos.y + dir.y;

            if (x >= 0 && x < width && y >= 0 && y < height)
                if(board[y][x] != Types.TILETYPE.FLAMES)
                    return nAction;

            actionsToTry.remove(nAction);
        }

        //Uh oh...
        return m_rnd.nextInt(num_actions);
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean finishRollout(GameState rollerState, int depth)
    {
        if (depth >= params.rollout_depth)      //rollout end condition.
            return true;

        if (rollerState.isTerminal())               //end of game
            return true;

        return false;
    }

    private void backUp(OpponentModelSingleTreeNode node, double result)
    {
        OpponentModelSingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }

        return selected;
    }

    private int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                double childValue = children[i].totValue / (children[i].nVisits + params.epsilon);
                childValue = Utils.noise(childValue, params.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    private boolean notFullyExpanded() {
        int childCount = 0;
        for (OpponentModelSingleTreeNode tn : children) {
//            if (tn == null) {
//                return true;
//            }
            if (tn != null) {
                childCount += 1;
            }
        }
        if(childCount + badActions.size() < 6){
            return true;
        }
        return false;
    }
}
