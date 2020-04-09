package graph.planarizer;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

import graph.generic.GeometricGraph;
import util.geometry.Envelope;
import util.structures.QuadTree;

public class PlanarGraph<V extends Point2D, E> extends GeometricGraph<V, E> {

	private Map<Point2D, List<DiGraphArc<V, E>>> crossPoints;
	private Map<DiGraphArc<V, E>, List<Point2D>> crossedLines;
	private Envelope envelope;

	public PlanarGraph(Envelope e) {
		this.qt = new QuadTree<DiGraphNode<V, E>>(e);
		this.envelope = e;
	}

	public Map<Point2D, List<DiGraphArc<V, E>>> getCrossPoints() {
		return crossPoints;
	}

	public void setCrosspoints(Map<Point2D, List<DiGraphArc<V, E>>> crossPoints) {
		this.crossPoints = crossPoints;
	}

	public Map<DiGraphArc<V, E>, List<Point2D>> getCrossedLines() {
		return crossedLines;
	}

	public void setCrossedLines(Map<DiGraphArc<V, E>, List<Point2D>> crossedLines) {
		this.crossedLines = crossedLines;
	}

	public Envelope getEnvelope() {
		return envelope;
	}

	public DiGraphNode<V, E> getDiGraphNode(Point2D position) {
		return getDiGraphNode(position.getX(), position.getY());
	}
}
