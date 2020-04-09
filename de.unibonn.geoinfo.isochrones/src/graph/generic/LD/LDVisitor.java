package graph.generic.LD;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import graph.generic.DiGraph.DiGraphNode;
import graph.routing.Dijkstra.NodeVisitor;
import graph.types.VisualizationEdge;

public class LDVisitor implements NodeVisitor<DiGraphNode<Point2D, VisualizationEdge>> {

	private DiGraphNode<Point2D, VisualizationEdge> target;

	List<DiGraphNode<Point2D, VisualizationEdge>> visitedNodes;

	public LDVisitor(DiGraphNode<Point2D, VisualizationEdge> target) {
		this.target = target;
		this.visitedNodes = new LinkedList<>();
	}

	@Override
	public boolean visit(DiGraphNode<Point2D, VisualizationEdge> node) {
		visitedNodes.add(node);
		return node != target;
	}

	public List<DiGraphNode<Point2D, VisualizationEdge>> getVisitedNodes() {
		return visitedNodes;
	}

}
