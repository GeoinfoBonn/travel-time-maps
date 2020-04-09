package graph.types;

import graph.generic.DoubleWeight;

public class TimeData extends DoubleWeight implements WalkingData {

	public TimeData(double time) {
		super(time);
	}

	@Override
	public double getValueAsDist() {
		return getValue() * WalkingData.WALKING_SPEED;
	}

	@Override
	public double getValueAsTime() {
		return getValue();
	}

	@Override
	public String toString() {
		return "[time=" + String.format("%7.2f", getValue()) + "]";
	}
}
