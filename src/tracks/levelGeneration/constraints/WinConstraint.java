package tracks.levelGeneration.constraints;

import tracks.levelGeneration.geneticLevelGenerator.SharedData;
import ontology.Types;
import ontology.Types.WINNER;

public class WinConstraint extends AbstractConstraint {

	/**
	 * number of wins
	 */
	public double numWins;
	
	/**
	 * check if the player win the game
	 * @return 	1 if the automated player wins and 0 otherwise
	 */
	@Override
	public double checkConstraint() {
		return numWins / SharedData.NUM_AGENT_TRIALS;
	}
}
