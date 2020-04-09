package viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

import gisviewer.MapObject;
import gisviewer.Transformation;
import util.geometry.Envelope;

public class PolygonMapObject implements MapObject {
	private com.vividsolutions.jts.geom.Polygon jtsPolygon;

	private Polygon myPolygon;
	private List<Point2D> points;

	public PolygonMapObject(List<Point2D> points) {
		this.points = new LinkedList<>();
		for (Point2D v : points) {
			this.points.add(v);
		}
	}

	public PolygonMapObject(com.vividsolutions.jts.geom.Polygon polygon) {
		this.jtsPolygon = polygon;
	}

	@Override
	public void draw(Graphics2D g, Transformation t) {
		if (this.points != null) {
			int npoints = this.points.size();
			int[] xpoints = new int[npoints];
			int[] ypoints = new int[npoints];
			int zaehler = 0;
			for (Point2D v : this.points) {
				xpoints[zaehler] = t.getColumn(v.getX());
				ypoints[zaehler] = t.getRow(v.getY());
				zaehler++;
			}
			this.myPolygon = new Polygon(xpoints, ypoints, npoints);
			g.drawPolygon(this.myPolygon);
			g.fillPolygon(this.myPolygon);
		} else {
			this.myPolygon = linearRingToAWTPolygon(this.jtsPolygon.getExteriorRing(), t);

			Area polyArea = new Area(this.myPolygon);

			Area inner;
			for (int i = 0; i < jtsPolygon.getNumInteriorRing(); i++) {
				inner = new Area(linearRingToAWTPolygon(this.jtsPolygon.getInteriorRingN(i), t));
				polyArea.subtract(inner);
			}

			g.draw(polyArea);
			Color c = g.getColor();
			g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 128));
			g.fill(polyArea);
			g.setColor(c);
		}

	}

	@Override
	public Envelope getBoundingBox() {
		if (this.points != null) {
			double xmax = Double.MIN_VALUE;
			double ymax = Double.MIN_VALUE;
			double xmin = Double.MAX_VALUE;
			double ymin = Double.MAX_VALUE;

			for (Point2D v : this.points) {
				double x = v.getX();
				double y = v.getY();
				if (x > xmax)
					xmax = x;
				else if (x < xmin)
					xmin = x;
				if (y > ymax)
					ymax = y;
				else if (y < ymin)
					ymin = y;

			}
			return new Envelope(xmin, xmax, ymin, ymax);
		}

		Geometry bb = jtsPolygon.getEnvelope();
		if (bb instanceof com.vividsolutions.jts.geom.Polygon) {
			com.vividsolutions.jts.geom.Polygon bbox = (com.vividsolutions.jts.geom.Polygon) bb;
			Coordinate[] c = bbox.getCoordinates();
			return new Envelope(c[0].x, c[2].x, c[0].y, c[2].y);
		}
		return null;
	}

	private Polygon linearRingToAWTPolygon(LineString ring, Transformation t) {
		int npoints = ring.getNumPoints();
		int[] xpoints = new int[npoints];
		int[] ypoints = new int[npoints];
		int zaehler = 0;
		for (Coordinate c : ring.getCoordinates()) {
			xpoints[zaehler] = t.getColumn(c.x);
			ypoints[zaehler] = t.getRow(c.y);
			zaehler++;
		}
		return new Polygon(xpoints, ypoints, npoints);
	}

}
