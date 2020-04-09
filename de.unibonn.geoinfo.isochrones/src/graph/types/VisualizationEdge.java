package graph.types;

import graph.generic.DoubleWeight;

public class VisualizationEdge extends DoubleWeight {

	public static final byte GRID_LINE = 1;
	public static final byte BOUNDARY_LINE = 2;
	public static final byte EXCEPTION_LINE = 3;

	private boolean isOctilinear;
	private byte type;

	public VisualizationEdge(double weight, boolean isOctilinear, byte type) {
		super(weight);
		this.isOctilinear = isOctilinear;
		this.type = type;
	}

	public boolean isOctilinear() {
		return isOctilinear;
	}

	public byte getType() {
		return type;
	}
}
