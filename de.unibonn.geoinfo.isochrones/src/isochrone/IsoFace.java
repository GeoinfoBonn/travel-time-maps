package isochrone;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.planarizer.PlanarGraph;
import graph.routing.Dijkstra;
import graph.types.ColoredNode;
import graph.types.VisualizationEdge;
import util.geometry.Envelope;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public abstract class IsoFace {

	protected Polygon facePoly = null;
	protected static Timezone<Point2D> polygon_limit = null;

	private int id;

	protected PlanarGraph<Point2D, VisualizationEdge> visualizationGraph;

	protected List<ColoredNode> boundary = new LinkedList<>();
	protected Envelope boundingBox;

	protected DiGraphNode<Point2D, VisualizationEdge> input = null;
	protected DiGraphNode<Point2D, VisualizationEdge> output = null;

	protected int boundaryInputId = -1;
	protected int boundaryOutputId = -1;

	private String debugMessage;

	private boolean isInner;

	IsoFace pred;
	IsoFace next;

	public IsoFace(List<ColoredNode> incidentFaceBoundary, boolean isInner, IsoFace pred, IdGenerator idGen)
			throws InvalidParameterException {
		this.id = idGen.nextFaceId();
		this.setInner(isInner);
		this.boundary = incidentFaceBoundary;
		this.calculateBoundingBox();
		this.visualizationGraph = new PlanarGraph<>(getBoundingBox());
		this.pred = pred;
		this.createPolyFromBoundary();
	}

	/**
	 * Sets the input node of this IsoFace, being the node where the final routing
	 * enters the face. Returns weather the node was a new node or an existing one.
	 * 
	 * @param input position of the input node
	 * @return true if no node was at this position before, else false
	 */
	public boolean setInput(Point2D input, Point2D nextNode) {

		if (nextNode != null)
			setIdx(input, nextNode, true);

		var n = visualizationGraph.getDiGraphNode(input);
		if (n == null) {
			n = visualizationGraph.addNode(new Point2D.Double(input.getX(), input.getY()));
			this.input = n;
			return true;
		}
		this.input = n;

		return false;
	}

	/**
	 * Sets the output node of this IsoFace, being the node where the final routing
	 * leaves the face. Returns weather the node was a new node or an existing one.
	 * 
	 * @param output position of the output node
	 * @return true if no node was at this position before, else false
	 */
	public boolean setOutput(Point2D output, Point2D nextNode) {

		if (nextNode != null)
			setIdx(output, nextNode, false);

		var n = visualizationGraph.getDiGraphNode(output.getX(), output.getY());
		if (n == null) {
			n = visualizationGraph.addNode(new Point2D.Double(output.getX(), output.getY()));
			this.output = n;
			return true;
		}
		this.output = n;

		return false;
	}

	private int setIdx(Point2D currNode, Point2D nextNode, boolean isInput) {
		Point2D curr, next;
		for (int i = 0; i < boundary.size(); ++i) {
			curr = boundary.get(i);
			next = boundary.get((i + 1) % boundary.size());

			if (curr.distance(currNode) < 1e-6 && next.distance(nextNode) < 1e-6) {
				if (isInput)
					boundaryInputId = i;
				else
					boundaryOutputId = i;
				return i;
			}
		}
		return -1;
	}

	public DiGraphNode<Point2D, VisualizationEdge> getInput() {
		return input;
	}

	DiGraphNode<Point2D, VisualizationEdge> getOutput() {
		return output;
	}

	private Envelope calculateBoundingBox() {
		boundingBox = new Envelope();
		for (ColoredNode boundaryVertex : boundary) {
			if (polygon_limit == null || polygon_limit.covers(boundaryVertex))
				boundingBox.expandToInclude(boundaryVertex.x, boundaryVertex.y);
		}
		return boundingBox;
	}

	public Envelope getBoundingBox() {
		if (boundingBox == null)
			calculateBoundingBox();
		return boundingBox;
	}

	@Override
	public String toString() {
		return "IsoFace[input=" + input + ", output=" + output + "]";
	}

	public int getId() {
		return id;
	}

	public List<ColoredNode> getBoundary() {
		return boundary;
	}

	public Point2D barycenterFromBoundary() {
		double x = 0;
		double y = 0;
		for (ColoredNode vertex : boundary) {
			x += vertex.getX();
			y += vertex.getY();
		}
		return new Point2D.Double(x / boundary.size(), y / boundary.size());
	}

	public static Point2D barycenterFromRing(Coordinate[] ring) {
		double x = 0;
		double y = 0;
		for (Coordinate c : ring) {
			x += c.x;
			y += c.y;
		}
		return new Point2D.Double(x / ring.length, y / ring.length);
	}

	public abstract void createVisualizationGraph();

	public ListLayer[] getLinesAsMapLayers() {
		return getLinesAsMapLayers(Color.BLUE, Color.CYAN, Color.RED);
	}

	public ListLayer[] getLinesAsMapLayers(Color grid, Color boundary, Color exception) {
		ListLayer gridArcs = new ListLayer(grid);
		ListLayer boundaryArcs = new ListLayer(boundary);
		ListLayer exceptionArcs = new ListLayer(exception);

		LineMapObject lmo;
		Set<DiGraphArc<Point2D, VisualizationEdge>> addedArcs = new HashSet<>();
		for (var arc : visualizationGraph.getArcs()) {
			if (addedArcs.contains(arc))
				continue;

			lmo = new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData());

			if (arc.getArcData().getType() == VisualizationEdge.GRID_LINE)
				gridArcs.add(lmo);
			else if (arc.getArcData().getType() == VisualizationEdge.BOUNDARY_LINE)
				boundaryArcs.add(lmo);
			else
				exceptionArcs.add(lmo);
			addedArcs.add(arc);
			if (arc.getTwin() != null)
				addedArcs.add(arc.getTwin());
		}

		return new ListLayer[] { gridArcs, boundaryArcs, exceptionArcs };
	}

	public ListLayer getLinesAsMapLayer(Color c) {
		ListLayer layer = new ListLayer(c);

		LineMapObject lmo;
		Set<DiGraphArc<Point2D, VisualizationEdge>> addedArcs = new HashSet<>();
		for (var arc : visualizationGraph.getArcs()) {
			if (addedArcs.contains(arc))
				continue;

			lmo = new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData());

			layer.add(lmo);
			addedArcs.add(arc);
			if (arc.getTwin() != null)
				addedArcs.add(arc.getTwin());
		}

		return layer;
	}

	public PlanarGraph<Point2D, VisualizationEdge> getVisualizationGraph() {
		return visualizationGraph;
	}

	public static void setPolygonLimit(Timezone<Point2D> limit) {
		IsoFace.polygon_limit = limit;
	}

	public void addMessage(String message) {
		if (debugMessage == null || debugMessage.isBlank())
			debugMessage = "[face " + id + ":";
		else
			debugMessage += ";";
		debugMessage += message;
	}

	public String getMessage() {
		if (debugMessage == null || debugMessage.isBlank())
			return "";
		return debugMessage += "]";
	}

	public boolean nodesConnected() {
		Dijkstra<Point2D, VisualizationEdge> dij = new Dijkstra<>(visualizationGraph);

		dij.run(input);

		if (dij.getPath(output).isEmpty())
			return false;
		return true;
	}

	public int[] findInOutIndices() {
		boolean inFound = false;
		boolean outFound = false;
		int[] res = new int[2];
		ColoredNode curr;
		for (int i = 0; i < boundary.size(); ++i) {
			curr = boundary.get(i);
			if (!inFound && curr.x == input.getNodeData().getX() && curr.y == input.getNodeData().getY()) {
				res[0] = i;
				inFound = true;
				continue;
			}

			if (!outFound && curr.x == output.getNodeData().getX() && curr.y == output.getNodeData().getY()) {
				res[1] = i;
				outFound = true;
			}

			if (inFound && outFound)
				return res;
		}
		System.err.println("Input and output indices not found correctly.");
		return null;
	}

	public int getMidpointId() {
		int[] ids = findInOutIndices();
		if (ids[0] < ids[1])
			return (ids[0] + ids[1]) / 2;
		return (ids[0] + ids[1] + boundary.size()) / 2 % boundary.size();
	}

//	public void splitFace(int firstSplitId, int secondSplitId, boolean keepFirstHalf, ColoredNode newNode,
//			boolean isIONode) {
//		boolean isInput = !keepFirstHalf;
//
//		// clean input
//		if (firstSplitId > secondSplitId) {
//			int tmp = firstSplitId;
//			firstSplitId = secondSplitId;
//			secondSplitId = tmp;
//			keepFirstHalf = !keepFirstHalf;
//		} // ==> firstSplitId < secondSplitId
//
//		for (int i = boundary.size() - 1; i >= 0; --i) {
//			if (keepFirstHalf) {
//				if (i < secondSplitId && i > firstSplitId) {
//					boundary.remove(i);
//				}
//			} else {
//				if (i > secondSplitId || i < firstSplitId) {
//					boundary.remove(i);
//				}
//			}
//		}
//
//		if (keepFirstHalf)
//			boundary.add(firstSplitId + 1, newNode);
//		else
//			boundary.add(newNode);
//
//		if (isIONode) {
//			if (isInput)
//				setInput(newNode);
//			else
//				setOutput(newNode);
//		}
//
//		reinitialize();
//	}

	protected Polygon createPolyFromBoundary() {
		facePoly = createPolygonFromPoints(boundary);
		return facePoly;
	}

	public static <P extends Point2D> Polygon createPolygonFromPoints(List<P> points) {
		GeometryFactory gf = new GeometryFactory();
		Coordinate[] coord;
		if (points.size() > 0) {
			// Create coordinate list for polygon
			coord = new Coordinate[points.size() + 1];
			int counter = 0;
			for (P c : points) {
				coord[counter] = new Coordinate(c.getX(), c.getY());
				if (counter == 0)
					coord[points.size()] = new Coordinate(c.getX(), c.getY());
				counter++;
			}
		} else {
			coord = new Coordinate[points.size()];
		}
		// Create polygon from coordinate list
		return gf.createPolygon(coord);
	}

	public IsochronePanel showVisualizationGraph(ResultFrame frame, String title) {
		IsochronePanel panel = new IsochronePanel(frame);

		ListLayer[] layers = getLinesAsMapLayers(Color.BLUE, Color.CYAN, Color.RED);

		panel.getMap().addLayer(frame.getRoadLayer(), 10);
		panel.getMap().addLayer(layers[0], 14);
		panel.getMap().addLayer(layers[1], 13);
		panel.getMap().addLayer(layers[2], 12);

		frame.addTab(title, panel);
		return panel;
	}

	public boolean isInner() {
		return isInner;
	}

	public void setInner(boolean isInner) {
		this.isInner = isInner;
	}

	public Polygon getFacePoly() {
		return facePoly;
	}

	public void setPred(IsoFace pred) {
		this.pred = pred;
	}

	public void setNext(IsoFace next) {
		this.next = next;
	}

	public IsoFace getPred() {
		return pred;
	}

	public IsoFace getNext() {
		return next;
	}
}
