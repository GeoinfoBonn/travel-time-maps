package graph.routing;

import java.util.Comparator;

import graph.generic.DiGraph.DiGraphNode;
import graph.types.IsoEdge;
import graph.types.IsoVertex;

public class DijkstraComparator implements Comparator<DiGraphNode<IsoVertex, IsoEdge>> {
	private Dijkstra<IsoVertex, IsoEdge> dij;

	public DijkstraComparator(Dijkstra<IsoVertex, IsoEdge> dij) {
		this.dij = dij;
	}

	@Override
	public int compare(DiGraphNode<IsoVertex, IsoEdge> o1, DiGraphNode<IsoVertex, IsoEdge> o2) {
		return Double.compare(this.dij.getDistance(o1), this.dij.getDistance(o2));
	}

}
