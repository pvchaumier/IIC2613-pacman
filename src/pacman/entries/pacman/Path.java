package pacman.entries.pacman;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * Stores information about the path between 2 adjacent junctions
 */
public class Path {
	public int	from;	//Node Index
	public int	to;		//Node Index
	public int	dir;	//Direction to head from the "from" node
	public Route route;	//The distance between the nodes and the direction of arrival
	
	public int	scoreBefore;	//Total score of pills before reaching a power pill
	public int	score;	//Total score of pills and power pills
	public int	hunter;	//The distance to the first hunting ghost heading towards "from" (or -1)
	public int	edible;	//The distance to the first edible ghost (or -1)
	public int	pill;	//The distance to the first pill in the path
	public int	lastPill; //The nodeID of the last pill in the path
	public int	lastPillDist;
	public int	power;	//The distance to the first power pill in the path
	public int	pacman;	//The distance to the pacman (or -1)
	public int[] ghosts; //The distance to each Ghost (if on the path)
	public boolean[] ghostTowardsHead;	//true if the ghost is heading towards the head of the path
	public Route found;	//The distance and arrival dir to the node passed in during creation
	private Game game;
	
	public Path(int from , int dir, Game g) {
		game = g;
		scanPath(from , dir, -1);
	}
	
	public Path(int from, int dir, int search, Game g) {
		game = g;
		scanPath(from , dir, search);
	}
	
	private void scanPath(int from , int dir, int search) {
		this.from = from;
		this.dir = dir;
		
		score = 0;
		scoreBefore = 0;
		hunter = -1;
		edible = -1;
		pill = -1;
		power = -1;
		pacman = -1;
		found = null;
		ghosts = new int[4];
		ghostTowardsHead = new boolean[4];
		for (int i=0; i<4; i++)
			ghosts[i] = -1;
		
		int d = dir;	
		int here = from;
		int dist = 0;
		
		boolean atEnd = (dir == -1);
		while (atEnd == false) {
			//Check for active Pill
	
			int p = game.getPillIndex(here);
			if (p != -1) {
				if (pill == -1)
					pill = dist;
				score += 10;
				lastPill = here;
				lastPillDist = dist;
			}
			//Check for active PowerPill
			p = game.getPowerPillIndex(here);
			if (p != -1) {
				if (power == -1)
					power = dist;
				scoreBefore = score;
				score += 50;
			}
			//Check for pacman
			if (here == game.getPacmanCurrentNodeIndex()) {
				pacman = dist;
				hunter = -1; //If we have a hunter ghost behind us, discount it
			}
			//Check for ghosts
			int i = 0;
			for (GHOST g: GHOST.values()) {
				if (game.getGhostCurrentNodeIndex(g) == here) {
					ghosts[i] = dist; 
					ghostTowardsHead[i] = (game.getGhostLastMoveMade(g).ordinal() != d);
					if (hunter == -1 && (!game.isGhostEdible(g) || game.getGhostEdibleTime(g) < dist) && ghostTowardsHead[i])
						hunter = dist;
					if (edible == -1 && game.isGhostEdible(g) && game.getGhostEdibleTime(g) >= dist)
						edible = dist;
				}
				i++;
			}
			//Check for optional search node
			if (here == search)
				found = new Route(dir, d, dist);
			
			if (here != from && game.isJunction(here))
				atEnd = true;
			else {
				if (game.getNeighbour(here, MOVE.values()[d]) == -1) { //Pick first valid direction that is not where we came from
					dir = d;
					d = 0;
					while (game.getNeighbour(here, MOVE.values()[d]) == -1 || d == MOVE.values()[d].opposite().ordinal())
						d++;
				}
				if (d > 3)
					atEnd = true;	//Copes with lair node which has no valid directions
				else {
					here = game.getNeighbour(here, MOVE.values()[d]);				
					dist++;
				}
			}
		}
		this.to = here;
		if (power == -1)
			scoreBefore = score;
		route = new Route(dir, d, dist); //Store arrival direction and total path distance
		//System.out.printf("Path (%d,%d) %d goes to (%d, %d) arrives %d dist %d\n", g.getX(this.from), g.getY(this.from), this.dir, g.getX(this.to), g.getY(this.to), route.dir, route.dist);
	}
	
	//Returns true if there is a hunter ghost on the path - one at the end point doesn't count as it may not come this way
	public boolean hasHunter() {
		return (hunter != -1 && hunter != route.dist);
	}
	
	public boolean hasGhost(int g) {
		return ghosts[g] != -1;
	}
	
	//Does given ghost block the pacman reaching the head of the path
	public boolean ghostBlocks(int g) {
		return (hasGhost(g) && !game.isGhostEdible(GHOST.values()[g]) && ghostTowardsHead[g] == false && (pacman == -1 || pacman > ghosts[g]));
	}
}

