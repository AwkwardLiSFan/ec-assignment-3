package tracks.levelGeneration.geneticLevelGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.lang.Math;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import tools.ElapsedCpuTimer;
import tools.GameAnalyzer;
import tools.LevelMapping;

public class LevelGeneratorAssignment3 extends AbstractLevelGenerator{

	/**
	 * Level mapping of the best chromosome
	 */
	private LevelMapping bestChromosomeLevelMapping;
	/**
	 * The best chromosome fitness across generations
	 */
	private ArrayList<Double> bestFitness;
	/**
	 * number of feasible chromosomes across generations
	 */
	private ArrayList<Integer> numOfFeasible;
	/**
	 * number of infeasible chromosomes across generations
	 */
	private ArrayList<Integer> numOfInFeasible;
	
	/**
	 * Initializing the level generator
	 * @param game			game description object
	 * @param elapsedTimer	amount of time for intiailization
	 */
	public LevelGeneratorAssignment3(GameDescription game, ElapsedCpuTimer elapsedTimer){
		SharedData.random = new Random();
		SharedData.gameDescription = game;
		SharedData.gameAnalyzer = new GameAnalyzer(game);
		SharedData.constructiveGen = new tracks.levelGeneration.constructiveLevelGenerator.LevelGenerator(game, null);
		bestChromosomeLevelMapping = null;
		bestFitness = null;
		numOfFeasible = null;
		numOfInFeasible = null;
	}
	
	/**
	 * Get the next population based on the current feasible infeasible population
	 * @param fPopulation	array of the current feasible chromosomes
	 * @param iPopulation	array of the current infeasible chromosomes
	 * @return				array of the new chromosomes at the new population
	 */
	private ArrayList<Chromosome> getNextPopulation(ArrayList<Chromosome> fPopulation, ArrayList<Chromosome> iPopulation){
		ArrayList<Chromosome> newPopulation = new ArrayList<Chromosome>();
		
		//collect some statistics about the current generation
		ArrayList<Double> fitnessArray = new ArrayList<Double>();
		for(int i=0;i<fPopulation.size();i++){
			fitnessArray.add(fPopulation.get(i).getFitness().get(0));
		}

		Collections.sort(fitnessArray);
		if(fitnessArray.size() > 0){
			bestFitness.add(fitnessArray.get(fitnessArray.size() - 1));
		}
		else{
			bestFitness.add((double) 0);
		}
		numOfFeasible.add(fPopulation.size());
		numOfInFeasible.add(iPopulation.size());
		
		while(newPopulation.size() < SharedData.POPULATION_SIZE){
			//choosing which population to work on with 50/50 probability 
			//of selecting either any of them
			ArrayList<Chromosome> population = fPopulation;
			if(fPopulation.size() <= 0){
				population = iPopulation;
			}
			if(SharedData.random.nextDouble() < 0.5){
				population = iPopulation;
				if(iPopulation.size() <= 0){
					population = fPopulation;
				}
			}
			

			//select the parents using roulletewheel selection
			Chromosome parent1 = selection(population);
			Chromosome parent2 = selection(population);
			Chromosome child1 = parent1.clone();
			Chromosome child2 = parent2.clone();
			//do cross over
			if(SharedData.random.nextDouble() < SharedData.CROSSOVER_PROB){
				ArrayList<Chromosome> children = parent1.crossOver(parent2);
				child1 = children.get(0);
				child2 = children.get(1);
				
				//do muation to the children
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child1.mutate();
				}
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child2.mutate();
				}
			}

			//mutate the copies of the parents
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child1.mutate();
			}
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child2.mutate();
			}
			

			//add the new children to the new population
			newPopulation.add(child1);
			newPopulation.add(child2);
		}
		

		//calculate fitness of the new population chromosomes 
		for(int i=0;i<newPopulation.size();i++){
			newPopulation.get(i).calculateFitness(SharedData.EVALUATION_TIME);
			if(newPopulation.get(i).getConstrainFitness() < 1){
				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + newPopulation.get(i).getConstrainFitness());
			}
			else{
				System.out.println("\tChromosome #" + (i+1) + " Fitness: " + newPopulation.get(i).getFitness());
			}
		}
		
		// insert NSGA-II code here 
		//add the best chromosome(s) from old population to the new population
		Collections.sort(newPopulation);
		for(int i=SharedData.POPULATION_SIZE - SharedData.ELITISM_NUMBER;i<newPopulation.size();i++){
			newPopulation.remove(i);
		}

		if(fPopulation.isEmpty()){
			Collections.sort(iPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(iPopulation.get(i));
			}
		}
		else{
			Collections.sort(fPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(fPopulation.get(i));
			}
		}
		
		return newPopulation;
	}

	private Integer dominate(ArrayList<Chromosome> population, Chromosome individual){
		Integer numSolutions=0, sizePop;
		Double winsI, movesI, wins, moves;

		winsI = individual.getFitness().get(0);
		movesI = individual.getFitness().get(1);

		sizePop = population.size();

		for (int i=0; i< sizePop; i++){
			wins = population.get(i).getFitness().get(0);
			moves = population.get(i).getFitness().get(1);
			if (winsI<=wins && movesI<moves)
				numSolutions = numSolutions + 1;
			else if (movesI<=moves && winsI<wins)
				numSolutions = numSolutions + 1;

		}
		return numSolutions;
	}

	private double getIndicatorHypervolume(Chromosome individual, ArrayList<Chromosome> population){
		
		// declare all variables that will be used multiple times to minimize function calls to getFitness()
		double individualFitnessZero = individual.getFitness().get(0) ;
		double individualFitnessOne = individual.getFitness().get(1) ;
		double closestToIndividualZero = Math.abs(population.get(0).getFitness().get(0) - individualFitnessZero);
		double closestToIndividualOne = Math.abs(population.get(0).getFitness().get(1) - individualFitnessOne);
		
		// find a chromosome 'y' in the population s.t. y.fitness[0] <= individual.fitness[0] and difference b/w them is minimal
		// Repeat for fitness[1] too  
		for (int i = 1; i < population.size(); i++){
			
			// for fitness[0]
			if (population.get(i).getFitness().get(0) <= individualFitnessZero){
				// check if difference is lesser than minimal difference found so far
				if (Math.abs(individualFitnessZero - population.get(i).getFitness().get(0)) < closestToIndividualZero){
					// assign new minimal difference to closestToIndividual
					closestToIndividualZero = Math.abs(individualFitnessZero - population.get(i).getFitness().get(0));
				}
			}

			// for fitness[1]
			if (population.get(i).getFitness().get(1) <= individualFitnessOne){
				// check if difference is lesser than minimal difference found so far
				if (Math.abs(individualFitnessOne - population.get(i).getFitness().get(1)) < closestToIndividualOne){
					// assign new minimal difference to closestToIndividual
					closestToIndividualOne = Math.abs(individualFitnessOne - population.get(i).getFitness().get(1));
				}
			}
		}

		// return hypervolume  indicator: (individual.fitness[0] - y.fitness[0]) + (individual.fitness[1] - y.fitness[1)
		return closestToIndividualZero + closestToIndividualOne;
	}

	/**
	 * Roullete wheel selection for the infeasible population
	 * @param population	array of chromosomes surviving in this population
	 * @return				the picked chromosome based on its constraint fitness
	 */
	// private Chromosome constraintRouletteWheelSelection(ArrayList<Chromosome> population){
	// 	//calculate the probabilities of the chromosomes based on their fitness
	// 	double[] probabilities = new double[population.size()];
	// 	probabilities[0] = population.get(0).getConstrainFitness();
	// 	for(int i=1; i<population.size(); i++){
	// 		probabilities[i] = probabilities[i-1] + population.get(i).getConstrainFitness() + SharedData.EIPSLON;
	// 	}

	// 	for(int i=0; i<probabilities.length; i++){
	// 		probabilities[i] = probabilities[i] / probabilities[probabilities.length - 1];
	// 	}


	// 	//choose a chromosome based on its probability
	// 	double prob = SharedData.random.nextDouble();
	// 	for(int i=0; i<probabilities.length; i++){
	// 		if(prob < probabilities[i]){
	// 			return population.get(i);
	// 		}
	// 	}

	// 	return population.get(0);
	// }
	
	//selection
	private Chromosome selection(ArrayList<Chromosome> population){
		//Check if the population is feasible or infeasible
		// if(population.get(0).getConstrainFitness() == 0){
		// 	constraintRouletteWheelSelection(population);
		// }

		//Variables 
		int sizePop=0, variables=0, wins, moves;
		// Chromosome randomChrom;
		// Random rand = new Random();
		// Integer randNum=0;
		Double  w1=10.0, w2=-1.0;
		// ArrayList<Chromosome> candidate = new ArrayList<Chromosome>();
		ArrayList<Double> candidateWins = new ArrayList<Double>();
		ArrayList<Double> candidateMoves = new ArrayList<Double>();
		ArrayList<Double> weightedSum = new ArrayList<Double>();

		//Get size of chromosome 
		sizePop = population.size();
		variables  = population.get(0).getFitness().size();

		//Index of objectives in fitness array 
		wins =variables-2;
		moves = variables-1;

		// //Select random chromosomes from population to undergo selection
		// for (int i=0; i<pool_size; i++){
		// 	randNum = rand.nextInt((sizePop - 0) + 1) + 0;
		// 	randomChrom = population.get(randNum);
		// 	for (int k=0; k<candidate.size(); k++){
		// 		if (randomChrom.equals(candidate.get(k))){
		// 			randNum = rand.nextInt((sizePop - 0) + 1) + 0;
		// 			randomChrom = population.get(randNum);
		// 			k=0;
		// 		}
		// 	}
		// 	candidate.add(randomChrom);
		// }

		//Go through each chromosome and get the wins and moves objectives and calculate the weighted sum 
		for (int i=0; i<sizePop; i++){
			candidateWins.add(population.get(i).getFitness().get(wins));
			candidateMoves.add(population.get(i).getFitness().get(moves));
			weightedSum.add(w1*candidateWins.get(i)+w2*candidateMoves.get(i));
		}

		Integer winnerIndex=0;

		for (int i=1; i<sizePop; i++){
			if (weightedSum.get(i)<weightedSum.get(i-1)){
				winnerIndex=i;
			}
		}

		return population.get(winnerIndex);
	}



	/**
	 * Get the fitness for any population
	 * @param population	array of chromosomes surviving in this population
	 * @return				the picked chromosome based on its fitness
	 */
	// private Chromosome rouletteWheelSelection(ArrayList<Chromosome> population){
	// 	//if the population is infeasible use the constraintRoulletWheel function
	// 	//if getConstrainedFitness <1 infeasible 

	// 	//Initialise the probability array for the current population
	// 	double[] probabilities = new double[population.size()];
	// 	double fitnessSum=0;

	// 	//Check if the population is feasible or infeasible
	// 	if(population.get(0).getConstrainFitness() < 1){
	// 		for (int i=0; i<population.size(); i++){
	// 			fitnessSum = fitnessSum + population.get(i).getConstrainFitness();
	// 		}
			
	// 		probabilities[0] = population.get(0).getConstrainFitness(); 
	// 		for(int i=1; i<population.size(); i++){
	// 			//For each chromosome it adds the previous fitness with the current fitness
	// 			probabilities[i] = probabilities[i-1] + (population.get(i).getCombinedFitness()/fitnessSum) + SharedData.EIPSLON;
	// 		}
	// 	} else {
	// 		for (int i=0; i<population.size(); i++){
	// 			fitnessSum = fitnessSum + population.get(i).getCombinedFitness();
	// 		}
			
	// 		probabilities[0] = population.get(0).getCombinedFitness(); 
	// 		for(int i=1; i<population.size(); i++){
	// 			//For each chromosome it adds the previous fitness with the current fitness
	// 			probabilities[i] = probabilities[i-1] + (population.get(i).getCombinedFitness()/fitnessSum) + SharedData.EIPSLON;
	// 		}

	// 	}

	// 	//choose random chromosome based on its fitness
	// 	double prob = SharedData.random.nextDouble();
	// 	for(int i=0; i<probabilities.length; i++){
	// 		if(prob < probabilities[i]){
	// 			return population.get(i);
	// 		}
	// 	}
		
	// 	return population.get(0);
	// }
	
	/**
	 * Generate a level using GA in a fixed amount of time and 
	 * return the level in form of a string
	 * @param game			the current game description object
	 * @param elapsedTimer	the amount of time allowed for generation
	 * @return				string for the generated level
	 */
	@Override
	public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
		//initialize the statistics objects
		bestFitness = new ArrayList<Double>();
		numOfFeasible = new ArrayList<Integer>();
		numOfInFeasible = new ArrayList<Integer>();
		
		SharedData.gameDescription = game;
		
		int size = 0;
		if(SharedData.gameAnalyzer.getSolidSprites().size() > 0){
			size = 2;
		}
		

		//get the level size
		int width = 10;
		int height = 10;
		
		System.out.println("Generation #1: ");
		ArrayList<Chromosome> fChromosomes = new ArrayList<Chromosome>();
		ArrayList<Chromosome> iChromosomes = new ArrayList<Chromosome>();
		for(int i =0; i < SharedData.POPULATION_SIZE; i++){

			//initialize the population using either randomly or using contructive level generator
			Chromosome chromosome = new Chromosome(width, height);
			if(SharedData.CONSTRUCTIVE_INITIALIZATION){
				chromosome.InitializeConstructive();
			}
			else{
				chromosome.InitializeRandom();
			}

			//calculate the fitness for all the chromosomes and add them to the correct population
			//either the feasible or the infeasible one
			chromosome.calculateFitness(SharedData.EVALUATION_TIME);
			if(chromosome.getConstrainFitness() < 1){
				iChromosomes.add(chromosome);
				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + chromosome.getConstrainFitness());
			}
			else{
				fChromosomes.add(chromosome);
				System.out.println("\tChromosome #" + (i+1) + " Fitness: " + chromosome.getFitness());
			}
		}
		

		//some variables to make sure not getting out of time
		double worstTime = SharedData.EVALUATION_TIME * SharedData.POPULATION_SIZE;
		double avgTime = worstTime;
		double totalTime = 0;
		int numberOfIterations = 0;

		System.out.println(elapsedTimer.remainingTimeMillis() + " " + avgTime + " " + worstTime);
		while(elapsedTimer.remainingTimeMillis() > 2 * avgTime &&
				elapsedTimer.remainingTimeMillis() > worstTime){
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			
			System.out.println("Generation #" + (numberOfIterations + 2) + ": ");
			

			//get the new population and split it to a the feasible and infeasible populations
			ArrayList<Chromosome> chromosomes = getNextPopulation(fChromosomes, iChromosomes);
			fChromosomes.clear();
			iChromosomes.clear();
			for(Chromosome c:chromosomes){
				if(c.getConstrainFitness() < 1){
					iChromosomes.add(c);
				}
				else{
					fChromosomes.add(c);
				}
			}
			
			numberOfIterations += 1;
			totalTime += timer.elapsedMillis();
			avgTime = totalTime / numberOfIterations;
		}
		

		//return the best infeasible chromosome
		if(fChromosomes.isEmpty()){
			for(int i=0;i<iChromosomes.size();i++){
				iChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
			}

			Collections.sort(iChromosomes);
			bestChromosomeLevelMapping = iChromosomes.get(0).getLevelMapping();
			System.out.println("Best Fitness: " + iChromosomes.get(0).getConstrainFitness());
			return iChromosomes.get(0).getLevelString(bestChromosomeLevelMapping);
		}
		
		//return the best feasible chromosome otherwise and print some statistics
		for(int i=0;i<fChromosomes.size();i++){
			fChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
		}
		Collections.sort(fChromosomes);
		bestChromosomeLevelMapping = fChromosomes.get(0).getLevelMapping();
		System.out.println("Best Chromosome Fitness: " + fChromosomes.get(0).getFitness());
		System.out.println(bestFitness);
		System.out.println(numOfFeasible);
		System.out.println(numOfInFeasible);
		return fChromosomes.get(0).getLevelString(bestChromosomeLevelMapping);
	}


	/**
	 * get the current used level mapping to create the level string
	 * @return	the level mapping used to create the level string
	 */
	@Override
	public HashMap<Character, ArrayList<String>> getLevelMapping(){
		return bestChromosomeLevelMapping.getCharMapping();
	}
}
