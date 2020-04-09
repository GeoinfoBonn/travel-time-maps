package graph.types;

import graph.generic.WeightedArcData;

public interface WalkingData extends WeightedArcData {

	public static final double WALKING_SPEED = 5 / 3.6; // m/s

	public double getValueAsTime();

	public double getValueAsDist();
}
