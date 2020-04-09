package graph.generic;

public class DoubleWeight implements WeightedArcData {
	private double value;

	@Override
	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public DoubleWeight(double weight) {
		super();
		this.value = weight;
	}

	public DoubleWeight() {

	}

	@Override
	public String toString() {
		return "DoubleWeight[value=" + value + "]";
	}

	public DoubleWeight split(double fraction) {
		return new DoubleWeight(value * fraction);
	}
}