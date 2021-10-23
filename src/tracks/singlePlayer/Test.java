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
        String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";

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

		// Game initialisation
		ForwardModel gameState = ArcadeMachine.gameInt(game, level1, visuals, sampleRHEAController, recordActionsFile, seed, 0);
		// Creating a copy of the game state
		ForwardModel gameStateCopy = gameState;

		// List of actions that you can do during the game
		ArrayList<Types.ACTIONS> actionList = gameState.getAvatarActions(true);
		int numberOfActions = actionList.size();

		// Example of what an individual will look like
		Individual i1 = new Individual();
		i1.score = -5;
		System.out.println("Score of the First individual class object = " + i1.score);

		Random rand = new Random();
		// Initial length of individuals
		int individualLength = 10000;

		// Creating a population of individuals
		ArrayList<Individual> population = new ArrayList<Individual>();
		// Size of the population
		int populationSize = 100;
		for (int pop = 0; pop < populationSize; pop++){
			// Create a new moveset
			ArrayList<Types.ACTIONS> moveset = new ArrayList<Types.ACTIONS>();
			// Populating the moveset with random moves
			for (int i = 0; i < individualLength; i++){
				int int_rand = rand.nextInt(numberOfActions);
				moveset.add(actionList.get(int_rand));
			}
			// Create a new individual with a moveset
			Individual ind = new Individual(moveset);

			// Add the individual created to the population
			population.add(ind);
		}

		// Playing out an individual and retrieving relevant results
		// Create a fresh copy of the game state
		gameStateCopy = gameState;
		// itterate through the game
		int lastMove = population.get(0).moveSet.size();
		for(int i = 0; i < population.get(0).moveSet.size(); i++){
			// Make the next move in the individual
			gameStateCopy.advance(population.get(0).moveSet.get(i));
			//if the game is over then exit the loop
			if(gameStateCopy.isGameOver()){
				System.out.println("Game finished Early at move " + i);
				lastMove = i;
				break;
			}
		}
		population.get(0).score = (float)gameStateCopy.getScore();
		System.out.println("Length of moveSet prior to cutoff = " + population.get(0).moveSet.size());
		// Cut off the rest of the moves after finishing the game 
		if (lastMove < population.get(0).moveSet.size()){
			population.get(0).moveSet.subList(lastMove, population.get(0).moveSet.size()).clear();
		}
		System.out.println("Length of moveSet = " + population.get(0).moveSet.size());
		System.out.println("Score: " + population.get(0).score);
		System.out.println("Winner: " + gameStateCopy.getGameWinner());
		System.out.println("Game Tick: " + gameStateCopy.gameTick);

		// //testing for crossover
		// for (int i = 0; i < individualLength; i++){
		// 	int int_rand = rand.nextInt(numberOfActions);
		// 	individual2.add(actionList.get(int_rand));
		// }

		// // Different methods you can use, see core/game/ForwardModel for full list of methods
		// System.out.println("List of possible actions  " + gameState.getAvatarActions(true));
		// System.out.println("Orientation " + gameState.getAvatarOrientation());
		// System.out.println("The individuals contents: " + individual);
		// System.out.println("The individuals 2 contents: " + individual2);
		// Types.ACTIONS a = individual.get(2);
		// System.out.println("gameState.gameTick = " + gameState.gameTick);
		// gameState.advance(a);
		// System.out.println("Last action done " + gameState.getAvatarLastAction());
		// System.out.println("gameState.gameTick = " + gameState.gameTick);
		// System.out.println("Is the game over? : " + gameState.isGameOver());
		// System.out.println("Game Score : " + gameState.getGameScore());
		
		// // test crossover
		// ArrayList<ArrayList<Types.ACTIONS>> result = crossover(individual, individual2);

		// System.out.println("Child is: " + result.get(0));
		// System.out.println("Child is: " + result.get(1));

		// // test mutation using 10% rate on the population (once we implement a population, we need to loop through it)
		// double probability = rand.nextDouble();
		
		// if (probability <= .1) {
	
		// 	// perform mutation
		// 	ArrayList<Types.ACTIONS> mutant = mutation(individual, actionList);
		// 	System.out.println("mutated ind: " + mutant);
		// }




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



class Individual {
	public ArrayList<Types.ACTIONS> moveSet;
	float score;

	public Individual(){
		this.score = -1;
	}
	public Individual(ArrayList<Types.ACTIONS> moves){
		this.moveSet = moves;
		this.score = -1;
	}
	public Individual(ArrayList<Types.ACTIONS> moves, float score){
		this.moveSet = moves;
		this.score = score;
	}
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