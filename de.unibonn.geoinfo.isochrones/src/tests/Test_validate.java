package tests;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.util.CoordinateArrayFilter;

public class Test_validate {

	public static void main(String[] args) {
		int count = (int) 1e7;
		int runs = 1;
		double xMax = 595;
		double yMax = 842;

		GeometryFactory gf = new GeometryFactory();

		Point[] points = new Point[count];
		for (int i = 0; i < count; i++) {
			points[i] = gf.createPoint(new Coordinate(Math.random() * xMax, Math.random() * yMax));
		}

		Coordinate[] boundary = new Coordinate[10];
		boundary[0] = new Coordinate(128, 112);
		boundary[1] = new Coordinate(192, 336);
		boundary[2] = new Coordinate(80, 464);
		boundary[3] = new Coordinate(128, 752);
		boundary[4] = new Coordinate(352, 512);
		boundary[5] = new Coordinate(560, 720);
		boundary[6] = new Coordinate(576, 96);
		boundary[7] = new Coordinate(384, 256);
		boundary[8] = new Coordinate(416, 400);
		boundary[9] = new Coordinate(128, 112);
		Polygon poly = gf.createPolygon(boundary);

		GeometryCollection pointCollection = new MultiPoint(points, gf);

		CoordinateArrayFilter filter = new CoordinateArrayFilter(count) {
			@Override
			public void filter(Coordinate coord) {
				poly.covers(gf.createPoint(coord));
			}
		};
		for (int i = 0; i < runs; ++i) {
			pointCollection.apply(filter);
		}

		Coordinate[] filtered = filter.getCoordinates();
		System.out.println(filtered.length);
	}

}
