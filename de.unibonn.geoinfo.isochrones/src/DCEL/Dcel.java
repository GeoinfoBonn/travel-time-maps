package DCEL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import graph.generic.DiGraph;

/**
 * implementation of a doubly connected edge list
 * 
 * @author
 *
 * @param <V>
 * @param <E>
 * @param <F>
 */
public class Dcel<V, E, F> {

	public interface EdgeVisitor<V, E, F> {
		boolean visit(HalfEdge<V, E, F> e);
	}

	public interface VertexVisitor<V, E, F> {
		boolean visit(Vertex<V, E, F> v);
	}

	public interface FaceVisitor<V, E, F> {
		boolean visit(Face<V, E, F> f);
	}

	public static class VertexCollector<V, E, F> implements VertexVisitor<V, E, F> {
		private List<Vertex<V, E, F>> list = new ArrayList<Vertex<V, E, F>>();

		@Override
		public boolean visit(Vertex<V, E, F> v) {
			list.add(v);
			return true;
		}

		public List<Vertex<V, E, F>> getList() {
			return list;
		}
	}

	public static class EdgeCollector<V, E, F> implements EdgeVisitor<V, E, F> {
		private List<HalfEdge<V, E, F>> list = new ArrayList<HalfEdge<V, E, F>>();

		@Override
		public boolean visit(HalfEdge<V, E, F> e) {
			list.add(e);
			return true;
		}

		public List<HalfEdge<V, E, F>> getList() {
			return list;
		}
	}

	public static class FaceCollector<V, E, F> implements FaceVisitor<V, E, F> {
		private List<Face<V, E, F>> list = new ArrayList<Face<V, E, F>>();

		@Override
		public boolean visit(Face<V, E, F> f) {
			list.add(f);
			return true;
		}

		public List<Face<V, E, F>> getList() {
			return list;
		}
	}

	public static class Vertex<V, E, F> {
		private int id;
		private HalfEdge<V, E, F> incidentEdge;
		private V vertexData;

		public Vertex() {
		};

		public Vertex(V vertexData, int id) {
			this.id = id;
			this.vertexData = vertexData;
		}

		public int getId() {
			return id;
		}

		public HalfEdge<V, E, F> getIncidentEdge() {
			return incidentEdge;
		}

		public V getVertexData() {
			return vertexData;
		}

		public void setVertexData(V vdata) {
			this.vertexData = vdata;
		}

		/**
		 * returns all edges with their origin being this vertex
		 * 
		 * @return List of HalfEdges
		 */
		public List<HalfEdge<V, E, F>> getOutgoingEdges() {
			EdgeCollector<V, E, F> eC = new EdgeCollector<V, E, F>();
			this.visitOutgoingEdges(eC);
			return eC.getList();
		}

		/**
		 * returns all edges with their target being this vertex
		 * 
		 * @return List of HalfEdges
		 */
		public List<HalfEdge<V, E, F>> getIncomingEdges() {
			EdgeCollector<V, E, F> eC = new EdgeCollector<V, E, F>();
			this.visitIncomingEdges(eC);
			return eC.getList();
		}

		/**
		 * returns all vertices with direct connection to this vertex
		 * 
		 * @return List of Vertices
		 */
		public List<Vertex<V, E, F>> getAdjacentVertices() {
			VertexCollector<V, E, F> vC = new VertexCollector<V, E, F>();
			this.visitAdjacentVertices(vC);
			return vC.getList();
		}

		@Override
		public String toString() {
			return "Vertex [ID=" + id + ", IncidentEdge=" + incidentEdge.id + ", VertexData=" + vertexData + "]";
		}

		public void visitOutgoingEdges(EdgeVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this.incidentEdge;
			do {
				if (!visitor.visit(tempEdge)) {
					break;
				}
				tempEdge = tempEdge.twin.next;
			} while (tempEdge != this.incidentEdge);
		}

		public void visitIncomingEdges(EdgeVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this.incidentEdge.twin;
			do {
				if (!visitor.visit(tempEdge)) {
					break;
				}
				tempEdge = tempEdge.next.twin;
			} while (tempEdge != this.incidentEdge.twin);
		}

		public void visitAdjacentVertices(VertexVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this.incidentEdge.twin;
			Vertex<V, E, F> tempVertex = tempEdge.origin;
			do {
				if (!visitor.visit(tempVertex)) {
					break;
				}
				tempEdge = tempEdge.next.twin;
				tempVertex = tempEdge.origin;
			} while (tempEdge != this.incidentEdge.twin);
		}
	}

	public static class HalfEdge<V, E, F> {
		private int id;
		private Vertex<V, E, F> origin;
		private HalfEdge<V, E, F> twin, next, prev;
		private Face<V, E, F> incidentFace;
		private E edgeData;

		public HalfEdge() {
		};

		public HalfEdge(E edgeData, int id) {
			this.edgeData = edgeData;
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public Vertex<V, E, F> getOrigin() {
			return origin;
		}

		public HalfEdge<V, E, F> getTwin() {
			return twin;
		}

		public HalfEdge<V, E, F> getNext() {
			return next;
		}

		public HalfEdge<V, E, F> getPrevious() {
			return prev;
		}

		public Face<V, E, F> getIncidentFace() {
			return incidentFace;
		}

		public E getEdgeData() {
			return edgeData;
		}

		public void setEdgeData(E edata) {
			this.edgeData = edata;
		}

		public Vertex<V, E, F> getTarget() {
			return twin.origin;
		}

		/**
		 * returns all edges forming a closed ring with this edge (including this edge)
		 * 
		 * @return List of HalfEdges
		 */
		public List<HalfEdge<V, E, F>> getRing() {
			EdgeCollector<V, E, F> eC = new EdgeCollector<V, E, F>();
			this.visitRing(eC);
			return eC.getList();
		}

		@Override
		public String toString() {
			return "HalfEdge [ID=" + id + ", Origin=" + origin.id + ", Twin=" + twin.id + ", Next=" + next.id
					+ ", Previous=" + prev.id + ", IncidentFace=" + incidentFace.id + ", EdgeData=" + edgeData + "]";
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			HalfEdge<?, ?, ?> edge = null;
			if ((o instanceof HalfEdge<?, ?, ?>)) {
				edge = (HalfEdge<?, ?, ?>) o;
			} else {
				return false;
			}

			if (edge.id == this.id) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return id;
		}

		public void visitRing(EdgeVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this;
			do {
				if (!visitor.visit(tempEdge)) {
					break;
				}
				tempEdge = tempEdge.next;
			} while (tempEdge != this);
		}

		public void visitRing(VertexVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this;
			Vertex<V, E, F> tempVertex = tempEdge.origin;
			do {
				if (!visitor.visit(tempVertex)) {
					break;
				}
				tempEdge = tempEdge.next;
				tempVertex = tempEdge.origin;
			} while (tempEdge != this);
		}
	}

	public static class Face<V, E, F> {
		private int id;
		private HalfEdge<V, E, F> outerComponent;
		private ArrayList<Dcel<V, E, F>> innerComponents = new ArrayList<Dcel<V, E, F>>();
		private F faceData;

		public Face() {
		};

		public Face(F faceData, int id) {
			this.faceData = faceData;
			this.id = id;
		}

		public Face(int id) {
			this.id = id;
			this.faceData = null;
		}

		public int getId() {
			return id;
		}

		public HalfEdge<V, E, F> getOuterComponent() {
			return outerComponent;
		}

		public ArrayList<Dcel<V, E, F>> getInnerComponents() {
			return innerComponents;
		}

		public F getFaceData() {
			return faceData;
		}

		public void setFaceData(F fdata) {
			faceData = fdata;
		}

		/**
		 * returns all edges incident to this face
		 * 
		 * @return List of HalfEdges
		 */
		public List<HalfEdge<V, E, F>> getIncidentEdges() {
			if (outerComponent != null) {
				EdgeCollector<V, E, F> eC = new EdgeCollector<V, E, F>();
				this.visitIncidentEdges(eC);
				return eC.getList();
			}
			return new ArrayList<HalfEdge<V, E, F>>();
		}

		/**
		 * returns all corner vertices of this face
		 * 
		 * @return List of Vertices
		 */
		public List<Vertex<V, E, F>> getOuterVertices() {
			if (outerComponent != null) {
				VertexCollector<V, E, F> vC = new VertexCollector<V, E, F>();
				outerComponent.visitRing(vC);
				return vC.getList();
			}
			return new ArrayList<Vertex<V, E, F>>();
		}

		/**
		 * returns all neighbored faces of this face
		 * 
		 * @return List of Faces
		 */
		public List<Face<V, E, F>> getNeighbours() {
			FaceCollector<V, E, F> fC = new FaceCollector<V, E, F>();
			this.visitNeighbourFaces(fC);
			return fC.getList();
		}

		/**
		 * checks if this face is a neighbored face of the outer face
		 * 
		 * @param backgroundFace Face of the corresponding DCEL
		 * @return true if this Face is adjacent to the background Face, false otherwise
		 */
		public boolean isOuterFace(Face<V, E, F> backgroundFace) {
			for (HalfEdge<V, E, F> e : getIncidentEdges()) {
				if (e.twin.incidentFace.equals(backgroundFace))
					return true;
			}
			return false;
		}

		@Override
		public String toString() {
			if (outerComponent != null) {
				return "Face [id=" + id + ", OuterComponent=" + outerComponent.id + ", Number of Holes="
						+ innerComponents.size() + " faceData=" + faceData + "]";
			}
			return "Face [id=" + id + ", OuterComponent=null, Number of Holes=" + innerComponents.size() + " faceData="
					+ faceData + "]";
		}

		public void visitIncidentEdges(EdgeVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = this.outerComponent;
			do {
				if (!visitor.visit(tempEdge)) {
					break;
				}
				tempEdge = tempEdge.next;
			} while (tempEdge != this.outerComponent);
		}

		public void visitOuterVertices(VertexVisitor<V, E, F> visitor) {
			HalfEdge<V, E, F> tempEdge = outerComponent;
			Vertex<V, E, F> tempVertex = tempEdge.origin;
			do {
				if (!visitor.visit(tempVertex)) {
					break;
				}
				tempEdge = tempEdge.next;
				tempVertex = tempEdge.origin;
			} while (tempEdge != outerComponent);
		}

		public void visitNeighbourFaces(FaceVisitor<V, E, F> visitor) {
			Set<Face<V, E, F>> visitedFaces = new TreeSet<Face<V, E, F>>(new Comparator<Face<V, E, F>>() {
				@Override
				public int compare(Face<V, E, F> f1, Face<V, E, F> f2) {
					if (f1.getId() == f2.getId()) {
						return 0;
					}
					return (f1.getId() > f2.getId()) ? 1 : -1;
				}
			});
			if (outerComponent != null) {
				outerComponent.visitRing(new EdgeVisitor<V, E, F>() {
					@Override
					public boolean visit(HalfEdge<V, E, F> e) {
						if (!visitedFaces.contains(e.twin.incidentFace) && e.twin.incidentFace != Face.this) {
							visitor.visit(e.twin.incidentFace);
							visitedFaces.add(e.twin.incidentFace);
						}
						return true;
					}
				});
			}
			for (Dcel<V, E, F> dcel : innerComponents) {
				for (Face<V, E, F> face : dcel.fList) {
					if (face.isOuterFace(dcel.fList.get(0)))
						visitor.visit(face);
				}
			}
		}
	}

	private Dcel<V, E, F> parent = null;
	private Face<V, E, F> parentFace = null;
	private List<Vertex<V, E, F>> vList = new ArrayList<Vertex<V, E, F>>();
	private List<HalfEdge<V, E, F>> eList = new ArrayList<HalfEdge<V, E, F>>();
	private List<Face<V, E, F>> fList = new ArrayList<Face<V, E, F>>();

	public Dcel() {
	}

	/**
	 * generates DCEL from a vertex table, a edge table and a face table
	 * 
	 * @param vertices   vertex table (id, incidentEdge)
	 * @param edges      edge table (id, origin, twin, next, previous, incident
	 *                   Face)
	 * @param faces      face table (id, outerComponent, innerComponent)
	 * @param vertexData Vertex data
	 * @param edgeData   Edge data
	 * @param faceData   Face data
	 */
	public Dcel(V[] vertexData, int[][] vertices, E[] edgeData, int[][] edges, F[] faceData, int[][] faces) {
		for (V v : vertexData) {
			addVertex(v);
		}
		for (E e : edgeData) {
			addEdge(e);
		}
		for (F f : faceData) {
			addFace(f);
		}
		for (int i = 0; i < vertices.length; i++) {
			editVertex(vertices[i]);
		}
		for (int i = 0; i < edges.length; i++) {
			editEdge(edges[i]);
		}
		for (int i = 0; i < faces.length; i++) {
			editFace(faces[i]);
		}
		addHole(this, fList.get(0));
	}

	private Vertex<V, E, F> addVertex(V vData) {
		Vertex<V, E, F> v = new Vertex<V, E, F>(vData, vList.size());
		vList.add(v);
		return v;
	}

	private HalfEdge<V, E, F> addEdge(E eData) {
		HalfEdge<V, E, F> e = new HalfEdge<V, E, F>(eData, eList.size());
		eList.add(e);
		return e;
	}

	private Face<V, E, F> addFace(F fData) {
		Face<V, E, F> f = new Face<V, E, F>(fData, fList.size());
		fList.add(f);
		return f;
	}

	private Vertex<V, E, F> editVertex(int[] vertices) {
		vList.get(vertices[0]).incidentEdge = eList.get(vertices[1]);
		return vList.get(vertices[0]);
	}

	private HalfEdge<V, E, F> editEdge(int[] edges) {
		HalfEdge<V, E, F> temp = eList.get(edges[0]);
		temp.origin = vList.get(edges[1]);
		temp.twin = eList.get(edges[2]);
		temp.next = eList.get(edges[3]);
		temp.prev = eList.get(edges[4]);
		temp.incidentFace = fList.get(edges[5]);
		eList.set(edges[0], temp);
		return eList.get(edges[0]);
	}

	private Face<V, E, F> editFace(int[] faces) {
		if (faces[1] != -1) {
			fList.get(faces[0]).outerComponent = eList.get(faces[1]);
		} else {
			fList.get(faces[0]).outerComponent = null;
		}
		return fList.get(faces[0]);
	}

	/**
	 * generates DCEL from graph
	 * 
	 * @param graph
	 * @param outerArc a outer Arc of the graph, used to define the background Face
	 */
	public Dcel(DiGraph<V, E> graph, DiGraph.DiGraphArc<V, E> outerArc) {
		this.vList = new ArrayList<Vertex<V, E, F>>();
		this.eList = new ArrayList<HalfEdge<V, E, F>>();
		TreeSet<HalfEdge<V, E, F>> eList = new TreeSet<HalfEdge<V, E, F>>(new Comparator<HalfEdge<V, E, F>>() {
			@Override
			public int compare(HalfEdge<V, E, F> o1, HalfEdge<V, E, F> o2) {
				if (o1.id == o2.id)
					return 0;
				return (o1.id > o2.id) ? 1 : -1;
			}
		});
		fList = new ArrayList<Face<V, E, F>>();
		for (DiGraph.DiGraphNode<V, E> node : graph.getNodes()) {
			Vertex<V, E, F> v = new Vertex<V, E, F>(node.getNodeData(), node.getId());
			vList.add(v);
		}
		int index = 0;
		for (DiGraph.DiGraphNode<V, E> node : graph.getNodes()) {
			for (int i = 0; i < node.getOutgoingArcs().size(); i++) {
				HalfEdge<V, E, F> outgoingHE = new HalfEdge<V, E, F>(node.getOutgoingArcs().get(i).getArcData(),
						node.getOutgoingArcs().get(i).getId());
				if (!eList.contains(outgoingHE)) {
					eList.add(outgoingHE);
				}
				outgoingHE.origin = vList.get(index);
				if (i == 0) {
					vList.get(index).incidentEdge = eList.floor(outgoingHE);
				}
				HalfEdge<V, E, F> incomingHE = new HalfEdge<V, E, F>(node.getIncomingArcs().get(i).getArcData(),
						node.getIncomingArcs().get(i).getId());
				if (!eList.contains(incomingHE)) {
					eList.add(incomingHE);
				}
				outgoingHE.twin = incomingHE;
				incomingHE.twin = outgoingHE;
			}
			index++;
		}
		int[] ids = new int[graph.m()];
		for (DiGraph.DiGraphNode<V, E> node : graph.getNodes()) {
			for (int i = 0; i < node.getIncomingArcs().size(); i++) {
				ids[node.getIncomingArcs().get(i).getId()] = i;
			}
		}
		for (DiGraph.DiGraphNode<V, E> node : graph.getNodes()) {
			for (int i = 0; i < node.getOutgoingArcs().size(); i++) {
				DiGraph.DiGraphNode<V, E> target = node.getOutgoingArcs().get(i).getTarget();
				HalfEdge<V, E, F> temp = new HalfEdge<V, E, F>(null, node.getOutgoingArcs().get(i).getId());
				eList.floor(temp).origin = vList.get(node.getId());

				int j = ids[node.getOutgoingArcs().get(i).getId()];

				int prev = j == 0 ? target.getIncomingArcs().size() - 1 : j - 1;
				HalfEdge<V, E, F> temp2 = new HalfEdge<V, E, F>(null, target.getOutgoingArcs().get(prev).getId());
				eList.floor(temp).next = eList.floor(temp2);

				int next = (i + 1) % node.getOutgoingArcs().size();
				HalfEdge<V, E, F> temp3 = new HalfEdge<V, E, F>(null, node.getIncomingArcs().get(next).getId());
				eList.floor(temp).prev = eList.floor(temp3);

			}
		}
		boolean[] visited = new boolean[eList.size()];
		Face<V, E, F> outerFace = new Face<V, E, F>(fList.size());
		fList.add(outerFace);
		outerFace.innerComponents.add(this);
		HalfEdge<V, E, F> temp4 = new HalfEdge<V, E, F>(null, outerArc.getId());
		eList.floor(temp4).visitRing(new EdgeVisitor<V, E, F>() {
			@Override
			public boolean visit(HalfEdge<V, E, F> e) {
				e.incidentFace = outerFace;
				visited[e.getId()] = true;
				return true;
			}
		});
		for (HalfEdge<V, E, F> e : eList) {
			if (!visited[e.getId()]) {
				Face<V, E, F> face = new Face<V, E, F>(fList.size());
				face.outerComponent = e;
				fList.add(face);
				e.visitRing(new EdgeVisitor<V, E, F>() {
					@Override
					public boolean visit(HalfEdge<V, E, F> e) {
						e.incidentFace = face;
						visited[e.getId()] = true;
						return true;
					}
				});
			}
		}
		this.eList.addAll(eList);
	}

	public void addHole(Dcel<V, E, F> dcel, Face<V, E, F> face) {
		dcel.parent = this;
		dcel.parentFace = face;
		face.innerComponents.add(dcel);
	}

	public Vertex<V, E, F> getVertex(int id) {
		return vList.get(id);
	}

	public HalfEdge<V, E, F> getHalfEdge(int id) {
		return eList.get(id);
	}

	public Face<V, E, F> getFace(int id) {
		return fList.get(id);
	}

	public Dcel<V, E, F> getParent() {
		return parent;
	}

	public Face<V, E, F> getParentFace() {
		return parentFace;
	}

	public List<Vertex<V, E, F>> getVertexList() {
		return vList;
	}

	public List<HalfEdge<V, E, F>> getEdgeList() {
		return eList;
	}

	public List<Face<V, E, F>> getFaceList() {
		return fList;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DCEL: \nVertices: \n");
		for (Vertex<V, E, F> v : vList) {
			sb.append(v + " \n");
		}
		sb.append("HalfEdges: \n");
		for (HalfEdge<V, E, F> e : eList) {
			sb.append(e + " \n");
		}
		sb.append("Faces: \n");
		for (Face<V, E, F> f : fList) {
			sb.append(f + " \n");
		}
		return sb.toString();
	}
}
