package pacman.controllers.examples;

import java.util.EnumMap;
import java.util.Random;

import pacman.controllers.Controller;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class RanDetGhost extends Controller<EnumMap<GHOST,MOVE>>{
	private EnumMap<GHOST,MOVE> moves=new EnumMap<GHOST,MOVE>(GHOST.class);
	private MOVE[] allMoves=MOVE.values();
	private Random rnd=new Random();
	EnumMap<GHOST,MOVE> myMoves=new EnumMap<GHOST,MOVE>(GHOST.class);
	private final static float CONSISTENCY=0.9f;	//attack Ms Pac-Man with this probability
	private final static int PILL_PROXIMITY=15;
	/* (non-Javadoc)
	 * @see pacman.controllers.Controller#getMove(pacman.game.Game, long)
	 */
	public EnumMap<GHOST,MOVE> getMove(Game game,long timeDue) 
	{	
		moves.clear();
		
		for(GHOST ghost : GHOST.values())
			if(game.doesGhostRequireAction(ghost))
				if(rnd.nextBoolean()){
				moves.put(ghost,allMoves[rnd.nextInt(allMoves.length)]);
				}
				else{
					if(game.getGhostEdibleTime(ghost)>0 || closeToPower(game))	//retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
						myMoves.put(ghost,game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
								game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
					else 
					{
						if(rnd.nextFloat()<CONSISTENCY)			//attack Ms Pac-Man otherwise (with certain probability)
							myMoves.put(ghost,game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
									game.getPacmanCurrentNodeIndex(),game.getGhostLastMoveMade(ghost),DM.PATH));
						else									//else take a random legal action (to be less predictable)
						{					
							MOVE[] possibleMoves=game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost),game.getGhostLastMoveMade(ghost));
							myMoves.put(ghost,possibleMoves[rnd.nextInt(possibleMoves.length)]);
						}
					}
				}
		
		return moves;
	}
	
	private boolean closeToPower(Game game)
    {
    	int[] powerPills=game.getPowerPillIndices();
    	
    	for(int i=0;i<powerPills.length;i++)
    		if(game.isPowerPillStillAvailable(i) && game.getShortestPathDistance(powerPills[i],game.getPacmanCurrentNodeIndex())<PILL_PROXIMITY)
    			return true;

        return false;
    }
}
