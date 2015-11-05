package pacman.entries.pacman;

public class Route {
	public int	startDir;
	public int	arrivalDir;
	public int	dist;
	
	public Route(int startDir, int arrivalDir, int dist) {
		this.startDir = startDir;
		this.arrivalDir = arrivalDir;
		this.dist = dist;
	}
}