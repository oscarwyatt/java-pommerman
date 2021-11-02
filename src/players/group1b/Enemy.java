package players.group1b;


import core.Game;
import core.GameState;
import utils.Types;
import utils.Vector2d;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Enemy {
    private Vector2d position;
    private Random m_rnd;
    private boolean dead;

    public Enemy(Vector2d position, Random rnd){
        this.dead = false;
        this.position = position;
        this.m_rnd = rnd;
    }

    //  If the enemy has been destroyed/killed
    public void setIsDead(){
        this.dead = true;
    }

    public Types.ACTIONS mostLikelyAction(GameState gs, HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics){
        // If the enemy has been destroyed, always return stop
        if(dead){
            return Types.ACTIONS.ACTION_STOP;
        }

        double highestProbability = -1;
        Types.ACTIONS mostLikelyAction = null;
        GameState gsCopy = gs.copy();
        ImmediatePlayerBoard playerBoard = new ImmediatePlayerBoard(position, gsCopy);
        ActionStatistic actionStatistics = new ActionStatistic(gsCopy);
        HashMap<Types.ACTIONS, Integer> statisticsForPosition = actionStatistics.statistics(playerBoard, parsedStatistics);
        ArrayList<Types.ACTIONS> actions = new ArrayList<>();
        if(statisticsForPosition.size() == 0){
            // We have no stats for the situation that the opponent is in, return a random move
            // (an alternative would be to return the globally most popular move);
//            System.out.println("Chosen at random");
            int actionIdx = m_rnd.nextInt(gs.nActions());
            return Types.ACTIONS.all().get(actionIdx);
        }
        // Find the most likely action
        for(Types.ACTIONS action : Types.ACTIONS.all()){
            double probability = prob(actionStatistics, statisticsForPosition, parsedStatistics, action);
            if(probability > highestProbability){
                highestProbability = probability;
                mostLikelyAction = action;
            }
        }

        return mostLikelyAction;
    }

    private double prob(ActionStatistic actionStatistics, HashMap<Types.ACTIONS, Integer> statisticsForPosition, HashMap<String, HashMap<Types.ACTIONS, Integer>> parsedStatistics, Types.ACTIONS action){
        double num_times_action_seen_in_board = 0;
        if(statisticsForPosition.containsKey(action)){
            num_times_action_seen_in_board = statisticsForPosition.get(action);
        }
        // the formula this is implementing is prob(action|board_around_the_player) = prob(board_around_the_player | action) * prob(action)
        //                                                                                 ------------------------------------------
        //                                                                                      prob(board_around_the_player)

        double total_num_action_seen = actionStatistics.totalNumberTimesActionSeen(action, parsedStatistics);
        double prob_board_given_action = num_times_action_seen_in_board / total_num_action_seen;
        double prob_action = total_num_action_seen / actionStatistics.totalNumberActions(parsedStatistics);
        //  TODO: This naively assumes that all boards are equally likely, in fact, some boards are more probable than others, account for that
        double prob_board = 1.0 / 495.0;
        return (prob_board_given_action * prob_action) / prob_board;
    }
}
