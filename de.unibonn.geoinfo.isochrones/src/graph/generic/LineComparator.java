package graph.generic;

import java.io.Serializable;
import java.util.Comparator;

import graph.generic.DiGraph.DiGraphArc;

public class LineComparator<V, E> implements Comparator<DiGraphArc<V, E>>, Serializable {

	private static final long serialVersionUID = 2797619343057911807L;

	private boolean incomingLine;

	public LineComparator(boolean incomingLine) {
		this.incomingLine = incomingLine;
	}

	@Override
	public int compare(DiGraphArc<V, E> l1, DiGraphArc<V, E> l2) {
		return Double.compare(l1.getInclination(incomingLine), l2.getInclination(incomingLine));
	}
}