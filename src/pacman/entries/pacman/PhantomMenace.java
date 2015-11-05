package pacman.entries.pacman;

import java.awt.Color;
import java.util.ArrayList;

import pacman.*;
import pacman.controllers.Controller;
import pacman.game.Constants.MOVE;

public class PhantomMenace {
	final int[]						dirs;		//Stores the direction for the 4 ghosts and the pacman
	private Game					game;		//The current game position
	private int						ticks;		//Game tick - stores the last processed result
	ArrayList<PointOfInterest>		poi;		//A list of all points of interest in the maze - the junctions plus pacman position
	ArrayList<PathOfInterest>		paths;		//A list of all the paths leading to the pacman that are used to evaluate the ghost blocking pattern
	private int						pmid;		//Index into Poi for pacman
	private static final boolean	SHOW_ROUTES = false;
	private static final boolean	CORE_PATH_DEBUG = false;
	private static final boolean	SHOW_ESCAPE_ROUTES = false;
	private static final int		MAX_OPTIONS = 64;
	final private ArrayList<Junction>	junctions;
	
	public PhantomMenace() {
		dirs = new int[Game.NUM_GHOSTS+1]; //Last entry is Pacman direction
		maze = null;
		ticks = -1;
		poi = null;
		paths = null;
	}
	
	public int[] getActions(Game game, long timeDue) {
		int[] results = new int[Game.NUM_GHOSTS];
		int[] hasOptions = new int[Game.NUM_GHOSTS];
		
		storeGame(game, true);
		
		boolean[][] reached = makeReachable(poi, paths, Game.NUM_GHOSTS);
		blockPossible(reached, true); //Solve as far as possible - then we loop over the remaining options
		
		for (int g = 0; g < Game.NUM_GHOSTS; g++) {
			int count = 0;
			for (int j = 0; j < paths.size(); j++) {
				if (reached[g][j]) count++;
			}
			
			if (count == 0) //This ghost has no valid options				
				hasOptions[g] = -1;
			else
				hasOptions[g] = 0;
		}
				
		double bestScore = 0;		
		int curPath[] = new int[Game.NUM_GHOSTS];
		int bestPath[] = new int[Game.NUM_GHOSTS];
		Heuristics h = new Heuristics(game, maze, paths, poi, pmid);
		int power = h.checkPowerPill();
		
		/*
		 * Now we have a semi-solved state - we scan over all possible solutions and pick the best one
		 */
		for (curPath[0]=hasOptions[0]; curPath[0]<paths.size(); curPath[0]++)
			if (curPath[0] == -1 || reached[0][curPath[0]])
				for (curPath[1]=hasOptions[1]; curPath[1]<paths.size(); curPath[1]++)
					if (curPath[1] == -1 || reached[1][curPath[1]])
						for (curPath[2]=hasOptions[2]; curPath[2]<paths.size(); curPath[2]++)
							if (curPath[2] == -1 || reached[2][curPath[2]])
								for (curPath[3]=hasOptions[3]; curPath[3]<paths.size(); curPath[3]++) 
									if (curPath[3] == -1 || reached[3][curPath[3]]) {
										h.addBlocks(curPath, false);
										double thisScore = h.score();
										if (thisScore > bestScore) {
											bestScore = thisScore;
											for (int g=0; g<Game.NUM_GHOSTS; g++)
												bestPath[g] = curPath[g];
										}
									}
		
		if (bestScore > 0) {
			if (SHOW_ESCAPE_ROUTES) h.addBlocks(bestPath, true);
			for (int g = 0; g<Game.NUM_GHOSTS; g++) {
				/*
				 * Now pick the direction based on the path we are to block
				 * A block of -1 indicates we have no good option
				 * 	Edible ghosts run away
				 *  Hunter ghosts head towards the pacman
				 */				
				if (bestPath[g] == -1) { //Either move towards or away from pacman depending on if we are edible or the pacman can access a power pill
					if (game.ghostRequiresAction(g)) {
						if (game.isEdible(g))
							dirs[g] = disperse(g);
						else
							dirs[g] = game.getNextGhostDir(g, game.getCurPacManLoc(), (power == -1), Game.DM.PATH);		
					}
				} else {
					PathOfInterest p = paths.get(bestPath[g]);
					PointOfInterest point = poi.get(p.jnIndex);
					if (game.getCurGhostLoc(g) == point.nodeIndex) //We are at the junction - head in the given path direction
						dirs[g] = p.dir;
					else
						dirs[g] = point.ghosts[g].startDir;
					if (SHOW_ROUTES) showRoute(g, p);
				}
			}
		}

		for (int g=0; g<results.length; g++)
			results[g] = dirs[g];
		return results;
	}
	
	public int getAction(Game game, long timeDue) {
		storeGame(game, false);
		Heuristics h = new Heuristics(game, maze, paths, poi, pmid);
		h.checkPowerPill();
		h.findEscape(false);
		
		//The paths in the list are meant for the ghosts - as the pacman we reverse them
		//The junction is set to the end point and the direction to the reverse of the arrival direction
		for (PathOfInterest p: paths) {
			Junction j = maze.getJunction(p.jnIndex);
			if (j != null) {
				p.jnIndex = j.getNeighbourJn(p.dir);
				p.dir = game.getReverse(j.getArrivalDir(p.dir));				
			}
		}
		return h.solve(paths, poi);
	}
	
	/*
	 * Called when an edible ghosts needs a place to hide
	 * Head away from the pacman and away from other edible ghosts and towards hunting ghosts
	 * Return the direction of travel
	 */
	private int disperse(int me) {
		int best = -1;
		double score = -100;
		int dist[] = new int[Game.NUM_GHOSTS];
		int nodes[] = new int[Game.NUM_GHOSTS];
		double scores[] = new double[Game.NUM_GHOSTS]; //Points awarded for moving towards node
		
		for (int g=0; g<Game.NUM_GHOSTS; g++) {
			if (g == me) {
				nodes[g] = game.getCurPacManLoc();
				scores[g] = -16;
			} else {
				nodes[g] = game.getCurGhostLoc(g);
				if (game.isEdible(g))
					scores[g] = -3;
				else
					scores[g] = 1;
			}
			dist[g] = game.getPathDistance(game.getCurGhostLoc(me), nodes[g]);
			if (dist[g] != 0)
				scores[g] /= dist[g];
		}
		
		for (int d=0; d<4; d++) {
			double myScore = 0;
			int node = game.getNeighbour(game.getCurGhostLoc(me), d);
			if (node != -1 && d != game.getReverse(game.getCurGhostDir(me))) {
				for (int g=0; g<Game.NUM_GHOSTS; g++) {
					int x = game.getPathDistance(node, nodes[g]);
					if (x < dist[g])
						myScore += scores[g];
					if (x > dist[g])
						myScore -= scores[g];
				}
				if (myScore > score) {
					score = myScore;
					best = d;
				}
			}
		}
		//System.out.printf("Tick %d - Ghost %d disperses %d\n", ticks, me, best);
		return best;	
	}
	
	/*
	 * Store the Game state
	 * Work out the distance to each junction for each ghost and pacman
	 * Ripple out along valid paths from the pacman until a ghost blocking solution is possible
	 */
	public void storeGame(Game game, boolean breakout) {	
		if (ticks == game.getLevelTime()) //If we have already been called - return those results without recalculation
			return;	
		
		ticks = game.getLevelTime();
		this.game = game;
		
		if (maze == null || game.getCurMaze() != maze.getCurMaze())
			maze = new Topology(game);
		else
			maze.update(game);
		
		poi = new ArrayList<PointOfInterest>();
		paths = new ArrayList<PathOfInterest>();
		PointOfInterest pm = null; //The pacman's point
		pmid = -1; //Array index of pacman in poi
		
		for (Junction j: maze.getJunctions()) {
			PointOfInterest p = new  PointOfInterest(j.nodeIndex, j.index);
			poi.add(j.index, p); //Poi is accessed as the Junction array but may contain 1 extra entry at the end for the pacman
			if (j.nodeIndex == game.getCurPacManLoc()) {
				pm = p;
				pmid = j.index;
			}
		}
		
		boolean[] pointAdded = new boolean[poi.size()+1]; //Set to true if ALL paths from this junction have been added to the path list
		if (pm == null) { // Create a POI for the pacman
			pmid = poi.size();
			pm = new PointOfInterest(game.getCurPacManLoc(), pmid);
			int n = game.getCurPacManLoc();
			for (int d = 0; d<4; d++) { //Add in the paths leading from the adjacent points
				if (game.getNeighbour(n, d) != -1) {
					Path p = new Path(n, d, game);
					int j = maze.getJunctionIndex(p.to);
					paths.add(new PathOfInterest(j, game.getReverse(p.route.arrivalDir)));
					if (!p.hasHunter() && p.power == -1) //Walk the path if there is no hunter ghost or power pill on it.								
						walkPoints(poi, poi.get(j), p.route.dist, d, p.route.arrivalDir, "/-1");
				}
			}
			poi.add(pmid, pm);
			pointAdded[pmid] = true;
		} else {
			walkPoints(poi, pm, 0, -1, -1, ""); //Populate the points with the distance from the pacman taking into account any blocked paths
		}		
		
		for (PointOfInterest p: poi) {
			p.assignDistances(game, maze);
			p.assignBlock();
		}
		
		boolean[][] reached = makeReachable(poi, paths, Game.NUM_GHOSTS);
		boolean solved = blockPossible(reached, false);
		while (!solved) {
			PointOfInterest nearest;
			ArrayList<PathOfInterest> lastAdded = new ArrayList<PathOfInterest>();
			//Add in the next set of paths - find the nearest point of interest without all its paths added
			//Keep doing this until we add a path that is blockable, then we check to see if a solution is possible
			nearest = findNearestPoint(poi, pointAdded);
			if (nearest == null)
				break;
			Junction j = maze.getJunction(nearest.jnIndex);				
			//Add in the paths heading back to this junction
			for (int d = 0; d<4; d++) {
				if (j.getNeighbourJn(d) != -1 && !pathExists(paths, j.getNeighbourJn(d), game.getReverse(j.getArrivalDir(d))) &&
						(j.paths[d].power != -1 || !pathExists(paths, nearest.jnIndex, d))) {
					PathOfInterest p = new PathOfInterest(j.getNeighbourJn(d), game.getReverse(j.getArrivalDir(d)));
					if (CORE_PATH_DEBUG) System.out.printf("Adding path from jn %d dir %d to block %d\n", j.getNeighbourJn(d), game.getReverse(j.getArrivalDir(d)), nearest.jnIndex);
					paths.add(p);
					lastAdded.add(p);
				}
			}
			pointAdded[nearest.jnIndex] = true;

			reached = makeReachable(poi, paths, Game.NUM_GHOSTS);
			if (breakout && countReachable(reached) > MAX_OPTIONS) { //We don't have time to process any more options
				for (PathOfInterest p: lastAdded)
					paths.remove(p);
				break;
			}
			solved = blockPossible(reached, false);
		}		
		//System.out.printf("Tick %d - %d Options to score from %d paths\n", ticks, countReachable(reached), paths.size());	
	}	
	
	private void showRoute(int g, PathOfInterest p) {
		Junction j = maze.getJunction(p.jnIndex);
		Color c;
		
		switch (g) {
		case 0:	c = Color.RED; break;
		case 1: c = Color.PINK; break;
		case 2: c = Color.ORANGE; break;
		case 3: c = Color.CYAN; break;
		default: c = Color.white; break;
		}
		GameView.addPoints(game, c, j.nodeIndex);
		GameView.addLines(game, c, j.nodeIndex, maze.getJunction(j.getNeighbourJn(p.dir)).nodeIndex);
	}
	
	private boolean pathExists(ArrayList<PathOfInterest> list, int j, int d)
	{
		for (PathOfInterest p: list) {
			if (p.jnIndex == j && p.dir == d) {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * Check the route to this point to ensure that the given ghost is not edible anywhere along it
	 * where the pacman can reach it. Only called if we are currently edible.
	 */
	public boolean routeEdible(PointOfInterest here, int g) {
		//If ghost is edible at the end point and can be reached there is no point scanning all the path to get here
		if (here.path == null || (here.edible[g] && here.pacman != null && here.ghosts[g].dist >= here.pacman.dist + Game.EAT_DISTANCE))
			return true;
		
		String[] s = here.path.split("/");
		int i = 1;
		int j = -1;
		int prev = -1;
		boolean isEdible = true;
		while (i < s.length && isEdible) {
			try {
				j = Integer.parseInt(s[i]);
				if (j>=0) {
					PointOfInterest me = poi.get(j);
					isEdible = me.edible[g];
					if (me.pacman != null && me.ghosts[g].dist >= me.pacman.dist + Game.EAT_DISTANCE)
						return true;
					if (isEdible)
						prev = j;
				}
				
			} catch (Exception e){} 
			i++;
		}
		//Finally check that the final path doesn't have the pacman on it
		if (j == prev)
			j = here.jnIndex;
		if (prev != -1 && j != -1) {
			Junction jn = maze.getJunction(prev);
			for (int d=0; d<4; d++) {
				if (jn.getNeighbourJn(d) == j) {
					return (jn.paths[d].pacman != -1);
				}
			}
		}
		return false;
	}
	
	/*
	 * Create an array of true/false values indicating whether a ghost can get to junction before the PacMan
	 * If the ghosts is already on the path mark it as reachable
	 */
	private boolean[][] makeReachable(ArrayList<PointOfInterest> poi, ArrayList<PathOfInterest> paths, int ghosts) {
		if (paths.size() == 0)
			return null;
		
		boolean[][] reachable = new boolean[ghosts][paths.size()];
		
		for (int j = 0; j<paths.size(); j++) {
			PathOfInterest path = paths.get(j);
			if (path.jnIndex != -1) {
				PointOfInterest p = poi.get(path.jnIndex);
				Junction jn = maze.getJunction(path.jnIndex);
				
				/*
				 * A path is reachable if we can get to the start of the path and head down the path before the pacman gets there
				 * If we are already on the path then it counts as reachable if we are nearer the start of the path than the pacman
				 * and heading towards the pacman
				 */
				for (int g = 0; g<ghosts; g++) {			
					if (!game.isEdible(g) || !routeEdible(p, g)) { //Ghost must not be edible anywhere on the route
						if (game.getCurGhostLoc(g) == p.nodeIndex && game.getCurGhostDir(g) != game.getReverse(path.dir))
							reachable[g][j] = true;
						if (jn.paths[path.dir].pacman != -1 && jn.paths[path.dir].ghostBlocks(g))
							reachable[g][j] = true;
						if (p.pacman != null && p.ghosts[g].dist - p.pacman.dist <= Game.EAT_DISTANCE && p.ghosts[g].arrivalDir != game.getReverse(path.dir))
							reachable[g][j] = true;
					}
				}
			}
		}
		
		return reachable;
	}	

	private int countReachable(boolean[][] reached) {
		if (reached == null)
			return 0;
		
		int count = 1;
		for (boolean[] a: reached) {
			int ghostCount = 0;
			for (boolean b: a)
				if (b) ghostCount++;
			if (ghostCount > 0)
				count *= ghostCount;
		}
					
		return count;
	}
	
	/*
	 * blockPossible
	 * Tries to find a solution to the N Taxis to M pickups problem.
	 * The input array is has 1 row per taxi and 1 column per pickup, the value in the array indicates if a taxi can do the pickup
	 * Returns True if a solution exists (but doesn't solve it - that takes longer!)
	 * The routine can loop until it is known to be unsolvable or until all nodes that can be solved have been.
	 */
	private boolean blockPossible(boolean[][] reachable, boolean solve) {
		boolean solvable = true;
		boolean looping = true;
		
		if (reachable == null)
			return false;
		
		int[] pickupCount = new int[reachable.length]; //For each taxi - the count of how many pickups it can reach
		int[] taxiCount = new int[reachable[0].length];//For each pickup - the count of how many taxis can reach it
		int activeTaxis = 0;
		int activePickups = 0;
		
		for (int g = 0; g<reachable.length; g++) {
			for (int j = 0; j<reachable[g].length; j++) {	
				if (reachable[g][j]) {
					pickupCount[g]++;
					taxiCount[j]++;
				}
			}
		}
		
		activePickups = taxiCount.length;
		activeTaxis = pickupCount.length;
		
		/*
		 * If we have more taxis than pickups then we fail if all pickups aren't reached
		 * If we have more pickups than taxis then we fail if all the taxis aren't used
		 */
		boolean failTaxi = (activePickups >= activeTaxis);
		boolean failPickup = (activeTaxis >= activePickups);
		
		//Loop around removing other candidates when there is a single solution in a row or column
		while (looping && (solvable || solve)) {
			looping = false;

			if (failTaxi) { //All active taxis must get to a pickup, fail if they cannot
				int failedTaxis = 0;
				for (int g = 0; g < reachable.length; g++) {
					switch (pickupCount[g]) {
					case 0:
						failedTaxis++;
						if (failedTaxis > Game.NUM_GHOSTS - activeTaxis)
							solvable = false;
						break;
					case 1:
						int j = 0;
						while (!reachable[g][j])
							j++;
						if (taxiCount[j] > 1) {
							for (int s = 0; s<reachable.length; s++) {
								if (s != g && reachable[s][j]) {
									reachable[s][j] = false;
									looping = true;
									taxiCount[j]--;
									pickupCount[s]--;
								}
							}	
						}
						break;
					}
				}
			}
			if (failPickup) { //All pickups must be met, fail if no taxis can get to them
				for (int j = 0; j < reachable[0].length; j++) {
					switch (taxiCount[j]) {
					case 0:
						solvable = false;
						break;
					case 1:
						int g = 0;
						
						while (!reachable[g][j])
							g++;
						if (pickupCount[g] > 1) {
							for (int s = 0; s<reachable[g].length; s++) {
								if (s != j && reachable[g][s]) {
									reachable[g][s] = false;
									looping = true;
									taxiCount[s]--;
									pickupCount[g]--;
								}
							}
						}
						break;
					}
				}
			}
		}
		
		if (failPickup) {
			//Count the number of Taxi's with a junction
			int count = 0;
			for (int i = 0; i<taxiCount.length; i++)
				if (taxiCount[i] > 0)
					count++;
			if (count < activePickups)
				return false;
		}
		
		if (failTaxi) {
			//Count the number of Pickups with a taxi
			int count = 0;
			for (int i=0; i<pickupCount.length; i++)
				if (pickupCount[i] > 0)
					count++;
			if (count < activeTaxis)
				return false;
		}

		return true;
	}
	
	
	/*
	 * Search through the poi list and find the nearest point that hasn't had all its paths added.
	 */
	private PointOfInterest findNearestPoint(ArrayList<PointOfInterest>poi, boolean[] pointAdded) {
		PointOfInterest nearest = null;
		int	distance = Integer.MAX_VALUE;
		int me;
		
		for (PointOfInterest p: poi) {
			if (!pointAdded[p.jnIndex] && p.pacman != null) {
				me = p.pacman.dist;
				if (me < distance) {
					distance = me;
					nearest = p;
				}
			}
		}
		return nearest;
	}
	
	/*
	 * Walk through the points of interest in the maze from the given point looking for the next junctions
	 * For each Junction found - we set the distance, direction and hops count for this point and then continue to walk along each path away from here
	 * Paths are blocked by hunter ghosts or power pills
	 */
	private void walkPoints(ArrayList<PointOfInterest> list, PointOfInterest current, int dist, int startDir, int dir, String hops) {
		int hopCount = hops.split("/").length;
		if (dist < 500 && (current.pacman == null || dist < current.pacman.dist ||
				(dist == current.pacman.dist && hopCount < current.hopCount) ||
				(dist == current.pacman.dist && hopCount == current.hopCount && Game.rnd.nextInt(2) == 0))) {
			//System.out.printf("walkPoints: Point %d (%d, %d, %d, %s)\n", current.jnIndex, startDir, dist, dir, hops);
			current.setPacman(startDir, dist, dir, hops);
			
			Junction j = maze.getJunction(current.jnIndex);
			if (j != null) {
				for (int i = 0; i < 4; i++)	{		
					if (j.paths[i] != null && j.paths[i].power == -1 && !j.paths[i].hasHunter()) {
						if (dist == 0)
							startDir = i;						
						walkPoints(list, list.get(j.getNeighbourJn(i)), dist + j.getDistance(i), startDir, j.getArrivalDir(i), hops+"/"+current.jnIndex);
					}
				}
			}
		}
	}
	
}
