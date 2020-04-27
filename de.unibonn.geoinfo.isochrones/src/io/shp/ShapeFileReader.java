package io.shp;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;
import com.vividsolutions.jump.io.DriverProperties;
import com.vividsolutions.jump.io.IllegalParametersException;
import com.vividsolutions.jump.io.ShapefileReader;

import graph.algorithms.GraphSearch;
import graph.algorithms.GraphSearch.BFSQueue;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.GeometricGraph;
import graph.generic.WeightedArcData;
import main.AbstractMain;

public class ShapeFileReader {

	public static <G extends GeometricGraph<V, E>, V extends Point2D, E extends WeightedArcData> G importFromSHP(
			File path, Factory<G, V, E> factory) throws IllegalParametersException, Exception {
		ShapefileReader shpIn = new ShapefileReader();
		DriverProperties dp = new DriverProperties(path.getAbsolutePath());
		FeatureCollection fc = shpIn.read(dp);

		G g = factory.getGraph(fc);

		Iterator<?> it = fc.iterator();
		while (it.hasNext()) {
			Feature f = (Feature) it.next();

			if (!factory.includeFeature(f))
				continue;

			Geometry geom = f.getGeometry();
			if (geom instanceof LineString) {
				processLineString((LineString) geom, f, g, factory);
			} else if (geom instanceof MultiLineString) {
				Geometry subgeom;
				for (int i = 0; i < geom.getNumGeometries(); ++i) {
					subgeom = geom.getGeometryN(i);
					if (subgeom instanceof LineString) {
						processLineString((LineString) subgeom, f, g, factory);
					} else {
						System.err.println("unable to handle geometry of type " + subgeom.getGeometryType());
						return null;
					}
				}
			} else {
				System.err.println("unable to handle geometry of type " + geom.getGeometryType());
				return null;
			}
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("RoadGraph with " + g.n() + " nodes and " + g.m() + " arcs loaded successfully");
		}
		return g;
	}

	private static <G extends GeometricGraph<V, E>, V extends Point2D, E extends WeightedArcData> void processLineString(
			LineString geom, Feature f, G g, Factory<G, V, E> factory) {
		int oneway = factory.getOneway(f);
		Coordinate[] c = geom.getCoordinates();
		DiGraphNode<V, E> q = g.getDiGraphNode(c[0].x, c[0].y);
		if (q == null) {
			q = g.addNode(factory.createNodeData(c[0].x, c[0].y));
		}
		double distance;
		for (int i = 1; i < c.length; ++i) {
			DiGraphNode<V, E> p = q;
			q = g.getDiGraphNode(c[i].x, c[i].y);
			if (q == null) {
				q = g.addNode(factory.createNodeData(c[i].x, c[i].y));
			}
			distance = c[i - 1].distance(c[i]);
			if (oneway >= 0) // both directions or just forward
				if (p.getFirstOutgoingArcTo(q) == null)
					g.addArc(p, q, factory.createArcData(distance, f));
				else if (AbstractMain.DEBUG)
					System.out.println("Arc already present, skipping. " + p.getNodeData() + " -> " + q.getNodeData());
			if (oneway <= 0) // both directions or just backwards
				if (q.getFirstOutgoingArcTo(p) == null)
					g.addArc(q, p, factory.createArcData(distance, f));
				else if (AbstractMain.DEBUG)
					System.out.println("Arc already present, skipping. " + q.getNodeData() + " -> " + p.getNodeData());
		}
	}

	public static <G extends GeometricGraph<V, E>, V extends Point2D, E extends WeightedArcData> void reduceToBiggestComponent(
			G g) {
		GraphSearch<V, E> searcher = new GraphSearch<>(g);
		BFSQueue<V, E> queue = new BFSQueue<>();

		ArrayList<ArrayList<Integer>> result = searcher.findAllComponents(queue);
		ArrayList<Integer> biggestList = new ArrayList<>();
		for (ArrayList<Integer> a : result) {
			if (a.size() > biggestList.size()) {
				biggestList = a;
			}
		}

		if (AbstractMain.VERBOSE) {
			System.out.println("Connected components found: " + result.size());
			System.out.println("Size of biggest component: " + biggestList.size());
		}

		HashSet<DiGraphNode<V, E>> nodesToBeRemoved = new HashSet<>(g.getNodes());
		for (int i : biggestList) {
			nodesToBeRemoved.remove(g.getNode(i));
		}
		g.removeNodes(nodesToBeRemoved);
		g.updateIDs();

		if (AbstractMain.VERBOSE) {
			System.out.println("Graph with " + g.n() + " nodes and " + g.m() + " arcs loaded successfully");
		}
	}

	public interface Factory<G extends DiGraph<V, E>, V, E extends WeightedArcData> {

		boolean includeFeature(Feature f);

		byte getOneway(Feature f);

		G getGraph(FeatureCollection fc);

		E createArcData(double distance, Feature f);

		V createNodeData(double x, double y);
	}
}