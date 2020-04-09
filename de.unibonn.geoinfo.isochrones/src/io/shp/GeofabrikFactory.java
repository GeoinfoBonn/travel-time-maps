package io.shp;

import java.awt.geom.Point2D;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;

import graph.types.GeofabrikData;
import graph.types.RoadGraph;
import main.AbstractMain;
import util.geometry.Envelope;

public class GeofabrikFactory
		implements ShapeFileReader.Factory<RoadGraph<Point2D, GeofabrikData>, Point2D, GeofabrikData> {

	private String colOneway;

	public GeofabrikFactory() {
		this(false);
	}

	public GeofabrikFactory(boolean withOneway) {
		if (withOneway) {
			this.colOneway = "oneway";
		}
	}

	public GeofabrikFactory(String colOneway) {
		this.colOneway = colOneway;
	}

	@Override
	public boolean includeFeature(Feature f) {
		if (!AbstractMain.KEEP_MOTORWAY) {
			String type = f.getAttribute("fclass").toString().trim();
			if (type.equals("motorway_link") || type.equals("motorway") || type.equals("trunk")
					|| type.equals("trunk_link"))
				return false;
		}
		return true;
	}

	@Override
	public byte getOneway(Feature f) {
		if (colOneway != null) {
			String onewayTag = (String) f.getAttribute(colOneway);
			switch (onewayTag) {
			case "B":
				return 0;
			case "F":
				return 1;
			case "T":
				return -1;
			default:
				System.err.println("Unknown oneway type: " + onewayTag);
			}
		}
		return 0;
	}

	@Override
	public RoadGraph<Point2D, GeofabrikData> getGraph(FeatureCollection fc) {
		return new RoadGraph<Point2D, GeofabrikData>(getEnvelopeFromShape(fc.getEnvelope()));
	}

	@Override
	public GeofabrikData createArcData(double distance, Feature f) {
		String osmId = ((String) f.getAttribute("osm_id")).trim();
		Integer code = (Integer) f.getAttribute("code");
		String fclass = ((String) f.getAttribute("fclass")).trim();
		byte oneway = getOneway(f);
		Integer maxspeed = (Integer) f.getAttribute("maxspeed");
		Integer layer = (Integer) f.getAttribute("layer");
		boolean bridge = ((String) f.getAttribute("bridge")).equals("T");
		boolean tunnel = ((String) f.getAttribute("tunnel")).equals("T");

		return new GeofabrikData(distance, osmId, code, fclass, oneway, maxspeed, layer, bridge, tunnel);
	}

	@Override
	public Point2D createNodeData(double x, double y) {
		return new Point2D.Double(x, y);
	}

	public static Envelope getEnvelopeFromShape(com.vividsolutions.jts.geom.Envelope e) {
		return new Envelope(e.getMinX(), e.getMaxX(), e.getMinY(), e.getMaxY());
	}
}
