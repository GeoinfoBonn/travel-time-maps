package graph.types;

import java.awt.geom.Point2D;

import graph.generic.GeometricGraph;
import graph.generic.WeightedArcData;
import util.geometry.Envelope;
import util.structures.QuadTree;

public class RoadGraph<V extends Point2D, E extends WeightedArcData> extends GeometricGraph<V, E> {

	public RoadGraph(Envelope e) {
		this.qt = new QuadTree<DiGraphNode<V, E>>(e);
	}

	public DiGraphNode<V, E> getDiGraphNode(Point2D position, double eps) {
		return qt.getNode(position.getX(), position.getY(), eps);
	}
}
