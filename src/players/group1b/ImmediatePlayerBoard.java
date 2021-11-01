package players.group1b;

import core.GameState;
import utils.Types;
import utils.Vector2d;

public class ImmediatePlayerBoard {
    private Vector2d position;
    private GameState gs;
    private Types.TILETYPE[][] playerArea;

    public ImmediatePlayerBoard(Vector2d position, GameState gs){
        this.position = position.copy();
        this.gs = gs.copy();
        this.playerArea = getBoard();
    }

    private Types.TILETYPE[][] getBoard() {
        Types.TILETYPE[][] board = gs.getBoard();
        playerArea = new Types.TILETYPE[3][3];
        int boardSizeX = board.length;
        int boardSizeY = board[0].length;
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                int boardPositionX = position.x + x;
                int boardPositionY = position.y + y;
                int areaX = x + 1;
                int areaY = y + 1;
                // Rigid by default (in case it's outside the main board etc)
                playerArea[areaY][areaX] = Types.TILETYPE.RIGID;
                // Don't look outside the confines of the board (it'll crash). If it is, it'll default to rigid
                if(boardPositionX >= 0 && boardPositionX < boardSizeX && boardPositionY >= 0 && boardPositionY < boardSizeY){
                    Types.TILETYPE tileType = Types.TILETYPE.valueOf(board[boardPositionY][boardPositionX].name());
                    // Don't differentiate between other agent types to reduce the statistical space
                    if(tileType == Types.TILETYPE.AGENT0 || tileType == Types.TILETYPE.AGENT1 || tileType == Types.TILETYPE.AGENT2 || tileType == Types.TILETYPE.AGENT3){
                        tileType = Types.TILETYPE.AGENT0;
                    }
                    playerArea[areaY][areaX] = tileType;
                }
            }
        }
        // Central tile (ie the one the agent is always in) is always Agent 1 (to differentiate between an enemy)
        playerArea[1][1] = Types.TILETYPE.AGENT1;
        return playerArea;
    }

    public String getStateDescription() {
        String description = "";
        for (Types.TILETYPE tileType : Types.TILETYPE.values()) {
            // Never include these
            if(tileType == Types.TILETYPE.AGENT2 || tileType == Types.TILETYPE.AGENT3 || tileType == Types.TILETYPE.AGENTDUMMY){
                continue;
            }
            for(int x=0; x < playerArea.length; x++){
                for(int y=0; y < playerArea[0].length; y++){
                    String key = String.valueOf(x) + String.valueOf(y) + tileType;
                    description += key + (playerArea[y][x] == tileType ? "1" : "0");
                }
            }
        }
        return description;
    }
}
