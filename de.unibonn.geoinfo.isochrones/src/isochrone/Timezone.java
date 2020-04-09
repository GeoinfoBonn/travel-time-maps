package isochrone;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import isochrone.FaceIdentifier.FaceFactory;
import main.AbstractMain;
import tools.Stopwatch;

public class Timezone<P extends Point2D> {
	private int zoneId;
	private String type;

	private List<IsoPolygon<P>> polyList = new LinkedList<>();
	private long time = 0;
	private String color = "";

	private GeometryFactory gf = new GeometryFactory();
	private Geometry visualizationPolygon;
	private boolean needCombine = true;

	private int fp = -1, tp = -1, fn = -1, tn = -1;

	private IdGenerator idGenerator;

	public Timezone(IdGenerator idGen) {
		this.zoneId = idGen.nextTimezoneId();
		this.idGenerator = idGen;
	}

	public boolean covers(Point2D location) {
		for (IsoPolygon<P> poly : polyList)
			if (poly.covers(location))
				return true;
		return false;
	}

	public Geometry getVisualizationPolygon() {
		if (!needCombine)
			return visualizationPolygon;

		LinkedList<Polygon> visualizationPolygons = new LinkedList<>();
		for (int i = 0; i < polyList.size(); ++i) {
			for (int j = 0; j < polyList.get(i).getVisualizationPolygon().getNumGeometries(); ++j) {
				visualizationPolygons.add((Polygon) polyList.get(i).getVisualizationPolygon().getGeometryN(j));
			}
		}

		Polygon[] polys = new Polygon[visualizationPolygons.size()];
		MultiPolygon combined = new MultiPolygon(visualizationPolygons.toArray(polys), gf);
		visualizationPolygon = combined.buffer(1e-12, 0).buffer(-1e-12, 0);
		needCombine = false;

		if (!((visualizationPolygon instanceof Polygon || visualizationPolygon instanceof MultiPolygon)))
			System.out.println("Type of buffered: " + visualizationPolygon.getClass().getName());

		return visualizationPolygon;
	}

	public Map<IsoPolygon<P>, Set<Integer>> needsInnerComponents(
			PlanarGraph<ColoredNode, GeofabrikData> planarColoredGraph, Stopwatch sw) {
		long innerTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Splitting inner components...");

		Map<IsoPolygon<P>, Set<Integer>> polysWithUnreachableNode = new HashMap<>();
		for (DiGraphNode<ColoredNode, GeofabrikData> node : planarColoredGraph.getNodes()) {
			if (node.getNodeData().getColor() == Colored.UNREACHABLE) {
				for (IsoPolygon<P> poly : this.getPolyList()) {
					if (poly.covers(node.getNodeData())) {
						if (!polysWithUnreachableNode.containsKey(poly))
							polysWithUnreachableNode.put(poly, new HashSet<>());

						if (polysWithUnreachableNode.get(poly).add(node.getId())) {
//							System.out.println(node.getId() + " " + node.getNodeData());
						}

						for (DiGraphArc<ColoredNode, GeofabrikData> arc : node.getOutgoingArcs()) {
							if (poly.covers(arc.getTarget().getNodeData())) {
								if (polysWithUnreachableNode.get(poly).add(arc.getTarget().getId())) {
//									System.out.println(arc.getTarget().getId() + " " + arc.getTarget().getNodeData());
								}
							}
						}
						break;
					}
				}
			}
		}

		innerTime = System.currentTimeMillis() - innerTime;
		sw.add("innerIdentification", innerTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Inner components splitted. (" + innerTime / 1000.0 + "s)");
		return polysWithUnreachableNode;
	}

	public List<IsoPolygon<P>> getPolyList() {
		return polyList;
	}

	public void setPolyList(List<IsoPolygon<P>> polyList) {
		this.polyList = polyList;
		this.needCombine = true;
	}

	public void addPolygon(List<P> polyCoords, String message) {
		IsoPolygon<P> poly = new IsoPolygon<P>(polyCoords, message, idGenerator);
		this.addPolygon(poly);
		this.needCombine = true;
	}

	public void addPolygon(IsoPolygon<P> poly) {
		this.polyList.add(poly);
		this.needCombine = true;
	}

	public int numPolys() {
		return polyList.size();
	}

	public double getArea() {
		double area = 0;
		for (IsoPolygon<P> poly : polyList) {
			area += poly.getArea();
		}
		return area;
	}

	public List<List<P>> getPolyBoundaries() {
		List<List<P>> bounds = new LinkedList<>();
		for (IsoPolygon<P> p : this.polyList) {
			bounds.add(p.getOuterRing());
		}
		return bounds;
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(long time) {
		this.time = time;
		float maxTime = AbstractMain.TIMEZONES[0];
		float minTime = AbstractMain.TIMEZONES[AbstractMain.TIMEZONES.length - 1];
		float hue = (120 - 120 * ((this.time - minTime) / (maxTime - minTime))) / 360;
		Color a = Color.getHSBColor(hue, 1, 1);
		this.color = String.format("#ff%02x%02x%02x", a.getBlue(), a.getGreen(), a.getRed());
	}

	/**
	 * @return the color
	 */
	public String getColor() {
		return color;
	}

	/**
	 * @param color the color to set
	 */
	public void setColor(String color) {
		this.color = color;
	}

	public double averageComplexity() {
		double complexity = 0;
		for (IsoPolygon<P> p : polyList) {
			complexity += p.getComplexity();
		}
		complexity /= polyList.size();
		return complexity;
	}

	public double averageComplexityW() {
		double complexity = 0;
		for (IsoPolygon<P> p : polyList) {
			complexity += p.getComplexity() * p.getOuterArea();
		}
		complexity /= this.getArea();
		return complexity;
	}

	public double averageCompactness() {
		double compactness = 0;
		for (IsoPolygon<P> p : polyList) {
			compactness += p.getCompactness();
		}
		compactness /= polyList.size();
		return compactness;
	}

	public double averageCompactnessW() {
		double compactness = 0;
		for (IsoPolygon<P> p : polyList) {
			compactness += p.getCompactness() * p.getOuterArea();
		}
		compactness /= this.getArea();
		return compactness;
	}

	public int getNumHoles() {
		int holes = 0;
		for (IsoPolygon<P> p : polyList) {
			holes += p.getNumHoles();
		}
		return holes;
	}

	public double getAreaHoles() {
		double area = 0;
		for (IsoPolygon<P> p : polyList) {
			area += p.getAreaHoles();
		}
		return area;
	}

	public int getNumTurns() {
		int numTurns = 0;
		for (IsoPolygon<P> p : polyList) {
			numTurns += p.getNumTurns();
		}
		return numTurns;
	}

	public int numTurnsWeighted() {
		int numTurns = 0;
		for (IsoPolygon<P> p : polyList) {
			numTurns += p.getNumTurnsWeighted();
		}
		return numTurns;
	}

	public double getPerimeter() {
		double outerDist = 0;
		for (IsoPolygon<P> p : polyList) {
			outerDist += p.getPerimeter();
		}
		return outerDist;
	}

	public double getOctiPerimeter() {
		double octiDist = 0;
		for (IsoPolygon<P> p : polyList) {
			octiDist += p.getOctiPerimeter();
		}
		return octiDist;
	}

	public void setQualityMeasures(int tp, int fp, int fn, int tn) {
		this.tp = tp;
		this.fp = fp;
		this.fn = fn;
		this.tn = tn;
	}

	public int getFP() {
		return fp;
	}

	public int getTP() {
		return tp;
	}

	public int getFN() {
		return fn;
	}

	public int getTN() {
		return tn;
	}

	public int getId() {
		return zoneId;
	}

	public int[] outerAngleHistogramm(int nBins) {
		int[] bins = new int[nBins + 1];
		int[] curr;
		for (IsoPolygon<P> poly : getPolyList()) {
			curr = poly.outerAngleHistogramm(nBins);
			for (int i = 0; i < nBins + 1; ++i) {
				bins[i] += curr[i];
			}
		}
		return bins;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public boolean isSuccess() {
		if (fp < 0 || fn < 0 || tp < 0 || tn < 0)
			throw new RuntimeException("Method should only be called after validation has been done.");
		int threshold = 10;
		return getFN() < threshold && (getType().equals(FaceFactory.TIMED_BUFFER) || getFP() < threshold);
	}
}
