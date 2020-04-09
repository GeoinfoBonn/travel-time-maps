package viewer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;

import gisviewer.MapObject;
import gisviewer.Transformation;
import graph.types.ColoredNode;
import isochrone.IsoFace;
import util.geometry.Envelope;

public class FaceMapObjectWithId implements MapObject {

	private boolean alternatingColors = true;
	private final static Color[] colors = { Color.decode("#a6cee3"), Color.decode("#1f78b4"), Color.decode("#b2df8a"),
			Color.decode("#33a02c"), Color.decode("#fb9a99"), Color.decode("#e31a1c"), Color.decode("#fdbf6f"),
			Color.decode("#ff7f00"), Color.decode("#cab2d6"), Color.decode("#6a3d9a"), Color.decode("#ffff99"),
			Color.decode("#b15928") };
	private final static double LABELING_MINM = 0.2;

	private IsoFace face;

	public FaceMapObjectWithId(IsoFace face) {
		this.face = face;
	}

	@Override
	public void draw(Graphics2D g, Transformation t) {
		// Color initialColor = g.getColor();
		if (alternatingColors) {
			g.setColor(colors[face.getId() % colors.length]);
		}

		int[][] coordinates = coordinatesFromPolygon(g, t);
		int[] xpoints = coordinates[0];
		int[] ypoints = coordinates[1];

		// draw polygon
		Color currColor = g.getColor();
		g.setColor(new Color(currColor.getRed(), currColor.getGreen(), currColor.getBlue(), 100));
		Polygon myPolygon = new Polygon(xpoints, ypoints, xpoints.length);
		g.fillPolygon(myPolygon);
		g.setColor(currColor);

		if (t.getM() > LABELING_MINM) {
			// write face id
			int r = t.getRow(face.barycenterFromBoundary().getY());
			int c = t.getColumn(face.barycenterFromBoundary().getX());

			String s = "[" + face.getId() + "]";

			int dR = (g.getFontMetrics().getHeight());
			int dC = (g.getFontMetrics().stringWidth(s));
			g.drawString(s, c - dC / 2, r - dR / 2);
		}
	}

	@SuppressWarnings("unused")
	private int[][] coordinatesFromBoundary(Graphics2D g, Transformation t) {
		int npoints = face.getBoundary().size();
		int[][] coordinates = new int[2][];
		coordinates[0] = new int[npoints];
		coordinates[1] = new int[npoints];

		Point2D first = null, prev = null, curr = null;
		int row1, col1, row2, col2;
		int zaehler = 0;
		for (ColoredNode vertex : face.getBoundary()) {
			if (prev == null) {
				first = vertex;
				prev = first;
				continue;
			}
			curr = vertex;

			row1 = t.getRow(prev.getY());
			col1 = t.getColumn(prev.getX());
			row2 = t.getRow(curr.getY());
			col2 = t.getColumn(curr.getX());
			g.drawLine(col1, row1, col2, row2);

			coordinates[0][zaehler] = col1;
			coordinates[1][zaehler] = row1;

			zaehler++;
			prev = curr;
		}

		// draw closing arc from end to start
		row1 = t.getRow(curr.getY());
		col1 = t.getColumn(curr.getX());
		row2 = t.getRow(first.getY());
		col2 = t.getColumn(first.getX());
		g.drawLine(col1, row1, col2, row2);
		coordinates[0][zaehler] = col1;
		coordinates[1][zaehler] = row1;

		return coordinates;
	}

	private int[][] coordinatesFromPolygon(Graphics2D g, Transformation t) {
		int npoints = face.getFacePoly().getNumPoints();
		int[][] coordinates = new int[2][];
		coordinates[0] = new int[npoints];
		coordinates[1] = new int[npoints];

		Coordinate first = null, prev = null, curr = null;
		int row1, col1, row2, col2;
		int zaehler = 0;
		for (var vertex : face.getFacePoly().getCoordinates()) {
			if (prev == null) {
				first = vertex;
				prev = first;
				continue;
			}
			curr = vertex;

			row1 = t.getRow(prev.y);
			col1 = t.getColumn(prev.x);
			row2 = t.getRow(curr.y);
			col2 = t.getColumn(curr.x);
			g.drawLine(col1, row1, col2, row2);

			coordinates[0][zaehler] = col1;
			coordinates[1][zaehler] = row1;

			zaehler++;
			prev = curr;
		}

		// draw closing arc from end to start
		row1 = t.getRow(curr.y);
		col1 = t.getColumn(curr.x);
		row2 = t.getRow(first.y);
		col2 = t.getColumn(first.x);
		g.drawLine(col1, row1, col2, row2);
		coordinates[0][zaehler] = col1;
		coordinates[1][zaehler] = row1;

		return coordinates;
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope env = new Envelope();
		for (ColoredNode vertex : face.getBoundary()) {
			env.expandToInclude(vertex.getX(), vertex.getY());
		}
		return env;
	}
}
