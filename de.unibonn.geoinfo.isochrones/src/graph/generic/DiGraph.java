package graph.generic;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class DiGraph<V, E> {

	public static class DiGraphNode<V, E> {
		private List<DiGraphArc<V, E>> outgoingArcs;
		private List<DiGraphArc<V, E>> incomingArcs;
		private V nodeData;
		private int id;

		private DiGraphNode(V nodeData, int id) {
			outgoingArcs = new ArrayList<DiGraphArc<V, E>>();
			incomingArcs = new ArrayList<DiGraphArc<V, E>>();
			this.nodeData = nodeData;
			this.id = id;
		}

		public List<DiGraphArc<V, E>> getOutgoingArcs() {
			return outgoingArcs;
		}

		public List<DiGraphArc<V, E>> getIncomingArcs() {
			return incomingArcs;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public V getNodeData() {
			return nodeData;
		}

		@Override
		public String toString() {
			return nodeData.toString();
		}

		public int outDegree() {
			return outgoingArcs.size();
		}

		public int inDegree() {
			return incomingArcs.size();
		}

		public DiGraphArc<V, E> getFirstOutgoingArcTo(DiGraphNode<V, E> target) {
			for (DiGraphArc<V, E> a : outgoingArcs) {
				if (a.getTarget() == target) {
					return a;
				}
			}
			return null;
		}

		public DiGraphArc<V, E> getFirstIncomingArcFrom(DiGraphNode<V, E> source) {
			for (DiGraphArc<V, E> a : incomingArcs) {
				if (a.getSource() == source) {
					return a;
				}
			}
			return null;
		}
	}

	public static class DiGraphArc<V, E> {
		private DiGraphNode<V, E> source;
		private DiGraphNode<V, E> target;
		private E arcData;
		private int id;

		protected DiGraphArc(DiGraphNode<V, E> source, DiGraphNode<V, E> target, E arcData, int id) {
			this.source = source;
			this.target = target;
			this.arcData = arcData;
			this.id = id;
		}

		public DiGraphNode<V, E> getSource() {
			return source;
		}

		public DiGraphNode<V, E> getTarget() {
			return target;
		}

		public E getArcData() {
			return arcData;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public DiGraphArc<V, E> getTwin() {
			for (DiGraphArc<V, E> a : source.getIncomingArcs()) {
				if (a.getSource() == this.getTarget())
					return a;
			}
			return null;
		}

		public void setArcData(E arcData) {
			this.arcData = arcData;
		}

		public double getInclination(boolean swapping) {
			double inc = 0;
			double Y = 0;
			double X = 0;

			if (swapping) {
				Y = ((Point2D) this.source.nodeData).getY() - ((Point2D) this.target.nodeData).getY();
				X = ((Point2D) this.source.nodeData).getX() - ((Point2D) this.target.nodeData).getX();
			} else {
				Y = ((Point2D) this.target.nodeData).getY() - ((Point2D) this.source.nodeData).getY();
				X = ((Point2D) this.target.nodeData).getX() - ((Point2D) this.source.nodeData).getX();
			}

			inc = Math.atan2(Y, X);

			if (inc >= Math.PI / 2 && inc <= Math.PI) {

				inc -= Math.PI / 2;
			} else {
				inc += 3 * Math.PI / 2;
			}

			return inc;
		}

		@Override
		public String toString() {
			return "[" + source.toString() + " -- " + target.toString() + "] (" + arcData.toString() + ")";
		}
	}

	private ArrayList<DiGraphNode<V, E>> nodeList;
	private ArrayList<DiGraphArc<V, E>> arcList;

	/**
	 * Generates empty graph
	 */
	public DiGraph() {
		nodeList = new ArrayList<DiGraphNode<V, E>>();
		arcList = new ArrayList<DiGraphArc<V, E>>();
	}

	/**
	 * Generates graph from adjacency matrix
	 * 
	 * @param nodes   - array with node data
	 * @param arcs    - adjacency matrix
	 * @param arcData - array with arc data
	 */
	public DiGraph(V[] nodes, boolean[][] arcs, E[][] arcData) {
		nodeList = new ArrayList<DiGraphNode<V, E>>();
		arcList = new ArrayList<DiGraphArc<V, E>>();
		for (V v : nodes) {
			addNode(v);
		}
		for (int i = 0; i < nodeList.size(); i++) {
			for (int j = 0; j < nodeList.size(); j++) {
				if (arcs[i][j]) {
					addArc(nodeList.get(i), nodeList.get(j), arcData[i][j]);
				}
			}
		}
	}

	public DiGraphNode<V, E> addNode(V nodeInfo) {
		DiGraphNode<V, E> v = new DiGraphNode<V, E>(nodeInfo, nodeList.size());
		nodeList.add(v);
		return v;
	}

	public DiGraphArc<V, E> addArc(DiGraphNode<V, E> v1, DiGraphNode<V, E> v2, E edgeData) {
		DiGraphArc<V, E> a = new DiGraphArc<V, E>(v1, v2, edgeData, arcList.size());
		v1.outgoingArcs.add(a);
		v2.incomingArcs.add(a);
		arcList.add(a);
		return a;
	}

	public int n() {
		return nodeList.size();
	}

	public int m() {
		return arcList.size();
	}

	public DiGraphNode<V, E> getNode(int index) {
		return nodeList.get(index);
	}

	public ArrayList<DiGraphNode<V, E>> getNodes() {
		return nodeList;
	}

	public DiGraphArc<V, E> getArc(int index) {
		return arcList.get(index);
	}

	public ArrayList<DiGraphArc<V, E>> getArcs() {
		return arcList;
	}

	public void setNodes(ArrayList<DiGraphNode<V, E>> nodes) {
		this.nodeList = nodes;
	}

	public void setArcs(ArrayList<DiGraphArc<V, E>> arcs) {
		this.arcList = arcs;
	}

	/**
	 * method to remove a set of arcs in O(m) time (assuming constant-time hashing)
	 * 
	 * @param arcsToBeRemoved
	 */
	public void removeArcs(HashSet<DiGraphArc<V, E>> arcsToBeRemoved) {
		ArrayList<DiGraphArc<V, E>> arcListNew = new ArrayList<DiGraphArc<V, E>>();
		for (DiGraphArc<V, E> a : arcList) {
			if (!arcsToBeRemoved.contains(a)) {
				arcListNew.add(a);
			} else {
				a.getSource().getOutgoingArcs().remove(a);
				a.getTarget().getIncomingArcs().remove(a);
			}
		}
		arcList = arcListNew;
	}

	/**
	 * method to remove an arc in O(m) time
	 * 
	 * @param arcToBeRemoved
	 */
	public void removeArc(DiGraphArc<V, E> arcToBeRemoved) {
		if (arcToBeRemoved == null)
			return;
		arcList.remove(arcToBeRemoved);
		arcToBeRemoved.source.getOutgoingArcs().remove(arcToBeRemoved);
		arcToBeRemoved.target.getIncomingArcs().remove(arcToBeRemoved);
	}

	/**
	 * method to remove a set of nodes and all their incident edges in O(n + m) time
	 * 
	 * @param nodesToBeRemoved
	 */
	public void removeNodes(HashSet<DiGraphNode<V, E>> nodesToBeRemoved) {
		// remove incident arcs
		HashSet<DiGraphArc<V, E>> arcsToBeRemoved = new HashSet<DiGraphArc<V, E>>();
		for (DiGraphNode<V, E> v : nodesToBeRemoved) {
			arcsToBeRemoved.addAll(v.incomingArcs);
			arcsToBeRemoved.addAll(v.outgoingArcs);
		}
		this.removeArcs(arcsToBeRemoved);

		ArrayList<DiGraphNode<V, E>> nodeListNew = new ArrayList<DiGraphNode<V, E>>();
		for (DiGraphNode<V, E> v : nodeList) {
			if (!nodesToBeRemoved.contains(v)) {
				nodeListNew.add(v);
			}
		}
		nodeList = nodeListNew;
	}

	/**
	 * method to remove a node and all its incident edges in O(n + m) time
	 * 
	 * @param nodeToBeRemoved
	 */
	public void removeNode(DiGraphNode<V, E> nodeToBeRemoved) {
		HashSet<DiGraphArc<V, E>> arcsToBeRemoved = new HashSet<DiGraphArc<V, E>>();
		arcsToBeRemoved.addAll(nodeToBeRemoved.incomingArcs);
		arcsToBeRemoved.addAll(nodeToBeRemoved.outgoingArcs);
		this.removeArcs(arcsToBeRemoved);
		nodeList.remove(nodeToBeRemoved);
	}

	public DiGraphArc<V, E> getOuterArc() {
		return nodeList.get(0).getIncomingArcs().get(0);
	}

	public DiGraphArc<V, E> getArc(DiGraphNode<V, E> source, DiGraphNode<V, E> target) {
		for (DiGraphArc<V, E> arc : source.outgoingArcs) {
			if (arc.getTarget() == target) {
				return arc;
			}
		}
		return null;
	}

	public LinkedList<DiGraphArc<V, E>> getPathArcs(List<DiGraphNode<V, E>> pathNodes) {
		LinkedList<DiGraphArc<V, E>> pathArcs = new LinkedList<DiGraphArc<V, E>>();
		DiGraphNode<V, E> u = null;
		for (DiGraphNode<V, E> v : pathNodes) {
			if (u != null) {
				// add arc uv to list
				DiGraphArc<V, E> uv = getArc(u, v);
				if (uv != null)
					pathArcs.add(uv);
			}
			u = v;
		}
		return pathArcs;
	}

	public List<DiGraphArc<V, E>> addDoubleArc(DiGraphNode<V, E> v1, DiGraphNode<V, E> v2) {
		return addDoubleArc(v1, v2, null);
	}

	public List<DiGraphArc<V, E>> addDoubleArc(DiGraphNode<V, E> v1, DiGraphNode<V, E> v2, E edgeData) {
		List<DiGraphArc<V, E>> result = new ArrayList<DiGraphArc<V, E>>();
		result.add(addArc(v1, v2, edgeData));
		result.add(addArc(v2, v1, edgeData));
		return result;
	}

//	public void sortNodes(Comparator<DiGraphNode<V, E>> nodeComp) {
//		nodeList.sort(nodeComp);
//		updateIDs();
//	}

	public void sort(Comparator<DiGraphArc<V, E>> outgoingComp, Comparator<DiGraphArc<V, E>> incomingComp) {
		for (DiGraphNode<V, E> node : nodeList) {
			node.outgoingArcs.sort(outgoingComp);
			node.incomingArcs.sort(incomingComp);
		}
//		updateIDs();
	}

	public ArrayList<ArrayList<Integer>> updateIDs() {
		ArrayList<ArrayList<Integer>> lookuptable = new ArrayList<>();
		ArrayList<Integer> nodeTable = new ArrayList<>();
		ArrayList<Integer> arcTable = new ArrayList<>();

		for (int i = 0; i < this.n(); i++) {
			DiGraphNode<V, E> node = this.getNode(i);
			nodeTable.add(node.getId());
			node.setId(i);
		}
		for (int i = 0; i < this.m(); i++) {
			DiGraphArc<V, E> arc = this.getArc(i);
			arcTable.add(arc.getId());
			arc.setId(i);
		}
		lookuptable.add(nodeTable);
		lookuptable.add(arcTable);

		return lookuptable;
	}

}
