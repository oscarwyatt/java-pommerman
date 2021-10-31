package players.group1b;


import core.GameState;
import utils.Types;
import utils.Vector2d;

import java.util.ArrayList;

public class Enemy {
    private Types.TILETYPE[][] surroundingBoard;
    private GameState gs;
    private int x;
    private int y;

    public Enemy(GameState gs, int y, int x) {
        this.gs = gs;
        this.y = y;
        this.x = x;
        populateSurroundingBoard();
    }

    public ArrayList<Types.ACTIONS> likelyActions() {
        ArrayList<Types.ACTIONS> actions = new ArrayList<>();



        return actions;
    }

    private void populateSurroundingBoard() {
        Vector2d position = new Vector2d(x, y);
        ImmediatePlayerBoard playerBoard = new ImmediatePlayerBoard(position, gs);
        this.surroundingBoard = playerBoard.getBoard();
    }
}
