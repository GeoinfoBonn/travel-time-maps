package ipeio.drawables;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ipeio.api.IpeDrawable;
import ipeio.api.IpeTransformation;
import util.geometry.Envelope;

public class IpeLineSegmentCollection extends IpeDrawable {
	
	List<Line2D> segments;
	String layerName = "segments";
	
	public IpeLineSegmentCollection(List<Line2D> segments) {
		this.segments = segments;
	}

	@Override
	public String toIpeString(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		for (Line2D segment : segments) {
			sb.append(writeSegment(segment, t, c, layerName));
		}

		return sb.toString();
	}
	
	public String writeSegment(Line2D segment, IpeTransformation t, Color c, String layer) {
		Point2D source = segment.getP1();
		Point2D target = segment.getP2();
		Point2D midpoint = new Point2D.Double((source.getX() + target.getX()) / 2, (source.getY() + target.getY()) / 2);
		
		StringBuilder sb = new StringBuilder();
		sb.append(ipeLabel(t, c, midpoint, "", SIZE_TINY));
		sb.append(ipeLine(t, c, new Line2D.Double(source, target), ARROW_FORWARD, SIZE_SMALL, 1));
		
		return sb.toString();
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope bb = new Envelope();
		
		for (Line2D ls : segments) {
			bb.expandToInclude(ls.getP1().getX(), ls.getP1().getY());
			bb.expandToInclude(ls.getP2().getX(), ls.getP2().getY());
		}
		return bb;
	}

	@Override
	public List<String> layerList() {
		List<String> ret = new ArrayList<>();
		ret.add(layerName);
		return ret;
	}
	
}
