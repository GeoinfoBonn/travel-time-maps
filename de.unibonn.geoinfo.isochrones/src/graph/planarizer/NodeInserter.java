package graph.planarizer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gisviewer.ListLayer;
import gisviewer.MapObject;
import gisviewer.PointMapObject;
import graph.algorithms.GraphSearch;
import graph.algorithms.GraphSearch.Visitor;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LineComparator;
import graph.generic.WeightedArcData;
import graph.routing.Dijkstra;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.WalkingData;
import isochrone.IsochroneCreator;
import main.AbstractMain;
import util.tools.Util;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public class NodeInserter<V extends Point2D, E extends WalkingData> {

	private final PlanarGraph<V, E> originalGraph;
	private PlanarGraph<ColoredNode, E> insertedGraph;

	private ArcDataSplitter<E> ads;

	public NodeInserter(final PlanarGraph<V, E> planarGraph) {
		this.originalGraph = planarGraph;
		copyGraph();
	}

	public void colorGraph(DiGraph<ColoredNode, ?> coloredGraph) {

		DiGraphNode<ColoredNode, E> planarNode;
		for (DiGraphNode<ColoredNode, ?> node : coloredGraph.getNodes()) {
			planarNode = insertedGraph.getDiGraphNode(node.getNodeData().x, node.getNodeData().y);

			if (planarNode != null)
				planarNode.getNodeData().setReachability(node.getNodeData());
		}

		colorCrosspoints();
	}

	private void colorCrosspoints() {
		ColoredNode planarData;
		DiGraphNode<ColoredNode, E> planarNode;
		ColoredNode sourceData;
		ColoredNode targetData;
		int color;
		double remTime;
		for (Point2D crossPoint : originalGraph.getCrossPoints().keySet()) {
			planarNode = insertedGraph.getDiGraphNode(crossPoint.getX(), crossPoint.getY());
			planarData = planarNode.getNodeData();
			color = Colored.UNREACHABLE;
			remTime = -1;

			for (DiGraphArc<V, E> arc : originalGraph.getCrossPoints().get(crossPoint)) {
				sourceData = insertedGraph
						.getDiGraphNode(arc.getSource().getNodeData().getX(), arc.getSource().getNodeData().getY())
						.getNodeData();
				targetData = insertedGraph
						.getDiGraphNode(arc.getTarget().getNodeData().getX(), arc.getTarget().getNodeData().getY())
						.getNodeData();

				if (Colored.edgeColor(sourceData.getColor(), targetData.getColor()) == Colored.REACHABLE) {
					color = Colored.REACHABLE;

					double fraction = crossPoint.distance(sourceData) / sourceData.distance(targetData);
					if (sourceData.getRemainingTime() > targetData.getRemainingTime()) {
						remTime = (sourceData.getRemainingTime() - targetData.getRemainingTime()) * fraction;
					} else {
						remTime = (targetData.getRemainingTime() - sourceData.getRemainingTime()) * (1 - fraction);
					}
					break;
				}
			}

			planarData.setReachability(color, remTime);
		}

	}

	private void copyGraph() {
		this.insertedGraph = new PlanarGraph<>(originalGraph.getEnvelope());

		// initialize maps to store relation between graphs
		Map<DiGraphNode<V, E>, DiGraphNode<ColoredNode, E>> original2inserted = new HashMap<>();

		DiGraphNode<ColoredNode, E> insertedNode;
		for (DiGraphNode<V, E> node : originalGraph.getNodes()) {
			insertedNode = insertedGraph.addNode(new ColoredNode(node.getNodeData()));
			original2inserted.put(node, insertedNode);
		}

		DiGraphNode<ColoredNode, E> insertedSource, insertedTarget;
		for (DiGraphArc<V, E> arc : originalGraph.getArcs()) {
			insertedSource = original2inserted.get(arc.getSource());
			insertedTarget = original2inserted.get(arc.getTarget());
			insertedGraph.addArc(insertedSource, insertedTarget, arc.getArcData());
		}
	}

	public List<DiGraphNode<ColoredNode, E>> insertSplitNodes() {
		HashSet<DiGraphArc<ColoredNode, E>> removedArcs = new HashSet<>();
		List<DiGraphNode<ColoredNode, E>> newSplitNodes = new LinkedList<>();

		int m = insertedGraph.m();
		HashSet<Integer> visitedArcs = new HashSet<>();
		DiGraphArc<ColoredNode, E> currentArc;
		DiGraphNode<ColoredNode, E> sourceNode, targetNode;
		ColoredNode sourceData, targetData;
		for (int arcId = 0; arcId < m; ++arcId) {
			if (visitedArcs.contains(arcId))
				continue;

			currentArc = insertedGraph.getArc(arcId);

			sourceNode = currentArc.getSource();
			targetNode = currentArc.getTarget();
			sourceData = sourceNode.getNodeData();
			targetData = targetNode.getNodeData();

			Point2D compare = new Point2D.Double(357262.365, 5625325.79);

			visitedArcs.add(arcId);
			if (currentArc.getTwin() != null)
				visitedArcs.add(currentArc.getTwin().getId());

			if (!(sourceData.getColor() == Colored.REACHABLE || targetData.getColor() == Colored.REACHABLE))
				continue;
			if (sourceData.getColor() == Colored.REACHABLE && originalGraph.getCrossPoints().containsKey(sourceData))
				continue;
			if (targetData.getColor() == Colored.REACHABLE && originalGraph.getCrossPoints().containsKey(targetData))
				continue;

			if (sourceData.distance(compare) < 1 || targetData.distance(compare) < 1)
				System.out.println();

			if (currentArc.getArcData().getValueAsDist() > sourceData.getRemainingDist()
					+ targetData.getRemainingDist()) {
				// case 1: standard case, either source or target are unreachable
				if (sourceData.getColor() == Colored.UNREACHABLE || targetData.getColor() == Colored.UNREACHABLE) {
					// case 1a: source is reachable
					if (sourceData.getColor() == Colored.REACHABLE)
						insertNodeOnArc(sourceNode, targetNode, sourceData.getRemainingDist(), removedArcs,
								newSplitNodes);
					// case 1b: target is reachable
					else // if (targetData.getColor() == Colored.REACHABLE)
						insertNodeOnArc(targetNode, sourceNode, targetData.getRemainingDist(), removedArcs,
								newSplitNodes);
				}
				// case 2: both source and target are reachable
				else {
					DiGraphNode<ColoredNode, E> newSource = insertNodeOnArc(sourceNode, targetNode,
							sourceData.getRemainingDist(), removedArcs, newSplitNodes);
					DiGraphNode<ColoredNode, E> newTarget = insertNodeOnArc(targetNode, newSource,
							targetData.getRemainingDist(), removedArcs, newSplitNodes);
					insertDummyOnArc(newSource, newTarget, removedArcs);
				}
			}
		}

		insertedGraph.removeArcs(removedArcs);
		insertedGraph.updateIDs();
		insertedGraph.sort(new LineComparator<>(false), new LineComparator<>(true));

		return newSplitNodes;
	}

	public PlanarGraph<V, E> getOriginalGraph() {
		return originalGraph;
	}

	public PlanarGraph<ColoredNode, E> getResultGraph() {
		return insertedGraph;
	}

	private DiGraphNode<ColoredNode, E> insertDummyOnArc(DiGraphNode<ColoredNode, E> source,
			DiGraphNode<ColoredNode, E> target, HashSet<DiGraphArc<ColoredNode, E>> removedArcs) {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);

		// skip node insertion at the border of the experimental region
		if (source.getNodeData().isFixed() || target.getNodeData().isFixed())
			return null;

		double fraction = .5;

		Point2D location = locationAlongLine(source.getNodeData(), target.getNodeData(), fraction);

		DiGraphArc<ColoredNode, E> planarArc = source.getFirstOutgoingArcTo(target);

		// add node
		DiGraphNode<ColoredNode, E> newNode = insertedGraph.addNode(new ColoredNode(location, Colored.UNREACHABLE, -1));
		if (newNode == null) {
			System.err.println("Already there???");
			newNode = insertedGraph.getDiGraphNode(location);
			newNode.getNodeData().setReachability(Colored.UNREACHABLE, -1);
//			System.out.println("split point is corner point: " + location);
			return newNode;
		}

		removedArcs.add(planarArc);
		if (planarArc.getTwin() != null)
			removedArcs.add(planarArc.getTwin());

		// add arcs to node
		insertedGraph.addDoubleArc(source, newNode, ads.splitArcData(planarArc.getArcData(), fraction));
		insertedGraph.addDoubleArc(newNode, target, ads.splitArcData(planarArc.getArcData(), 1 - fraction));
		return newNode;
	}

	private DiGraphNode<ColoredNode, E> insertNodeOnArc(DiGraphNode<ColoredNode, E> planarSource,
			DiGraphNode<ColoredNode, E> planarTarget, double remDistSource,
			HashSet<DiGraphArc<ColoredNode, E>> removedArcs, List<DiGraphNode<ColoredNode, E>> newSplitNodes) {
		Objects.requireNonNull(planarSource);
		Objects.requireNonNull(planarTarget);

		// skip node insertion at the border of the experimental region
		if (planarSource.getNodeData().isFixed() || planarTarget.getNodeData().isFixed())
			return null;

		double fraction = remDistSource / planarSource.getNodeData().distance(planarTarget.getNodeData());
		if (fraction < 0)
			throw new IllegalArgumentException("Illegal fraction! " + fraction);

		Point2D location = locationAlongLine(planarSource.getNodeData(), planarTarget.getNodeData(), fraction);
		DiGraphNode<ColoredNode, E> oldPlanarSource = planarSource;

		DiGraphArc<ColoredNode, E> planarArc = planarSource.getFirstOutgoingArcTo(planarTarget);

		if (fraction > 1) { // arc has been split up by the planarizer, search correct arc
			GraphSearch<ColoredNode, E> search = new GraphSearch<>(insertedGraph);
			ArcSearchVisitor af = new ArcSearchVisitor(location);
			search.runBFS(planarSource, af);

			planarArc = af.getArc();

			if (planarArc == null || planarArc.getSource().getNodeData().isFixed()
					|| planarArc.getTarget().getNodeData().isFixed())
				return null;

			planarSource = planarArc.getSource();
			planarTarget = planarArc.getTarget();

			fraction = planarSource.getNodeData().distance(location)
					/ planarSource.getNodeData().distance(planarTarget.getNodeData());
		}

		// add node
		DiGraphNode<ColoredNode, E> newNode = insertedGraph.addNode(new ColoredNode(location, Colored.REACHABLE, 0));
		if (newNode == null) {
			if (fraction > 1e-10)
				System.err.println("node already present, skipping: " + location);
			newNode = insertedGraph.getDiGraphNode(location);
			newNode.getNodeData().setReachability(Colored.REACHABLE, 0);
			newSplitNodes.add(newNode);
			return newNode;
		}

		removedArcs.add(planarArc);
		if (planarArc.getTwin() != null)
			removedArcs.add(planarArc.getTwin());

		newSplitNodes.add(newNode);

		// add arcs to node
		insertedGraph.addDoubleArc(planarSource, newNode, ads.splitArcData(planarArc.getArcData(), fraction));
		insertedGraph.addDoubleArc(newNode, planarTarget, ads.splitArcData(planarArc.getArcData(), 1 - fraction));

		Dijkstra<ColoredNode, E> dij = new Dijkstra<>(insertedGraph);
		dij.run(oldPlanarSource, newNode);
		List<DiGraphNode<ColoredNode, E>> path = dij.getPath(newNode);
		for (DiGraphNode<ColoredNode, E> node : path) {
			node.getNodeData().setReachability(Colored.REACHABLE, oldPlanarSource.getNodeData().getRemainingTime()
					- dij.getDistance(node) / WalkingData.WALKING_SPEED);
		}
		return newNode;
	}

	private Point2D locationAlongLine(Point2D p1, Point2D p2, double fraction) {
		double x = p1.getX() + fraction * (p2.getX() - p1.getX());
		double y = p1.getY() + fraction * (p2.getY() - p1.getY());
		return new Point2D.Double(x, y);
	}

	public class ArcSearchVisitor implements Visitor<ColoredNode, E> {

		private final static double EPS = 2e-3;

		private DiGraphArc<ColoredNode, E> arc;
		private Point2D searchPoint;

		public ArcSearchVisitor(Point2D searchPoint) {
			this.searchPoint = searchPoint;
		}

		@Override
		public boolean visitArc(DiGraphArc<ColoredNode, E> arc) {
			if (pointIsOnArc(arc)) {
				this.arc = arc;
			}

			if (this.arc == null)
				return true;
			return false;
		}

		@Override
		public void settleNode(DiGraphNode<ColoredNode, E> node) {

		}

		@Override
		public void visitNeighbor(DiGraphNode<ColoredNode, E> node) {

		}

		public DiGraphArc<ColoredNode, E> getArc() {
			return arc;
		}

		private boolean pointIsOnArc(DiGraphArc<ColoredNode, E> arc) {
			return Util.pointIsOnLine(searchPoint, arc.getSource().getNodeData(), arc.getTarget().getNodeData(), EPS);
		}
	}

	public void setArcDataSplitter(ArcDataSplitter<E> ads) {
		this.ads = ads;
	}

	public static interface ArcDataSplitter<E extends WeightedArcData> {

		E splitArcData(E data, double fraction);
	}

	public static final ArcDataSplitter<GeofabrikData> GEOFABRIK_SPLITTER = new ArcDataSplitter<>() {

		@Override
		public GeofabrikData splitArcData(GeofabrikData data, double fraction) {
			return new GeofabrikData(data.getValue() * fraction);
		}
	};

	public IsochronePanel showColoredNodes(ResultFrame frame, String title) {
		IsochronePanel panel = IsochroneCreator.showColoredArcs(frame, title, insertedGraph);

		ListLayer reachableLayer = new ListLayer(AbstractMain.COLOR_STYLE.reachable());
		ListLayer unreachableLayer = new ListLayer(AbstractMain.COLOR_STYLE.unreachable());
		ListLayer undefindedLayer = new ListLayer(Color.BLACK);

		MapObject mo;
		for (DiGraphNode<ColoredNode, E> node : insertedGraph.getNodes()) {
			mo = new PointMapObject(node.getNodeData());
			if (node.getNodeData().getColor() == Colored.REACHABLE)
				reachableLayer.add(mo);
			else if (node.getNodeData().getColor() == Colored.UNREACHABLE)
				unreachableLayer.add(mo);
			else
				undefindedLayer.add(mo);
		}

		panel.getMap().addLayer(unreachableLayer, 15);
		panel.getMap().addLayer(undefindedLayer, 20);
		panel.getMap().addLayer(reachableLayer, 25);

		return panel;
	}
}
