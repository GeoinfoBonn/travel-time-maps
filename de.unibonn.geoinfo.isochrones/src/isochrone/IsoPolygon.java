package isochrone;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;

import graph.generic.LD.factory.TurncostFactory;
import main.AbstractMain;
import util.tools.Util;

public class IsoPolygon<P extends Point2D> {

	private int componentId;

	private List<P> outerRing = new LinkedList<P>();
	private List<List<P>> innerRings = new LinkedList<List<P>>();

	private Geometry visualizationPolygon = null;
	private List<Polygon> outerPolygons = null;
	private List<Polygon> innerPolygons = null;
	private boolean needCombine = true;

	private GeometryFactory gf = new GeometryFactory();

	private String debugMessage;

	public IsoPolygon(List<P> outerRing, String message, IdGenerator idGen) {
		super();
		this.outerRing = outerRing;
		this.debugMessage = message;
		this.componentId = idGen.nextComponentId();
	}

	public IsoPolygon(List<P> outerRing, List<List<P>> innerRings, IdGenerator idGen) {
		super();
		this.outerRing = outerRing;
		this.setInnerRings(innerRings);
		this.componentId = idGen.nextComponentId();
	}

	public Geometry fromRing(List<P> ring) {
		Coordinate[] c = new Coordinate[ring.size()];
		int counter = 0;
		// Iterate through all streetnodes of one polygon
		for (P p : ring) {
			c[counter] = new Coordinate(p.getX(), p.getY());
			counter++;
		}
		// Create polygon from coordinate list
		Geometry poly = this.gf.createPolygon(c);
		return poly.buffer(1e-12, 0).buffer(-1e-12, 0);
	}

	public synchronized Geometry getVisualizationPolygon() {
		if (needCombine) {
			Geometry inner = null;
			Geometry outer = null;

			outer = fromRing(outerRing);

			for (List<P> innerRing : innerRings) {
				inner = fromRing(innerRing);

				try {
					outer = outer.difference(inner);
				} catch (TopologyException e) {
					System.err.println("Topology exception: " + e.getMessage());
				}
			}
			visualizationPolygon = outer;

			outerPolygons = new LinkedList<>();
			innerPolygons = new LinkedList<>();

			Polygon currOuter;
			for (int i = 0; i < visualizationPolygon.getNumGeometries(); i++) {
				currOuter = (Polygon) visualizationPolygon.getGeometryN(i);
				if (currOuter != null) {
					outerPolygons.add(currOuter);
					for (int j = 0; j < currOuter.getNumInteriorRing(); ++j) {
						innerPolygons.add(new Polygon((LinearRing) currOuter.getInteriorRingN(j), null, gf));
					}
				} else {
					System.err.println("currOuter WAS null!! WHY??");
				}
			}

			needCombine = false;
			if (visualizationPolygon.isEmpty()) {
				String message = "Empty visualization polygon";
				addToMessage(message);
				System.err.println(message);
			}
			return outer;
		} else {
			return visualizationPolygon;
		}
	}

	public synchronized List<Polygon> getOuterPolygons() {
		if (needCombine) {
			getVisualizationPolygon();
		}
		return outerPolygons;
	}

	public synchronized List<Polygon> getInnerPolygons() {
		if (needCombine) {
			getVisualizationPolygon();
		}
		return innerPolygons;
	}

	public double getCompactness() {
		if (getOuterPolygons().isEmpty())
			return 0;

		return 4 * Math.PI * getArea() / Math.pow(getPerimeter(), 2);
	}

	public double getArea() {
		return getVisualizationPolygon().getArea();
	}

	public double getOuterArea() {
		double area = 0;
		for (Polygon poly : outerPolygons) {
			area += poly.getArea();
		}
		return area;
	}

	public List<P> getOuterRing() {
		return this.outerRing;
	}

	public void setOuterRing(List<P> outerRing) {
		this.outerRing = outerRing;
		needCombine = true;
	}

	public List<List<P>> getInnerRings() {
		return this.innerRings;
	}

	public void setInnerRings(List<List<P>> innerRings) {
		if (innerRings == null)
			return;
		this.innerRings = innerRings;
		needCombine = true;
	}

	public void addInnerRing(List<P> pointList) {
		this.innerRings.add(pointList);
		needCombine = true;
	}

	public boolean covers(Point2D point) {
		com.vividsolutions.jts.geom.Point p = this.gf.createPoint(new Coordinate(point.getX(), point.getY()));
		return this.getVisualizationPolygon().covers(p);
	}

	public boolean covers(Point2D point1, Point2D point2) {
		Coordinate[] coordinates = new Coordinate[2];
		coordinates[0] = new Coordinate(point1.getX(), point1.getY());
		coordinates[1] = new Coordinate(point2.getX(), point2.getY());
		LineString linestring = this.gf.createLineString(coordinates);
		return this.getVisualizationPolygon().covers(linestring);
	}

	public int getNumHoles() {
		return getInnerPolygons().size();
	}

	public double getAreaHoles() {
		double area = 0;
		for (Polygon inner : getInnerPolygons()) {
			area += inner.getArea();
		}
		return area;
	}

//	public int getNumOuterTurns() {
//		return numTurns(ringToCoordinate(false));
//	}

	/**
	 * According to: Measuring the Complexity of Polygonal Objects. Brinkhoff et al.
	 * (1995). A notch is a node that is not on the convex hull of a polygon.
	 */
	public double getNotchNorm() {
		int numTurns = this.getNumTurns();

		MeasureCalculator notchCalculator = new MeasureCalculator() {
			@Override
			public double calculateMeasure(Point2D prev, Point2D curr, Point2D next) {
				int direction = TurncostFactory.direction(prev, curr, next);
				if (direction < 0)
					return 1;
				return 0;
			}
		};

		int notches = (int) calculateRingMeasure(ringToCorners(outerRing, false), notchCalculator);
		for (List<P> inner : innerRings) {
			notches += (int) calculateRingMeasure(ringToCorners(inner, true), notchCalculator);
		}

		if (AbstractMain.DEBUG) {
			System.out.println("|N|: " + notches);
			System.out.println("|V|: " + numTurns);
			System.out.println("NN: " + notches / (numTurns - 3.0));
		}

		if (notches > (numTurns - 3)) {
			String message = "NN: number of notches too high! |N|=" + notches + " |V|=" + numTurns;
			addToMessage(message);
			System.err.println(message);
		}

		return notches / (numTurns - 3.0);
	}

	/**
	 * According to: Measuring the Complexity of Polygonal Objects. Brinkhoff et al.
	 * (1995).
	 */
	public double getFrequencyOfVibration() {
		double notchNorm = getNotchNorm();
		if (AbstractMain.DEBUG)
			System.out.println("FOV: " + (16 * Math.pow(notchNorm - 0.5, 4) - 8 * Math.pow(notchNorm - 0.5, 2) + 1));
		return 16 * Math.pow(notchNorm - 0.5, 4) - 8 * Math.pow(notchNorm - 0.5, 2) + 1;
	}

	/**
	 * According to: Measuring the Complexity of Polygonal Objects. Brinkhoff et al.
	 * (1995).
	 */
	public double getAmplitudeOfVibration() {
		Geometry convHull = visualizationPolygon.convexHull();

		double convBoundary = 0;
		if (convHull instanceof Polygon) {
			Polygon conv = (Polygon) convHull;
			convBoundary = conv.getBoundary().getLength();
		} else if (convHull instanceof LineString) {
			LineString conv = (LineString) convHull;
			convBoundary = conv.getLength();
		} else {
			String message = "AOV: Unexpected Geometry type: " + convHull.getClass().getName();
			addToMessage(message);
			System.err.println(message);
			return -1;
		}

		double boundary = getOuterPerimeter();
		if (boundary == 0)
			return -1;
		if (AbstractMain.DEBUG)
			System.out.println("AOV: " + ((boundary - convBoundary) / boundary));
		return (boundary - convBoundary) / boundary;
	}

	/**
	 * According to: Measuring the Complexity of Polygonal Objects. Brinkhoff et al.
	 * (1995).
	 */
	public double getConvexity() {
		Geometry convHull = visualizationPolygon.convexHull();

		double convArea = 0;
		if (convHull instanceof Polygon)
			convArea = ((Polygon) convHull).getArea();
		else {
			String message = "C: Unexpected Geometry type: " + convHull.getClass().getName();
			addToMessage(message);
			System.err.println(message);
			return -1;
		}
		if (AbstractMain.DEBUG)
			System.out.println("C: " + ((convArea - getOuterArea()) / convArea));
		return (convArea - getOuterArea()) / convArea;
	}

	/**
	 * According to: Measuring the Complexity of Polygonal Objects. Brinkhoff et al.
	 * (1995).
	 */
	public double getComplexity() {
		double aov = getAmplitudeOfVibration();
		double fov = getFrequencyOfVibration();
		double conv = getConvexity();
		if (aov < 0 || fov < 0 || conv < 0)
			return 0;
		return 0.8 * aov * fov + 0.2 * conv;
	}

	public int getNumTurns() {
		int outerTurns = calculateNumTurns(ringToCorners(outerRing, false));

		int turns = outerTurns;
		for (List<P> inner : innerRings) {
			turns += calculateNumTurns(ringToCorners(inner, true));
		}

		return turns;
	}

	private int calculateNumTurns(Coordinate[] c) {
		return (int) calculateRingMeasure(c, new MeasureCalculator() {
			@Override
			public double calculateMeasure(Point2D prev, Point2D curr, Point2D next) {
				return TurncostFactory.computeTurncosts(prev, curr, next) == 0 ? 0 : 1;
			}
		});
	}

	public int getNumTurnsWeighted() {
		int outerTurns = calculateNumTurnsWeighted(ringToCorners(outerRing, false));

		int turns = outerTurns;
		for (List<P> inner : innerRings) {
			turns += calculateNumTurnsWeighted(ringToCorners(inner, true));
		}

		return turns;
	}

	private int calculateNumTurnsWeighted(Coordinate[] c) {
		return (int) calculateRingMeasure(c, new MeasureCalculator() {
			@Override
			public double calculateMeasure(Point2D prev, Point2D curr, Point2D next) {
				return TurncostFactory.computeTurncosts(prev, curr, next);
			}
		});
	}

	public double getOuterPerimeter() {
		return getPerimeter(ringToCorners(outerRing, false));
	}

	public double getPerimeter() {
		double distance = getOuterPerimeter();
		for (List<P> inner : innerRings) {
			distance += getPerimeter(ringToCorners(inner, true));
		}
		return distance;
	}

	private double getPerimeter(Coordinate[] c) {
		return calculateRingMeasure(c, new MeasureCalculator() {
			@Override
			public double calculateMeasure(Point2D prev, Point2D curr, Point2D next) {
				return curr.distance(next);
			}
		});
	}

	public double getOctiPerimeter() {
		double distance = 0;
		distance += getOctiPerimeter(ringToCorners(outerRing, false));

		for (List<P> inner : innerRings) {
			distance += getOctiPerimeter(ringToCorners(inner, true));
		}
		return distance;
	}

	private double getOctiPerimeter(Coordinate[] c) {
		return calculateRingMeasure(c, new MeasureCalculator() {
			@Override
			public double calculateMeasure(Point2D prev, Point2D curr, Point2D next) {
				return TurncostFactory.isOcti(Util.getInclination(curr, next)) ? curr.distance(next) : 0;
			}
		});
	}

	public int[] outerAngleHistogramm(int nBins) {
		int[] bins = new int[nBins + 1];

//		Coordinate[] c = groupCorners(ringToCorners(outerRing, false));

		Integer num = null;
		if (AbstractMain.DEBUG)
			num = 0;
		bins = collectBins(groupCorners(ringToCorners(outerRing, false)), bins, num);

		for (List<P> inner : innerRings) {
			if (AbstractMain.DEBUG)
				num++;
			bins = collectBins(ringToCorners(inner, true), bins, num);
		}

		return bins;
	}

	public static double[] getBinBounds(int nBins) {
		double[] binBounds = new double[nBins + 1];
		double binSize = 2 * Math.PI / nBins;
		double b = binSize / 2;
		for (int i = 0; i < nBins; ++i) {
			binBounds[i] = b;
			b += binSize;
		}
		binBounds[nBins] = 2 * Math.PI;
		return binBounds;
	}

	private int[] collectBins(Coordinate[] c, int[] bins, Integer num) {
		if (c.length < 2)
			return bins;

		BufferedWriter bw = null;
		if (num != null) {
			try {
				bw = new BufferedWriter(new FileWriter(
						new File(AbstractMain.OUTPUT_DIRECTORY + "/turns_" + componentId + "_" + num + ".csv")));
				bw.write("x,y,turn,inclination");
				bw.newLine();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}
		}

		int nBins = bins.length - 1;
		double binSize = 2 * Math.PI / nBins;

		Point2D prev = new Point2D.Double(c[c.length - 1].x, c[c.length - 1].y);
		Point2D curr, next;
		double inc, out, incident;
		int currBin;
		for (int i = 0; i < c.length; i++) {
			curr = new Point2D.Double(c[i].x, c[i].y);
			next = new Point2D.Double(c[(i + 1) % c.length].x, c[(i + 1) % c.length].y);

			inc = Util.getInclination(curr, prev);
			out = Util.getInclination(curr, next);

			incident = inc - out >= 0 ? inc - out : inc - out + 2 * Math.PI;

//			if (curr.distance(next) < AbstractMain.FACE_BOUNDARY_BUFFER) {
//				prev = curr;
//				continue;
//			}

//			// this block filters out u-turns (mainly in boundary type), where two 270deg
//			// turns would be recognized otherwise (due to the FACE_BOUNDARY_BUFFER)
//			if (Math.abs(incident - 3.0 / 2 * Math.PI) < 0.15) {
//				if (curr.distance(next) <= AbstractMain.FACE_BOUNDARY_BUFFER * 3) {
//					// System.out.println("Altered " + curr);
//					incident = 2 * Math.PI;
//				} else if (curr.distance(prev) <= AbstractMain.FACE_BOUNDARY_BUFFER * 3) {
//					// System.out.println("Skipped " + curr);
//					prev = curr;
//					continue;
//				}
//			}

			incident = (incident + binSize / 2);
			if (incident >= 2 * Math.PI)
				currBin = nBins;
			else
				currBin = (int) (incident / binSize);

			if (bw != null) {
				try {
					bw.write(curr.getX() + "," + curr.getY() + "," + currBin + "," + inc);
					bw.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e.getMessage());
				}
			}
			bins[currBin]++;

			prev = curr;
		}

		if (bw != null)
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException(e.getMessage());
			}

		return bins;
	}

	private double calculateRingMeasure(Coordinate[] c, MeasureCalculator mc) {
		double dist = 0;
		if (c.length < 2)
			return dist;

		Point2D prev = new Point2D.Double(c[c.length - 1].x, c[c.length - 1].y);
		Point2D curr, next;
		for (int i = 0; i < c.length; i++) {
			curr = new Point2D.Double(c[i].x, c[i].y);
			next = new Point2D.Double(c[(i + 1) % c.length].x, c[(i + 1) % c.length].y);

			dist += mc.calculateMeasure(prev, curr, next);

			prev = curr;
		}
		return dist;
	}

	public String getMessage() {
		if (debugMessage == null || debugMessage.isBlank())
			return null;
		return debugMessage;
	}

	public void addToMessage(String additional) {
		if (debugMessage == null || debugMessage.isBlank())
			debugMessage = additional;
		else
			debugMessage += ", " + additional;
	}

	public int getComponentId() {
		return componentId;
	}

	/**
	 * Returns an array of coordinates of points in the given <code>ring</code>. The
	 * points are filtered in a way that only corner points are inserted into the
	 * output array (put differently, all points on straight lines are removed). The
	 * first point in the array is NOT repeated at the end of the array.
	 * 
	 * If <code>reverse</code> is <code>true</true>, the output array is given in
	 * reverse order of points.
	 * 
	 * @param ring    list of input points
	 * @param reverse if nodes in output should be given in reverse order
	 * @return list of corner coordinates
	 */
	public Coordinate[] ringToCorners(final List<P> ring, boolean reverse) {

		int nPoints = ring.size();
		if (nPoints < 3) {
			System.err.println("Ring too small");
			return new Coordinate[0];
		}

		LinkedList<Coordinate> points = new LinkedList<>();
		P prev = ring.get(nPoints - 2);
		P curr, next;
		for (int i = 0; i < nPoints - 1; ++i) {
			if (i != 0)
				prev = ring.get(i - 1);
			curr = ring.get(i % nPoints);
			next = ring.get((i + 1) % nPoints);

			if (TurncostFactory.computeTurncosts(prev, curr, next) != 0) {
				points.add(new Coordinate(curr.getX(), curr.getY()));
			}
		}

		if (reverse)
			Collections.reverse(points);
		return points.toArray(new Coordinate[points.size()]);
	}

	public Coordinate[] groupCorners(Coordinate[] corners) {
		int nPoints = corners.length;
		if (nPoints == 0)
			return new Coordinate[0];

		LinkedList<Coordinate> grouped = new LinkedList<>();

		Coordinate current = null;
		Coordinate currGroupBegin = corners[0];
		LinkedList<Coordinate> currGroup = new LinkedList<>();
		currGroup.add(currGroupBegin);
		for (int i = 0; i < nPoints; ++i) {
			current = corners[i];
			if (currGroupBegin.distance(current) < 3 * AbstractMain.FACE_BOUNDARY_BUFFER) {
				currGroup.add(current);
			} else {
				grouped.add(meanCoordinate(currGroup));
				currGroupBegin = current;
				currGroup = new LinkedList<>();
				currGroup.add(currGroupBegin);
			}
		}
		Coordinate lastGroup = meanCoordinate(currGroup);
		if (grouped.getFirst().distance(lastGroup) > 3 * AbstractMain.FACE_BOUNDARY_BUFFER)
			grouped.add(lastGroup);
		return grouped.toArray(new Coordinate[grouped.size()]);
	}

	private Coordinate meanCoordinate(List<Coordinate> c) {
		double x = 0, y = 0;
		for (var coord : c) {
			x += coord.x;
			y += coord.y;
		}
		return new Coordinate(x / c.size(), y / c.size());
	}

	public interface MeasureCalculator {
		public double calculateMeasure(Point2D prev, Point2D curr, Point2D next);
	}
}
