package graph.generic.LD.factory;

import java.awt.geom.Point2D;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;

public class DistanceFactory extends TurncostFactory {

	private PlanarGraph<ColoredNode, GeofabrikData> coloredGraph;
	private Set<DiGraphNode<Point2D, VisualizationEdge>> splitnodes;

	public DistanceFactory(double offsetFactor, PlanarGraph<ColoredNode, GeofabrikData> coloredGraph,
			Set<DiGraphNode<Point2D, VisualizationEdge>> splitnodes) {
		super(offsetFactor);
		this.coloredGraph = coloredGraph;
		this.splitnodes = splitnodes;
	}

	@Override
	public VisualizationEdge createEdgeData(DiGraphArc<Point2D, VisualizationEdge> incomingArc,
			DiGraphArc<Point2D, VisualizationEdge> outgoingArc, boolean connectsTwinNodes) {

		if (incomingArc.getSource().getNodeData().distance(outgoingArc.getTarget().getNodeData()) < 1e-6) {
//			System.out.println("UTurn " + incomingArc + " -> " + outgoingArc);
			return null;
		}

		if (incomingArc.getSource() == incomingArc.getTarget() || outgoingArc.getSource() == outgoingArc.getTarget())
			return null;

		if (!splitnodes.contains(incomingArc.getTarget()))
			return new VisualizationEdge(incomingArc.getArcData().getValue(), false, (byte) 0);

		DiGraphNode<Point2D, VisualizationEdge> splitNode = incomingArc.getTarget();

		if (splitNode.getNodeData().distance(new Point2D.Double(365310.62, 5623191.14)) < 1)
			System.out.println();

		DiGraphNode<ColoredNode, GeofabrikData> roadSplitNode = coloredGraph.getDiGraphNode(splitNode.getNodeData());
		if (roadSplitNode == null)
			return new VisualizationEdge(incomingArc.getArcData().getValue(), false, (byte) 0);

		var incOverlay = roadSplitNode.getIncomingArcs().stream()
				.filter(x -> x.getSource().getNodeData().getColor() == Colored.REACHABLE).collect(Collectors.toList());
		var outOverlay = roadSplitNode.getOutgoingArcs().stream()
				.filter(x -> x.getTarget().getNodeData().getColor() == Colored.UNREACHABLE)
				.collect(Collectors.toList());

		TreeSet<LineSegment> compareSet = LineSegment.compareSet(incomingArc, outgoingArc, incOverlay, outOverlay);

		int direction = LineSegment.crossingDirection(compareSet);

//		if (direction == -1)
//			System.out.println("from left");
//		if (direction == 0)
//			System.out.println("no cross");
//		if (direction == 1)
//			System.out.println("from right");

		if (direction != -1) {
			return null;
		}

		return new VisualizationEdge(incomingArc.getArcData().getValue(), false, (byte) 0);
	}
}
