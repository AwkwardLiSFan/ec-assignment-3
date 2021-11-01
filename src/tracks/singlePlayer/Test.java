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
	// counter to track the number of advance calls
	public static int advancesRan = 0;
	// Size of the population of 50
	static int populationSize = 50;
	// tracks the hypervolume after the Environmental Selection
	static int globalHyperVolume = 0;
    public static void main(String[] args) {
		// games we are playing: Bomber{8}, Boulder Chase{10}, Chase{18}, Garbage Collector{45}
		int[] gameIndex = {8, 10, 18, 45};
		int game = 0;
		System.out.println("Playing gameIndex: " + gameIndex[game]);
		
		//for each level in the game
		for (int gameLevel = 0; gameLevel < 5; gameLevel++) {
			//go through it 10 times
			for (int iteration = 0; iteration < 10; iteration++) {
				System.out.println("Iteration cycle: " + iteration + " Game level: " + gameLevel);
				advancesRan = 0;
				// Creating a copy of the game state\
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
				
				// Initialize the population
				initializePopulation(populationSize, individualLength, actionList, population);
				
				int generationLimit = 1000000000;
				double previousHyperVolume = 0;
				double currentHyperVolume = 0;
				
				//Individual previousBestInd = new Individual();
				int advanceRunLimit = 5000000;
				// Break points during which we return the results
				int[] breakPoints = {200000, 1000000, 5000000, 100000000};
				int currentBreakPoint = 0;

				ArrayList<Double> resultArray = new ArrayList<Double>();
				
				// Start of the EA
				for (int gen = 0; gen < generationLimit; gen++){
					System.out.println("Gen: " + gen + "    advances: " + advancesRan);
					// update what the previous generations hypervolume was
					previousHyperVolume = currentHyperVolume;

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
					//System.out.println("Number of advance calls so far: " + advancesRan);
					
					// Saving the scores after each breakpoint
					if(advancesRan > breakPoints[currentBreakPoint]){
						System.out.println("Saving for breakpoint[" + breakPoints[currentBreakPoint] + "]    with hyperVolume: " + previousHyperVolume);
						resultArray.add(previousHyperVolume);
						currentBreakPoint++;
					}
					// Checking if we have hit the advance limit cap
					if(advancesRan > advanceRunLimit){
						System.out.println("Completed 5 million advances: (actual number: " + advancesRan + ")");
						// Saving the scores after each breakpoint
						if(advancesRan > breakPoints[currentBreakPoint]){
							resultArray.add(previousHyperVolume);
						}
						break;
					}
			
					// Elitism to get best Individual
					//bestInd = elitism(population);

					// This is where we do the hypervolume calculations and trimming down the population
					population = selection(population);

					// if there is ever only 1 individual, then just randomly create more Individuals do diversify the population
					if(population.size() <= 1){
						Individual survivor = new Individual(population.get(0));
						population.clear();
						initializePopulation(populationSize, individualLength, actionList, population);
						population.set(0, survivor);
					}
					currentHyperVolume = globalHyperVolume;

					// Create a subset of population that we will be crossing over and mutating and then the result is re-added to the population
					ArrayList<Individual> newPopulation = new ArrayList<Individual>();
					for (int i = 0; i < population.size(); i++){
						float prob = rand.nextFloat();
						if(prob <= 0.7){
							Individual child1 = new Individual(population.get(i));
							newPopulation.add(child1);
						}
					}

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

					// Mutate the new population
					for(int x = 0; x < population.size(); x++){
						newPopulation.set(x, mutation(newPopulation.get(x), actionList));
					}
					// Update the population with the new population
					population.addAll(newPopulation);

				}

				//System.out.println("The best one sequence: " + bestInd.moveSet);
				//System.out.println("The best score for level " + gameLevel + " is: " + bestInd.score);
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
		Individual child = new Individual(individual);
		int length = child.moveSet.size();
		int numberOfActions = actionList.size();
		Random rand = new Random();

		// set individual mutation rate
		double ind_rate = 1 / length;

		// Mutates the Individuals moveset randomly
		for (int i = 0; i < length; i++) {
			double prob = rand.nextDouble();
			if (prob <= ind_rate) {
				// get a random action and mutate child
				int int_rand = rand.nextInt(numberOfActions);
				child.moveSet.set(i, actionList.get(int_rand));
			}
		}

		// add 50 random moves to the end of the Individuals moveSet
		for (int j = 0; j < 50; j++) {
			int actionIndex = rand.nextInt(numberOfActions);
			child.moveSet.add(actionList.get(actionIndex));
		}
		return child;
	}

	// returns the best individual within the current population
	public static Individual elitism(ArrayList<Individual> population) {
		// sort the population from highest score to lowest
		Collections.sort(population, new SORTBYSCORE()); 
		
		Individual copyInd = new Individual(population.get(0));
		return copyInd;
	}

	// Initializes the game state for a given game and it's respective level
	// Returns the resulting initialized game state
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

	// Initializes a population of individuals up to populationSize and each individuals moveset is as long as individualLength
	// The movesets of the individuals are a random series of actions from the actionList ArrayList
	// These Individuals are added to the ArrayList population
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

	// Takes an individual object and a copy of the game state and simulates a player with a set of moves in the game from the start
	// Returns the individual with the updated move set and score
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
		// Update the moveset with all moves that lead up to the game being completed, if not completed
		// All moves are kept.
		ArrayList<Types.ACTIONS> newMoves = new ArrayList<Types.ACTIONS>();
		newMoves.addAll(ind.moveSet.subList(0, lastMove));
		double newScore = gameStateCopy.getScore();
		// Create a new individual with the updated moveset and score
		Individual newIndividual = new Individual(newMoves, newScore);
		return newIndividual;
	}

	// returns the population after using the enviromental selection method in SIBEA
	public static ArrayList<Individual> selection(ArrayList<Individual> population){
		// Calculate the Dominance ranking
		for(int i = 0; i < population.size(); i++){
			population.get(i).dominance = 0;
			double indScore = population.get(i).score;
			int indSize = population.get(i).moveSet.size();
			for(int x = 0; x < population.size(); x++){
				// if the individual has a lower score OR a larger length it is dominated
				if (indScore <= population.get(x).score && indSize > population.get(x).moveSet.size()){
					population.get(i).dominance++;
				}
				else if (indScore < population.get(x).score && indSize >= population.get(x).moveSet.size()){
					population.get(i).dominance++;
				}
			}
		}
		// sort by dominance ranking
		Collections.sort(population);

		// create P' such that P' is a subset of P of the worst rank individuals
		ArrayList<Individual> nonDominatedPopulation = new ArrayList<Individual>();

		// populate worstPopulation with rank 0 individuals 
		for (int ind = 0; ind < population.size(); ind++) {
			if (population.get(ind).dominance == 0) {
				nonDominatedPopulation.add(population.get(ind));
			}
		}
		
		// sort by score and then length
		Collections.sort(nonDominatedPopulation, new SORTBYSCORE());

		// if the non-dominated population size is already less than that of the population size then return
		if(nonDominatedPopulation.size() <= populationSize){
			return nonDominatedPopulation;
		}

		// pre-process that removes individuals that give nothing for the hypervolume
		// remove the nonDominatedPopulation Individuals that contribute nothing
		// for(int i = 0; i < nonDominatedPopulation.size(); i++){
		// 	if ((nonDominatedPopulation.get(i).score == 0 || nonDominatedPopulation.get(i).moveSet.size() == 0) && nonDominatedPopulation.size() > populationSize){
		// 		System.out.println("does this activate? : " + nonDominatedPopulation.size());
		// 		nonDominatedPopulation.get(i).hyperVolume = 0;
		// 		nonDominatedPopulation.remove(i);
		// 	}
		// }

		// instead we could remove duplicates with a random chance whilst the non dominated population size is larger than the population size
		if(nonDominatedPopulation.size() > populationSize){
			Random rand = new Random();
			for(int i = 0; i < nonDominatedPopulation.size()-1;){
				if(nonDominatedPopulation.get(i).score == nonDominatedPopulation.get(i).score && nonDominatedPopulation.get(i).moveSet.size() == nonDominatedPopulation.get(i).moveSet.size() 
				&& nonDominatedPopulation.size() > populationSize){
					float f1 = rand.nextFloat();
					if(f1 < 0.5){
						nonDominatedPopulation.remove(i);
					} else {
						nonDominatedPopulation.remove(i+1);
					}
				} else {
					i++;
				}
			}
		}
		
		// if we've already removed the required amount of useless individuals then just return the new population
		if(nonDominatedPopulation.size() <= populationSize){
			globalHyperVolume = hypervolumeIndicator(nonDominatedPopulation);
			return nonDominatedPopulation;
		}

		// keep removing the worst/least useful individual in each loop until the size of the population is small enough
		while(nonDominatedPopulation.size() > populationSize){
			// get hypervolume sum for nonDominatedPopulation
			int hypervolumeSum = hypervolumeIndicator(nonDominatedPopulation);
			globalHyperVolume = hypervolumeSum;

			// calculate loss of hypervolume: I(P') - I(P'\(each individual))
			// System.out.println("Start of loss calculations");
			for (int j = 0; j < nonDominatedPopulation.size(); j++) {
				ArrayList<Individual> removedIndividuals = new ArrayList<Individual>();
				removedIndividuals.addAll(nonDominatedPopulation);
				removedIndividuals.remove(nonDominatedPopulation.get(j));

				// calculate the hypervolume of the set without the individual in question
				int removedHypervolumeSum = hypervolumeIndicator(removedIndividuals);
				// the loss is the difference between the original sets hypervolume and the sets hypervolume without the individual considered
				nonDominatedPopulation.get(j).loss = hypervolumeSum - removedHypervolumeSum;

				//System.out.println("loss:" + nonDominatedPopulation.get(j).loss);
			}

			//Sort the population by loss in ascending order
			Collections.sort(nonDominatedPopulation, new SORTBYLOSS());

			// remove the least useful individual
			nonDominatedPopulation.remove(nonDominatedPopulation.size()-1);
		}
		// get the final hypervolume
		globalHyperVolume = hypervolumeIndicator(nonDominatedPopulation);
		return nonDominatedPopulation;
	}

	// Sub-step 2
	// Calculates the hypervolume of a given set of non dominated individuals
	public static int hypervolumeIndicator (ArrayList<Individual> nonDominatedPopulation) {
		int hypervolumeSum = 0;
		// The reference point is (score = 0, length = 11000)
		int referenceSize = 11000;
		int referenceScore = 0;
		
		// Want to calculate the hypervolume of the first individual and then move the reference point in regards to that individual
		// So we will have the starting reference point of score = 0 and length = 11000
		// If we have the first point of score = 1 and size = 745
		// The the hypervolume is (1 - 0) * (11000 * 745) i.e. (point.score - reference.score) * (reference.size - point.size)
		// After this the reference point's score is updated to score = 1 and size = 11000

		// itterate through the population and calculate the hypervolume of each point whilst moving the reference point
		for(int i = 0; i < nonDominatedPopulation.size(); i++){
			hypervolumeSum += (nonDominatedPopulation.get(i).score - referenceScore) * (referenceSize - nonDominatedPopulation.get(i).moveSet.size());
			// update the new reference point
			referenceScore = (int)nonDominatedPopulation.get(i).score;
		}

		//System.out.println("Sum is:" + hypervolumeSum);
		return hypervolumeSum;
	}
	
}

// The class that will hold all information regarding an individual in the population
class Individual implements Comparable<Individual>{
	public ArrayList<Types.ACTIONS> moveSet;
	public double score;
	public int dominance;
	public int loss;
	public int hyperVolume;

	public Individual(){
		this.score = -1;
		this.dominance = 1000000;
	}
	public Individual(ArrayList<Types.ACTIONS> moves){
		this.moveSet = moves;
		this.score = -1;
		this.dominance = 1000000;
	}
	public Individual(ArrayList<Types.ACTIONS> moves, double score){
		this.moveSet = moves;
		this.score = score;
		this.dominance = 1000000;
	}
	public Individual(ArrayList<Types.ACTIONS> moves, double score, int dominance){
		this.moveSet = moves;
		this.score = score;
		this.dominance = dominance;
	}
	public Individual(ArrayList<Types.ACTIONS> moves, double score, int dominance, int hyperVolume){
		this.moveSet = moves;
		this.score = score;
		this.dominance = dominance;
		this.hyperVolume = hyperVolume;
	}
	// This is used to create a deep copy of the individual
	public Individual(Individual that){
		this(new ArrayList<Types.ACTIONS>(that.moveSet), that.score, that.dominance, that.hyperVolume);
	}
	// A generic sort function that will sort in ascending order via the dominance
	public int compareTo(Individual compareInd){
		int compareScore=(int)((Individual)compareInd).dominance;
		return (int)(this.dominance - compareScore);
	}
	
}

// A class that will sort in descending order via the score
class SORTBYSCORE implements Comparator<Individual> {
    public int compare(Individual a, Individual b){
        return (int)(a.score - b.score);
    }
}

// A class that will sort the individual objects in ascending order via the loss
class SORTBYLOSS implements Comparator<Individual> {
    public int compare(Individual a, Individual b){
        return (int)(b.loss - a.loss);
    }
}