package graph.types;

import java.awt.geom.Point2D;

public class ColoredNode extends Point2D.Double implements Colored {

	private static final long serialVersionUID = 790547863434886732L;

	private int color;
	private double remainingTime;

	private boolean fixed;

	public ColoredNode(double x, double y) {
		this(x, y, Colored.UNDEFINED, java.lang.Double.MAX_VALUE);
	}

	public ColoredNode(Point2D location) {
		this(location.getX(), location.getY());
	}

	public ColoredNode(ColoredNode copy) {
		this(copy.getX(), copy.getY(), copy.getColor(), copy.getRemainingTime());
	}

	public ColoredNode(double x, double y, int color, double remainingTime) {
		this.setLocation(x, y);
		this.color = color;
		this.remainingTime = remainingTime;
		this.fixed = false;
	}

	public ColoredNode(Point2D location, int color, double remainingDist) {
		this(location.getX(), location.getY(), color, remainingDist);
	}

	@Override
	public int getColor() {
		return color;
	}

	@Override
	public void setReachability(int color, double remainingTime) {
		if (!fixed) {
			this.color = color;
			this.remainingTime = remainingTime;
		} else {
//			System.err.println("Tried to change fixed reachability.");
		}
	}

	public void setReachability(ColoredNode copy) {
		setReachability(copy.getColor(), copy.getRemainingTime());
	}

	public void resetReachabilityToUndefined() {
		this.color = Colored.UNDEFINED;
		this.remainingTime = -java.lang.Double.MAX_VALUE;
	}

	@Override
	public double getRemainingTime() {
		return remainingTime;
	}

	@Override
	public String toString() {
		String ret = "ColoredNode[" + super.toString() + ", color=";
		switch (this.color) {
		case Colored.UNDEFINED:
			ret += "UNDEFINED";
			break;
		case Colored.REACHABLE:
			ret += "REACHABLE";
			break;
		case Colored.UNREACHABLE:
			ret += "UNREACHABLE";
			break;
		case Colored.BUFFER:
			ret += "BUFFER";
			break;
		default:
			throw new IllegalArgumentException("Unknown color " + color);
		}
		ret += ", remTime=" + remainingTime + "]";
		return ret;
	}

	@Override
	public double getRemainingDist() {
		return remainingTime * WalkingData.WALKING_SPEED;
	}

	public void fixReachability() {
		this.fixed = true;
	}

	public boolean isFixed() {
		return this.fixed;
	}
}
