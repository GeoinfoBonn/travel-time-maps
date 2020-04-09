package graph.routing;

import graph.generic.DiGraph.DiGraphNode;
import graph.routing.Dijkstra.NodeVisitor;
import graph.types.IsoEdge;
import graph.types.IsoVertex;

public class SplitVisitor implements NodeVisitor<DiGraphNode<IsoVertex, IsoEdge>> {
	private Dijkstra<IsoVertex, IsoEdge> dij;

	private long maxtime;

	public SplitVisitor(long maxtime, Dijkstra<IsoVertex, IsoEdge> dij) {
		this.dij = dij;
		this.maxtime = maxtime;
	}

	@Override
	public boolean visit(DiGraphNode<IsoVertex, IsoEdge> node) {
		if (this.dij.getCurrDist() > maxtime) {
			return false;
		}
		return true;
	}
}
