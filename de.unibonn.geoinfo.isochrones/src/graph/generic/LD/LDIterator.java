package graph.generic.LD;

import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.stream.Collectors;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.LinearDualCreator.LinearDualGraphIdentifier;
import graph.generic.LD.factory.LineSegment;
import graph.generic.LD.factory.TurncostFactory;
import graph.planarizer.PlanarGraph;
import graph.routing.Dijkstra.NodeIterator;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import main.AbstractMain;

public class LDIterator implements NodeIterator<Point2D, VisualizationEdge> {

	DiGraphNode<Point2D, VisualizationEdge> source;
	DiGraphNode<Point2D, VisualizationEdge> target;
	LinearDualGraphIdentifier<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> ldi;

	PlanarGraph<ColoredNode, GeofabrikData> coloredGraph;

	DiGraphNode<Point2D, VisualizationEdge> originalSource;
	DiGraphNode<Point2D, VisualizationEdge> originalTarget;

	public LDIterator(DiGraphNode<Point2D, VisualizationEdge> originalSource,
			DiGraphNode<Point2D, VisualizationEdge> originalTarget, DiGraphNode<Point2D, VisualizationEdge> source,
			DiGraphNode<Point2D, VisualizationEdge> target,
			LinearDualGraphIdentifier<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> ldi) {
		this.originalSource = originalSource;
		this.originalTarget = originalTarget;

		this.source = source;
		this.target = target;
		this.ldi = ldi;
	}

	public void setColoredGraph(PlanarGraph<ColoredNode, GeofabrikData> coloredGraph) {
		this.coloredGraph = coloredGraph;
	}

	@Override
	public Iterator<DiGraphNode<Point2D, VisualizationEdge>> getIterator(DiGraphNode<Point2D, VisualizationEdge> s) {
		LinkedList<DiGraphNode<Point2D, VisualizationEdge>> a = new LinkedList<>();
		if (s == source) {

			TreeSet<LineSegment> compareSet;
			int direction;

			DiGraphNode<ColoredNode, GeofabrikData> roadSplitNode = coloredGraph.getDiGraphNode(s.getNodeData());
			if (roadSplitNode == null)
				throw new RuntimeException("roadSplitNode is null!");

			var incOverlay = roadSplitNode.getIncomingArcs().stream()
					.filter(x -> x.getSource().getNodeData().getColor() == Colored.REACHABLE)
					.collect(Collectors.toList());
			var outOverlay = roadSplitNode.getOutgoingArcs().stream()
					.filter(x -> x.getTarget().getNodeData().getColor() == Colored.UNREACHABLE)
					.collect(Collectors.toList());

			for (DiGraphArc<Point2D, VisualizationEdge> arc : originalSource.getOutgoingArcs()) {
				compareSet = LineSegment.compareSet(null, arc, incOverlay, outOverlay);
				direction = LineSegment.exitDirection(compareSet);

//				if (direction == -1)
//					System.out.println(s.getNodeData() + " " + arc.getTarget().getNodeData() + " to left");
				if (direction == 0)
					System.err.println(s.getNodeData() + " " + arc.getTarget().getNodeData() + " invalid");
//				if (direction == 1)
//					System.out.println(s.getNodeData() + " " + arc.getTarget().getNodeData() + " to right");

				if (direction == 1)
					a.add(ldi.getLDNode(arc));
			}
		}

		if (ldi.getOriginalArc(s) != null && ldi.getOriginalArc(s).getTarget() == originalTarget) {

			TreeSet<LineSegment> compareSet;
			int direction;

			DiGraphNode<ColoredNode, GeofabrikData> roadSplitNode = coloredGraph
					.getDiGraphNode(originalTarget.getNodeData());
			if (roadSplitNode == null)
				throw new RuntimeException("roadSplitNode is null!");

			var incOverlay = roadSplitNode.getIncomingArcs().stream()
					.filter(x -> x.getSource().getNodeData().getColor() == Colored.REACHABLE)
					.collect(Collectors.toList());
			var outOverlay = roadSplitNode.getOutgoingArcs().stream()
					.filter(x -> x.getTarget().getNodeData().getColor() == Colored.UNREACHABLE)
					.collect(Collectors.toList());

			compareSet = LineSegment.compareSet(ldi.getOriginalArc(s), null, incOverlay, outOverlay);
			direction = LineSegment.arrivalDirection(compareSet);

//			if (direction == 1)
//				System.out.println(originalTarget.getNodeData() + " " + ldi.getOriginalArc(s).getSource().getNodeData()
//						+ " from right");
			if (direction == 0)
				System.err.println(originalTarget.getNodeData() + " " + ldi.getOriginalArc(s).getSource().getNodeData()
						+ " invalid");
//			if (direction == -1)
//				System.out.println(originalTarget.getNodeData() + " " + ldi.getOriginalArc(s).getSource().getNodeData()
//						+ " from left");

			if (direction == -1)
				a.add(target);
		}

		if (s != source && s != target) {
			for (DiGraphArc<Point2D, VisualizationEdge> arc : s.getOutgoingArcs()) {
				a.add(arc.getTarget());
			}
		}
		return a.iterator();
	}

	@Override
	public double getWeightOfCurrentArc(DiGraphNode<Point2D, VisualizationEdge> s,
			DiGraphNode<Point2D, VisualizationEdge> t) {
		if (s == source)
			return 0;
		if (ldi.getOriginalArc(s) != null && ldi.getOriginalArc(s).getTarget() == originalTarget) {
			double nonOctiMalus = TurncostFactory.isOcti(s.getNodeData(), t.getNodeData()) ? 1
					: AbstractMain.NON_OCTI_MALUS;
			return ldi.getOriginalArc(s).getArcData().getValue() * AbstractMain.DILATION_FACTOR * nonOctiMalus;
		}
		return s.getFirstOutgoingArcTo(t).getArcData().getValue();
	}
}
