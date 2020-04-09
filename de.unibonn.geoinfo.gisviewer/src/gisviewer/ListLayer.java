package gisviewer;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import util.geometry.Envelope;

/**
 * This class represents a layer that can be displayed in a Map.
 * 
 * @author Jan-Henrik Haunert
 */
public class ListLayer extends Layer {

	/**
	 * The MapObjects of this Layer.
	 */
	private LinkedList<MapObject> myObjects;

	/**
	 * Constructs a new empty Layer with a specified ID.
	 * 
	 * @param myID the ID
	 */
	public ListLayer(Color c) {
		super(c);
		extent = null;
		myObjects = new LinkedList<MapObject>();
	}

	/**
	 * Adds a MapObject to this Layer.
	 * 
	 * @param m the MapObject to be added
	 */
	public void add(MapObject m) {
		myObjects.add(m);
		if (extent == null) {
			extent = m.getBoundingBox();
		} else {
			extent.expandToInclude(m.getBoundingBox());
		}
	}

	/**
	 * Returns the map objects of this layer that intersect a specified envelope.
	 * 
	 * @param searchEnv the query envelope
	 * @return the map objects of this layer that intersect the envelope
	 */
	@Override
	public List<MapObject> query(Envelope searchEnv) {
		List<MapObject> result = new LinkedList<MapObject>();
		for (MapObject m : myObjects) {
			if (searchEnv.intersects(m.getBoundingBox())) {
				result.add(m);
			}
		}
		return result;
	}

	public TreeLayer toTreeLayer() {
		TreeLayer tl = new TreeLayer(this.myColor);
		for (MapObject mo : myObjects) {
			tl.add(mo);
		}
		return tl;
	}

	public boolean isEmpty() {
		return myObjects.isEmpty();
	}
}
