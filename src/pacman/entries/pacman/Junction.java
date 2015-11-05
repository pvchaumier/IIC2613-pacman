package pacman.entries.pacman;

import pacman.game.Game;

/*
 * Stores information about a junction in the maze
 * A junction is defined as somewhere with more than 2 neighbours OR a point with a power pill
 */
public class Junction {
	public int		index;			//maps into game.getJunctionIndices array
	public int		nodeIndex;		//Node index of this junction
	public Path[]	paths;			//Path information in each of the 4 directions
	public int[]	next;			//The junction index of the next junction in each direction
	public int		numNeighbours;	//If this is 2 we can assume it is a power pill
	
	public Junction(int n, int j) {
		nodeIndex = n;
		index = j;
		paths = new Path[4];
		next = new int[4];
		numNeighbours = 0;
	}
	
	//Finds the direction to the given adjacent junction node
	public int getNodeDir(int n) {
		for (int dir=0; dir<paths.length; dir++)
			if (paths[dir].to == n)
				return dir;
		return -1;
	}
	
	//Finds the direction to the given adjacent junction
	public int getJnDir(int j) {
		for (int dir=0; dir<next.length; dir++)
			if (next[dir] == j)
				return dir;
		return -1;
	}
	
	public int getNeighbourNode(int d) {
		if (paths[d] == null)
			return -1;
		return paths[d].to;
	}
	
	public int getNeighbourJn(int d) {
		return next[d];
	}
	
	public int getDistance(int d) {
		if (paths[d] == null || paths[d].route == null)
			return -1;
		return paths[d].route.dist;
	}
	
	public void addPath(int dir, Game g) {
		if (paths[dir] == null)
			numNeighbours++;
		paths[dir] = new Path(nodeIndex, dir, g);
	}
	
	public int getArrivalDir(int dir) {
		if (paths[dir] != null && paths[dir].route != null)
			return paths[dir].route.arrivalDir;
		return -1;
	}
}
