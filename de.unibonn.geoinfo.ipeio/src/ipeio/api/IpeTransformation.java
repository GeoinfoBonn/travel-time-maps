package ipeio.api;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import util.geometry.Envelope;

public class IpeTransformation extends AffineTransform {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2300877930425878520L;
	
//	private final static int IPE_WIDTH = 595;
//	private final static int IPE_HEIGHT = 842;
	private final static int IPE_WIDTH = 1500;
	private final static int IPE_HEIGHT = 1500;
	private double m;
	double minX, minY;

	public IpeTransformation(ArrayList<IpeDrawable> list) {
		Envelope env = new Envelope();
		for (IpeDrawable o : list) {
			env.expandToInclude(o.getBoundingBox());
		}
		double scaleX = IPE_WIDTH / env.getWidth();
		double scaleY = IPE_HEIGHT / env.getHeight();
		if (scaleX < scaleY) {
			m = scaleX;
		} else {
			m = scaleY;
		}
		minX = env.getxMin() * m;
		minY = env.getyMin() * m;
		
		this.setTransform(m, 0, 0, m, -minX, -minY);
	}

	public double scaleX(double d) {
		return m * d - minX;
	}

	public double scaleY(double d) {
		return m * d - minY;
	}
}
