package gisviewer;

import java.awt.Color;
import java.util.List; 
 
import util.geometry.Envelope;

/**
 * A layer that can be added to a map.
 * 
 * @author haunert
 */
public abstract class Layer {

	/**
	 * the total extent of this layer
	 */
	protected Envelope extent;

	/**
	 * the default color used for rendering the objects in this layer
	 */
	protected Color myColor;

	public Layer(Color c) {
		myColor = c;
	}

	/**
	 * Returns the extent of this layer as an envelope
	 * 
	 * @return
	 */
	public Envelope getExtent() {
		return extent;
	}

	/**
	 * Queries all Objects whose bounding boxes intersect the search envelope
	 * 
	 * @param searchEnv
	 * @return
	 */
	public abstract List<MapObject> query(Envelope searchEnv);

	/**
	 * creates a new layer in which all objects are stored in a list, based on this
	 * layer
	 * 
	 * @return
	 */
	public ListLayer toCachedLayer() {
		ListLayer myCachedLayer = new ListLayer(myColor);
		for (MapObject mo : this.query(extent)) {
			myCachedLayer.add(mo);
		}
		return myCachedLayer;
	}

	public Color getColor() {
		return myColor;
	}

	public void setColor(Color c) {
		myColor = c;
	}
}
