package pacman.entries.pacman;

import java.util.Map.Entry;

import pacman.controllers.Controller;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getAction() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.pacman.mypackage).
 */
public class MyPacMan extends Controller<MOVE>
{
	private MOVE myMove=MOVE.NEUTRAL;
	
	public MOVE getMove(Game game, long timeDue) 
	{
		return minimax(game);
	}
	
	public MOVE minimax(Game game)
	{
		MOVE bestMove = myMove;
		int best = Integer.MIN_VALUE;
		
		for(MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex()))
		{
			int radioAfraid = 0;
			int radioNotAfraid = 100;
			int radioPill = 0;
			System.out.println(evaluationFunction(game, move, radioAfraid, radioNotAfraid, radioPill));
			if(best < evaluationFunction(game, move, radioAfraid, radioNotAfraid, radioPill))
			{
				bestMove = move;
				best = evaluationFunction(game, move, radioAfraid, radioNotAfraid, radioPill);
			}
		}
		
		System.out.println("best move is " + bestMove);
		return bestMove;
	}
	
	public int evaluationFunction(Game game, MOVE move, int radioAfraid, int radioNotAfraid, int radioPill)
	{
		int result = 0;
		int pacman_old = game.getPacmanCurrentNodeIndex();
		int pacman = game.getNeighbour(pacman_old, move);
		//System.out.println(pacman_old + " " + pacman);
		
		//get all active pills
		int[] activePills=game.getActivePillsIndices();
				
		//get all active power pills
		// int[] activePowerPills=game.getActivePowerPillsIndices();
				
		for(GHOST ghost : GHOST.values())
		{
			if(game.doesGhostRequireAction(ghost))	
			{
				if(game.getGhostEdibleTime(ghost)>0)
				{
					if(game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)) < radioAfraid)
					{
						result += 4 * (radioAfraid - game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)));
					}
				}
				else
				{
					System.out.println("Shortest Distance" + game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)));
					if(game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)) < radioNotAfraid)
					{
						result -= 1000 * (radioNotAfraid - game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)));
					} 
				}
			}
		}
		
		for(int pill : activePills)
		{
			if(game.getShortestPathDistance(pacman, game.getPillIndex(pill)) < radioPill)
			{
				result += radioPill - game.getShortestPathDistance(pacman, game.getPillIndex(pill));
			}
		}
		
		return result;
	}
}