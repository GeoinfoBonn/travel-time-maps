package isochrone;

import java.awt.geom.Point2D;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;

import graph.planarizer.sweep.LineSegment;
import graph.planarizer.sweep.Point;
import util.tools.Util;

public class IsoBufferedCreator {

	private double offroadSpeedFactor;
	private int numSegments = 36;

	public IsoBufferedCreator(double offroadSpeedFactor) {
		this.offroadSpeedFactor = offroadSpeedFactor;
	}

	public Coordinate[] createBeltdrive(Point2D c1, double remDist1, Point2D c2, double remDist2) {

		Coordinate[] beltdrive = null;

		double a = c1.distance(c2); // a - distance between points

		double D = remDist1; // D - remDist of bigger circle
		double d = remDist2; // d - remDist of smaller circle

		double R = D * offroadSpeedFactor; // R - radius of bigger circle
		double r = d * offroadSpeedFactor; // r - radius of smaller circle

		Point2D C = c1; // C - center of bigger circle
		Point2D c = c2; // c - center of smaller circle
		if (r > R) { // swap if needed
			{
				double tmp = R;
				R = r;
				r = tmp;
			}
			{
				Point2D tmp = C;
				C = c;
				c = tmp;
			}
			{
				double tmp = D;
				D = d;
				d = tmp;
			}
		}

		if (a > remDist1 && a > remDist2 && r > 1e-6) {
//			System.out.println("Degenerate case");
			beltdrive = degenerateCase(C, R, D, c, r, d);
		} else {
			if (R >= r + a) {
//				System.out.println("Dominating case");
				beltdrive = circleShape(C, R);
			} else if (r < 1e-6) {
//				System.out.println("Final case");
				beltdrive = dropShape(C, R, c);
			} else if ((R - r) < 1e-2) {
//				System.out.println("Same radii case");
				beltdrive = sameRadiiCase(c1, c2, (R + r) / 2);
			} else {
//				System.out.println("Standard case");
				beltdrive = standardCase(C, R, c, r);
			}
		}

		return beltdrive;
	}

	/**
	 * http://www.mathematische-basteleien.de/2kreise.htm
	 * 
	 * @param C
	 * @param R
	 * @param c
	 * @param r
	 * @return
	 */
	private Coordinate[] standardCase(Point2D C, double R, Point2D c, double r) {
		double dr = R - r;
		double a = C.distance(c);
		double x = (r * a) / dr;
		double t = Math.sqrt(a * a - dr * dr);
		double s = r * t / (dr);

		Point2D direction = Util.normedVector(c, C);
		Point2D X = new Point2D.Double(c.getX() - direction.getX() * x, c.getY() - direction.getY() * x);

		double gamma = Math.atan2(r, s); // angle from X to p1
		double beta = Math.atan2(C.getY() - X.getY(), C.getX() - X.getX()); // angle from X to C (and thus also c)
		double theta = beta + gamma;

		Coordinate p1 = new Coordinate(X.getX() + s * Math.cos(theta), X.getY() + s * Math.sin(theta));
		Coordinate p4 = new Coordinate(X.getX() + (s + t) * Math.cos(theta), X.getY() + (s + t) * Math.sin(theta));

		theta = beta - gamma;

		Coordinate p2 = new Coordinate(X.getX() + s * Math.cos(theta), X.getY() + s * Math.sin(theta));
		Coordinate p3 = new Coordinate(X.getX() + (s + t) * Math.cos(theta), X.getY() + (s + t) * Math.sin(theta));

		Coordinate centerSmall = new Coordinate(c.getX(), c.getY());
		Coordinate centerBig = new Coordinate(C.getX(), C.getY());

		Coordinate[] arc2 = approximateArc(centerSmall, p1, p2, numSegments);
		Coordinate[] arc1 = approximateArc(centerBig, p3, p4, numSegments);
		Coordinate[] closure = new Coordinate[] { arc2[0] };

		return Util.concatAll(arc2, arc1, closure);
	}

	private Coordinate[] sameRadiiCase(Point2D c1, Point2D c2, double r) {
		Point2D direction = Util.normedVector(c1, c2);
		Point2D orthoDir = new Point2D.Double(-direction.getY(), direction.getX());

		Coordinate p1 = new Coordinate(c1.getX() + orthoDir.getX() * r, c1.getY() + orthoDir.getY() * r);
		Coordinate p2 = new Coordinate(c1.getX() - orthoDir.getX() * r, c1.getY() - orthoDir.getY() * r);
		Coordinate p3 = new Coordinate(c2.getX() - orthoDir.getX() * r, c2.getY() - orthoDir.getY() * r);
		Coordinate p4 = new Coordinate(c2.getX() + orthoDir.getX() * r, c2.getY() + orthoDir.getY() * r);

		Coordinate center1 = new Coordinate(c1.getX(), c1.getY());
		Coordinate center2 = new Coordinate(c2.getX(), c2.getY());

		Coordinate[] arc2 = approximateArc(center1, p1, p2, numSegments);
		Coordinate[] arc1 = approximateArc(center2, p3, p4, numSegments);
		Coordinate[] closure = new Coordinate[] { arc2[0] };

		return Util.concatAll(arc2, arc1, closure);
	}

	private Coordinate[] dropShape(Point2D c, double r, Point2D end) {
		Point2D direction = Util.normedVector(c, end);
		double d = c.distance(end);

		Point2D px = new Point2D.Double(c.getX() + direction.getX() * d, c.getY() + direction.getY() * d);
		double s = Math.sqrt(d * d - r * r);
		double gamma = Math.atan2(r, s);
		double beta = Math.atan2(c.getY() - end.getY(), c.getX() - end.getX());

		Coordinate p3 = new Coordinate(px.getX() + s * Math.cos(beta + gamma), px.getY() + s * Math.sin(beta + gamma));
		Coordinate p2 = new Coordinate(px.getX() + s * Math.cos(beta - gamma), px.getY() + s * Math.sin(beta - gamma));

		Coordinate center = new Coordinate(c.getX(), c.getY());

		Coordinate[] start = new Coordinate[] { new Coordinate(end.getX(), end.getY()) };
		Coordinate[] arc = approximateArc(center, p2, p3, numSegments);
		Coordinate[] closure = new Coordinate[] { start[0] };

		return Util.concatAll(start, arc, closure);
	}

	private Coordinate[] degenerateCase(Point2D C, double R, double D, Point2D c, double r, double d) {
		Point2D direction = Util.normedVector(C, c);

		Point2D pX = new Point2D.Double(C.getX() + direction.getX() * D, C.getY() + direction.getY() * D);
		Point2D px = new Point2D.Double(c.getX() - direction.getX() * d, c.getY() - direction.getY() * d);

		Coordinate[] big = dropShape(C, R, pX);
		Coordinate[] small = dropShape(c, r, px);

		Coordinate bigBeginArc = big[1];
		Coordinate bigEndArc = big[big.length - 2];
		Coordinate smallBeginArc = small[1];
		Coordinate smallEndArc = small[small.length - 2];

		// calculate crossing points
		LineSegment l1 = new LineSegment(new Point(pX.getX(), pX.getY()), new Point(bigBeginArc.x, bigBeginArc.y),
				"l1");
		LineSegment l2 = new LineSegment(new Point(px.getX(), px.getY()), new Point(smallEndArc.x, smallEndArc.y),
				"l2");
		Point cross1_ = l1.getIntersection(l2);
		Coordinate cross1 = new Coordinate(cross1_.getX(), cross1_.getY());

		LineSegment l3 = new LineSegment(new Point(pX.getX(), pX.getY()), new Point(bigEndArc.x, bigEndArc.y), "l3");
		LineSegment l4 = new LineSegment(new Point(px.getX(), px.getY()), new Point(smallBeginArc.x, smallBeginArc.y),
				"l4");
		Point cross2_ = l3.getIntersection(l4);
		Coordinate cross2 = new Coordinate(cross2_.getX(), cross2_.getY());

		Coordinate[] arc2 = Arrays.copyOfRange(big, 2, big.length - 2);
		Coordinate[] closure1 = new Coordinate[] { cross2 };
		Coordinate[] arc1 = Arrays.copyOfRange(small, 2, small.length - 2);
		Coordinate[] closure2 = new Coordinate[] { cross1, arc2[0] };

		return Util.concatAll(arc2, closure1, arc1, closure2);
	}

	private Coordinate[] circleShape(Point2D c, double r) {
		Coordinate center = new Coordinate(c.getX(), c.getY());
		Coordinate outer = new Coordinate(c.getX(), c.getY() + r);

		return approximateArc(center, outer, outer, numSegments);
	}

	private Coordinate[] approximateArc(Coordinate center, Coordinate arcStart, Coordinate arcEnd, int numSegments) {

		double radius = (center.distance(arcStart) + center.distance(arcEnd)) / 2;

		double eps = 1e-8;
		if (center.distance(arcEnd) - radius > eps || center.distance(arcStart) - radius > eps) {
			System.err.println("Radii implied by two arc endpoints are not consistent.");
		}

		double etaS = Math.atan2(arcStart.y - center.y, arcStart.x - center.x); // angle to start
		double etaT = Math.atan2(arcEnd.y - center.y, arcEnd.x - center.x); // angle to end

		if (etaS >= etaT)
			etaT += 2 * Math.PI;

		double dEta = (etaT - etaS);
		int n = (int) (numSegments * dEta / (2 * Math.PI));

		Coordinate[] arc = new Coordinate[n + 1];
		double phi;
		for (int i = 0; i <= n; ++i) {
			phi = etaS + dEta * i / n;
			arc[i] = new Coordinate(center.x + radius * Math.cos(phi), center.y + radius * Math.sin(phi));
		}

		return arc;
	}
}
