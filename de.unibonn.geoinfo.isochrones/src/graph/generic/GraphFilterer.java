package graph.generic;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import graph.algorithms.GraphSearch;
import graph.algorithms.GraphSearch.BFSQueue;
import graph.algorithms.GraphSearch.ComponentFilter;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.routing.Dijkstra;
import isochrone.IsochroneCreator;
import main.AbstractMain;

public class GraphFilterer {

	public static <G extends DiGraph<V, E>, V extends Point2D, E extends WeightedArcData> G filterArcs(
			final DiGraph<V, E> graph, ArcFilter<E> filter, GraphFactory<G, V, E> factory, boolean takeMin,
			boolean keepNodes) {
		G filtered = copyGraph(graph, factory);
		int n = filtered.n();

		Dijkstra<V, E> dijkstra = computeShortestPaths(filtered);

		HashSet<DiGraphArc<V, E>> arcsToBeRemoved = new HashSet<>();
		for (var arc : filtered.getArcs()) {
			if (!filter.keep(arc.getArcData())) {
				arcsToBeRemoved.add(arc);
			}
		}
		filtered.removeArcs(arcsToBeRemoved);

		restoreConnectivity(filtered, dijkstra, factory, takeMin, n);

		if (!keepNodes) {
			HashSet<DiGraphNode<V, E>> nodesToBeRemoved = new HashSet<>();
			for (var node : filtered.getNodes()) {
				if (node.getOutgoingArcs().size() == 0 && node.getIncomingArcs().size() == 0) {
					nodesToBeRemoved.add(node);
				}
			}
			filtered.removeNodes(nodesToBeRemoved);

			filtered.updateIDs();
		}

		if (AbstractMain.SHOW_RESULTS)
			IsochroneCreator.showFilteredGraph(AbstractMain.GUI, arcsToBeRemoved);

		return filtered;

	}

	public static <G extends DiGraph<V, E>, V extends Point2D, E extends WeightedArcData> G filterArcs(
			final DiGraph<V, E> graph, ArcFilter<E> filter, GraphFactory<G, V, E> factory, boolean takeMin) {
		return filterArcs(graph, filter, factory, takeMin, false);
	}

	private static <G extends DiGraph<V, E>, V extends Point2D, E extends WeightedArcData> G copyGraph(
			final DiGraph<V, E> graph, GraphFactory<G, V, E> factory) {
		G copied = factory.createGraph();

		HashMap<Point2D, DiGraphNode<V, E>> nodeMap = new HashMap<>();

		Point2D loc;
		for (var node : graph.getNodes()) {
			loc = new Point2D.Double(node.getNodeData().getX(), node.getNodeData().getY());
			if (!nodeMap.containsKey(loc))
				nodeMap.put(loc, copied.addNode(factory.createNodeData(node.getNodeData())));
		}

		for (var arc : graph.getArcs()) {
			copied.addArc(nodeMap.get(arc.getSource().getNodeData()), nodeMap.get(arc.getTarget().getNodeData()),
					factory.createEdgeData(arc.getArcData()));
		}

		return copied;
	}

	private static <G extends DiGraph<V, E>, V extends Point2D, E extends WeightedArcData> void restoreConnectivity(
			G graph, Dijkstra<V, E> dijkstra, GraphFactory<G, V, E> factory, boolean takeMin, int n) {
		if (AbstractMain.VERBOSE)
			System.out.println("Restoring connectivity...");

		List<DiGraphNode<V, E>> componentNodes = computeComponentNodes(graph, dijkstra, takeMin, n);

		List<DiGraphNode<V, E>> path;
		DiGraphArc<V, E> currentArc;
		DiGraphNode<V, E> next;
		for (var node : componentNodes) {
			path = dijkstra.getPath(node);
			for (int i = 0; i < path.size() - 1; ++i) {
				DiGraphNode<V, E> curr = path.get(i);
				next = path.get(i + 1);
				currentArc = curr.getFirstOutgoingArcTo(next);
				if (currentArc == null)
//					graph.addDoubleArc(curr, next,
//							removedArcs.stream().filter(x -> x.getSource() == curr).findFirst().get().getArcData())
//							.get(0);
					graph.addDoubleArc(curr, next,
							factory.createEdgeData(curr.getNodeData().distance(next.getNodeData())));
			}
		}

		if (AbstractMain.VERBOSE)
			System.out.println("Connectivity restored.");
	}

	private static <V, E extends WeightedArcData> List<DiGraphNode<V, E>> computeComponentNodes(DiGraph<V, E> graph,
			Dijkstra<V, E> dijkstra, boolean takeMin, int n) {
		if (AbstractMain.VERBOSE)
			System.out.println("Computing component nodes...");

		GraphSearch<V, E> searcher = new GraphSearch<>(graph);
		BFSQueue<V, E> queue = new BFSQueue<>();

		ArrayList<ArrayList<Integer>> components = searcher.findAllComponents(queue, new ComponentFilter<V, E>() {
			@Override
			public boolean startComponent(DiGraphNode<V, E> node) {
				if (node.getIncomingArcs().size() == 0 && node.getOutgoingArcs().size() == 0)
					return false;
				return true;
			}
		});
		if (AbstractMain.VERBOSE)
			System.out.println(components.size() + " components found.");

		List<DiGraphNode<V, E>> componentNodes = new ArrayList<>();

		double currMinDist, currDist;
		DiGraphNode<V, E> currMin = null, currNode;
		for (ArrayList<Integer> component : components) {
			if (component.size() == 1)
				continue;

			currMinDist = Double.MAX_VALUE;

			if (takeMin)
				for (int i : component) {
					currNode = graph.getNode(i);
					currDist = dijkstra.getDistance(currNode);
					if (currDist < currMinDist) {
						currMin = currNode;
						currMinDist = currDist;
					}
				}
			else
				currMin = graph.getNode(component.get(0));

			if (currMin != null)
				componentNodes.add(currMin);
			else
				System.err.println("No min node found.");
		}

		if (AbstractMain.VERBOSE)
			System.out.println("Component nodes computed. There are " + componentNodes.size() + " components.");

		return componentNodes;
	}

	private static <V, E extends WeightedArcData> Dijkstra<V, E> computeShortestPaths(DiGraph<V, E> graph) {
		Dijkstra<V, E> dijkstra = new Dijkstra<>(graph);
		dijkstra.run(graph.getNode(0));
		return dijkstra;
	}

	public interface ArcFilter<E> {
		boolean keep(E edgeData);
	}

	public static <E extends WeightedArcData> ArcFilter<E> joinedArcFilter(ArcFilter<E>[] filters) {
		return new ArcFilter<E>() {

			@Override
			public boolean keep(E edgeData) {
				for (var filter : filters)
					if (!filter.keep(edgeData))
						return false;
				return true;
			}
		};
	}

	public interface GraphFactory<G, V, E> {
		G createGraph();

		V createNodeData(V in);

		E createEdgeData(E in);

		E createEdgeData(double weight);
	}
}
