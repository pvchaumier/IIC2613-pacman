package pacman.entries.pacman;


import java.util.ArrayList;

import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * Stores the information about a maze's topology
 * This includes the static list of all junctions and the distances between them
 * and the dynamic information about paths between junctions which is updated each game tick with the current score and positions of the moving elements
 */
public class Topology {
	final private int	mazeNumber;
	final private ArrayList<Junction>	junctions;
	final private int[][][][] distances;
	final private int startJunction; //The index of the junction nearest the ghost's starting position
	private int scoreRemaining;		//The total score of the remaining pills and power pills
	private int scoringPaths;		//The number of paths with pills on them
	
	public Topology(Game game) {
		mazeNumber = 1;
		junctions = new ArrayList<Junction>();
		
		int[]	indices = game.getJunctionIndices();
		
		//Add the junctions
		for (int i=0; i< indices.length; i++) {
			Junction j = new Junction(indices[i], i);

			for (int d = 0; d<4; d++)
				if (game.getNeighbour(j.nodeIndex, MOVE.values()[d]) != -1)
					j.addPath(d, game);
			junctions.add(i, j);
		}
			
		//Now we can go back and fill in the array of adjacent junctions (next)
		for (Junction j: junctions)			
			for (int d = 0; d<4; d++) {
				int n = j.getNeighbourNode(d);
				if (n != -1)
					j.next[d] = getJunctionIndex(n);
				else
					j.next[d] = -1;
			}
				
		//Now create an array of distances from one junction to another based on starting and ending directions of travel.
		distances = new int[junctions.size()][junctions.size()][4][4];
		
		for (int r = 0; r<junctions.size(); r++) {
			for (int dir=0; dir<4; dir++) {
				Junction me = junctions.get(r);
				if (me.getNeighbourJn(dir) != -1) {
					travel(r,dir,me.getNeighbourJn(dir),me.getArrivalDir(dir),me.getDistance(dir));
				}
			}
		}
		
		//Find and store the junction nearest the ghost start position
		int s = game.getInitialGhostsPosition();
		Path p = new Path(s, Game.INITIAL_GHOST_DIRS[0], game);
		startJunction = getJunctionIndex(p.to);	
	}
	
	private int getReverse(int direction)
	{
		switch(direction)
		{
			case 0: return 2;
			case 1: return 3;
			case 2: return 0;
			case 3: return 1;
		}		
		return 4;
	}
	
	public int getStartJunction() {
		return startJunction;
	}
	
	/*
	 * Finds a ghost's next valid direction given a node and current direction
	 * Only works for ghosts on a path (i.e. not a junction)
	 */
	private int getValidDir(Game game, int n, int cur) {
		if (game.getNeighbour(n, MOVE.values()[cur]) != -1)
			return cur;
		int d = 0;
		while (d == getReverse(cur) || game.getNeighbour(n, MOVE.values()[d]) == -1)
			d++;
		
		if (d == 4)
			return (-1);
		return d;
	}
	
	/*
	 * Recursive routine to calculate the distances from a junction to all other junctions
	 * There is a separate distance recorded for each starting and arriving direction (4x4 results)
	 */
	private void travel(int fromJn, int fromDir, int toJn, int toDir, int curDist) {
		if (distances[fromJn][toJn][fromDir][toDir] == 0 || curDist < distances[fromJn][toJn][fromDir][toDir]) {
			distances[fromJn][toJn][fromDir][toDir] = curDist;
			for (int dir=0; dir<4; dir++) {
				Junction me = getJunction(toJn);
				if (me.getNeighbourJn(dir) != -1 && toDir != getReverse(dir)) {
					travel(fromJn, fromDir, me.getNeighbourJn(dir), me.getArrivalDir(dir), curDist+me.getDistance(dir));
				}
			}
		}
	}
	
	public int getCurMaze() {
		return mazeNumber;
	}
	
	public Junction getJunction(int i) {
		if (i<0 || i>=junctions.size())
			return null;
		return junctions.get(i);
	}
	
	/*
	 * Store the current path information, this includes anything that can change such as pill scores, ghost and pacman positions
	 */
	public void update(Game game) {
		scoringPaths = 0;
		scoreRemaining = 0;
		for (Junction j: junctions)
			for (int d = 0; d<4; d++)
				if (game.getNeighbour(j.nodeIndex, MOVE.values()[d]) != -1) {
					j.addPath(d, game);
					if (j.paths[d].score > 0) {
						scoreRemaining += j.paths[d].score;
						scoringPaths++;
					}
				}
		
		//Some junctions have a pill - this mean some paths appear to have only 1 pill (at the start or end) and these pills
		//are counted in other paths leading from this junction - remove the singletons for simplicity
		for (Junction j: junctions)
			for (int d = 0; d<4; d++) {
				if (j.paths[d] != null && j.paths[d].score == 10 && j.paths[d].pill == 0) {
					//Check to see if this pill is scored in another path from here
					boolean scored = false;
					for (int p=0; p<4; p++) {
						if (p != d && j.paths[p] != null && j.paths[p].pill == 0 && j.paths[p].score > 0)
							scored = true;
					}
					if (scored) { //Remove this one
						j.paths[d].score = 0;
						j.paths[d].pill = -1;
						j.paths[d].lastPill = -1;
						j.paths[d].lastPillDist  =-1;
						//Now remove the corresponding pill from the path heading back towards us
						Junction end = getJunction(j.getNeighbourJn(d));
						int dir = MOVE.values()[(j.getArrivalDir(d))].opposite().ordinal();
						end.paths[dir].score = 0;
						end.paths[dir].pill = -1;
						end.paths[dir].lastPill = -1;
						end.paths[dir].lastPillDist = -1;
					}
				}
			}
	}
	
	public int scoreRemaining() {
		return scoreRemaining;
	}
	
	public int scoringPaths() {
		return scoringPaths;
	}
	
	public ArrayList<Junction> getJunctions() {
		return junctions;
	}
	
	public int getJunctionIndex(int node) {
		for (Junction j: junctions)
			if (j.nodeIndex == node)
				return j.index;
		return -1;
	}
	
	/*
	 * Find shortest ghost route between 2 nodes returning the distance and initial direction of travel
	 * The starting direction is passed in and an optional banned arrival direction (-1 indicating any direction is allowed)
	 *
	 * There are 4 main options to consider
	 * A. The start point is a junction and the end point is a junction
	 * B. The start point is a junction and the end point is not a junction
	 * C. The start point is not a junction and the end point is a junction
	 * D. The start point is not a junction and the end point is not a junction
	 * 
	 * For cases A and B we don't know the starting direction but it can't be the reverse of the one supplied as a ghost can't reverse direction.
	 * For cases B and D we either have 1 or 2 junctions from which to approach the end point. If one of the arrival directions is banned then the case drops down to A or C.
	 * 
	 * We can strip out some simple cases with minimal processing
	 * 1. The start and end points are the same and the start direction is not banned - we are already here - return distance = 0
	 * 2. When travelling from the start point (C,D) to the first junction, if we find the end point and the direction of arrival is not banned we can return the distance travelled so far
	 * 3. If a valid junction from the end point is found to be the start point (special case of B) and ghost can head this way, we don't need to add in an inter-junction distance.
	 * 
	 *  All other cases involve adding in the distance from the start to the first Junction and the distance from the end to a valid junction to the inter-junction distance
	 */
	public Route getGhostRoute(Game game, int from, int startDir, int to, int dontArriveFacing) {
		int fjn;	//Junction ID of first junction in route
		int LeavingDir; //The direction leaving the "from" node
		int fjnArrivalDir; //The direction of arrival at the start junction
		int dist; //Distance travelled so far
		
		//Check to see we are already here - Special case 1
		if (from == to && startDir != dontArriveFacing)
			return new Route(startDir, startDir, 0);
		
		/*
		 * Set up the starting junction variables (fjn, LeavingDir)
		 * It is either the starting point (if it is a junction) or the junction at the end of the path in the current direction if not
		 */
		if (game.isJunction(from)) {
			LeavingDir = -1;	//We don't know it at this point - but it can't be the reverse of startDir;
			fjnArrivalDir = startDir;
			fjn = getJunctionIndex(from);	
			dist = 0;
		} else {
			LeavingDir = startDir;	
			Path p = new Path(from, getValidDir(game, from, startDir), to, game);
			if (p.found != null && p.found.arrivalDir != dontArriveFacing) //We found the end point on the path - Special case 2
				return p.found;
			
			fjn = getJunctionIndex(p.to);
			if (fjn == -1)
				return null;
			dist = p.route.dist;
			fjnArrivalDir = p.route.arrivalDir;
		}
		/*
		 * We now have a starting junction, a distance travelled to get here and the arrival direction at this point
		 * We also know the direction of travel from the start point if it was not a junction, otherwise this is set to -1 and filled in later
		 *
		 * Next set up the valid end point junctions.
		 * If the End point is a junction - use that one. If not then there can be 2 junctions to approach it from.
		 * These are stored in an array (size 2). The second entry is set to -1 if there is only one valid approach direction
		 */
		int ix = 0;
		int[] tjn = new int[2];
		int[] tjndir = new int[2]; //Holds the best direction to leave the start jn to arrive at the end jn
		int[] tjnarrival = new int[2]; //Holds the direction of arrival at the end jn
		int[] tjndist = new int[2]; // Holds the route from the final junction to the "to" node
		int[] banned = new int[2]; //Contains the direction we cannot arrive at the end junction in
		boolean[] loop = new boolean[2]; //Whether we need to loop around and add in the inter-junction distance
		
		tjn[1] = -1;
		if (game.isJunction(to)) {
			tjn[0] = getJunctionIndex(to);
			tjndist[0] = 0;
			banned[0] = dontArriveFacing;
			loop[0] = true;
		} else { //Find Junctions at either end of target "to" node
			for (int dir = 0; dir < 4; dir++) {
				if (game.getNeighbour(to, MOVE.values()[dir]) != -1 && MOVE.values()[dir].opposite().ordinal() != dontArriveFacing) {
					Path p = new Path(to, dir, game);
					loop[ix] = true;
					if (p.to == from && p.route.arrivalDir != fjnArrivalDir) {//Special Case 3
						loop[ix] = false;
						tjndir[ix] = MOVE.values()[p.route.arrivalDir].opposite().ordinal();
						tjnarrival[ix] = MOVE.values()[dir].opposite().ordinal();
					}

					tjn[ix] = getJunctionIndex(p.to);
					tjndist[ix] = p.route.dist;
					banned[ix] =  p.route.arrivalDir;
					if (tjn[ix] == fjn) {
						loop[ix] = false;
						tjnarrival[ix] = MOVE.values()[dir].opposite().ordinal();
					}
					ix++;
				}
			}
		}

		/*
		 * Loop through each possible end junction and work out the distance from the fjn to the end point
		 */
		for (int end=0; end<2; end++) {
			tjndir[end] = LeavingDir;
			if (tjn[end] != -1 && loop[end]) {
				/*
				 * Add in the travel distance between the from Jn and the End Jn
				 * Search for the shortest route given the arrival and banned options
				 */
				int shortest = Integer.MAX_VALUE;
				int invalidDir = MOVE.values()[fjnArrivalDir].opposite().ordinal();
				int bestDir = -1;

				for (int fdir=0; fdir<4; fdir++) {
					for (int tdir=0; tdir<4; tdir++) {
						if (fdir != invalidDir && tdir != banned[end]) {
							int d = distances[fjn][tjn[end]][fdir][tdir];
						
							if (d > 0 && (d < shortest || (d == shortest && Game.rnd.nextInt(2) == 0))) {
								shortest = d;
								bestDir = fdir;
								tjnarrival[end] = tdir;
							}
						}
					}
				}
				if (LeavingDir == -1) //If we don't know the starting direction (because we are at a junction) use the best direction found
					tjndir[end] = bestDir;
				tjndist[end] += shortest;
			}
		}
		
		//Pick shortest route
		if (tjn[1] == -1 || tjndist[0] < tjndist[1] || (tjndist[0] == tjndist[1] && Game.rnd.nextInt(2) == 0))
			ix = 0;
		else
			ix = 1;
		
		//System.out.printf("shortestRoute((%d,%d) %d to (%d,%d) %d) = (%d,%d)\n",game.getX(from), game.getY(from), startDir, game.getX(to), game.getY(to), dontArriveFacing, tjndir[ix], dist + tjndist[ix]);
		return new Route(tjndir[ix], tjnarrival[ix], dist + tjndist[ix]);
	}
}
