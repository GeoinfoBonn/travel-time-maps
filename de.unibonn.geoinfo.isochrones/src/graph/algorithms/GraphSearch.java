package graph.algorithms;

import java.util.ArrayList;
import java.util.LinkedList;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;

public class GraphSearch<V, E> {

	public static interface Visitor<V, E> {
		/**
		 * Is called each time when an arc is found by the search. Returns true if the
		 * arc is accepted, i.e., the target is added to the queue.
		 */
		boolean visitArc(DiGraphArc<V, E> arc);

		/**
		 * Is called when a node is removed from the queue. CHANGE: from DiGraphArc<V,E>
		 * to DiGraphNode<V,E>
		 */
		void settleNode(DiGraphNode<V, E> node);

		/**
		 * Is called each time when a neighbor is added to the queue. CHANGE: from
		 * DiGraphArc<V,E> to DiGraphNode<V,E>
		 **/
		void visitNeighbor(DiGraphNode<V, E> node);
	}

	public static interface CollectingVisitor<V, E> extends Visitor<V, E> {

		ArrayList<ArrayList<Integer>> getCollectedIds();

		void nextComponent();
	}

	/**
	 * Models the queue of the search.
	 */
	interface SearchQueue<V, E> {
		public DiGraphNode<V, E> next();

		public boolean hasNext();

		public void add(DiGraphNode<V, E> node);
	}

	public static class BFSQueue<V, E> implements SearchQueue<V, E> {
		private LinkedList<DiGraphNode<V, E>> nodes;

		public BFSQueue() {
			nodes = new LinkedList<>();
		}

		@Override
		public DiGraphNode<V, E> next() {
			return nodes.removeFirst();
		}

		@Override
		public boolean hasNext() {
			return !nodes.isEmpty();
		}

		@Override
		public void add(DiGraphNode<V, E> node) {
			nodes.add(node);
		}
	}

	static class DFSQueue<V, E> implements SearchQueue<V, E> {
		private LinkedList<DiGraphNode<V, E>> nodes;

		public DFSQueue() {
			nodes = new LinkedList<>();
		}

		@Override
		public DiGraphNode<V, E> next() {
			return nodes.removeLast();
		}

		@Override
		public boolean hasNext() {
			return !nodes.isEmpty();
		}

		@Override
		public void add(DiGraphNode<V, E> node) {
			nodes.add(node);
		}
	}

	private DiGraph<V, E> graph;
	private int visited[];
	private int timeStamp = 0;
	private int predecessors[];

	public GraphSearch(DiGraph<V, E> graph) {
		super();
		this.graph = graph;
		visited = new int[graph.n()];
	}

	public void runBFS(DiGraphNode<V, E> start, Visitor<V, E> visitor) {
		run(start, visitor, new BFSQueue<V, E>());
	}

	public void runDFS(DiGraphNode<V, E> start, Visitor<V, E> visitor) {
		run(start, visitor, new DFSQueue<V, E>());
	}

	public void run(DiGraphNode<V, E> start, Visitor<V, E> visitor, SearchQueue<V, E> queue) {
		timeStamp++;
		predecessors = new int[graph.n()];
		queue.add(start);
		visited[start.getId()] = timeStamp;
		while (queue.hasNext()) {
			DiGraphNode<V, E> node = queue.next();
			visitor.settleNode(node);
			for (DiGraphArc<V, E> arc : node.getOutgoingArcs()) {
				if (!visitor.visitArc(arc))
					continue;
				DiGraphNode<V, E> v = arc.getTarget();
				if (visited[v.getId()] < timeStamp) {
					visited[v.getId()] = timeStamp;
					predecessors[v.getId()] = node.getId();

					queue.add(v);
				}
			}
		}
	}

	public ArrayList<ArrayList<Integer>> findAllComponents(SearchQueue<V, E> queue, CollectingVisitor<V, E> visitor,
			ComponentFilter<V, E> cfilter) {
		timeStamp = 0;
		visited = new int[graph.n()];
		for (DiGraphNode<V, E> node : this.graph.getNodes()) {
			if (cfilter.startComponent(node) && visited[node.getId()] == 0) {
				visitor.nextComponent();
				run(node, visitor, queue);
			}
		}
		return visitor.getCollectedIds();
	}

	public ArrayList<ArrayList<Integer>> findAllComponents(SearchQueue<V, E> queue) {
		return findAllComponents(queue, new ComponentFilter<V, E>() {
			@Override
			public boolean startComponent(DiGraphNode<V, E> node) {
				return true;
			}
		});
	}

	public ArrayList<ArrayList<Integer>> findAllComponents(SearchQueue<V, E> queue, ComponentFilter<V, E> cfilter) {
		ArrayList<ArrayList<Integer>> listCollection = new ArrayList<>();
		timeStamp = 0;
		visited = new int[graph.n()];
		for (DiGraphNode<V, E> node : this.graph.getNodes()) {
			if (cfilter.startComponent(node) && visited[node.getId()] == 0) {
				ArrayList<Integer> list = new ArrayList<>();
				run(node, new Visitor<V, E>() {

					@Override
					public boolean visitArc(DiGraphArc<V, E> arc) {
						return true;
					}

					@Override
					public void settleNode(DiGraphNode<V, E> node) {
						list.add(node.getId());
					}

					@Override
					public void visitNeighbor(DiGraphNode<V, E> node) {
						//
					}

				}, queue);
				listCollection.add(list);
			}
		}
		return listCollection;
	}

	public interface ComponentFilter<V, E> {
		public boolean startComponent(DiGraphNode<V, E> node);
	}
}
