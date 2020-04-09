package isochrone;

import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.generic.LD.factory.DistanceFactory;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import isochrone.FaceIdentifier.FaceFactory;
import main.AbstractMain;
import util.tools.Util;

public class MinimumDistFace extends IsoFace {

	public MinimumDistFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred, IdGenerator idGen)
			throws InvalidParameterException {
		super(incidentFaceBoundary, isInner, pred, idGen);
	}

	@Override
	public void createVisualizationGraph() {
		generateSampledLines();

//		if (AbstractMain.DEBUG && AbstractMain.SHOW_RESULTS)
//		showVisualizationGraph(AbstractMain.GUI, "face " + getId());
	}

	private boolean connectInAndOutputAndCheckEasySolution(Point2D movedInput, Point2D movedOutput, Polygon comparePoly,
			GeometryFactory gf) {

		DiGraphNode<Point2D, VisualizationEdge> source = visualizationGraph.addNode(movedInput);
		if (source == null)
			source = visualizationGraph.getDiGraphNode(movedInput);

		DiGraphNode<Point2D, VisualizationEdge> target = visualizationGraph.addNode(movedOutput);
		if (target == null)
			target = visualizationGraph.getDiGraphNode(movedOutput);

		visualizationGraph.addDoubleArc(source, input,
				new VisualizationEdge(movedInput.distance(input.getNodeData()), false, VisualizationEdge.GRID_LINE));
		visualizationGraph.addDoubleArc(target, output,
				new VisualizationEdge(movedOutput.distance(output.getNodeData()), false, VisualizationEdge.GRID_LINE));

		LineString line = gf.createLineString(new Coordinate[] { new Coordinate(movedInput.getX(), movedInput.getY()),
				new Coordinate(movedOutput.getX(), movedOutput.getY()) });

		if (comparePoly.covers(line)) {
			// System.out.println("easy solution!");
			visualizationGraph.addDoubleArc(source, target,
					new VisualizationEdge(movedInput.distance(movedOutput), false, VisualizationEdge.GRID_LINE));
			return true;
		}
		return false;
	}

	private void generateSampledLines() {
		double offset = AbstractMain.FACE_BOUNDARY_BUFFER / 4;

		LinkedList<Integer> newIOids = new LinkedList<>();
		List<Point2D> sampledBoundary = sampleBoundary(boundary, 1000, offset, newIOids);
		int inputId = newIOids.get(0);
		int outputId = newIOids.get(1);

		Point2D sampledInput = sampledBoundary.get(inputId);
		Point2D sampledOutput = sampledBoundary.get(outputId);

		if (inputId < 0 || outputId < 0) {
			throw new RuntimeException("new IO ids are not set correctly!!!");
		}

		GeometryFactory gf = new GeometryFactory();
		Polygon comparePoly = createPolygonFromPoints(sampledBoundary);
		Geometry buffer = comparePoly.buffer(offset * 1e-3);
		if (buffer instanceof Polygon) { // in a perfectly planarized graph, a face **must** be a simple polygon
			comparePoly = (Polygon) buffer;
		}
		// THIS WHOLE ELSE BLOCK IS ONLY NEEDED DUE TO NON-FOUND INTERSECTIONS DURING
		// THE PLANARIZATION STEP, CAN BE DELETED ONCE PLANARIZER IS FIXED
		else if (buffer instanceof MultiPolygon) {
			gf = new GeometryFactory();
			Polygon p;
			boolean found = false;
			for (int i = 0; i < buffer.getNumGeometries(); ++i) {
				p = (Polygon) buffer.getGeometryN(i);
				if (p.contains(gf.createPoint(new Coordinate(sampledInput.getX(), sampledInput.getY())))) {
					if (p.contains(gf.createPoint(new Coordinate(sampledOutput.getX(), sampledOutput.getY())))) {
						comparePoly = p;
						found = true;
						break;
					}
				}
			}
			if (!found)
				throw new RuntimeException("Multi Polygon... possibly an error in the planarization step.");
		} else {
			throw new RuntimeException("buffered comparePoly is of type: " + buffer.getGeometryType());
		}

		if (connectInAndOutputAndCheckEasySolution(sampledInput, sampledOutput, comparePoly, gf))
			return;

		Point2D sourceLoc, targetLoc;
		DiGraphNode<Point2D, VisualizationEdge> source, target;
		LineString line;

		int maxDist = (int) Math.max(2000, Math.ceil(input.getNodeData().distance(output.getNodeData())));
		for (int i = 0; i < sampledBoundary.size(); ++i) {
			sourceLoc = sampledBoundary.get(i);
			if (sourceLoc.distance(input.getNodeData()) > maxDist && sourceLoc.distance(output.getNodeData()) > maxDist)
				continue;

			source = visualizationGraph.addNode(sourceLoc);
			if (source == null)
				source = visualizationGraph.getDiGraphNode(sourceLoc);

			for (int j = i + 1; j < sampledBoundary.size(); ++j) {
				targetLoc = sampledBoundary.get(j);
				if (targetLoc.distance(input.getNodeData()) > maxDist
						&& targetLoc.distance(output.getNodeData()) > maxDist)
					continue;

				if (sourceLoc.distance(targetLoc) > maxDist)
					continue;

				line = gf.createLineString(new Coordinate[] { new Coordinate(sourceLoc.getX(), sourceLoc.getY()),
						new Coordinate(targetLoc.getX(), targetLoc.getY()) });

				if (comparePoly.contains(line)) {
					target = visualizationGraph.addNode(targetLoc);
					if (target == null)
						target = visualizationGraph.getDiGraphNode(targetLoc);

					visualizationGraph.addDoubleArc(source, target,
							new VisualizationEdge(sourceLoc.distance(targetLoc), false, VisualizationEdge.GRID_LINE));
				}
			}
		}

		checkVisualizationGraph(comparePoly);
	}

	public List<Point2D> sampleBoundary(List<ColoredNode> boundary, double maxSamplingDistance, double dangleOffset,
			LinkedList<Integer> newIOids) {
		List<Point2D> newBoundary = new LinkedList<>();

		boolean moveAll = true;

		Set<Integer> duplicates = duplicateIndices(boundary);

		int colorToBuffer = isInner() ? Colored.REACHABLE : Colored.UNREACHABLE;

		int prevId, nextId, overnextId;
		Point2D sampleSource, sampleTarget;
		for (int currId = 0; currId < boundary.size(); ++currId) {

			prevId = currId > 0 ? currId - 1 : boundary.size() - 1;
			nextId = (currId + 1) % boundary.size();
			overnextId = (currId + 2) % boundary.size();

			if (currId == boundaryInputId)
				newIOids.addFirst(newBoundary.size() - 1);
			else if (currId == boundaryOutputId)
				newIOids.addLast(newBoundary.size() - 1);

			if (!duplicates.contains(currId) && !(boundary.get(currId).getColor() == colorToBuffer) && !moveAll) {
				sampleSource = boundary.get(currId);
			} else {
				Point2D[] moved = Util.movePointIntoPolygon(boundary.get(prevId), boundary.get(currId),
						boundary.get(nextId), dangleOffset);
				sampleSource = moved[0];
				if (moved.length == 2) {
					sampleTarget = moved[1];
					newBoundary.addAll(Util.sampleAlongLine(sampleSource, sampleTarget, maxSamplingDistance));
					sampleSource = moved[1];
				}
			}

			if (!duplicates.contains(nextId) && !(boundary.get(nextId).getColor() == colorToBuffer) && !moveAll) {
				sampleTarget = boundary.get(nextId);
			} else {
				Point2D[] moved = Util.movePointIntoPolygon(boundary.get(currId), boundary.get(nextId),
						boundary.get(overnextId), dangleOffset);
				sampleTarget = moved[0];
			}

			newBoundary.addAll(Util.sampleAlongLine(sampleSource, sampleTarget, maxSamplingDistance));
		}

		if (newIOids.get(0) == -1)
			newIOids.set(0, newBoundary.size() - 1);
		if (newIOids.get(1) == -1)
			newIOids.set(1, newBoundary.size() - 1);

		return newBoundary;
	}

	private static Set<Integer> duplicateIndices(List<ColoredNode> boundary) {
		Map<Point2D, Integer> map = new HashMap<>();
		Set<Integer> duplicates = new HashSet<>();
		ColoredNode node;
		Point2D asPoint;
		for (int i = 0; i < boundary.size(); ++i) {
			node = boundary.get(i);
			asPoint = new Point2D.Double(node.x, node.y);
			if (map.containsKey(asPoint)) {
				duplicates.add(i);
				duplicates.add(map.get(asPoint));
			} else {
				map.put(asPoint, i);
			}
		}
		return duplicates;
	}

	public void checkVisualizationGraph(Polygon comparePoly) {
		if (!nodesConnected()) {

			if (AbstractMain.SHOW_RESULTS) {
				showVisualizationGraph(AbstractMain.GUI, "face " + getId());
				IsochroneCreator.showPolygon(AbstractMain.GUI, "poly " + getId(), comparePoly);
			}

			// dirty solution
			String message = String.format(
					"non-connected face " + this.getId() + " [%10.3f, %10.3f] -> [%10.3f, %10.3f]",
					input.getNodeData().getX(), input.getNodeData().getY(), output.getNodeData().getX(),
					output.getNodeData().getY());
			System.err.println(message);
			this.addMessage(message);
			visualizationGraph.addDoubleArc(input, output, new VisualizationEdge(
					input.getNodeData().distance(output.getNodeData()), false, VisualizationEdge.EXCEPTION_LINE));

		}
	}

	public static final FaceFactory<MinimumDistFace> FACTORY = new FaceFactory<MinimumDistFace>() {

		@Override
		public MinimumDistFace createFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred,
				IdGenerator idGen) {
			return new MinimumDistFace(incidentFaceBoundary, isInner, pred, idGen);
		}

		@Override
		public LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> getLDFactory(
				PlanarGraph<ColoredNode, GeofabrikData> coloredGraph,
				Set<DiGraphNode<Point2D, VisualizationEdge>> componentSplit) {
//			return new MinimumLinkLDFactory(0.1, coloredGraph);
//			return new SplitNodeFactory(0.1, coloredGraph, componentSplit);
			return new DistanceFactory(0.1, coloredGraph, componentSplit);
		}

		@Override
		public String getName() {
			return "MinDist";
		}
	};
}
