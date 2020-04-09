package graph.types;

public interface Colored {

	public static final int UNDEFINED = -1;
	public static final int REACHABLE = 0;
	public static final int UNREACHABLE = 1;
	public static final int BUFFER = 2;

	public int getColor();

	public double getRemainingTime();

	public double getRemainingDist();

	public void setReachability(int color, double remDist);

	public static int edgeColor(int sourceColor, int targetColor) {
		if (sourceColor == UNDEFINED || targetColor == UNDEFINED)
			return UNDEFINED;
		if (sourceColor == UNREACHABLE || targetColor == UNREACHABLE)
			return UNREACHABLE;
		if (sourceColor == BUFFER || targetColor == BUFFER)
			return BUFFER;
		return REACHABLE;
	}

}
