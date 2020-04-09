package graph.planarier.union;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.GeometricGraph;
import graph.generic.LineComparator;
import graph.planarizer.PlanarGraph;
import graph.planarizer.Planarizer;
import main.AbstractMain;
import util.geometry.Envelope;
import util.tools.Util.PointComparator;

public class UnionPlanarizer<V extends Point2D, E> extends Planarizer<V, E> {

	private boolean createMapping = false;
 
	private GeometryFactory gf = new GeometryFactory();

	public UnionPlanarizer(PlanarizerFactory<V, E> factory) {
		this(factory, false);
	}

	public UnionPlanarizer(PlanarizerFactory<V, E> factory, boolean createMapping) {
		this.factory = factory;
		this.createMapping = createMapping;
	}

	@Override
	public void planarize() {
		// remove arcs in 3rd and 4th quadrant if they have a twin
		GeometryCollection lineSegments = preprocessArcs();

		// calculate union
		Geometry union = lineSegments.union();
		GeometryCollection collection;
		if (union instanceof GeometryCollection)
			collection = (GeometryCollection) union;
		else if (union instanceof LineString)
			collection = new GeometryCollection(new Geometry[] { union }, gf);
		else
			throw new RuntimeException("Unknown geometry type of union: " + union.getClass().getName() + "!");

		// calculate graph
		unionToPlanarGraph(collection);
	}

	private GeometryCollection preprocessArcs() {

		List<Geometry> geometries = new LinkedList<>();
		Coordinate source, target;

		for (DiGraphArc<V, E> a : inputGraph.getArcs()) {
			source = new Coordinate(a.getSource().getNodeData().getX(), a.getSource().getNodeData().getY());
			target = new Coordinate(a.getTarget().getNodeData().getX(), a.getTarget().getNodeData().getY());

			// if an arc has a twin, only keep one of the arcs
			if (a.getTwin() != null) {
				// don't consider arcs with x_source > x_target
				if (source.x > target.x)
					continue;
				// in case that arc is along x axis...
				else if (source.x == target.x)
					// ... don't consider arcs with y_source > y_target
					if (source.y > target.y)
						continue;
			}

			// transform all remaining arcs into JTS LineStrings
			Coordinate[] pts = { source, target };
			geometries.add(gf.createLineString(pts));
		}

		return new GeometryCollection(geometries.toArray(new Geometry[0]), gf);
	}

	private void unionToPlanarGraph(GeometryCollection union) {
		Objects.requireNonNull(union);

		this.planarGraph = new PlanarGraph<>(jts2geometryEnvelope(union.getEnvelopeInternal()));
		this.crossPoints = new HashMap<>();
		this.crossedLines = new HashMap<>();

		TreeSet<V> sortedPoints = new TreeSet<>(new PointComparator());
		LinkedList<Coordinate[]> arcList = new LinkedList<>();

		for (int i = 0; i < union.getNumGeometries(); i++) {
			Geometry geo = union.getGeometryN(i);
			if (geo instanceof LineString) {
				Coordinate[] coord = geo.getCoordinates();
				assert coord.length == 2;
				arcList.add(coord);
				for (Coordinate c : coord) {
					V p = factory.createNodeData(c.x, c.y);
					if (!sortedPoints.contains(p)) {
						sortedPoints.add(p);
					}
				}
			}
		}

		Map<V, DiGraphNode<V, E>> nodeMap = new TreeMap<>(new PointComparator());
		for (V p : sortedPoints) {
			DiGraphNode<V, E> node = planarGraph.addNode(p);
			if (node == null)
				node = planarGraph.getDiGraphNode(p);
			nodeMap.put(p, node);
			if (inputGraph.getDiGraphNode(p.getX(), p.getY()) == null) {
				crossPoints.put(p, new LinkedList<>());
			}
		}
		for (Coordinate[] a : arcList) {
			for (int i = 0; i < a.length - 1; i++) {
				Point2D source = new Point2D.Double(a[i].x, a[i].y);
				Point2D target = new Point2D.Double(a[i + 1].x, a[i + 1].y);
				this.planarGraph.addDoubleArc(nodeMap.get(source), nodeMap.get(target),
						factory.createEdgeData(source.distance(target)));
			}
		}
		this.planarGraph.sort(new LineComparator<>(false), new LineComparator<>(true));

		if (createMapping) {
			long start = 0;
			if (AbstractMain.VERBOSE) {
				System.out.println("Starting to map from crosspoints to input arcs...");
				start = System.currentTimeMillis();
			}
			this.mapCrosspointsArcs(inputGraph, crossPoints, crossedLines);
			if (AbstractMain.VERBOSE)
				System.out
						.println("Finished mapping in " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");

			this.planarGraph.setCrosspoints(crossPoints);
			this.planarGraph.setCrossedLines(crossedLines);
		}
	}

	public void mapCrosspointsArcs(GeometricGraph<V, E> inputGraph, Map<Point2D, List<DiGraphArc<V, E>>> crossPoints,
			Map<DiGraphArc<V, E>, List<Point2D>> crossedLines) {

		Set<Integer> visitedIds = new HashSet<>();
		for (DiGraphArc<V, E> arc : inputGraph.getArcs()) {
			if (visitedIds.contains(arc.getId()))
				continue;
//			for (Point2D crossing : crossPoints.keySet()) {
//				if (pointIsOnArc(crossing, arc)) {
//					crossPoints.get(crossing).add(arc);
//					if (!crossedLines.containsKey(arc)) {
//						crossedLines.put(arc, new LinkedList<>());
//					}
//					crossedLines.get(arc).add(crossing);
//				}
//			}

			for (Entry<Point2D, List<DiGraphArc<V, E>>> crossing : crossPoints.entrySet()) {
				if (pointIsOnArc(crossing.getKey(), arc)) {
					crossing.getValue().add(arc);
					if (!crossedLines.containsKey(arc)) {
						crossedLines.put(arc, new LinkedList<>());
					}
					crossedLines.get(arc).add(crossing.getKey());
				}
			}
			visitedIds.add(arc.getId());
			if (arc.getTwin() != null)
				visitedIds.add(arc.getTwin().getId());
		}
	}

	private boolean pointIsOnArc(Point2D v, DiGraphArc<V, E> arc) {
		double eps = 2e-3;
		Point2D s = arc.getSource().getNodeData();
		Point2D t = arc.getTarget().getNodeData();
		if ((s.getX() <= v.getX()) && (v.getX() <= t.getX()) || (s.getX() >= v.getX()) && (v.getX() >= t.getX())) {
			if ((s.getY() <= v.getY()) && (v.getY() <= t.getY()) || (s.getY() >= v.getY()) && (v.getY() >= t.getY())) {
				if (Math.abs(s.getY() + ((v.getX() - s.getX()) / (t.getX() - s.getX())) * (t.getY() - s.getY())
						- v.getY()) < eps) {
					return true;
				}
			}
		}
		return false;
	}

	private Envelope jts2geometryEnvelope(com.vividsolutions.jts.geom.Envelope env) {
		return new Envelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY());
	}
}
