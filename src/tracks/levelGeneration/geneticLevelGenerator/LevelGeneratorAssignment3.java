package tracks.levelGeneration.geneticLevelGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.lang.Math;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

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
		/*Collections.sort(newPopulation);
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
			}*/

		/*
		 * NOTE: tentative code below
		 *
		 * The code below assumes that we have a single population called population
		 */
		newPopulation.addAll(fPopulation);
		newPopulation.addAll(iPopulation);

		newPopulation = greedyHypervolumeSubsetSelection(newPopulation, SharedData.POPULATION_SIZE);

		return newPopulation;
	}

	/**
	 * Return an array of all fitnesses in the population as points.
	 */
	private ArrayList<ArrayList<Double>> getFitnessPoints(ArrayList<Chromosome> population) {
		ArrayList<ArrayList<Double>> fitnessPoints = new ArrayList<ArrayList<Double>>();

		for (Chromosome c : population) {
			ArrayList<Double> point = new ArrayList<Double>();

			point.addAll(c.getFitness());

			fitnessPoints.add(point);
		}

		return fitnessPoints;
	}

	/**
 	 * Find the joint hypervolume contribution givem two points p1 and p2 and points.
	 */
	private double jointHypervolumeContribution(ArrayList<Double> p1, ArrayList<Double> p2, ArrayList<ArrayList<Double>> points) {
		ArrayList<Double> jointPoint = new ArrayList<Double>();

		for (int i = 0; i < p1.size(); i++) {
			jointPoint.add(Math.max(p1.get(i), p2.get(i)));
		}

		double indicator = getHypervolumeIndicator(points);

		points.add(jointPoint);

		double indicatorWithJoint = getHypervolumeIndicator(points);

		return indicatorWithJoint - indicator;
	}

	/**
	 * Use a greedy method to select the subset maximising hypervolume.
	 */
	private ArrayList<Chromosome> greedyHypervolumeSubsetSelection(ArrayList<Chromosome> population, int subsetSize) {
		ArrayList<Chromosome> subset = new ArrayList<Chromosome>();

		// get hypervolumes
		ArrayList<Double> contributions = new ArrayList<Double>();
		for (Chromosome individual : population) {
			double hypervolume = individual.getFitness().get(0) * individual.getFitness().get(1);
			contributions.add(hypervolume);
		}

		for (int i = 0; i < subsetSize; i++) {
			// find the largest contributor
			int largestContributorIndex = 0;
			double maxContribution = contributions.get(0);
			for (int j = 1; j < population.size(); j++) {
				if (maxContribution < contributions.get(j)) {
					largestContributorIndex = j;
					maxContribution = contributions.get(j);
				}
			}

			Chromosome largestContributor = population.get(largestContributorIndex);
			population.remove(largestContributorIndex);
			contributions.remove(largestContributorIndex);

			for (int j = 0; j < population.size(); j++) {
				ArrayList<Double> p1 = new ArrayList<Double>();
				p1.addAll(largestContributor.getFitness());

				ArrayList<Double> p2 = new ArrayList<Double>();
				p2.addAll(population.get(j).getFitness());

				contributions.set(j, contributions.get(j)
								  - jointHypervolumeContribution(p1, p2, getFitnessPoints(population)));
			}

			subset.add(largestContributor);
		}

		return subset;
	}

	/**
	 * Returns the number of points that a given point is dominated by in the given population.
	 */
	private int dominate(ArrayList<Double> point, ArrayList<ArrayList<Double>> set){
		Integer numSolutions = 0;
		Double pointX, pointY, otherX, otherY;
		pointX = point.get(0);
		pointY = point.get(1);

		for (int i = 0; i < set.size(); i++) {
			otherX = set.get(i).get(0);
			otherY = set.get(i).get(1);

			if (pointX<=otherX && pointY>otherY) {
				numSolutions = numSolutions + 1;
			}
			else if (pointY>=otherY && pointX<otherX) {
				numSolutions = numSolutions + 1;
			}
		}

		return numSolutions;
	}

	/**
	 * Retrieves the hypervolume indicator for a list of points
	 */
	private double getHypervolumeIndicator(ArrayList<ArrayList<Double>> points) {
		ArrayList<ArrayList<Double>> nondominatedPoints = new ArrayList<ArrayList<Double>>();
		//nondominatedPoints.addAll(points);

		// TODO: make this for-loop more efficient
		for (ArrayList<Double> c : points) {
			if (dominate(c, points)==(0))
				nondominatedPoints.add(c);
		}

		// (insertion) sort in terms of one of the fitnesses
		for (int i = 1; i < points.size(); i++) {
			int j = i;
			while ((j > 0) && (points.get(j).get(0) > points.get(j - 1).get(0))) {
				Collections.swap(points, i, j);
				j--;
			}
		}

		ArrayList<Double> current = nondominatedPoints.get(0);
		double indicatorSum = current.get(0) * current.get(1);
		for (int i = 1; i < nondominatedPoints.size(); i++) {
			ArrayList<Double> previous = current;
			current = nondominatedPoints.get(i);
			indicatorSum += current.get(0) * current.get(1)
				- previous.get(0) * previous.get(1);
		}

		return indicatorSum;

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
	//

	/**
	 * Outputs the fitness and the level file
	 */
	public void outputDetailsToFile(Chromosome c, String filename) {
		try {
			String levelString = c.getLevelString(c.getLevelMapping());
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

			writer.write(String.format("fitness: %f, %f", c.getFitness().get(0), c.getFitness().get(1)));
			writer.newLine();
			writer.write("LevelMapping:");
			writer.newLine();
			for (Map.Entry<Character, ArrayList<String>> e : c.getLevelMapping().getCharMapping().entrySet()) {
				writer.write("    " + e.getKey() + " > ");
				for (String s : e.getValue()) {
					writer.write(s + " ");
				}
				writer.newLine();
			}
			writer.newLine();
			writer.write("LevelDescription");
			writer.newLine();
			writer.write(levelString);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
		int width = SharedData.LEVEL_WIDTH;
		int height = SharedData.LEVEL_HEIGHT;
		
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
		for (int i = 0; i < SharedData.NUM_GENERATIONS; i++) {
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			
			System.out.println("Generation #" + (numberOfIterations + 2) + ": ");
			

			//get the new population and split it to a the feasible and infeasible populations
			ArrayList<Chromosome> chromosomes = getNextPopulation(fChromosomes, iChromosomes);
			fChromosomes.clear();
			iChromosomes.clear();
			for (int j = 0; j < chromosomes.size(); j++) {
				Chromosome c = chromosomes.get(j);
				outputDetailsToFile(c, String.format("generation%d-chromosome%d.txt", numberOfIterations+2, j));

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
