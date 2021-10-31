package players.group1b;

import core.Game;
import core.GameState;
import utils.Types;
import players.group1b.Enemy;

import java.util.ArrayList;


public class BayesianNetwork {

    private GameState gs;
    private ArrayList enemies;

    public BayesianNetwork(GameState gs) {
        this.gs = gs;
        this.enemies = new ArrayList<>();
        findEnemies();
    }

    public ArrayList<Types.ACTIONS> actions() {

        ArrayList<Types.ACTIONS> actions = new ArrayList<Types.ACTIONS>();
        return actions;
    }

    private void findEnemies(){
        Types.TILETYPE[][] board = gs.getBoard();

        int boardSizeX = board.length;
        int boardSizeY = board[0].length;

        for (int x = 0; x < boardSizeX; x++) {
            for (int y = 0; y < boardSizeY; y++) {
                if (Types.TILETYPE.getAgentTypes().contains(board[y][x]) &&
                        board[y][x].getKey() != gs.getPlayerId()) {
                    Enemy enemy = new Enemy(gs, y, x);
                    enemies.add(enemy);
                }
            }
        }
    }
}
