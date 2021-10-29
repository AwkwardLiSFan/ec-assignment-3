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
		// games we are playing: Bomber{8}, Boulder Chase{10}, Chase{18}, Garbage Collector{45}
		int[] gameIndex = {8, 10, 18, 45};
		int game = 3;
		System.out.println("Playing gameIndex: " + gameIndex[game]);
		//int gameLevel = 0;
		
		// for each level\
		for (int gameLevel = 0; gameLevel < 5; gameLevel++) {
			
			// go through it 10 times
			for (int iteration = 0; iteration < 10; iteration++) {
				System.out.println("Iteration cycle: " + iteration + " Game level: " + gameLevel);
				advancesRan = 0;
				// Creating a copy of the game state
				ForwardModel gameState = init(gameIndex[game], gameLevel);
				gameState.setNewSeed(-131244659);
				ForwardModel gameStateCopy = gameState.copy();
				gameStateCopy.setNewSeed(-131244659);

				// List of actions that you can do during the game
				ArrayList<Types.ACTIONS> actionList = gameStateCopy.getAvatarActions(true);
				int numberOfActions = actionList.size();

				Random rand = new Random();
				// Initial length of individuals
				int individualLength = 10000;

				// Creating a population of individuals
				ArrayList<Individual> population = new ArrayList<Individual>();
				// Size of the population of 100
				int populationSize = 100;
				// Initialize the population
				initializePopulation(populationSize, individualLength, actionList, population);
				
				int generationLimit = 1000000;
				Individual bestInd = new Individual();
				//Individual previousBestInd = new Individual();
				int advanceRunLimit = 5000000;
				// Break points during which we return the results
				int[] breakPoints = {200000, 1000000, 5000000, 100000000};
				int currentBreakPoint = 0;

				ArrayList<Double> resultArray = new ArrayList<Double>();
				// Start of the EA
				for (int gen = 0; gen < generationLimit; gen++){
					Individual previousBestInd = new Individual(population.get(0));
					// Loop through the population and play the game with each one
					for(int i = 0; i < population.size(); i++){
						gameStateCopy = gameState.copy();
						gameStateCopy.setNewSeed(-131244659);
						// Play the game with the selected individual
						population.set(i, playGame(population.get(i), gameStateCopy));
						if(advancesRan > advanceRunLimit){
							break;
						}
					}
					System.out.println("Number of advance calls so far: " + advancesRan);
					
					// Saving the scores after each breakpoint
					if(advancesRan > breakPoints[currentBreakPoint]){
						System.out.println("Saving for breakpoint[" + breakPoints[currentBreakPoint] + "]    with score: " + previousBestInd.score);
						resultArray.add(previousBestInd.score);
						currentBreakPoint++;
					}
					
					if(advancesRan > advanceRunLimit){
						System.out.println("Completed 5 million advances: (actual number: " + advancesRan + ")");
						// Saving the scores after each breakpoint
						if(advancesRan > breakPoints[currentBreakPoint]){
							resultArray.add(previousBestInd.score);
						}
						break;
					}

					ArrayList<Individual> newPopulation = new ArrayList<Individual>();
			
					// Elitism to get best Individual
					bestInd = elitism(population);
					System.out.println("The best ind : " + bestInd.score);
					// Add best individual to the new population
					newPopulation.add(bestInd);

					

					// Select the top 50 individuals
					population.subList(population.size()/4, population.size()).clear();

					// Randomly crossover those until we have populationSize individuals
					while(newPopulation.size() < populationSize){
						int p1, p2;
						p1 = rand.nextInt(population.size());
						p2 = rand.nextInt(population.size());

						while (p1 == p2) {
							p2 = rand.nextInt(population.size());
						}
						
						newPopulation.addAll(crossover(population.get(p1), population.get(p2)));
					}
					// if we've added too many individuals then remove 1
					while(newPopulation.size() > populationSize){
						// remove the last element in the ArrayList of Individuals
						newPopulation.remove(newPopulation.size()-1);
					}
					// Add the Elitist Individual to the newPopulation
					newPopulation.set(0, bestInd);

					// Mutate the new population excluding the elite
					for(int x = 1; x < population.size(); x++){
						newPopulation.set(x, mutation(newPopulation.get(x), actionList));
					}
					// Update the population with the new population
					population.clear();
					population.addAll(newPopulation);

				}

				System.out.println("The best one sequence: " + bestInd.moveSet);
				System.out.println("The best score for level " + gameLevel + " is: " + bestInd.score);
				System.out.println("Scores at 200,000; 1,000,000; 5,000,000: " + resultArray);
			}
		}
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

		// add 50 random moves to moveSet
		for (int j = 0; j < 50; j++) {
			int actionIndex = rand.nextInt(numberOfActions);
			child.moveSet.add(actionList.get(actionIndex));
		}
		return child;
	}

	// returns the best individual within the current population
	public static Individual elitism(ArrayList<Individual> population) {
		// sort the population from highest score to lowest
		Collections.sort(population); 
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
		int seed = new Random().nextInt();
		seed = -131244659;
		System.out.println("This is the random seed, it's set due to randomness in Chaser NPC's " + seed);
	
		// Game and level to play
		int gameIdx = 8;
		int levelIdx = level; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
	
		String recordActionsFile = "actions_" + games[gameIdx] + "_lvl" + levelIdx + "_" + seed + ".txt";
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
		// itterate through the game
		int lastMove = ind.moveSet.size();
		for(int i = 0; i < ind.moveSet.size(); i++){
			// Make the next move in the individual
			gameStateCopy.advance(ind.moveSet.get(i));
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
	// This is used to create a deep copy of the individual
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