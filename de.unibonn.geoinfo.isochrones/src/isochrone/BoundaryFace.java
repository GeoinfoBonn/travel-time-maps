package isochrone;

import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.generic.LD.factory.SplitNodeFactory;
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

public class BoundaryFace extends IsoFace {

	public BoundaryFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred, IdGenerator idGen)
			throws InvalidParameterException {
		super(incidentFaceBoundary, isInner, pred, idGen);
	}

	@Override
	public void createVisualizationGraph() {
		boundaryLines();
	}

	public void boundaryLines() {
		DiGraphNode<Point2D, VisualizationEdge> prevNode = null;
		DiGraphNode<Point2D, VisualizationEdge> currNode = null;
		DiGraphNode<Point2D, VisualizationEdge> firstNode = null;

		int colorToSkip = isInner() ? Colored.REACHABLE : Colored.UNREACHABLE;
		colorToSkip = Colored.UNREACHABLE;

		for (int i = 1; i < boundary.size() + 1; ++i) { // start from input node
			ColoredNode prev = boundary.get((i - 1) % boundary.size());
			ColoredNode curr = boundary.get(i % boundary.size());
			ColoredNode next = boundary.get((i + 1) % boundary.size());

			if (curr.getColor() == colorToSkip
					&& !(curr.distance(input.getNodeData()) < 1e-6 || curr.distance(output.getNodeData()) < 1e-6)) {
				prevNode = null;
				continue;
			}

			// calculate point slightly moved to the inside of the face
			Point2D[] moved = Util.movePointIntoPolygon(prev, curr, next, AbstractMain.FACE_BOUNDARY_BUFFER);

			// add node and arcs
			currNode = visualizationGraph.addNode(moved[0]);
			if (currNode == null)
				currNode = visualizationGraph.getDiGraphNode(moved[0]);

			if (firstNode == null)
				firstNode = currNode;

			if (curr.distance(input.getNodeData()) < 1e-6) {
				visualizationGraph.addDoubleArc(currNode, input, new VisualizationEdge(
						currNode.getNodeData().distance(input.getNodeData()), false, VisualizationEdge.EXCEPTION_LINE));
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
			}

			if (moved.length == 2) {
				prevNode = currNode;
				currNode = visualizationGraph.addNode(moved[1]);
				visualizationGraph.addDoubleArc(prevNode, currNode,
						new VisualizationEdge(prevNode.getNodeData().distance(currNode.getNodeData()), false,
								VisualizationEdge.BOUNDARY_LINE));

				if (i == 1)
					if (firstNode == null)
						firstNode = currNode;
			}

			prevNode = currNode;
		}

		if (prevNode != null)
			visualizationGraph.addDoubleArc(currNode, firstNode, new VisualizationEdge(
					currNode.getNodeData().distance(firstNode.getNodeData()), false, VisualizationEdge.BOUNDARY_LINE));
		this.cutLines();

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

	/**
	 * Removes line segments outside of the polygon.
	 */
	private void cutLines() {

//		if (getId() == 38 || getId() == 47)
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
		visualizationGraph.removeArcs(del);
		visualizationGraph.updateIDs();

		// Update original nodes
		this.input = visualizationGraph.getDiGraphNode(input.getNodeData().getX(), input.getNodeData().getY());
		this.output = visualizationGraph.getDiGraphNode(output.getNodeData().getX(), output.getNodeData().getY());

//		if (getId() == 38 || getId() == 47)
//			showVisualizationGraph(AbstractMain.GUI, "face " + getId() + " after cut");
	}

	public static final FaceFactory<BoundaryFace> FACTORY = new FaceFactory<BoundaryFace>() {

		@Override
		public BoundaryFace createFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred,
				IdGenerator idGen) {
			return new BoundaryFace(incidentFaceBoundary, isInner, pred, idGen);
		}

		@Override
		public LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> getLDFactory(
				PlanarGraph<ColoredNode, GeofabrikData> coloredGraph,
				Set<DiGraphNode<Point2D, VisualizationEdge>> componentSplit) {
			return new SplitNodeFactory(0.1, coloredGraph, componentSplit);
		}

		@Override
		public String getName() {
			return "Boundary";
		};
	};
}
