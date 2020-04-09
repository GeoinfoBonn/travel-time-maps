package ipeio.drawables;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import ipeio.api.IpeDrawable;
import ipeio.api.IpeTransformation;
import util.geometry.Envelope;

public class IpePoints  extends IpeDrawable {
	
	ArrayList<Point2D> points;
	
	public IpePoints(ArrayList<Point2D> points) {
		this.points = points;
	}

	@Override
	public String toIpeString(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();
		
		changeLayer("points");
		
		Point2D prev = null;
		for (Point2D curr : points) {
			if (prev != null)
				sb.append(ipeLine(t, c, new Line2D.Double(prev, curr), ARROW_FORWARD, SIZE_NORMAL, 3));
			sb.append(ipeMarker(t, Color.BLACK, curr, MARKER_CROSS, SIZE_NORMAL));
			
			Point2D labelPos = new Point2D.Double(curr.getX(), curr.getY()+5);
			
			sb.append(ipeLabel(t,c,labelPos,curr.toString(),SIZE_TINY));
			prev = curr;
		}		
		
		return sb.toString();
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope bb = new Envelope();

		for (Point2D point : points) {
			bb.expandToInclude(point.getX(), point.getY());
		}
		return bb;
	}

	@Override
	public List<String> layerList() {
		List<String> layers = new ArrayList<>();
		layers.add("points");
		return layers;
	}
}