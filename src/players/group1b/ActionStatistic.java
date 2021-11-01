package players.group1b;

import core.GameState;
import utils.Types;
import java.util.*;
import java.io.*;
import java.io.FileWriter;
import java.io.FileInputStream;

public class ActionStatistic {

    private GameState gs;


    public ActionStatistic(GameState gs)
    {
        this.gs = gs.copy();
    }

    public void update(Types.ACTIONS actionTaken) {
        ImmediatePlayerBoard playerBoard = new ImmediatePlayerBoard(gs.getPosition(), gs);
        String stateKey = playerBoard.getStateDescription();
        updateStatistics(stateKey, actionTaken);
    }


    public HashMap<Types.ACTIONS, Integer> statistics(ImmediatePlayerBoard playerBoard, HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStatistics){
        String stateDescription = playerBoard.getStateDescription();
        HashMap<Types.ACTIONS, Integer> stateStatistics = new HashMap<>();
        if(allStateStatistics.containsKey(stateDescription)){
            stateStatistics = allStateStatistics.get(stateDescription);
        }
        return stateStatistics;
    }

    public double totalNumberTimesActionSeen(Types.ACTIONS action, HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics){
        double total = 0;
        for (HashMap.Entry<String, HashMap<Types.ACTIONS, Integer>> state : parsedStatistics.entrySet()){
            HashMap<Types.ACTIONS, Integer> stateStatistics = state.getValue();
            if (stateStatistics.containsKey(action)){
                total += stateStatistics.get(action);
            }
        }
        return total;
    }

    public double totalNumberActions(HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics){
        double total = 0;
        for (HashMap.Entry<String, HashMap<Types.ACTIONS, Integer>> state : parsedStatistics.entrySet()){
            HashMap<Types.ACTIONS, Integer> stateStatistics = state.getValue();
            for(Integer count  : stateStatistics.values()) {
                total += count;
            }
        }
        return total;
    }

    private static String statisicsFilePath() {
        return "stats/stats.txt";
    }

    public static HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatisticsFile() {
        File file = new File(ActionStatistic.statisicsFilePath());
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
        return parseStatistics(stateStatistics);
    }

    private void updateStatistics(String stateDescription, Types.ACTIONS actionTaken){
        HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStats = parsedStatisticsFile();
        allStateStats = updateStatistics(allStateStats, stateDescription, actionTaken);
        saveFile(allStateStats);
    }

    private static String readFile(File stateFile) {
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

    private static HashMap<String, HashMap<Types.ACTIONS, Integer>> parseStatistics(String stateStatistics){
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

    private void saveFile(HashMap<String, HashMap<Types.ACTIONS, Integer>> allStateStatistics){
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
            FileWriter writer = new FileWriter(statisicsFilePath());
            writer.write(actionData);
            writer.close();
        }  catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
