package graph.types;

import graph.generic.WeightedArcData;

public abstract class IsoEdge implements WeightedArcData {

	public static final int UNDEFINED = 10;

	private int type;
	private double value;

	public IsoEdge(double value) {
		this.value = value;
		this.type = UNDEFINED;
	}

	public IsoEdge(double value, int type) {
		this.value = value;
		this.type = type;
	}

	public void setValue(double val) {
		this.value = val;
	}

	@Override
	public double getValue() {
		return value;
	}

	public int getType() {
		return type;
	}
}
