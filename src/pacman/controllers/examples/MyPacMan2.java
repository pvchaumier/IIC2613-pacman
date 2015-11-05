package pacman.controllers.examples;

import java.util.ArrayList;
import pacman.controllers.Controller;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

import static pacman.game.Constants.*;

/*
 * This controller utilises 3 tactics, in order of importance:
 * 1. Get away from any non-edible ghost that is in close proximity
 * 2. Go after the nearest edible ghost
 * 3. Go to the nearest pill/power pill
 */
public class MyPacMan2 extends Controller<MOVE>
{	
	private static final int MIN_DISTANCE=20;	//if a ghost is this close, run away
	
	public MOVE getMove(Game game,long timeDue)
	{				
		for(GHOST g: GHOST.values()){
			System.out.println(g.name()+": "+game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(g)));
		}
		MOVE bestMove = MOVE.NEUTRAL;
		return bestMove;
	}
	

}






















