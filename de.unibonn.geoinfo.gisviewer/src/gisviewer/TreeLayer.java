package gisviewer;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.index.strtree.STRtree;

import util.geometry.Envelope;

/**
 * a layer of a map that organizes the objects in a tree to enable fast querying
 * with rectangles
 * 
 * @author haunert_admin
 *
 */
public class TreeLayer extends Layer {

	/**
	 * the map objects in a tree
	 */ 
	private STRtree myObjects; 
 
	/**
	 * constructor for generating an empty layer
	 * 
	 * @param c
	 */
	public TreeLayer(Color c) {
		super(c);
		extent = null;
		myObjects = new STRtree();
	}

	/**
	 * method for retrieving all objects intersecting a query envelope
	 */
	@Override
	public List<MapObject> query(Envelope bb) {
		LinkedList<MapObject> result = new LinkedList<MapObject>();
		com.vividsolutions.jts.geom.Envelope jtsEnv = new com.vividsolutions.jts.geom.Envelope(bb.getxMin(),
				bb.getxMax(), bb.getyMin(), bb.getyMax());
		for (Object o : myObjects.query(jtsEnv)) {
			MapObject mo = (MapObject) o;
			result.add(mo);
		}
		return result;
	}

	/**
	 * method for adding an object to the layer
	 * 
	 * @param mo: the object to be added
	 */
	public void add(MapObject mo) {
		Envelope bb = mo.getBoundingBox();
		com.vividsolutions.jts.geom.Envelope jtsEnv = new com.vividsolutions.jts.geom.Envelope(bb.getxMin(),
				bb.getxMax(), bb.getyMin(), bb.getyMax());
		myObjects.insert(jtsEnv, mo);
		if (extent == null) {
			extent = mo.getBoundingBox();
		} else {
			extent.expandToInclude(mo.getBoundingBox());
		}
	}

}
