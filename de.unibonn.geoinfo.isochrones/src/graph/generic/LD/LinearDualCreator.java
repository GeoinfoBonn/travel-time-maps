package graph.generic.LD;

import java.util.HashMap;
import java.util.Map;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;

public class LinearDualCreator<G extends DiGraph<V, E>, V, E> {

	// input: original graph, output: linear dual graph
	private G inputGraph;

	private G linearDualGraph;

	private LinearDualGraphIdentifier<G, V, E> identifier;

	public LinearDualCreator(G inputGraph, LinearDualFactory<G, V, E> factory) {
		this.inputGraph = inputGraph;
		this.identifier = new LinearDualGraphIdentifier<G, V, E>(inputGraph);

		createLinearDual(factory);
	}

	private void createLinearDual(LinearDualFactory<G, V, E> factory) {
		linearDualGraph = factory.createGraph();

		// 1st run through all arcs -> define nodes of new graph
		for (DiGraphArc<V, E> arc : inputGraph.getArcs()) {
			V newNodeData = factory.createNodeData(arc, 0);
			DiGraphNode<V, E> newNode = linearDualGraph.addNode(newNodeData);

			// if G instance of GeometricGraph, then no node can be at the position of
			// another node instead, null will be returned by addNode. Here the location of
			// the added node is iteratively moved by a little until it can be inserted
			if (newNode == null) {
				int i = 0;
				while (newNode == null) {
					newNodeData = factory.createNodeData(arc, ++i);
					newNode = linearDualGraph.addNode(newNodeData);
				}
			}

			identifier.setLinearDualNodeOf(arc, newNode);
			identifier.setOriginalArcOf(newNode, arc);
		}

		// 2nd run though all nodes -> for each pair of incoming and outgoing arcs,
		// create new arc
		boolean connectsTwinNodes;
		for (DiGraphNode<V, E> node : inputGraph.getNodes()) {
			for (DiGraphArc<V, E> incomingArc : node.getIncomingArcs()) {
				DiGraphNode<V, E> incNode = identifier.getLDNode(incomingArc);
				for (DiGraphArc<V, E> outgoingArc : node.getOutgoingArcs()) {
					DiGraphNode<V, E> outNode = identifier.getLDNode(outgoingArc);

					connectsTwinNodes = false;
					if (incomingArc.getTwin() == outgoingArc)
						connectsTwinNodes = true;

					E newArcData = factory.createEdgeData(incomingArc, outgoingArc, connectsTwinNodes);
					if (incNode == null || outNode == null)
						System.out.println("" + outgoingArc);

					if (newArcData == null) // in case the factory returns zero, no arc is added
						continue;

					linearDualGraph.addArc(incNode, outNode, newArcData);
				}
			}
		}

		// System.out.println("Linear dual graph created.");
	}

	public G getLinearDualGraph() {
		return linearDualGraph;
	}

	public LinearDualGraphIdentifier<G, V, E> getIdentifier() {
		return identifier;
	}

	public interface LinearDualFactory<G extends DiGraph<V, E>, V, E> {

		public V createNodeData(DiGraphArc<V, E> arc, int iteration);

		public E createEdgeData(DiGraphArc<V, E> incomingArc, DiGraphArc<V, E> outgoingArc, boolean connectsTwinNodes);

		public G createGraph();
	}

	public static class LinearDualGraphIdentifier<G extends DiGraph<V, E>, V, E> {

		private G inputGraph;

		private Map<DiGraphArc<V, E>, DiGraphNode<V, E>> arc2node;
		private Map<DiGraphNode<V, E>, DiGraphArc<V, E>> node2arc;

		public LinearDualGraphIdentifier(G inputGraph) {
			this.inputGraph = inputGraph;
			this.arc2node = new HashMap<>(inputGraph.m());
			this.node2arc = new HashMap<>(inputGraph.m());
		}

		public G getInputGraph() {
			return inputGraph;
		}

		public DiGraphArc<V, E> getOriginalArc(DiGraphNode<V, E> ldNode) {
			return node2arc.get(ldNode);
		}

		public DiGraphNode<V, E> getLDNode(DiGraphArc<V, E> roadArc) {
			return arc2node.get(roadArc);
		}

		protected void setOriginalArcOf(DiGraphNode<V, E> node, DiGraphArc<V, E> arc) {
			node2arc.put(node, arc);
		}

		protected void setLinearDualNodeOf(DiGraphArc<V, E> arc, DiGraphNode<V, E> node) {
			arc2node.put(arc, node);
		}

		public void add(DiGraphArc<V, E> arc, DiGraphNode<V, E> node) {
			setLinearDualNodeOf(arc, node);
			setOriginalArcOf(node, arc);
		}

		public DiGraphNode<V, E> getTwinNode(DiGraphNode<V, E> ldNode) {
			DiGraphArc<V, E> twinArc;
			if (getOriginalArc(ldNode) != null)
				if ((twinArc = getOriginalArc(ldNode).getTwin()) != null)
					return getLDNode(twinArc);
			return null;
		}
	}
}
