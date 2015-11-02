package pacman.entries.pacman;

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
		int bestEval = Integer.MIN_VALUE;
		
		System.out.println("###########");
		
		for(MOVE move : game.getPossibleMoves(game.getPacmanCurrentNodeIndex()))
		{
			int radioAfraid = 0;
			int radioNotAfraid = 50;
			int radioPill = 0;
			int moveEval = evaluationFunction(game, move, radioAfraid, radioNotAfraid, radioPill);
			System.out.println("Eval = " + moveEval);
			if(bestEval < moveEval)
			{
				bestMove = move;
				bestEval = moveEval;
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
		
		if (pacman == -1)
		{
			System.out.println("Invalid move.");
			return Integer.MIN_VALUE;
		}
		
		//get all active pills
		int[] activePills=game.getActivePillsIndices();
				
		//get all active power pills
		// int[] activePowerPills=game.getActivePowerPillsIndices();
				
		System.out.println(move);
		
		for(GHOST ghost : GHOST.values())
		{
			if(game.getGhostLairTime(ghost) > 0)	
			{
				if(game.getGhostEdibleTime(ghost) > 0)
				{
					if(game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)) < radioAfraid)
					{
						result += 4 * (radioAfraid - game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)));
					}
				}
				else
				{
					if(game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)) < radioNotAfraid)
					{
						result -= 1000 * (radioNotAfraid - game.getShortestPathDistance(pacman, game.getGhostCurrentNodeIndex(ghost)));
					} 
				}
			}
		}
		
//		for(int pill : activePills)
//		{
//			if(game.getShortestPathDistance(pacman, game.getPillIndex(pill)) < radioPill)
//			{
//				// result += radioPill - game.getShortestPathDistance(pacman, game.getPillIndex(pill));
//				result++;
//			}
//		}
		
		return result;
	}
}