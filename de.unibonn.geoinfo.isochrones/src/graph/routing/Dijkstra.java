package graph.routing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.WeightedArcData;
import graph.types.ArrivalNode;
import util.structures.MinHeap;
import util.structures.MinHeap.HeapItem;

/**
 * Simple implementation of the Dijkstra algorithm. The data structures are
 * initialized. Due to some internal time stamp mechanism, the class supports
 * multiple executions without the need to reinitialize the data structures.
 */
public class Dijkstra<V, E extends WeightedArcData> {

//	private static Dijkstra<IsoVertex, IsoEdge> mInstance;
	private double starttime = 0;
	protected double dist[];
	protected double curr_dist = 0;
	protected int stamps[];
	protected HeapItem<DiGraphNode<V, E>> items[];
	public DiGraphNode<V, E> pred[];

	protected int currentStamp = 0;

//	static int i = 0;

	@SuppressWarnings("unchecked")
	public Dijkstra(DiGraph<V, E> g) {
		this.dist = new double[g.n()];
		this.stamps = new int[g.n()];
		this.items = new HeapItem[g.n()];
		this.pred = new DiGraphNode[g.n()];
//		mInstance = (Dijkstra<IsoVertex, IsoEdge>) this;
	}

	@SuppressWarnings("unchecked")
	public Dijkstra(DiGraph<V, E> g, double startTime) {
		this.dist = new double[g.n()];
		this.stamps = new int[g.n()];
		this.items = new HeapItem[g.n()];
		this.pred = new DiGraphNode[g.n()];
		this.starttime = startTime;
//		mInstance = (Dijkstra<IsoVertex, IsoEdge>) this;
	}

//	public static Dijkstra<IsoVertex, IsoEdge> getInstance() {
//		return mInstance;
//	}

	public double getCurrDist() {
		return curr_dist;
	}

	public DiGraphNode<V, E> getPred(int nodeId) {
		return pred[nodeId];
	}

	/**
	 * Runs the algorithm starting at the source node. When the target is reached,
	 * the search is aborted and the distance to the target is returned. In case
	 * that the target is not reachable from the source, the method returns
	 * Double.MAX_VALUE.
	 * 
	 * @throws OutOfStreetNetworkException
	 */
	public double run(DiGraphNode<V, E> source, DiGraphNode<V, E> target) {
		run(source, new NodeVisitor<DiGraphNode<V, E>>() {
			@Override
			public boolean visit(DiGraphNode<V, E> node) {
				return node != target;
			}
		});
		if (stamps[target.getId()] == currentStamp) {
			return dist[target.getId()];
		}
		return Double.MAX_VALUE;
	}

	/**
	 * Runs the algorithm starting at the source node. Runs until each reachable
	 * node is visited.
	 * 
	 * @throws OutOfStreetNetworkException
	 */
	public void run(DiGraphNode<V, E> source) {

		run(source, new NodeVisitor<DiGraphNode<V, E>>() {
			@Override
			public boolean visit(DiGraphNode<V, E> node) {
				return true;
			}
		});
	}

	/**
	 * Runs the algorithm starting at the source node using the given NodeVisitor.
	 * Runs as long as the NodeVisitor's method visit returns true.
	 * 
	 * @throws OutOfStreetNetworkException
	 */
	public boolean run(DiGraphNode<V, E> source, NodeVisitor<DiGraphNode<V, E>> visitor) {
		return run(source, visitor, DEFAULT_ADJACENT_NODE_ITERATOR);
	}

	/**
	 * Runs the algorithm starting at the source node using the given NodeVisitor.
	 * Runs as long as the NodeVisitor's method visit returns true. Ignores the
	 * graph structure; adjacencies depend on the NodeIterator.
	 * 
	 * @throws OutOfStreetNetworkException
	 */
	public boolean run(DiGraphNode<V, E> source, NodeVisitor<DiGraphNode<V, E>> visitor, NodeIterator<V, E> nit) {
		currentStamp++;
		dist[source.getId()] = starttime;
		pred[source.getId()] = null;
		double weightOfArc;

		MinHeap<DiGraphNode<V, E>> queue = new MinHeap<DiGraphNode<V, E>>();

		items[source.getId()] = queue.insertItem(starttime, source);
		stamps[source.getId()] = currentStamp;

		while (queue.size() > 0) {
			HeapItem<DiGraphNode<V, E>> item = queue.extractMin();
			DiGraphNode<V, E> u = item.getValue();
			curr_dist = item.getKey();

			if (u.getNodeData() instanceof ArrivalNode)
				System.out.println(u.getNodeData());

			if (!visitor.visit(u)) {
				return false;
			}

			for (Iterator<DiGraphNode<V, E>> it = nit.getIterator(u); it.hasNext();) {
				DiGraphNode<V, E> v = it.next();

				weightOfArc = nit.getWeightOfCurrentArc(u, v);
				discoverNode(u, v, queue, dist[u.getId()] + weightOfArc);
			}
		}
		return true;
	}

	private void discoverNode(DiGraphNode<V, E> curr, DiGraphNode<V, E> target, MinHeap<DiGraphNode<V, E>> queue,
			double alt) {
		if (stamps[target.getId()] < currentStamp || alt < dist[target.getId()]) {
			dist[target.getId()] = alt;
			pred[target.getId()] = curr;
			if (stamps[target.getId()] < currentStamp || items[target.getId()] == null) {
				items[target.getId()] = queue.insertItem(alt, target);
			} else {
				queue.decreaseKey(items[target.getId()], alt);
			}
			stamps[target.getId()] = currentStamp;
		}
	}

	/**
	 * Assumes that the Dijkstra algorithm has been executed before. Returns the
	 * path of node to the target (stored in an ArrayList from start to target).
	 */

	public List<DiGraphNode<V, E>> getPath(DiGraphNode<V, E> target) {
		LinkedList<DiGraphNode<V, E>> path = new LinkedList<DiGraphNode<V, E>>();

		if (stamps[target.getId()] < currentStamp) {
			return new ArrayList<DiGraphNode<V, E>>();
		}

		DiGraphNode<V, E> current = target;
		while (current != null) {
			path.addFirst(current);
			current = pred[current.getId()];
		}

		return new ArrayList<DiGraphNode<V, E>>(path);
	}

	public List<DiGraphArc<V, E>> getPathArcs(DiGraphNode<V, E> target) {
		LinkedList<DiGraphArc<V, E>> path = new LinkedList<DiGraphArc<V, E>>();

		if (stamps[target.getId()] < currentStamp) {
			return new ArrayList<DiGraphArc<V, E>>();
		}

		DiGraphNode<V, E> prev = null;
		DiGraphNode<V, E> current = target;
		while (current != null) {
			if (prev == null) {
				prev = current;
				current = pred[prev.getId()];
				continue;
			}
			path.addFirst(current.getFirstOutgoingArcTo(prev));
			prev = current;
			current = pred[prev.getId()];
		}

		return new ArrayList<DiGraphArc<V, E>>(path);
	}

	/**
	 * Assumes that the dijkstra has been executed before. Returns the distance of
	 * <code>node</code> to the start node of the last run.
	 * 
	 * @param node
	 * @return distance of the node to the start node of the last run. If the node
	 *         is not reachable from the start node, then it returns
	 *         Double.MAX_VALUE.
	 */
	public double getDistance(DiGraphNode<V, E> node) {
		return stamps[node.getId()] < currentStamp ? Double.MAX_VALUE : dist[node.getId()];
	}

	public void setStarttime(long starttime) {
		this.starttime = starttime;
	}

	public interface NodeVisitor<V> {
		boolean visit(V node);
	}

	public static interface NodeIterator<V, E extends WeightedArcData> {
		Iterator<DiGraphNode<V, E>> getIterator(DiGraphNode<V, E> s);

		double getWeightOfCurrentArc(DiGraphNode<V, E> s, DiGraphNode<V, E> t);
	}

	/**
	 * NodeIterator that depends mainly on the graph structure. Additional
	 * adjacencies may be added via an additional NodeIterator.
	 */
	public static class BasicAdjacentNodeIterator<V, E extends WeightedArcData> implements NodeIterator<V, E> {

		protected DiGraphArc<V, E> currArc;
		protected NodeIterator<V, E> addIt;
		protected boolean start;

		public BasicAdjacentNodeIterator(NodeIterator<V, E> additionalIterator) {
			this.addIt = additionalIterator;
			this.start = true;
			this.currArc = null;
		}

		@Override
		public double getWeightOfCurrentArc(DiGraphNode<V, E> s, DiGraphNode<V, E> t) {
			if (currArc != null) {
				return currArc.getArcData().getValue();
			}
			if (start) {
				return Double.NaN;
			}
			return addIt.getWeightOfCurrentArc(s, t);
		}

		@Override
		public Iterator<DiGraphNode<V, E>> getIterator(DiGraphNode<V, E> s) {
			Iterator<DiGraphArc<V, E>> it = s.getOutgoingArcs().iterator();
			Iterator<DiGraphNode<V, E>> extraIt;
			if (addIt == null) {
				extraIt = null;
			} else {
				extraIt = addIt.getIterator(s);
			}
			return new Iterator<DiGraphNode<V, E>>() {

				@Override
				public boolean hasNext() {
					if (it.hasNext()) {
						return true;
					}
					if (extraIt == null) {
						return false;
					}
					return extraIt.hasNext();
				}

				@Override
				public DiGraphNode<V, E> next() {
					start = false;
					if (it.hasNext()) {
						currArc = it.next();
						return currArc.getTarget();
					}
					if (extraIt == null) {
						return null;
					}
					currArc = null;
					return extraIt.next();
				}
			};
		}
	}

	/**
	 * NodeIterator that depends only on the graph structure.
	 */
	protected NodeIterator<V, E> DEFAULT_ADJACENT_NODE_ITERATOR = new BasicAdjacentNodeIterator<V, E>(null);

}
