package pacman.entries.pacman;

import java.util.ArrayList;

import pacman.game.Game;

/*
 * PointOfInterest stores information about how the pacman and ghosts get to this point in the maze
 */
public class PointOfInterest {
	int			nodeIndex;	//The node index of this place
	int			jnIndex;	//If this is a junction, the index into the junctions array, -1 otherwise
	Route 		pacman;		//The distance the pacman needs to travel to get here and its direction of arrival
	int			distance;	//The distance away from the pacman (ignoring any ghosts)
	Route[]		ghosts;		//One entry per ghost - the distance and start direction to get here
	boolean[]	edible; 	//One entry per ghost - true if it is edible when it gets here
	boolean		blockable;	//True if a non-edible ghost can get here before the pacman
	int			hopCount;	//Number of junctions away from the pacman this point is
	int			safetyMargin; //The amount of time (ticks) before the point becomes blocked
	String		path;		//The path the pacman takes to get here in the format 15/23/16/... where each number is a junction visited in order
	
	public PointOfInterest(int nodeIndex, int jnIndex) {
		this.nodeIndex = nodeIndex;
		this.jnIndex = jnIndex;
		ghosts = new Route[4];
		edible = new boolean[4];
	}
	
	public void setPacman(int pacDir, int dist, int arrivalDir, String hops) {
		this.pacman = new Route(pacDir, arrivalDir, dist);
		this.path = hops;
		hopCount = hops.split("/").length;
		if (hopCount >= 2) hopCount -= 2;
	}
	
	public int parent() {
		String[] s = path.split("/");
		if (s.length < 2)
			return -1;
		return Integer.parseInt(s[s.length-1]);
	}
	
	public boolean hasParent(int jn) {
		if (path == null)
			return false;
		
		String[] s = path.split("/");
		for (String j: s)
			if (j.length() > 0 && Integer.parseInt(j) == jn)
				return true;
		return false;
	}
	
	public void assignBlock() {
		if (pacman == null) {
			blockable = true;
			safetyMargin = -1;
		} 
		else {
			safetyMargin = Integer.MAX_VALUE;
			blockable = false;

			for (int g = 0; g<4; g++) {
				if (ghosts != null) {
					int i = ghosts[g].dist - pacman.dist - 10; //esto lo inventé, arreglar
					if (!edible[g]) {
						if (i <= 0) {
							blockable = true;
							safetyMargin = -1;
						} else if (i < safetyMargin)
							safetyMargin = i;
					}
				}
			}
		}
	}
	
	//Populate the distances for each ghost to get here.
	public void assignDistances(Game g, Topology maze) {
		int banned = -1;
		if (pacman != null && pacman.dist > 0)
			banned = pacman.arrivalDir;
		
		for (int i = 0; i < 4; i++) {
			edible[i] = false;
			if (g.getLairTime(i) > 0) { //Measure distance from start point
				ghosts[i] = maze.getGhostRoute(g, g.getInitialGhostsPosition(), Game.INITIAL_GHOST_DIRS[i], nodeIndex, banned);
				ghosts[i].dist += g.getLairTime(i);
			} else {
				ghosts[i] = maze.getGhostRoute(g, g.getCurGhostLoc(i), g.getCurGhostDir(i), nodeIndex, banned);
				if (g.isEdible(i)) { //Edible ghosts move at half speed so pretend the distance is longer
					if (g.getEdibleTime(i) > ghosts[i].dist * 2) {
						ghosts[i].dist *= 2;
						edible[i] = true;
					} else
						ghosts[i].dist += g.getEdibleTime(i) / 2;
				}
			}				
		}
		distance = g.getPathDistance(g.getCurPacManLoc(),nodeIndex);
	}
	
	/*
	 * Check the junctions along the point's path and ensure each one is safe to visit
	 */
	public boolean isSafe(ArrayList<PointOfInterest> poi) {
		if (path == null || safetyMargin <= 0)
			return false;
		
		String[] s = path.split("/");
		
		for (int i = 1; i<s.length; i++) {
			try {
				int j = Integer.parseInt(s[i]);
				if (j>=0) {
					PointOfInterest me = poi.get(j);
					if (me.safetyMargin <= 0)
						return false;
				}
			} catch (Exception e){} 
		}
		return true;
	}
}
