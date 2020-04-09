package graph.planarizer.sweep;

import java.util.Objects;

public class Point implements Comparable<Point> {
	private double x;
	private double y;
	private final static double eps = 1e-5;

	private boolean reduced;
	private double x_original = 0;
	private double y_original = 0;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public void reduce(double dx, double dy) {
		this.x_original = this.x;
		this.y_original = this.y;
		this.x -= dx;
		this.y -= dy;
		this.reduced = true;
	}

	public void restore(double dx, double dy) {
		this.x += dx;
		this.y += dy;
		this.x_original = 0;
		this.y_original = 0;
		this.reduced = false;
	}

	public void restore() {
		if (this.reduced) {
			this.x = x_original;
			this.y = y_original;
			this.reduced = false;
		}
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getDistance(Point p) {
		double dx = x - p.getX();
		double dy = y - p.getY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	public boolean isToTheLeftOf(Point q, Point r) {
		double ax = r.getX() - q.getX();
		double ay = r.getY() - q.getY();
		double bx = x - q.getX();
		double by = y - q.getY();
		return (ax * by - ay * bx > 0.0);
	}

	@Override
	public String toString() {
		return "(" + String.format("%11.3f", x) + "," + String.format("%11.3f", y) + ")";
	}

	public static Point[] closestPair(Point[] points) {
		double d2Min = Double.POSITIVE_INFINITY;
		Point[] opt = new Point[2];

		for (int i = 0; i < points.length - 1; i++) {
			for (int j = i + 1; j < points.length; j++) {
				double dx = points[i].x - points[j].x;
				double dy = points[i].y - points[j].y;
				double d2 = dx * dx + dy * dy;
				if (d2 < d2Min) {
					d2Min = d2;
					opt[0] = points[i];
					opt[1] = points[j];
				}
			}
		}
		return opt;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Point) {
			Point p = (Point) o;
			return (Math.abs(this.x - p.getX()) < eps && Math.abs(this.y - p.getY()) < eps);
		}
		System.err.println("Input not of type Point.");
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	public boolean isLessThan(Point p) {
		return this.getX() < p.getX() || (Math.abs(this.getX() - p.getX()) < eps && this.getY() < p.getY());

	}

	public boolean isGreaterThan(Point p) {
		return (!this.isLessThan(p) && !this.equals(p));
	}

	@Override
	public int compareTo(Point p) {
		if (this.isLessThan(p)) {
			return -1;
		} else if (this.equals(p)) {
			return 0;
		} else {
			return 1;
		}
	}
}
