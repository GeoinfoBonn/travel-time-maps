package gisviewer;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import util.geometry.Envelope;

/**
 * A line segment that can be added to a map
 * 
 * @author haunert
 */
public class LineMapObject implements MapObject {

	/**
	 * an array holding the first and last point of the line
	 */
	protected Point2D[] ls;

	/**
	 * constructor
	 * 
	 * @param ls: the array with points for this line
	 */
	public LineMapObject(Point2D[] ls) {
		this.ls = ls;
	}

	/**
	 * constructor
	 * 
	 * @param p1: the first point for this line
	 * @param p2: the second point for this line
	 */
	public LineMapObject(Point2D p1, Point2D p2) {
		ls = new Point2D.Double[2];
		ls[0] = p1;
		ls[1] = p2;
	}

	@Override
	public void draw(Graphics2D g, Transformation t) {
		Point2D p1 = ls[0];
		Point2D p2 = ls[1];
		int row1 = t.getRow(p1.getY());
		int col1 = t.getColumn(p1.getX());
		int row2 = t.getRow(p2.getY());
		int col2 = t.getColumn(p2.getX());
		g.drawLine(col1, row1, col2, row2);
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope env = new Envelope();
		env.expandToInclude(ls[0].getX(), ls[0].getY());
		env.expandToInclude(ls[1].getX(), ls[1].getY());
		return env;
	}

	@Override
	public String toString() {
		return "(" + ls[0].getX() + "|" + ls[0].getY() + ")->(" + ls[1].getX() + "|" + ls[1].getY() + ")";
	}

}
