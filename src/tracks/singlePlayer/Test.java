package tracks.singlePlayer;

import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;

import core.logging.Logger;
import tools.Utils;
import tracks.ArcadeMachine;

//used for game initialisation
import core.game.Game;
import core.game.ForwardModel;
import ontology.Types;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Test {

    public static void main(String[] args) {

		// Available tracks:
		String sampleRandomController = "tracks.singlePlayer.simple.sampleRandom.Agent";
		String doNothingController = "tracks.singlePlayer.simple.doNothing.Agent";
		String sampleOneStepController = "tracks.singlePlayer.simple.sampleonesteplookahead.Agent";
		String sampleFlatMCTSController = "tracks.singlePlayer.simple.greedyTreeSearch.Agent";

		String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
        String sampleRSController = "tracks.singlePlayer.advanced.sampleRS.Agent";
        String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
		String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";

		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);
		//The games that we have to test with
		int[] gameIndex = {8, 10, 18, 45};

		//Game settings
		boolean visuals = true;
		//Set the seed to a single value as Chaser NPC's are random >_>
		int seed = new Random().nextInt();
		seed = -131244659;
		System.out.println("This is the random seed, it's set due to randomness in Chaser NPC's " + seed);

		// Game and level to play
		int gameIdx = 46;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// where to record the actions
						// executed. null if not to save.

		ForwardModel gameState = ArcadeMachine.gameInt(game, level1, visuals, sampleRHEAController, recordActionsFile, seed, 0);
		ForwardModel gameStateCopy = gameState;

		// List of actions that you can do during the game
		ArrayList<Types.ACTIONS> actionList = gameState.getAvatarActions(true);
		int numberOfActions = actionList.size();

		// Example of what an individual will look like
		ArrayList<Types.ACTIONS> individual = new ArrayList<Types.ACTIONS>();
		ArrayList<Types.ACTIONS> individual2 = new ArrayList<Types.ACTIONS>();

		Random rand = new Random();
		int individualLength = 10;
		// Example of how to populate an individual
		for (int i = 0; i < individualLength; i++){
			int int_rand = rand.nextInt(numberOfActions);
			individual.add(actionList.get(int_rand));
		}

		//testing for crossover
		for (int i = 0; i < individualLength; i++){
			int int_rand = rand.nextInt(numberOfActions);
			individual2.add(actionList.get(int_rand));
		}

		// Different methods you can use, see core/game/ForwardModel for full list of methods
		System.out.println("List of possible actions  " + gameState.getAvatarActions(true));
		System.out.println("Orientation " + gameState.getAvatarOrientation());
		System.out.println("The individuals contents: " + individual);
		System.out.println("The individuals 2 contents: " + individual2);
		Types.ACTIONS a = individual.get(2);
		System.out.println("gameState.gameTick = " + gameState.gameTick);
		gameState.advance(a);
		System.out.println("Last action done " + gameState.getAvatarLastAction());
		System.out.println("gameState.gameTick = " + gameState.gameTick);
		System.out.println("Is the game over? : " + gameState.isGameOver());
		System.out.println("Game Score : " + gameState.getGameScore());
		
		// test crossover
		ArrayList<ArrayList<Types.ACTIONS>> result = crossover(individual, individual2);

		System.out.println("Child is: " + result.get(0));
		System.out.println("Child is: " + result.get(1));

		// test mutation using 10% rate on the population (need to loop through the population)
		double probability = rand.nextDouble();
		
		if (probability <= .1) {
	
			// perform mutation
			ArrayList<Types.ACTIONS> mutant = mutation(individual, actionList);
			System.out.println("mutated ind: " + mutant);
		}

		// 3. This replays a game from an action file previously recorded
	//	 String readActionsFile = recordActionsFile;
	//	 ArcadeMachine.replayGame(game, level1, visuals, readActionsFile);

		// 4. This plays a single game, in N levels, M times :
//		String level2 = new String(game).replace(gameName, gameName + "_lvl" + 1);
//		int M = 10;
//		for(int i=0; i<games.length; i++){
//			game = games[i][0];
//			gameName = games[i][1];
//			level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
//			ArcadeMachine.runGames(game, new String[]{level1}, M, sampleMCTSController, null);
//		}

		//5. This plays N games, in the first L levels, M times each. Actions to file optional (set saveActions to true).
//		int N = games.length, L = 2, M = 1;
//		boolean saveActions = false;
//		String[] levels = new String[L];
//		String[] actionFiles = new String[L*M];
//		for(int i = 0; i < N; ++i)
//		{
//			int actionIdx = 0;
//			game = games[i][0];
//			gameName = games[i][1];
//			for(int j = 0; j < L; ++j){
//				levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//				if(saveActions) for(int k = 0; k < M; ++k)
//				actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_" + k + ".txt";
//			}
//			ArcadeMachine.runGames(game, levels, M, sampleRHEAController, saveActions? actionFiles:null);
//		}


    }

	/**
     * Performs 1-point crossover on two individuals of the same length and returns the crossover children
     * 
     * @param parent1
     *            first individual of ACTIONS in the crossover
     * @param parent2
     *            second individual of ACTIONS in the crossover
     */
	public static ArrayList<ArrayList<Types.ACTIONS>> crossover(ArrayList<Types.ACTIONS> parent1, ArrayList<Types.ACTIONS> parent2) {
		ArrayList<Types.ACTIONS> child1 = new ArrayList<Types.ACTIONS>();
		ArrayList<Types.ACTIONS> child2 = new ArrayList<Types.ACTIONS>();

		// Get the smallest individual
		int length = Math.min(parent1.size(), parent2.size());

		// get crossover point
		Random rand = new Random();
		int crossoverPoint = rand.nextInt(length);
		System.out.println("Crossover point is:" + crossoverPoint);
		
		// produce children using crossover point
		for (int i = 0; i < crossoverPoint; i++) {
			child1.add(parent1.get(i));
			child2.add(parent2.get(i));
		}
		// Populate child 1
		for (int j = crossoverPoint; j < parent2.size(); j++) {
			child1.add(parent2.get(j));
		}
		// Populate child 2
		for (int j = crossoverPoint; j < parent1.size(); j++) {
			child2.add(parent1.get(j));
		}
		
		ArrayList<ArrayList<Types.ACTIONS>> result = new ArrayList<ArrayList<Types.ACTIONS>>();
		result.add(child1);
		result.add(child2);
		return result;
	}


	/**
     * Performs mutation on an individual and returns the mutated child
     * 
     * @param individual
     *            an individual of ACTIONS in the mutation
	 * @param actionList
	 *            list of possible actions for an individual
     */
	public static ArrayList<Types.ACTIONS> mutation(ArrayList<Types.ACTIONS> individual, ArrayList<Types.ACTIONS> actionList) {
		ArrayList<Types.ACTIONS> child = new ArrayList<Types.ACTIONS>();
		
		child = individual;
		int length = individual.size();
		int numberOfActions = actionList.size();
		Random rand = new Random();

		// set individual mutation rate
		double ind_rate = 1 / length;

		for (int i = 0; i < length; i++) {

			double prob = rand.nextDouble();
			if (prob <= ind_rate) {
				// get a random action and mutate child
				int int_rand = rand.nextInt(numberOfActions);
				child.set(i, actionList.get(int_rand));
			}
		}

		return child;
	}


}
