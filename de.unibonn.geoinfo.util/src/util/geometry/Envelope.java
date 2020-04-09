package util.geometry;

import java.awt.geom.Point2D;

public class Envelope {

	private double x1;
	private double y1;
	private double x2;
	private double y2;

	public Envelope(double x1, double x2, double y1, double y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}

	public Envelope() {
		this.x1 = Double.POSITIVE_INFINITY;
		this.x2 = Double.NEGATIVE_INFINITY;
		this.y1 = Double.POSITIVE_INFINITY;
		this.y2 = Double.NEGATIVE_INFINITY;
	}

	public double getxMin() {
		return x1;
	}

	public double getyMin() {
		return y1;
	}

	public double getxMax() {
		return x2;
	}

	public double getyMax() {
		return y2;
	}

	public void expandToInclude(double x, double y) {
		x1 = Math.min(x, x1);
		x2 = Math.max(x, x2);
		y1 = Math.min(y, y1);
		y2 = Math.max(y, y2);

	}

	public void expandToInclude(Envelope boundingBox) {
		x1 = Math.min(x1, boundingBox.x1);
		y1 = Math.min(y1, boundingBox.y1);
		x2 = Math.max(x2, boundingBox.x2);
		y2 = Math.max(y2, boundingBox.y2);

	}

	public boolean intersects(Envelope boundingBox) {
		if (x1 > boundingBox.x2)
			return false;
		if (y1 > boundingBox.y2)
			return false;
		if (x2 < boundingBox.x1)
			return false;
		if (y2 < boundingBox.y1)
			return false;
		return true;
	}

	public boolean isBounded() {
		if (this.x1 == Double.POSITIVE_INFINITY)
			return false;
		if (this.x2 == Double.NEGATIVE_INFINITY)
			return false;
		if (this.y1 == Double.POSITIVE_INFINITY)
			return false;
		if (this.y2 == Double.NEGATIVE_INFINITY)
			return false;
		return true;
	}

	public boolean contains(Envelope env) {
		if (x1 > env.x1)
			return false;
		if (y1 > env.y1)
			return false;
		if (x2 < env.x2)
			return false;
		if (y2 < env.y2)
			return false;
		return true;
	}

	public boolean contains(Point2D p) {
		if (x1 > p.getX())
			return false;
		if (y1 > p.getY())
			return false;
		if (x2 < p.getX())
			return false;
		if (y2 < p.getY())
			return false;
		return true;
	}

	public double getWidth() {
		return x2 - x1;
	}

	public double getHeight() {
		return y2 - y1;
	}
}
