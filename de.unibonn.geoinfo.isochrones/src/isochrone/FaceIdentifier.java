package isochrone;

import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import DCEL.Dcel;
import DCEL.Dcel.Face;
import DCEL.Dcel.HalfEdge;
import DCEL.Dcel.Vertex;
import graph.algorithms.GraphSearch;
import graph.algorithms.GraphSearch.BFSQueue;
import graph.algorithms.GraphSearch.CollectingVisitor;
import graph.algorithms.GraphSearch.ComponentFilter;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import graph.types.WalkingData;
import main.AbstractMain;
import tools.Stopwatch;

public class FaceIdentifier<F extends IsoFace, E extends WalkingData> {

	private PlanarGraph<ColoredNode, E> planarGraph;
	private Dcel<ColoredNode, E, F> dcel;
	private List<List<Vertex<ColoredNode, E, F>>> seperatedSplitVertices;

	private ArrayList<ColoredNode> savedColor;

	public FaceIdentifier(PlanarGraph<ColoredNode, E> planarGraph, Stopwatch sw) {
		long dcelTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Creating DCEL...");
		this.dcel = new Dcel<>(planarGraph, planarGraph.getOuterArc());

		dcelTime = System.currentTimeMillis() - dcelTime;
		sw.add("dcel", dcelTime);
		if (AbstractMain.VERBOSE)
			System.out.println("DCEL created. (" + dcelTime / 1000.0 + "s)");

		this.planarGraph = planarGraph;
		this.initialize(sw);
	}

	private void initialize(Stopwatch sw) {
		List<DiGraphNode<ColoredNode, E>> splitNodes = SplitNodeFinder.findSplitNodes(planarGraph, sw);

		long time = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Seperating splitnodes...");

		ArrayList<ArrayList<Integer>> components = seperateGraphColors(planarGraph);

		seperatedSplitVertices = seperatedSplitVertices(splitNodes, components);

		time = System.currentTimeMillis() - time;
		sw.add("seperateSplitnodes", time);
		if (AbstractMain.VERBOSE)
			System.out.println("Splitnodes seperated. (" + time / 1000.0 + "s)");
	}

	public List<List<IsoFace>> identifyFaces(FaceFactory<?> factory, Stopwatch sw, IdGenerator idGen) {
		return identifyFaces(factory, sw, idGen, false);
	}

	public List<List<IsoFace>> identifyFaces(FaceFactory<?> factory, Stopwatch sw, IdGenerator idGen, boolean isInner) {
		long identifyTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Identifying" + (isInner ? " inner " : " ") + "faces...");

		if (seperatedSplitVertices.isEmpty()) {
			if (AbstractMain.VERBOSE)
				System.out.println("No split vertices, face identification stopped.");
			return null;
		}

		int count = 0;
		List<List<IsoFace>> seperatedFaces = new LinkedList<>();
		List<IsoFace> facesRedGreen = null;
//		Set<Face<ColoredNode, E, F>> incidentFaces;

		for (List<Vertex<ColoredNode, E, F>> splitVertices : seperatedSplitVertices) {
			if (splitVertices.isEmpty()) {
				System.err.println("Empty split vertices. Should not happen!");
				continue;
			}

//			incidentFaces = new HashSet<>();

			IsoFace predecessor = null;
			IsoFace activeFace = null;
			IsoFace firstFace = null;
			Vertex<ColoredNode, E, F> nextVertex = null;
			HalfEdge<ColoredNode, E, F> activeEdge = null;

			facesRedGreen = new ArrayList<>();

			Vertex<ColoredNode, E, F> leftSplit = splitVertices.get(0);
			for (Vertex<ColoredNode, E, F> sn : splitVertices) {
				if (leftSplit.getAdjacentVertices().size() > 2 && sn.getAdjacentVertices().size() == 2)
					leftSplit = sn;
				else if (leftSplit.getVertexData().getX() > sn.getVertexData().getX())
					leftSplit = sn;
			}

//			if (leftSplit.getAdjacentVertices().size() != 2) {
//				System.err.println("No valid split point " + leftSplit.getVertexData());
//			}

			Vertex<ColoredNode, E, F> activeVertex = leftSplit;

			// search edge to reachable node and start first face
			for (HalfEdge<ColoredNode, E, F> e : activeVertex.getOutgoingEdges()) {
				nextVertex = e.getTarget();
				if (nextVertex.getVertexData().getColor() == Colored.REACHABLE) {
					activeEdge = e;

					while (activeEdge.getOrigin().getVertexData().getColor() == Colored.UNREACHABLE
							&& activeEdge.getNext().getTarget().getVertexData().getColor() == Colored.UNREACHABLE) {
//						System.out.println("Skipped face.");
						activeEdge = activeEdge.getNext().getTwin();
					}

					try {
						activeFace = factory.createFace(getBoundary(activeEdge.getIncidentFace()), isInner, predecessor,
								idGen);
						activeFace.setInput(activeVertex.getVertexData(), nextVertex.getVertexData());
//						incidentFaces.add(activeEdge.getIncidentFace());

						// update pointers to next and previous
						predecessor = activeFace;
						firstFace = activeFace;
					} catch (InvalidParameterException ex) {
						System.err.println("Zone touches outer face! " + activeEdge.getOrigin().getVertexData());
					}

					break;
				}
			}

			HalfEdge<ColoredNode, E, F> startEdge = activeEdge;
			if (activeEdge == null) {
				throw new RuntimeException("Active edge is null");
			} else {
				activeEdge = activeEdge.getNext();
			}
			while (activeEdge != startEdge) {
				activeVertex = activeEdge.getOrigin();
				nextVertex = activeEdge.getTarget();
				if (nextVertex.getVertexData().getColor() == Colored.UNREACHABLE) {
					// same in- and output
					if (activeFace != null) {
						activeFace.setOutput(activeVertex.getVertexData(), nextVertex.getVertexData());
						facesRedGreen.add(activeFace);
					} else {
						System.err.println("Active face is null!! Should not happen.");
					}

					// This next-twin-next-step is needed in case that a crossing point is split
					// node. Otherwise a twin-step would be enough.
					activeEdge = activeEdge.getTwin();

					while (activeEdge.getOrigin().getVertexData().getColor() == Colored.UNREACHABLE
							&& activeEdge.getNext().getTarget().getVertexData().getColor() == Colored.UNREACHABLE) {
//						System.out.println("Skipped face.");
						activeEdge = activeEdge.getNext().getTwin();
					}

					try {
						activeFace = factory.createFace(getBoundary(activeEdge.getIncidentFace()), isInner, predecessor,
								idGen);
						activeFace.setInput(activeVertex.getVertexData(),
								activeEdge.getNext().getTarget().getVertexData());
//						incidentFaces.add(activeEdge.getIncidentFace());

						// update pointers to next and previous
						if (predecessor != null)
							predecessor.setNext(activeFace);
						predecessor = activeFace;
						if (firstFace == null)
							firstFace = activeFace;
					} catch (InvalidParameterException ex) {
						System.err.println("Zone touches outer face! " + activeEdge.getOrigin().getVertexData());
						activeFace = null;
					}

				} else {
					activeEdge = activeEdge.getNext();
				}
			}

			// update pointers
			firstFace.setPred(activeFace);
			activeFace.setNext(firstFace);

			if (!facesRedGreen.isEmpty())

			{
				seperatedFaces.add(facesRedGreen);
				count += facesRedGreen.size();
			} else
				System.err.println("No face found. Is that correct?");
		}

		identifyTime = System.currentTimeMillis() - identifyTime;
		sw.add("faceIdentification", identifyTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Faces identified. " + count + " faces found. (" + identifyTime / 1000.0 + "s)");

		return seperatedFaces;
	}

	private List<ColoredNode> getBoundary(Face<ColoredNode, E, F> face) {
		List<ColoredNode> boundary = new LinkedList<>();
		for (Vertex<ColoredNode, E, F> node : face.getOuterVertices()) {
			boundary.add(node.getVertexData());
		}
		return boundary;
	}

	public void recolor(Set<Integer> reachableNodeIDs, Stopwatch sw) {
		long recolorTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Recoloring...");

		if (savedColor == null)
			this.storeColor();

		for (int i = 0; i < planarGraph.n(); ++i) {
			if (reachableNodeIDs.contains(i)) {
				dcel.getVertex(i).getVertexData().setReachability(Colored.REACHABLE, 0);
				planarGraph.getNode(i).getNodeData().setReachability(Colored.REACHABLE, 0);
			} else {
				dcel.getVertex(i).getVertexData().setReachability(Colored.UNREACHABLE, -1);
				planarGraph.getNode(i).getNodeData().setReachability(Colored.UNREACHABLE, -1);
			}
		}

		recolorTime = System.currentTimeMillis() - recolorTime;
		sw.add("recolor", recolorTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Recoloring done. (" + recolorTime / 1000.0 + "s)");

		this.initialize(sw);
	}

	private void storeColor() {
		this.savedColor = new ArrayList<>();
		for (DiGraphNode<ColoredNode, E> node : planarGraph.getNodes()) {
			savedColor.add(new ColoredNode(node.getNodeData()));
		}
	}

	public void restoreColor(Stopwatch sw) {
		if (savedColor == null)
			return;

		long restoreTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Restoring color...");

		for (int i = 0; i < planarGraph.n(); ++i) {
			dcel.getVertex(i).getVertexData().setReachability(savedColor.get(i));
			planarGraph.getNode(i).getNodeData().setReachability(savedColor.get(i));
		}

		this.savedColor = null;

		restoreTime = System.currentTimeMillis() - restoreTime;
		sw.add("recolor", restoreTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Restoring done. (" + restoreTime / 1000.0 + "s)");

		this.initialize(sw);
	}

	private List<List<Vertex<ColoredNode, E, F>>> seperatedSplitVertices(List<DiGraphNode<ColoredNode, E>> splitNodes,
			ArrayList<ArrayList<Integer>> seperateGraphColors) {
		List<List<Vertex<ColoredNode, E, F>>> seperatedSplitNodes = new ArrayList<>();
		for (int i = 0; i < seperateGraphColors.size(); ++i) {

			// if only one point is in this separated component, skip it (no (proper)
			// visualization possible anyways)
			if (seperateGraphColors.get(i).size() == 1) {
				continue;
			}

			List<Vertex<ColoredNode, E, F>> component = new LinkedList<>();
			for (DiGraphNode<ColoredNode, E> splitnode : splitNodes) {
				if (seperateGraphColors.get(i).contains(splitnode.getId()))
					component.add(dcel.getVertex(splitnode.getId()));
			}

			if (!component.isEmpty())
				seperatedSplitNodes.add(component);
		}

		return seperatedSplitNodes;
	}

	/**
	 * Returns a list containing of one list per connected, reachable component.
	 * These (internal) lists store the indices of the components node IDs.
	 * 
	 * @param graph graph for which to search the connected, reachable components
	 * @return see method description
	 */
	private ArrayList<ArrayList<Integer>> seperateGraphColors(final PlanarGraph<ColoredNode, E> graph) {
		GraphSearch<ColoredNode, E> searcher = new GraphSearch<>(graph);
		BFSQueue<ColoredNode, E> queue = new BFSQueue<>();
		CollectingVisitor<ColoredNode, E> reachableComponentVisitor = new ReachableComponentVisitor<>();
		return searcher.findAllComponents(queue, reachableComponentVisitor, new ComponentFilter<ColoredNode, E>() {

			@Override
			public boolean startComponent(DiGraphNode<ColoredNode, E> node) {
				if (node.getIncomingArcs().size() == 0 && node.getOutgoingArcs().size() == 0)
					return false;
				return true;
			}
		});
	}

	public static class ReachableComponentVisitor<E> implements CollectingVisitor<ColoredNode, E> {

		ArrayList<Integer> currentList;
		ArrayList<ArrayList<Integer>> seperatedColors;

		public ReachableComponentVisitor() {
			this.seperatedColors = new ArrayList<>();
		}

		@Override
		public boolean visitArc(DiGraphArc<ColoredNode, E> arc) {
			if (arc.getSource().getNodeData().getColor() == arc.getTarget().getNodeData().getColor())
				return true;
			return false;
		}

		@Override
		public void settleNode(DiGraphNode<ColoredNode, E> node) {
			if (node.getNodeData().getColor() == Colored.REACHABLE)
				currentList.add(node.getId());
		}

		@Override
		public void visitNeighbor(DiGraphNode<ColoredNode, E> node) {

		}

		@Override
		public ArrayList<ArrayList<Integer>> getCollectedIds() {
			if (currentList.size() == 0)
				seperatedColors.remove(currentList);
			return seperatedColors;
		}

		@Override
		public void nextComponent() {
			if (currentList == null || currentList.size() > 0) {
				currentList = new ArrayList<>();
				seperatedColors.add(currentList);
			}
		}
	}

	public static interface FaceFactory<F extends IsoFace> {
		public static String TIMED_BUFFER = "TimedBuffer";

		F createFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred, IdGenerator idGen);

		LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> getLDFactory(
				PlanarGraph<ColoredNode, GeofabrikData> coloredGraph,
				Set<DiGraphNode<Point2D, VisualizationEdge>> componentSplit);

		String getName();
	}
}
