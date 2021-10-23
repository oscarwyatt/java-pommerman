package players.group1b;

import core.GameState;
import utils.Types;
import utils.Vector2d;

import java.util.*;
import java.io.*;
import java.io.FileWriter;
import java.io.FileInputStream;

public class ActionStatistic {

    private GameState gs;

    public ActionStatistic(GameState gs)
    {
        this.gs = gs;
    }

    public void update(Types.ACTIONS actionTaken) {
        Types.TILETYPE[][] playerArea = getAreaAroundPlayer();
        String stateKey = getStateDescription(playerArea);
        updateStatistics(stateKey, actionTaken);
    }

    private Types.TILETYPE[][] getAreaAroundPlayer(){
        Vector2d position = gs.getPosition();
        Types.TILETYPE[][] board = gs.getBoard();
        Types.TILETYPE[][] playerArea = new Types.TILETYPE[3][3];
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
                    Types.TILETYPE tileType = board[boardPositionY][boardPositionX];
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

    private String getStateDescription(Types.TILETYPE[][] playerArea){
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

    private void updateStatistics(String stateDescription, Types.ACTIONS actionTaken){
        String filepath = "stats/stats.txt";
        File file = new File(filepath);
        if(!file.isFile() ) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("An error occurred creating stats file.");
                e.printStackTrace();
            }
        }
        // Update the stats
        String stateStatistics = readFile(file);
        HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStats = parseStatistics(stateStatistics);
        allStateStats = updateStatistics(allStateStats, stateDescription, actionTaken);
        saveFile(filepath, allStateStats);
    }

    private String readFile(File stateFile) {
        String stateStatistics = "";
        try {
            FileInputStream inputStream = new FileInputStream(stateFile);
            // TODO: There _has_ to be a better way to do this!
            int chr=0;
            while((chr=inputStream.read())!=-1){
                stateStatistics += Character.toString(chr);
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return stateStatistics;
    }

    private HashMap<String, HashMap<Types.ACTIONS, Integer>> parseStatistics(String stateStatistics){
        HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStats = new HashMap<>();
        if(stateStatistics.length() == 0){
            return allStateStats;
        }
        String[] states = stateStatistics.split("\n");
        for (String state : states) {
            String[] rowData = state.split(",");
            // the state description should be the first entry
            String thisStateDescription = rowData[0];
            String[] rows = Arrays.copyOfRange(rowData, 1, rowData.length);
            HashMap<Types.ACTIONS, Integer> stateStats = new HashMap<>();
            for (String row : rows) {
                String[] rowStats = row.split(":");
                if(rowStats.length == 2){
                    Types.ACTIONS action = Types.ACTIONS.valueOf(rowStats[0]);
                    Integer count = Integer.valueOf(rowStats[1]);
                    stateStats.put(action, count);
                }
            }
            allStateStats.put(thisStateDescription, stateStats);
        }
        return allStateStats;
    }

    private HashMap<String, HashMap<Types.ACTIONS, Integer>> updateStatistics(HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStatistics, String stateDescription, Types.ACTIONS actionTaken){
        HashMap<Types.ACTIONS, Integer> stateStatistics = new HashMap<>();
        if(allStateStatistics.containsKey(stateDescription)){
            stateStatistics = allStateStatistics.get(stateDescription);
        }
        Integer actionTakenCount = 0;
        if(stateStatistics.containsKey(actionTaken)){
            actionTakenCount = stateStatistics.get(actionTaken);
        }
        actionTakenCount += 1;
        stateStatistics.put(actionTaken, actionTakenCount);
        allStateStatistics.put(stateDescription, stateStatistics);
        return allStateStatistics;
    }

    private void saveFile(String filepath, HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStatistics){
        String actionData = "";
        for (HashMap.Entry<String, HashMap<Types.ACTIONS, Integer>> state : allStateStatistics.entrySet()){
            String stateDescription = state.getKey();
            HashMap<Types.ACTIONS, Integer> stateStatistics = state.getValue();
            actionData += stateDescription + ",";
            for (HashMap.Entry<Types.ACTIONS, Integer> statistics : stateStatistics.entrySet()){
                actionData += String.valueOf(statistics.getKey()) + ":" + String.valueOf(statistics.getValue()) + ",";
            }
            actionData += "\n";
        }
        try {
            FileWriter writer = new FileWriter(filepath);
            writer.write(actionData);
            writer.close();
        }  catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
