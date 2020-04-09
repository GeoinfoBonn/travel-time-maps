package isochrone;

import java.util.LinkedList;
import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.types.Colored;
import graph.types.ColoredNode;
import main.AbstractMain;
import tools.Stopwatch;

public class SplitNodeFinder {

	public static <V extends ColoredNode, E> List<DiGraphNode<V, E>> findSplitNodes(DiGraph<V, E> g, Stopwatch sw,
			boolean withBuffer) {
		long time = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Finding splitnodes...");

		List<DiGraphNode<V, E>> splitNodes = new LinkedList<>();

		for (DiGraphNode<V, E> n : g.getNodes()) {
			if (n.getNodeData().getColor() == Colored.UNREACHABLE) {
				continue;
			}

			if (!withBuffer && n.getNodeData().getColor() == Colored.BUFFER) {
				System.err.println("ERROR!!!");
//				n.getNodeData().setReachability(Colored.REACHABLE, n.getNodeData().getRemainingTime());
				continue;
			}

			for (DiGraphArc<V, E> arc : n.getOutgoingArcs()) {
				if (arc.getTarget().getNodeData().getColor() == Colored.UNREACHABLE) {
					assert n.getNodeData().getColor() == Colored.REACHABLE
							|| (withBuffer && n.getNodeData().getColor() == Colored.BUFFER);
					splitNodes.add(n);
					break;
				}
			}
		}

		time = System.currentTimeMillis() - time;
		sw.add("findSplitnodes", time);
		if (AbstractMain.VERBOSE)
			System.out.println("Splitnodes found. (" + time / 1000.0 + "s)");

		return splitNodes;
	}

	public static <V extends ColoredNode, E> List<DiGraphNode<V, E>> findSplitNodes(DiGraph<V, E> g, Stopwatch sw) {
		return findSplitNodes(g, sw, false);
	}
}
