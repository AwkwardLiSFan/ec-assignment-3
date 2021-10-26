package tracks.singlePlayer;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

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
	public static int advancesRan = 0;
    public static void main(String[] args) {
		int[] gameIndex = {8, 10, 18, 45};
		// Change this when you want to change levels
		int gameLevel = 0;
		// Change this when you want to change games
		int game = 1;
		
		// Creating a copy of the game state
		ForwardModel gameState = init(gameIndex[game], gameLevel);

		gameState.setNewSeed(-131244659);

		ForwardModel gameStateCopy = gameState.copy();
		gameStateCopy.setNewSeed(-131244659);
		System.out.println("GameIndex being played: " + gameIndex[game] + "    level being played: " + gameLevel);
		
		// List of actions that you can do during the game
		ArrayList<Types.ACTIONS> actionList = gameStateCopy.getAvatarActions(true);
		int numberOfActions = actionList.size();

		Random rand = new Random();
		// Initial length of individuals
		int individualLength = 150;

		// Creating a population of individuals
		ArrayList<Individual> population = new ArrayList<Individual>();
		// Size of the population of 100
		int populationSize = 100;
		// Initialize the population
		initializePopulation(populationSize, individualLength, actionList, population);
		
		int generationLimit = 6;
		// Current best individual
		Individual bestInd = new Individual();
		// Save the previous best Individual so if generation goes over time we can revert
		Individual previousBestInd = new Individual();
		// Have a maximum limit of advance of 5 million Advance calls
		int advanceRunLimit = 5000000;
		// Break points during which we return the results
		int[] breakPoints = {200000, 1000000, 5000000, 100000000};
		int currentBreakPoint = 0;
		// Where we store the best results at each breakpoint
		ArrayList<Double> resultArray = new ArrayList<Double>();

		// Start of the EA
		for (int gen = 0; gen < generationLimit; gen++){
			// Save the previous best individual
			//previousBestInd = bestInd;
			System.out.println("before previous best: " + previousBestInd.score);
			System.out.println("before current best: " + bestInd.score);
			previousBestInd = new Individual(population.get(0));
			System.out.println("before population.get(0).score: " + population.get(0).score);
			System.out.println("before population.get(0).moveSet: " + population.get(0).moveSet);
			
			// Loop through the population and play the game with each one
			for(int i = 0; i < population.size(); i++){
				gameStateCopy = gameState.copy();
				// Play the game with the selected individual
				System.out.println("The individual before playGame. population.get("+i+") : " + population.get(i).score + "    population.get("+i+") : " + population.get(i).moveSet);
				population.set(i, playGame(population.get(i), gameStateCopy));
				System.out.println("The individual after playGame. population.get("+i+") : " + population.get(i).score + "    population.get("+i+") : " + population.get(i).moveSet);
				if(advancesRan > advanceRunLimit){
					break;
				}
				System.out.println("gameStateCopy ticks: " + gameStateCopy.getGameTick() + "    score: " + gameStateCopy.getGameScore() + "    winner: " +gameStateCopy.getGameWinner());
			}

			

			if(advancesRan > advanceRunLimit){
				System.out.println("Completed 5 million advances: (actual number: " + advancesRan + ")");
				// Saving the scores after each breakpoint
				if(advancesRan > breakPoints[currentBreakPoint]){
					resultArray.add(previousBestInd.score);
				}
				break;
			}

			// Elitism to get best Individual
			bestInd = elitism(population);

			System.out.println("after population.get(0).moveSet: " + population.get(0).moveSet);
			System.out.println("after previous best after: " + previousBestInd.score);
			System.out.println("after current best after: " + bestInd.score);
			System.out.println("after pop.size() " + population.size());

			// Remove worst individuals
			population.subList(population.size()/2, population.size()).clear();

			// Randomly crossover those until we have populationSize individuals
			ArrayList<Individual> newPopulation = new ArrayList<Individual>();
			newPopulation.add(bestInd);
			System.out.println("After -- setting elite, new popInd.score = " + newPopulation.get(0).score);
			
			while(newPopulation.size() < populationSize){
				int p1, p2;
				p1 = rand.nextInt(population.size());
				p2 = rand.nextInt(population.size());
				newPopulation.addAll(crossover(population.get(p1), population.get(p2)));
			}
			// if we've added too many individuals then remove 1
			while(newPopulation.size() > populationSize){
				// remove the last element in the ArrayList of Individuals
				newPopulation.remove(newPopulation.size()-1);
				System.out.println("removed Individual from newPopulation");
			}

			// Mutate the new population excluding the elite
			for(int x = 1; x < population.size(); x++){
				newPopulation.set(x, mutation(population.get(x), actionList));
			}
			// Update the population with the new population
			population.clear();
			population.addAll(newPopulation);
			//newPopulation.clear();
			// Saving the scores after each breakpoint
			if(advancesRan > breakPoints[currentBreakPoint]){
				resultArray.add(previousBestInd.score);
				currentBreakPoint++;
			}
			System.out.println("end gen: " + gen);
			System.out.println("end gen: " + gen);
			System.out.println("end gen: " + gen);
			System.out.println("end gen: " + gen);
			
		}
		System.out.println("The best one sequence: " + bestInd.moveSet);
		System.out.println("The best score: " + bestInd.score);
		System.out.println("Scores at 200,000; 1,000,000; 5,000,000: " + resultArray);
		

    }

	/**
     * Performs 1-point crossover on two individuals of the same length and returns the crossover children
     * 
     * @param parent1
     *            first individual in the crossover
     * @param parent2
     *            second individual in the crossover
     */
	public static ArrayList<Individual> crossover(Individual parent1, Individual parent2) {
		ArrayList<Types.ACTIONS> moves1 = new ArrayList<Types.ACTIONS>();
		ArrayList<Types.ACTIONS> moves2 = new ArrayList<Types.ACTIONS>();

		// Get the smallest individual
		int length = Math.min(parent1.moveSet.size(), parent2.moveSet.size());

		// get crossover point
		Random rand = new Random();
		int crossoverPoint = rand.nextInt(length);
		//System.out.println("Crossover point is:" + crossoverPoint);
		
		// produce children moves using crossover point
		for (int i = 0; i < crossoverPoint; i++) {
			moves1.add(parent1.moveSet.get(i));
			moves2.add(parent2.moveSet.get(i));
		}

		//initialise children
		Individual child1 = new Individual(moves1);
		Individual child2 = new Individual(moves2);

		// Populate child 1
		for (int j = crossoverPoint; j < parent2.moveSet.size(); j++) {
			child1.moveSet.add(parent2.moveSet.get(j));
		}
		// Populate child 2
		for (int j = crossoverPoint; j < parent1.moveSet.size(); j++) {
			child2.moveSet.add(parent1.moveSet.get(j));
		}
		
		ArrayList<Individual> result = new ArrayList<Individual>();
		result.add(child1);
		result.add(child2);

		return result;
	}


	/**
     * Performs mutation on an individual and returns the mutated child
     * 
     * @param individual
     *            an individual in the mutation
	 * @param actionList
	 *            list of possible actions for an individual
     */
	public static Individual mutation(Individual individual, ArrayList<Types.ACTIONS> actionList) {
		Individual child = new Individual();
		
		child = individual;
		int length = child.moveSet.size();
		int numberOfActions = actionList.size();
		Random rand = new Random();

		// set individual mutation rate
		double ind_rate = 1 / length;

		for (int i = 0; i < length; i++) {

			double prob = rand.nextDouble();
			if (prob <= ind_rate) {
				// get a random action and mutate child
				int int_rand = rand.nextInt(numberOfActions);
				child.moveSet.set(i, actionList.get(int_rand));
			}
		}
		return child;
	}

	// returns the best individual within the current population
	public static Individual elitism(ArrayList<Individual> population) {
		// sort the population from highest score to lowest
		Collections.sort(population);
		// for(int i = 0; i < population.size(); i++){
		// 	System.out.println("population["+i+"] score: " + population.get(i).score + "     length: " + population.get(i).moveSet.size());
		// }
		Individual copyInd = new Individual(population.get(0));
		return copyInd;
	}

	public static ForwardModel init(int gameIndex, int level){
		// Available tracks:
		String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
	
		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);
		//The games that we have to test with
		
	
		//Game settings
		boolean visuals = false;
		//Set the seed to a single value as Chaser NPC's are random >_>
		//int seed = new Random().nextInt();
		int seed = -131244659;
		System.out.println("This is the random seed, it's set due to randomness in Chaser NPC's " + seed);
	
		// Game and level to play
		int gameIdx = gameIndex;
		int levelIdx = level; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
	
		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// where to record the actions
						// executed. null if not to save.
	
		// Game initialisation
		ForwardModel gameState = ArcadeMachine.gameInt(game, level1, visuals, sampleRHEAController, recordActionsFile, seed, 0);
		return gameState;
	}

	public static void initializePopulation(int populationSize, int individualLength, ArrayList<Types.ACTIONS> actionList, ArrayList<Individual> population){
		int numberOfActions = actionList.size();
		Random rand = new Random();
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
	}

	public static Individual playGame(Individual ind, ForwardModel gameStateCopy){
		//System.out.println("Starting game : Game State settings, tick: " + gameStateCopy.gameTick + "   isGameOver: " + gameStateCopy.isGameOver() + "   alive: " + gameStateCopy.isAvatarAlive() + "   score: " + gameStateCopy.getGameScore());
		// itterate through the game
		//System.out.println("move set: " + ind.moveSet);
		int lastMove = ind.moveSet.size();
		for(int i = 0; i < ind.moveSet.size(); i++){
			// Make the next move in the individual
			gameStateCopy.advance(ind.moveSet.get(i));
			//System.out.println("Game State settings, tick: " + gameStateCopy.gameTick + "   isGameOver: " + gameStateCopy.isGameOver() + "   alive: " + gameStateCopy.isAvatarAlive() + "   score: " + gameStateCopy.getGameScore());
			advancesRan++;
			//if the game is over then exit the loop
			if(gameStateCopy.isGameOver()){
				//System.out.println("Game finished Early at move " + i);
				lastMove = i;
				break;
			}
		}

		ArrayList<Types.ACTIONS> newMoves = new ArrayList<Types.ACTIONS>();
		newMoves.addAll(ind.moveSet.subList(0, lastMove));
		double newScore = gameStateCopy.getScore();
		Individual newIndividual = new Individual(newMoves, newScore);
		return newIndividual;
		// Cut off the rest of the moves after finishing the game 
		// if (lastMove < ind.moveSet.size()){
		// 	ind.moveSet.subList(lastMove+1, ind.moveSet.size()).clear();
		// }
		// Get the final score
		// ind.score = gameStateCopy.getScore();
		// return ind;
	}
	
}


class Individual implements Comparable<Individual>{
	public ArrayList<Types.ACTIONS> moveSet;
	public double score;

	public Individual(){
		this.score = -1;
	}
	public Individual(ArrayList<Types.ACTIONS> moves){
		this.moveSet = moves;
		this.score = -1;
	}
	public Individual(ArrayList<Types.ACTIONS> moves, double score){
		this.moveSet = moves;
		this.score = score;
	}
	// This is used to create 
	public Individual(Individual that){
		this(new ArrayList<Types.ACTIONS>(that.moveSet), that.score);
	}
	public int compareTo(Individual compareInd){
		int compareScore=(int)((Individual)compareInd).score;
		if (compareScore == this.score){
			int compareLength = (int)((Individual)compareInd).moveSet.size();
			return (int)(this.moveSet.size() - compareLength);
		}
		return (int)(compareScore-this.score);
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

