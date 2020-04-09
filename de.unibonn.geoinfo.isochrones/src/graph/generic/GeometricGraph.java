package graph.generic;

import java.awt.geom.Point2D;
import java.util.HashSet;

import util.structures.QuadTree;

public abstract class GeometricGraph<V extends Point2D, E> extends DiGraph<V, E> {

	protected QuadTree<DiGraphNode<V, E>> qt;

	public void setQuadTree(QuadTree<DiGraphNode<V, E>> qt) {
		this.qt = qt;
	}

	public QuadTree<DiGraphNode<V, E>> getQuadTree() {
		return qt;
	}

	public DiGraphNode<V, E> getDiGraphNode(double lon, double lat) {
		return qt.getNode(lon, lat);
	}

	@Override
	public DiGraphNode<V, E> addNode(V v) {
		if (qt.getNode(v.getX(), v.getY()) == null) {
			DiGraphNode<V, E> n = super.addNode(v);
			qt.add(n, v.getX(), v.getY());
			return n;
		}
		return null;
	}

	@Override
	public void removeNodes(HashSet<DiGraphNode<V, E>> nodesToBeRemoved) {
		super.removeNodes(nodesToBeRemoved);
		for (DiGraphNode<V, E> n : nodesToBeRemoved) {
			qt.removeNode(n.getNodeData().getX(), n.getNodeData().getY(), n);
		}
	}
}
