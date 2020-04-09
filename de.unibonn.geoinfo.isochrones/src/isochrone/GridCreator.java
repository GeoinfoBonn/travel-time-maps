package isochrone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import util.geometry.Envelope;
import util.tools.Util;

public class GridCreator {

	public static final byte HORIZONTAL_LINES = 1;
	public static final byte VERTICAL_LINES = 2;
	public static final byte DIAG_UP_LINES = 4;
	public static final byte DIAG_DOWN_LINES = 8;

	public static final byte OCTILINEAR = (byte) (HORIZONTAL_LINES | VERTICAL_LINES | DIAG_UP_LINES | DIAG_DOWN_LINES);
	public static final byte RECTILINEAR = (byte) (HORIZONTAL_LINES | VERTICAL_LINES);

	private Envelope bbox;

	public GridCreator(Envelope bbox) {
		this.bbox = bbox;
	}

	public MultiLineString createGrid(double gridWidth, byte type) {
		LineString[] ls = new LineString[0];
		GeometryFactory gf = new GeometryFactory();

		if ((type & HORIZONTAL_LINES) > 0) {
			LineString[] hor = createHorizontalLines(gridWidth, gf);
			ls = Util.concat(ls, hor);
		}

		if ((type & VERTICAL_LINES) > 0) {
			LineString[] ver = createVerticalLines(gridWidth, gf);
			ls = Util.concat(ls, ver);
		}

		if ((type & DIAG_UP_LINES) > 0) {
			LineString[] dup = createDiagUpLines(gridWidth, gf);
			ls = Util.concat(ls, dup);
		}

		if ((type & DIAG_DOWN_LINES) > 0) {
			LineString[] ddown = createDiagDownLines(gridWidth, gf);
			ls = Util.concat(ls, ddown);
		}

		return new MultiLineString(ls, gf);
	}

	private LineString[] createHorizontalLines(double gridWidth, GeometryFactory gf) {
		int nH = (int) Math.ceil((bbox.getxMax() - bbox.getxMin()) / gridWidth);
		int nV = (int) Math.ceil((bbox.getyMax() - bbox.getyMin()) / gridWidth);

		LineString[] ls = new LineString[nH * (nV + 1)];

		int index = 0;
		double x0, x1, y;
		Coordinate[] pts;
		for (int h = 0; h < nH; ++h) {
			x0 = bbox.getxMin() + h * gridWidth;
			x1 = x0 + gridWidth;

			for (int v = 0; v < nV + 1; ++v) {
				y = bbox.getyMin() + v * gridWidth;

				pts = new Coordinate[2];
				pts[0] = new Coordinate(x0, y);
				pts[1] = new Coordinate(x1, y);

				ls[index++] = gf.createLineString(pts);
			}
		}

		return ls;
	}

	private LineString[] createVerticalLines(double gridWidth, GeometryFactory gf) {
		int nH = (int) Math.ceil((bbox.getxMax() - bbox.getxMin()) / gridWidth);
		int nV = (int) Math.ceil((bbox.getyMax() - bbox.getyMin()) / gridWidth);

		LineString[] ls = new LineString[(nH + 1) * nV];

		int index = 0;
		double x, y0, y1;
		Coordinate[] pts;
		for (int v = 0; v < nV; ++v) {
			y0 = bbox.getyMin() + v * gridWidth;
			y1 = y0 + gridWidth;

			for (int h = 0; h < nH + 1; ++h) {
				x = bbox.getxMin() + h * gridWidth;

				pts = new Coordinate[2];
				pts[0] = new Coordinate(x, y0);
				pts[1] = new Coordinate(x, y1);

				ls[index++] = gf.createLineString(pts);
			}
		}

		return ls;
	}

	private LineString[] createDiagUpLines(double gridWidth, GeometryFactory gf) {
		int nH = (int) Math.ceil((bbox.getxMax() - bbox.getxMin()) / gridWidth) * 2;
		int nV = (int) Math.ceil((bbox.getyMax() - bbox.getyMin()) / gridWidth) * 2;

		double d = gridWidth / 2;

		LineString[] ls = new LineString[nH * nV / 2];

		int index = 0;
		double x0, x1, y0, y1;
		Coordinate[] pts;
		for (int v = 0; v < nV; ++v) {
			y0 = bbox.getyMin() + v * d;
			y1 = y0 + d;

			for (int h = 0; h < nH; ++h) {
				if (v % 2 == h % 2) {
					x0 = bbox.getxMin() + h * d;
					x1 = x0 + d;

					pts = new Coordinate[2];
					pts[0] = new Coordinate(x0, y0);
					pts[1] = new Coordinate(x1, y1);

					ls[index++] = gf.createLineString(pts);
				}
			}
		}

		return ls;
	}

	private LineString[] createDiagDownLines(double gridWidth, GeometryFactory gf) {
		int nH = (int) Math.ceil((bbox.getxMax() - bbox.getxMin()) / gridWidth) * 2;
		int nV = (int) Math.ceil((bbox.getyMax() - bbox.getyMin()) / gridWidth) * 2;

		double d = gridWidth / 2;

		LineString[] ls = new LineString[nH * nV / 2];

		int index = 0;
		double x0, x1, y0, y1;
		Coordinate[] pts;
		for (int v = 0; v < nV; ++v) {
			y1 = bbox.getyMin() + v * d;
			y0 = y1 + d;

			for (int h = 0; h < nH; ++h) {
				if (v % 2 != h % 2) {
					x0 = bbox.getxMin() + h * d;
					x1 = x0 + d;

					pts = new Coordinate[2];
					pts[0] = new Coordinate(x0, y0);
					pts[1] = new Coordinate(x1, y1);

					ls[index++] = gf.createLineString(pts);
				}
			}
		}

		return ls;
	}
}
