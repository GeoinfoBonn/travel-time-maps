package ipeio.drawables;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import ipeio.api.IpeDrawable;
import ipeio.api.IpeTransformation;
import util.geometry.Envelope;

public class IpePath extends IpeDrawable {
	
	private Path2D path;
	
	public IpePath(Path2D path) {
		this.path = path;
	}

	@Override
	public String toIpeString(IpeTransformation t, Color c) {
		changeLayer("path");
		return ipePath(t, c, path, ARROW_FORWARD, SIZE_NORMAL, 1);
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope bb = new Envelope();

		PathIterator pi = path.getPathIterator(null);
		
		double[] coords = new double[6];
		while (!pi.isDone()) {
			int status = pi.currentSegment(coords);
			
			bb.expandToInclude(coords[0], coords[1]);				
			if (status == PathIterator.SEG_QUADTO || status == PathIterator.SEG_CUBICTO) {
				bb.expandToInclude(coords[2], coords[3]);
			}
			if (status == PathIterator.SEG_CUBICTO) {
				bb.expandToInclude(coords[4], coords[5]);
			}
			
			pi.next();
		}
		return bb;
	}

	@Override
	public List<String> layerList() {
		List<String> layers = new ArrayList<>();
		layers.add("path");
		return layers;
	}		
}