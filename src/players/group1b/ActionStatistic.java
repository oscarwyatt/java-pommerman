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
        ImmediatePlayerBoard playerBoard = new ImmediatePlayerBoard(gs.getPosition(), gs);
        Types.TILETYPE[][] playerArea = playerBoard.getBoard();
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
