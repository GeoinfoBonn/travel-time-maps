package tests;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import util.geometry.Envelope;
import util.tools.Util;

public class Test_rayCast {

	public static void main(String[] args) {
		List<Point2D> directions = new LinkedList<>();
		directions.add(new Point2D.Double(59.231, 575.282));
		directions.add(new Point2D.Double(16, 496));
		directions.add(new Point2D.Double(80, 368));
		directions.add(new Point2D.Double(320, 384));
		directions.add(new Point2D.Double(368, 432));
		directions.add(new Point2D.Double(272, 528));
		directions.add(new Point2D.Double(128, 560));

		Point2D split = new Point2D.Double(228.862, 537.586);
		Envelope bbox = new Envelope(128, 464, 432, 720);

		for (Point2D direction : directions) {
			Point2D point = Util.castRay(split, direction, bbox);
			System.out.println(point);
		}
	}

}
