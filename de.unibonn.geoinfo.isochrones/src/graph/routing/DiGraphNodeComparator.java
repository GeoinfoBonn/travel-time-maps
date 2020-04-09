package graph.routing;

import java.io.Serializable;
import java.util.Comparator;

import graph.generic.DiGraph.DiGraphNode;
import graph.types.IsoEdge;
import graph.types.IsoVertex;

public class DiGraphNodeComparator implements Comparator<DiGraphNode<IsoVertex, IsoEdge>>, Serializable {

	private static final long serialVersionUID = -2262182991836860977L;

	@Override
	public int compare(DiGraphNode<IsoVertex, IsoEdge> n1, DiGraphNode<IsoVertex, IsoEdge> n2) {
		return n1.getNodeData().compareTo(n2.getNodeData());
	}

}
