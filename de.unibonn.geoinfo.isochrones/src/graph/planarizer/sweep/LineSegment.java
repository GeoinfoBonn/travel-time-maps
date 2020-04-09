package graph.planarizer.sweep;

public class LineSegment {
    private Point p1;
    private Point p2;
    private String name;

    // straight line: nx * x + ny * y = d
    private double nx;
    private double ny;
    private double d;

    private final static double eps = 1e-6;

    public LineSegment(Point p1, Point p2, String name) {
	this.name = name;
	if (p1.getX() < p2.getX() || (Math.abs(p1.getX() - p2.getX()) < eps && p1.getY() < p2.getY())) {
	    this.p1 = p1;
	    this.p2 = p2;
	} else {
	    this.p1 = p2;
	    this.p2 = p1;
	}
	calculateNormal();
    }

    private void calculateNormal() {
	nx = p1.getY() - p2.getY();
	ny = p2.getX() - p1.getX();
	d = nx * p1.getX() + ny * p1.getY();
    }

    public void reduce(double dx, double dy) {
	p1.reduce(dx, dy);
	p2.reduce(dx, dy);
	calculateNormal();
    }

    public void restore() {
	this.p1.restore();
	this.p2.restore();
	calculateNormal();
    }

    public Point getP1() {
	return p1;
    }

    public Point getP2() {
	return p2;
    }

    public String getName() {
	return name;
    }

    public String toString() {
	return name + ": [" + p1 + "," + p2 + "]";
    }

    public Point getIntersection(LineSegment ls) {

	// line segments parallel?
	if (Math.abs(ny * ls.nx - ls.ny * nx) < eps)
	    return null;

	/*
	 * nx1 * x + ny1 * y = d1 nx2 * x + ny2 * y = d2
	 * 
	 * (nx1 * nx2) x + (ny1 * nx2) * y = d1 * nx2 (nx2 * nx1) x + (ny2 * nx1) * y =
	 * d2 * nx1
	 * 
	 * y = (d1 * nx2 - d2 * nx1) / (ny1 * nx2 - ny2 * nx1)
	 */
	double y = (d * ls.nx - ls.d * nx) / (ny * ls.nx - ls.ny * nx);

	/*
	 * (nx1 * ny2) x + (ny1 * ny2) * y = d1 * ny2 (nx2 * ny1) x + (ny2 * ny1) * y =
	 * d2 * ny1
	 * 
	 * x = (d1 * ny2 - d2 * ny1) / (nx1 * ny2 - nx2 * ny1)
	 */
	double x = (d * ls.ny - ls.d * ny) / (nx * ls.ny - ls.nx * ny);

	// System.out.println("p= " + x + " " + y);

	if ((p1.getX() != p2.getX() && (x <= p1.getX() || x >= p2.getX()))
		|| (ls.p1.getX() != ls.p2.getX() && (x <= ls.p1.getX() || x >= ls.p2.getX())))
	    return null;
	if ((p1.getY() != p2.getY() && (y <= Math.min(p1.getY(), p2.getY()) || y >= Math.max(p1.getY(), p2.getY())))
		|| (ls.p1.getY() != ls.p2.getY())
			&& (y <= Math.min(ls.p1.getY(), ls.p2.getY()) || y >= Math.max(ls.p1.getY(), ls.p2.getY())))
	    return null;

	Point crossing = new Point(x, y);

	// if crossing point is beginning or end of segment, it is no crossing!
	if (crossing.getDistance(p1) < eps || crossing.getDistance(p2) < eps || crossing.getDistance(ls.p1) < eps
		|| crossing.getDistance(ls.p2) < eps) {
	    return null;
	}

	return crossing;
    }

    public Point getIntersection2(LineSegment ls) {
	boolean partial = false;

	double x0 = this.p1.getX();
	double x1 = this.p2.getX();
	double y0 = this.p1.getY();
	double y1 = this.p2.getY();

	double a0 = ls.p1.getX();
	double a1 = ls.p2.getX();
	double b0 = ls.p1.getY();
	double b1 = ls.p2.getY();

	double xy, ab;

	double denom = (b0 - b1) * (x0 - x1) - (y0 - y1) * (a0 - a1);
	if (denom != 0) {
	    xy = (a0 * (y1 - b1) + a1 * (b0 - y1) + x1 * (b1 - b0)) / denom;
	    partial = LineSegment.isBetween(0, xy, 1);
	    if (partial) {
		ab = (y1 * (x0 - a1) + b1 * (x1 - x0) + y0 * (a1 - x1)) / denom;

		if (LineSegment.isBetween(0, ab, 1)) {
		    xy = 1 - xy;
		    Point c1 = new Point(x0 + xy * (x1 - x0), y0 + xy * (y1 - y0));

		    ab = 1 - ab;
		    Point c2 = new Point(a0 + ab * (a1 - a0), b0 + ab * (b1 - b0));

		    Point c = new Point((c1.getX() + c2.getX()) / 2, (c1.getY() + c2.getY()) / 2);
		    return c;
		}
	    }
	}
	return null;
    }

    public static boolean isBetween(double x0, double x, double x1) {
	double eps = 1e-12;
	return (x > x0 + eps) && (x < x1 - eps);
    }

    public double getY(Point p) {
	if (Math.abs(p.getX() - p1.getX()) < eps && Math.abs(p.getY() - p1.getY()) < eps)
	    return p.getY();
	if (Math.abs(p.getX() - p2.getX()) < eps && Math.abs(p.getY() - p2.getY()) < eps)
	    return p.getY();
	if (ny < eps)
	    return p.getY();
	return (d - nx * p.getX()) / ny;
    }

    public double getSlope() {
	if (Math.abs(p2.getX() - p1.getX()) < eps)
	    return Double.POSITIVE_INFINITY;
	return (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());
    }

}