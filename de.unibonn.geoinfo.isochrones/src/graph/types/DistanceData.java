package graph.types;

import graph.generic.DoubleWeight;

public class DistanceData extends DoubleWeight implements WalkingData {

	public DistanceData(double gemetricDist) {
		super(gemetricDist);
	}

	@Override
	public double getValueAsTime() {
		return getValue() / WalkingData.WALKING_SPEED;
	}

	@Override
	public double getValueAsDist() {
		return getValue();
	}

	@Override
	public String toString() {
		return "[distance=" + String.format("%7.2f", getValue()) + "]";
	}
}
