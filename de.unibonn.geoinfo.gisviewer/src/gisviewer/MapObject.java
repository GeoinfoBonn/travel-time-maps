package gisviewer;

import java.awt.Graphics2D;

import util.geometry.Envelope;

/**
 * interface that must be implemented by all classes representing objects that
 * can be displayed in a map
 * 
 * @author haunert
 */
public interface MapObject {
	/**
	 * This method is called to draw this MapObject into a specified Graphics
	 * object.
	 * 
	 * @param g the Graphics object
	 * @param t the Transformation that maps world coordinates to image coordinates
	 */
	public void draw(Graphics2D g, Transformation t);

	/**
	 * @return the bounding box
	 */
	public Envelope getBoundingBox();
}
