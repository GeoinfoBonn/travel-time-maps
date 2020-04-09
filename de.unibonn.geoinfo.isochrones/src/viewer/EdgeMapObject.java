package viewer;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;

import gisviewer.LineMapObject;
import gisviewer.Transformation;

public class EdgeMapObject extends LineMapObject {

	private int strokeWidth = 1;

	public EdgeMapObject(Point2D p1, Point2D p2) {
		super(new Double(p1.getX(), p1.getY()), new Double(p2.getX(), p2.getY()));
	}

	public void setStrokeWidth(int width) {
		this.strokeWidth = width;
	}

	@Override
	public void draw(Graphics2D g, Transformation t) {
		Point2D p1 = ls[0];
		Point2D p2 = ls[1];
		int row1 = t.getRow(p1.getY());
		int col1 = t.getColumn(p1.getX());
		int row2 = t.getRow(p2.getY());
		int col2 = t.getColumn(p2.getX());

		Stroke s = g.getStroke();
		g.setStroke(new BasicStroke(strokeWidth));
		g.drawLine(col1, row1, col2, row2);
		g.setStroke(s);
	}

}
