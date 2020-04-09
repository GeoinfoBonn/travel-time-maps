package isochrone;

import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.generic.LD.factory.LineSegment;
import graph.generic.LD.factory.TurncostFactory;
import graph.planarier.union.UnionPlanarizer;
import graph.planarizer.PlanarGraph;
import graph.planarizer.Planarizer;
import graph.planarizer.Planarizer.PlanarizerFactory;
import graph.planarizer.sweep.SweepPlanarizer;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import isochrone.FaceIdentifier.FaceFactory;
import main.AbstractMain;
import util.tools.Util;

public class OctilinearFace extends IsoFace {

	public OctilinearFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred, IdGenerator idGen)
			throws InvalidParameterException {
		super(incidentFaceBoundary, isInner, pred, idGen);
	}

	private boolean needsIteration() {
		return !nodesConnected() || !AbstractMain.ITERATE_DoR || input == output;
	}

	@Override
	public void createVisualizationGraph() {
		if (needsIteration()) {
			octiLinear();
			octilinearLines();
		}
	}

	/**
	 * Adding octilinear lines to the graph. The lines are only inside the
	 * boundingBox of this face. They are created for every node of the graph of the
	 * class.
	 * 
	 * @throws OutputNotMovedException
	 */
	public void octiLinear() {
		HashMap<Point2D, List<Point2D>> newArcs = new HashMap<>();
		this.octiLinearLines(newArcs);
		this.processNewArcs(newArcs);
		this.cutLines();
	}

	/**
	 * Increase density of octilinear lines.
	 * 
	 * @throws InputNotFoundException
	 * @throws NodesNotConnectedException
	 * @throws OutputNotMovedException
	 * @throws NoOutputException
	 */
	public void octilinearLines() {
		int pointsPerSide = AbstractMain.ITERATE_DoR ? 4 : AbstractMain.MAX_DoR;
		this.iterate(pointsPerSide);
	}

	private void iterate(int pointsPerSide) {
		boolean finished = false;
		do {
			if (pointsPerSide >= AbstractMain.MAX_DoR || input == output) {
				if (needsIteration())
					this.boundaryAndOctiLines();
				finished = true;
				continue;
			}
			this.startVerdichten(pointsPerSide);
			this.cutLines();
			pointsPerSide *= 4;
		} while (!this.nodesConnected() && !finished);
	}

	private void startVerdichten(int pointsPerSide) {
		HashMap<Point2D, List<Point2D>> newArcs = new HashMap<>();
		this.resetGraph();
		this.octiLinearLines(newArcs);
		this.produceAdditionalPoints(pointsPerSide, newArcs);
		this.processNewArcs(newArcs);
	}

	private List<Double> filter(List<Double> vals, int numberNewPoints) {
		List<Double> result = new ArrayList<>();
		for (int i = 1; i < vals.size(); i++) {
			switch (numberNewPoints) {
			case 1:
				result.add((vals.get(i - 1) + vals.get(i)) / 2);
				break;
			case 2:
				result.add((3 * vals.get(i - 1) + vals.get(i)) / 4);
				result.add((vals.get(i - 1) + 3 * vals.get(i)) / 4);
				break;
			default:
				result.add((vals.get(i - 1) + vals.get(i)) / 2);
				break;
			}

		}
		return result;
	}

	private void produceAdditionalPoints(int pointsPerSide, HashMap<Point2D, List<Point2D>> newArcs) {
		List<Double> xVals = new ArrayList<>();
		List<Double> yVals = new ArrayList<>();
		double width = boundingBox.getxMax() - boundingBox.getxMin();
		double height = boundingBox.getyMax() - boundingBox.getyMin();

		xVals.add(boundingBox.getxMin());
		xVals.add(boundingBox.getxMax());
		yVals.add(boundingBox.getyMin());
		yVals.add(boundingBox.getyMax());
		for (int i = 1; i <= pointsPerSide; i++) {
			xVals.add(boundingBox.getxMin() + (double) i / (pointsPerSide + 1) * width);
			yVals.add(boundingBox.getyMin() + (double) i / (pointsPerSide + 1) * height);
		}
		Collections.sort(xVals);
		Collections.sort(yVals);

		this.horAndVert(xVals, yVals, newArcs);

		this.diags(xVals, yVals, newArcs);
	}

	private void processNewArcs(HashMap<Point2D, List<Point2D>> newArcs) {
		for (Entry<Point2D, List<Point2D>> n : newArcs.entrySet()) {
			DiGraphNode<Point2D, VisualizationEdge> startNode = visualizationGraph.getDiGraphNode(n.getKey().getX(),
					n.getKey().getY());
			if (startNode == null) {
				for (int i = 0; i < n.getValue().size(); i += 2) {
					Point2D source = n.getValue().get(i);
					Point2D target = n.getValue().get(i + 1);

					DiGraphNode<Point2D, VisualizationEdge> targetNode = visualizationGraph.getDiGraphNode(target);
					if (targetNode == null) {
						targetNode = visualizationGraph.addNode(target);
					}

					startNode = visualizationGraph.addNode(source);
					if (startNode == null)
						startNode = visualizationGraph.getDiGraphNode(source);

//					if (startNode == null || targetNode == null)
//						System.out.println();

					visualizationGraph.addDoubleArc(startNode, targetNode,
							new VisualizationEdge(startNode.getNodeData().distance(targetNode.getNodeData()), true,
									VisualizationEdge.GRID_LINE));
				}
			} else {
				for (Point2D target : n.getValue()) {
					if (target == null)
						continue;
					DiGraphNode<Point2D, VisualizationEdge> targetNode = visualizationGraph
							.getDiGraphNode(target.getX(), target.getY());
					if (targetNode == null) {
						targetNode = visualizationGraph.addNode(target);
					}
					visualizationGraph.addDoubleArc(startNode, targetNode,
							new VisualizationEdge(startNode.getNodeData().distance(targetNode.getNodeData()), true,
									VisualizationEdge.GRID_LINE));
				}
			}
		}
	}

	public void boundaryLines() {
		if (needsIteration()) {
			this.resetGraph();

			this.startVerdichten(AbstractMain.MAX_DoR);

			DiGraphNode<Point2D, VisualizationEdge> prevNode = null;
			DiGraphNode<Point2D, VisualizationEdge> currNode = null;

			for (int i = 0; i <= boundary.size(); ++i) { // start from input node
				ColoredNode prev = boundary.get(i % boundary.size());
				ColoredNode curr = boundary.get((i + 1) % boundary.size());
				ColoredNode next = boundary.get((i + 2) % boundary.size());

				// calculate point slightly moved to the inside of the face
				Point2D[] moved = Util.movePointIntoPolygon(prev, curr, next, AbstractMain.FACE_BOUNDARY_BUFFER);

				// add node and arcs
				currNode = visualizationGraph.addNode(moved[0]);
				if (currNode == null)
					currNode = visualizationGraph.getDiGraphNode(moved[0]);

				if (curr.distance(input.getNodeData()) < 1e-6) {
					visualizationGraph.addDoubleArc(currNode, input,
							new VisualizationEdge(currNode.getNodeData().distance(input.getNodeData()), false,
									VisualizationEdge.EXCEPTION_LINE));
				}

				if (curr.distance(output.getNodeData()) < 1e-6) {
					visualizationGraph.addDoubleArc(currNode, output,
							new VisualizationEdge(currNode.getNodeData().distance(output.getNodeData()), false,
									VisualizationEdge.EXCEPTION_LINE));
				}

				if (prevNode != null) {
					visualizationGraph.addDoubleArc(prevNode, currNode,
							new VisualizationEdge(prevNode.getNodeData().distance(currNode.getNodeData()), false,
									VisualizationEdge.BOUNDARY_LINE));

					if (moved.length == 2) {
						prevNode = currNode;
						currNode = visualizationGraph.addNode(moved[1]);
						visualizationGraph.addDoubleArc(prevNode, currNode,
								new VisualizationEdge(prevNode.getNodeData().distance(currNode.getNodeData()), false,
										VisualizationEdge.BOUNDARY_LINE));
					}
				}

				prevNode = currNode;
			}
			this.cutLines();
		}

		if (!nodesConnected()) {
			if (AbstractMain.SHOW_RESULTS) {
				showVisualizationGraph(AbstractMain.GUI, "face " + getId());
			}

			// dirty solution
			String message = String.format("non-connected face %d [%10.3f, %10.3f] -> [%10.3f, %10.3f]", getId(),
					input.getNodeData().getX(), input.getNodeData().getY(), output.getNodeData().getX(),
					output.getNodeData().getY());
			System.err.println(message);
			this.addMessage(message);
			visualizationGraph.addDoubleArc(input, output, new VisualizationEdge(
					input.getNodeData().distance(output.getNodeData()), false, VisualizationEdge.EXCEPTION_LINE));
		}
	}

	public void boundaryAndOctiLines() {
		boundaryLines();
		assert nodesConnected();
	}

	/**
	 * Removes line segments outside of the polygon.
	 */
	private void cutLines() {

//		if (getId() == 55 && AbstractMain.DEBUG)
//			showVisualizationGraph(AbstractMain.GUI, "face " + getId() + " before cut");

		GeometryFactory gf = new GeometryFactory();
		if (this.facePoly == null)
			createPolyFromBoundary();

		Planarizer<Point2D, VisualizationEdge> planarizer;
		PlanarizerFactory<Point2D, VisualizationEdge> planFac = new PlanarizerFactory<>() {

			@Override
			public Point2D createNodeData(double x, double y) {
				return new Point2D.Double(x, y);
			}

			@Override
			public VisualizationEdge createEdgeData(double dist) {
				return new VisualizationEdge(dist, false, (byte) 0);
			}
		};

		// Find segment crossing
		boolean useUnion = true;
		if (useUnion)
			planarizer = new UnionPlanarizer<>(planFac);
		else
			planarizer = new SweepPlanarizer<>(planFac);

		planarizer.setInputGraph(visualizationGraph);
		planarizer.planarize();
		visualizationGraph = planarizer.getPlanarGraph();

		HashSet<DiGraphArc<Point2D, VisualizationEdge>> del = new HashSet<>();
		for (DiGraphArc<Point2D, VisualizationEdge> e : visualizationGraph.getArcs()) {
			Coordinate source = new Coordinate((e.getSource().getNodeData()).getX(),
					(e.getSource().getNodeData()).getY());
			Coordinate target = new Coordinate((e.getTarget().getNodeData()).getX(),
					(e.getTarget().getNodeData()).getY());
			Coordinate[] pts = { source, target };
			LineString ls = gf.createLineString(pts);

			Geometry comparePoly = facePoly;

			if (!comparePoly.covers(ls)) {
				del.add(e);
				continue;
			}
			if (del.contains(e)) {
				continue;
			}
//			System.out.println("Gibt es Limit?  " + (IsoFace.polygon_limit != null));
			if (IsoFace.polygon_limit != null) {
				boolean covered = false;
				Point2D s1 = e.getSource().getNodeData();
				Point2D s2 = e.getTarget().getNodeData();
				for (IsoPolygon<Point2D> poly : IsoFace.polygon_limit.getPolyList()) {
					if (poly.covers(s1, s2)) {
						covered = true;
						break;
					}
				}
				if (!covered) {
					del.add(e);
				}
			}
		}

		// Update original nodes
		this.input = visualizationGraph.getDiGraphNode(input.getNodeData().getX(), input.getNodeData().getY());
		this.output = visualizationGraph.getDiGraphNode(output.getNodeData().getX(), output.getNodeData().getY());

		if (boundaryInputId == -1 || boundaryOutputId == -1)
			System.err.println("no idx");

		// pre-deleting invalid arcs
		int prev = getPrevNextId(boundaryInputId)[0];
		int next = getPrevNextId(boundaryInputId)[1];

		List<ColoredNode> incomingOverlay = new LinkedList<>();
		List<ColoredNode> outgoingOverlay = new LinkedList<>();

		if (boundary.get(prev).getColor() == Colored.REACHABLE) {
			incomingOverlay.add(boundary.get(prev));
			outgoingOverlay.add(boundary.get(next));
		} else {
			outgoingOverlay.add(boundary.get(prev));
			incomingOverlay.add(boundary.get(next));
		}

		for (var outgoingArc : input.getOutgoingArcs()) {
			if (!del.contains(outgoingArc)) {
				TreeSet<LineSegment> compareSet = LineSegment.compareSet2(null, outgoingArc, incomingOverlay,
						outgoingOverlay);
				if (LineSegment.exitDirection(compareSet) == 1)
					continue;
				del.add(outgoingArc);
			}
		}

		prev = getPrevNextId(boundaryOutputId)[0];
		next = getPrevNextId(boundaryOutputId)[1];

		incomingOverlay = new LinkedList<>();
		outgoingOverlay = new LinkedList<>();

		if (boundary.get(prev).getColor() == Colored.REACHABLE) {
			incomingOverlay.add(boundary.get(prev));
			outgoingOverlay.add(boundary.get(next));
		} else {
			outgoingOverlay.add(boundary.get(prev));
			incomingOverlay.add(boundary.get(next));
		}

		for (var incomingArc : output.getIncomingArcs()) {
			if (!del.contains(incomingArc)) {
				TreeSet<LineSegment> compareSet = LineSegment.compareSet2(incomingArc, null, incomingOverlay,
						outgoingOverlay);
				if (LineSegment.arrivalDirection(compareSet) == -1)
					continue;
				del.add(incomingArc);
			}
		}

		visualizationGraph.removeArcs(del);
		visualizationGraph.updateIDs();
	}

	public int[] getPrevNextId(int id) {
		int prevId = id > 0 ? id - 1 : boundary.size() - 1;
		int nextId = id < boundary.size() - 1 ? id + 1 : 0;
		return new int[] { prevId, nextId };
	}

	private void resetGraph() {
		this.visualizationGraph = new PlanarGraph<>(this.getBoundingBox());

		if (input != null)
			this.setInput(input.getNodeData(), null);
		else
			this.setInput(output.getNodeData(), null);
		this.setOutput(output.getNodeData(), null);
	}

	/**
	 * Adding octilinear lines to the graph. The lines are only inside the
	 * boundingBox of this face.
	 * 
	 * @param nodes List of nodes to create octilinear lines
	 */
	public void octiLinearLines(HashMap<Point2D, List<Point2D>> newArcs) {
		this.createAllLines(input.getNodeData(), newArcs);
		this.createAllLines(output.getNodeData(), newArcs);
	}

	private void createAllLines(Point2D node, HashMap<Point2D, List<Point2D>> newArcs) {
		// Horizontal lines
		this.horizontal(node, newArcs);

		// Vertical lines
		this.vertical(node, newArcs);

		// First quadrant
		this.diagUp(node, newArcs);

		// Second quadrant
		this.diagDown(node, newArcs);
	}

	private void horAndVert(List<Double> xVals, List<Double> yVals, HashMap<Point2D, List<Point2D>> newArcs) {
		List<Double> midX = this.filter(xVals, 1);
		List<Double> midY = this.filter(yVals, 1);
		for (int i = 0; i < midX.size(); i++) {
			Point2D node = new Point2D.Double(midX.get(i) - 1e-4, 0);
			this.vertical(node, newArcs);
		}
		for (int i = 0; i < midY.size(); i++) {
			Point2D node = new Point2D.Double(0, midY.get(i) - 1e-4);
			this.horizontal(node, newArcs);
		}
	}

	private void horizontal(Point2D node, HashMap<Point2D, List<Point2D>> newArcs) {
		if (!newArcs.containsKey(node))
			newArcs.put(node, new LinkedList<>());
		Point2D h1 = null, h2 = null;
		boolean isVerdichtungsNode = visualizationGraph.getDiGraphNode(node) == null;
		if (node.getX() - boundingBox.getxMin() > 1e-4 || isVerdichtungsNode)
			h1 = new Point2D.Double(boundingBox.getxMin(), node.getY());
		if (boundingBox.getxMax() - node.getX() > 1e-4 || isVerdichtungsNode)
			h2 = new Point2D.Double(boundingBox.getxMax(), node.getY());

		if (h1 != null)
			newArcs.get(node).add(h1);
		if (h2 != null)
			newArcs.get(node).add(h2);
	}

	private void vertical(Point2D node, HashMap<Point2D, List<Point2D>> newArcs) {
		if (!newArcs.containsKey(node))
			newArcs.put(node, new LinkedList<>());
		Point2D v1 = null, v2 = null;
		boolean isVerdichtungsNode = visualizationGraph.getDiGraphNode(node) == null;
		if (node.getY() - boundingBox.getyMin() > 1e-4 || isVerdichtungsNode)
			v1 = new Point2D.Double(node.getX(), boundingBox.getyMin());
		if (boundingBox.getyMax() - node.getY() > 1e-4 || isVerdichtungsNode)
			v2 = new Point2D.Double(node.getX(), boundingBox.getyMax());

		if (v1 != null)
			newArcs.get(node).add(v1);
		if (v2 != null)
			newArcs.get(node).add(v2);
	}

	private void diags(List<Double> xVals, List<Double> yVals, HashMap<Point2D, List<Point2D>> newArcs) {
		if (xVals.size() == 1 && yVals.size() == 1) {
			this.createAllLines(new Point2D.Double(xVals.get(0), yVals.get(0)), newArcs);
			return;
		}
		List<Double> midX = this.filter(xVals, 2);
		List<Double> midY = this.filter(yVals, 2);
		if (midX.size() == midY.size()) {
			for (int i = 0; i < midX.size(); i++) {
				Point2D newNode = new Point2D.Double(midX.get(i) + 1e-4, midY.get(midY.size() - 1 - i));
				this.diagUp(newNode, newArcs);
				newNode = new Point2D.Double(midX.get(i) + 1e-4, midY.get(i));
				this.diagDown(newNode, newArcs);
			}
		}
	}

	private void diagUp(Point2D node, HashMap<Point2D, List<Point2D>> newArcs) {
		if (!newArcs.containsKey(node))
			newArcs.put(node, new LinkedList<>());
		Point2D q1 = null, q3 = null;
		// boolean isVerdichtungsNode = this.getNodeAtPosition(node) == null;
		double slope = 1;
		double limx = (boundingBox.getyMax() - node.getY()) / slope + node.getX();
		double limy = slope * (boundingBox.getxMax() - node.getX()) + node.getY();
		if (limy <= boundingBox.getyMax()) {
			q1 = new Point2D.Double(boundingBox.getxMax(), limy);
		} else if (limx <= boundingBox.getxMax()) {
			q1 = new Point2D.Double(limx, this.boundingBox.getyMax());
		}

		limx = (this.boundingBox.getyMin() - node.getY()) / slope + node.getX();
		limy = slope * (boundingBox.getxMin() - node.getX()) + node.getY();
		if (limy >= boundingBox.getyMin()) {
			q3 = new Point2D.Double(boundingBox.getxMin(), limy);
		} else if (limx >= boundingBox.getxMin()) {
			q3 = new Point2D.Double(limx, boundingBox.getyMin());
		}

		// if (this.getNodeAtPosition(q1) != null || isVerdichtungsNode)
		newArcs.get(node).add(q1);
		// if (this.getNodeAtPosition(q3) != null || isVerdichtungsNode)
		newArcs.get(node).add(q3);
	}

	private void diagDown(Point2D node, HashMap<Point2D, List<Point2D>> newArcs) {
		if (!newArcs.containsKey(node))
			newArcs.put(node, new LinkedList<>());
		Point2D q2 = null, q4 = null;
		// boolean isVerdichtungsNode = this.getNodeAtPosition(node) == null;
		double slope = 1;
		double limx = (boundingBox.getyMin() - node.getY()) / -slope + node.getX();
		double limy = -slope * (boundingBox.getxMax() - node.getX()) + node.getY();
		if (limy >= boundingBox.getyMin()) {
			q2 = new Point2D.Double(boundingBox.getxMax(), limy);
		} else if (limx <= this.boundingBox.getxMax()) {
			q2 = new Point2D.Double(limx, boundingBox.getyMin());
		}

		limx = (boundingBox.getyMax() - node.getY()) / -slope + node.getX();
		limy = -slope * (boundingBox.getxMin() - node.getX()) + node.getY();
		if (limy <= this.boundingBox.getyMax()) {
			q4 = new Point2D.Double(boundingBox.getxMin(), limy);
		} else if (limx >= boundingBox.getxMin()) {
			q4 = new Point2D.Double(limx, boundingBox.getyMax());
		}

		// if (this.getNodeAtPosition(q2) != null || isVerdichtungsNode)
		newArcs.get(node).add(q2);
		// if (this.getNodeAtPosition(q4) != null || isVerdichtungsNode)
		newArcs.get(node).add(q4);
	}

	public static final FaceFactory<OctilinearFace> FACTORY = new FaceFactory<OctilinearFace>() {

		@Override
		public OctilinearFace createFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred,
				IdGenerator idGen) {
			return new OctilinearFace(incidentFaceBoundary, isInner, pred, idGen);
		}

		@Override
		public LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> getLDFactory(
				PlanarGraph<ColoredNode, GeofabrikData> coloredGraph,
				Set<DiGraphNode<Point2D, VisualizationEdge>> componentSplit) {
			return new TurncostFactory(0.1);
//			return new SplitNodeFactory(0.1, coloredGraph, componentSplit);
		}

		@Override
		public String getName() {
			return "Octi";
		};
	};
}
