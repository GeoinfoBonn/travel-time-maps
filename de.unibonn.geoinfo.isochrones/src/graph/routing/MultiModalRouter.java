package graph.routing;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;

import com.vividsolutions.jump.io.IllegalParametersException;

import gisviewer.ListLayer;
import gisviewer.MapObject;
import gisviewer.PointMapObject;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.routing.Dijkstra.BasicAdjacentNodeIterator;
import graph.routing.Dijkstra.NodeIterator;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.IsoEdge;
import graph.types.IsoVertex;
import graph.types.RoadGraph;
import graph.types.RoadNode;
import graph.types.WalkingData;
import io.gtfs.GTFSLoader;
import isochrone.IsochroneCreator;
import isochrone.SplitNodeFinder;
import main.AbstractMain;
import tools.Stopwatch;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public class MultiModalRouter<E extends WalkingData, E_iso extends IsoEdge, E_road extends WalkingData>
		implements Router<Point2D, E> {

	private int numNodesRoad;

	private DiGraph<IsoVertex, IsoEdge> routingGraph;
	private DiGraph<ColoredNode, E> coloredGraph;
	private List<DiGraphNode<ColoredNode, E>> splitNodes;

	private Dijkstra<IsoVertex, IsoEdge> dijkstra;
	private NodeIterator<IsoVertex, IsoEdge> it;
	private NodeIterator<IsoVertex, IsoEdge> adj_it;
	private SplitVisitor visit;

	private long starttime;
	private DiGraphNode<ColoredNode, E> lastSource;

	Map<DiGraphNode<Point2D, E_road>, DiGraphNode<IsoVertex, IsoEdge>> road2routing;
	Map<DiGraphNode<IsoVertex, IsoEdge>, DiGraphNode<ColoredNode, E>> routing2color;

	// maps to dynamically add arcs to next transfer node
	private final HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> transferNodes;
	private final HashMap<Integer, Integer> transferTimes;

	Factory<E, E_iso, E_road> factory;

	public MultiModalRouter(RoadGraph<Point2D, E_road> roadGraph, File gtfsDirectory, Factory<E, E_iso, E_road> factory)
			throws IllegalParametersException, Exception {
		this.factory = factory;
		numNodesRoad = roadGraph.n();
		initializeRoutingGraphWithRoadGraph(roadGraph);

		GTFSLoader loader = new GTFSLoader(routingGraph);
		loader.loadGTFS(gtfsDirectory);

		transferNodes = loader.getTransferNodes();
		transferTimes = loader.getTransferTimes();

		dijkstra = new Dijkstra<>(routingGraph);
		it = new OwnIterator(transferNodes, transferTimes, dijkstra);
	}

	/**
	 * Creates a copy of <code>roadGraph</code>, changing the node and edge data
	 * type to be suitable for the combined routing graph in the process.
	 * 
	 * Additionally, a copy of <code>roadGraph</code> is created, exchanging the
	 * node data to <code>ColoredNode</code> to later color the graph.
	 * 
	 * Due to the copy steps, the original <code>roadGraph</code> remains unchanged.
	 * 
	 * @param roadGraph previously loaded road graph
	 */
	private void initializeRoutingGraphWithRoadGraph(RoadGraph<Point2D, E_road> roadGraph) {
		if (factory == null)
			System.err.println("Factory not set");

		this.routingGraph = new DiGraph<>();
		this.coloredGraph = new DiGraph<>();

		// initialize maps to store relation between graphs
		road2routing = new HashMap<>();
		routing2color = new HashMap<>();

		DiGraphNode<IsoVertex, IsoEdge> routingGraphNode;
		DiGraphNode<ColoredNode, E> coloredGraphNode;
		for (DiGraphNode<Point2D, E_road> node : roadGraph.getNodes()) {
			routingGraphNode = routingGraph.addNode(new RoadNode(node.getNodeData()));
			coloredGraphNode = coloredGraph.addNode(new ColoredNode(node.getNodeData()));

			road2routing.put(node, routingGraphNode);
			routing2color.put(routingGraphNode, coloredGraphNode);
		}

		DiGraphNode<IsoVertex, IsoEdge> routingGraphSource, routingGraphTarget;
		DiGraphNode<ColoredNode, E> coloredGraphSource, coloredGraphTarget;
		for (DiGraphArc<Point2D, E_road> arc : roadGraph.getArcs()) {
			routingGraphSource = road2routing.get(arc.getSource());
			routingGraphTarget = road2routing.get(arc.getTarget());
			routingGraph.addArc(routingGraphSource, routingGraphTarget, factory.createIsoEdgeData(arc.getArcData()));

			coloredGraphSource = routing2color.get(routingGraphSource);
			coloredGraphTarget = routing2color.get(routingGraphTarget);
			coloredGraph.addArc(coloredGraphSource, coloredGraphTarget, factory.createEdgeData(arc.getArcData()));
		}
	}

	@Override
	public void setStarttime(long starttime) {
		this.starttime = starttime;
		this.dijkstra.setStarttime(starttime);
	}

	@Override
	public void run(DiGraphNode<Point2D, E> originalSource, long maxTime, long bufferTime) {
		long totalTime = maxTime + bufferTime;

		DiGraphNode<IsoVertex, IsoEdge> source = road2routing.get(originalSource);
		if (!AbstractMain.KEEP_MOTORWAY)
			adj_it = new BasicAdjacentNodeIterator<>(it);
		else
			adj_it = new GeofabrikWalkingIterator<>(it);
		visit = new SplitVisitor(starttime + totalTime, dijkstra);

		dijkstra.run(source, visit, adj_it);
		lastSource = routing2color.get(source);

		double time;
		// road nodes are the first ones in the routing graph
		for (int i = 0; i < numNodesRoad; ++i) {
//			if (i == 78138)
//				System.out.println();

			time = dijkstra.getDistance(routingGraph.getNode(i)) - starttime;

			ColoredNode nodeData = coloredGraph.getNode(i).getNodeData();

			if (time <= maxTime) {
				nodeData.setReachability(Colored.REACHABLE, totalTime - time);
			} else if (time <= totalTime) {
				nodeData.setReachability(Colored.BUFFER, totalTime - time);
			} else {
				nodeData.setReachability(Colored.UNREACHABLE, -1);
			}
		}

		splitNodes = SplitNodeFinder.findSplitNodes(coloredGraph, new Stopwatch(), bufferTime > 0);

		if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG) {
			if (coloredGraph.n() >= 10000)
				showRoutingResult(AbstractMain.GUI, "RR");
			else
				showColoredNodes(AbstractMain.GUI, "RR nodes");
		}

		removeBuffer(bufferTime);
	}

	@Override
	public void run(DiGraphNode<Point2D, E> originalSource, int maxDistance) {
		this.run(originalSource, maxDistance, 0);
	}

	/**
	 * Colors the outer nodes being colored as <code>BUFFER</code> to
	 * <code>UNREACHABLE</code>. On top of that, the remaining distance for all
	 * nodes is adjusted by the previously entered buffer distance.
	 * 
	 * @param bufferTime previously used distance for buffering
	 */
	private void removeBuffer(double bufferTime) {
		if (bufferTime == 0)
			return;

		for (DiGraphNode<ColoredNode, E> splitNode : splitNodes) {
			blueToRedRecursive(splitNode);
		}

		int color;
		double remTime;
		for (DiGraphNode<ColoredNode, E> node : coloredGraph.getNodes()) {
			color = node.getNodeData().getColor();
			remTime = node.getNodeData().getRemainingTime() - bufferTime;
			if (node.getNodeData().getColor() == Colored.BUFFER) {
				color = Colored.REACHABLE;
				remTime = 0;
				remTime = node.getIncomingArcs().stream()
						.flatMapToDouble(x -> DoubleStream.of(x.getArcData().getValue())).max().getAsDouble() * 2;
			}
			node.getNodeData().setReachability(color, remTime);
		}

		splitNodes = SplitNodeFinder.findSplitNodes(coloredGraph, new Stopwatch());

		if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG) {
			showRoutingResult(AbstractMain.GUI, "RR w/o buffer");
			showColoredNodes(AbstractMain.GUI, "RR w/o buffer nodes");
		}
	}

	/**
	 * Recursively coloring blue nodes red starting at a given node
	 * 
	 * @param node Starting node
	 */
	private void blueToRedRecursive(DiGraphNode<ColoredNode, E> node) {
		if (node.getNodeData().getColor() == Colored.REACHABLE)
			return;
		node.getNodeData().setReachability(Colored.UNREACHABLE, node.getNodeData().getRemainingTime());
		for (DiGraphArc<ColoredNode, E> a : node.getOutgoingArcs()) {
			if (a.getTarget().getNodeData().getColor() == Colored.BUFFER)
				blueToRedRecursive(a.getTarget());
		}
	}

	public IsochronePanel showRoutingResult(ResultFrame frame, String title) {
		IsochronePanel panel = IsochroneCreator.showColoredArcs(frame, title, coloredGraph);

		ListLayer sourceLayer = new ListLayer(Color.ORANGE);
		ListLayer splitNodeBufferLayer = new ListLayer(AbstractMain.COLOR_STYLE.buffer());
		ListLayer splitNodeReachableLayer = new ListLayer(AbstractMain.COLOR_STYLE.reachable());

		if (splitNodes != null)
			for (DiGraphNode<ColoredNode, E> splitnode : splitNodes) {
				if (splitnode.getNodeData().getColor() == Colored.BUFFER) {
					splitNodeBufferLayer.add(new PointMapObject(splitnode.getNodeData()));
				} else if (splitnode.getNodeData().getColor() == Colored.REACHABLE) {
					splitNodeReachableLayer.add(new PointMapObject(splitnode.getNodeData()));
				} else {
					System.err.println("error");
				}
			}

		sourceLayer.add(new PointMapObject(lastSource.getNodeData()));

		panel.getMap().addLayer(splitNodeBufferLayer, 15);
		panel.getMap().addLayer(splitNodeReachableLayer, 20);
		panel.getMap().addLayer(sourceLayer, 25);

		return panel;
	}

	public IsochronePanel showColoredNodes(ResultFrame frame, String title) {
		IsochronePanel panel = IsochroneCreator.showColoredArcs(frame, title, coloredGraph);

		ListLayer sourceLayer = new ListLayer(Color.ORANGE);
		ListLayer reachableLayer = new ListLayer(AbstractMain.COLOR_STYLE.reachable());
		ListLayer unreachableLayer = new ListLayer(AbstractMain.COLOR_STYLE.unreachable());
		ListLayer bufferLayer = new ListLayer(AbstractMain.COLOR_STYLE.buffer());

		MapObject mo;
		for (DiGraphNode<ColoredNode, E> node : coloredGraph.getNodes()) {
			mo = new PointMapObject(node.getNodeData());
			if (node.getNodeData().getColor() == Colored.REACHABLE)
				reachableLayer.add(mo);
			else if (node.getNodeData().getColor() == Colored.UNREACHABLE)
				unreachableLayer.add(mo);
			else
				bufferLayer.add(mo);
		}

		sourceLayer.add(new PointMapObject(lastSource.getNodeData()));

		panel.getMap().addLayer(unreachableLayer, 15);
		panel.getMap().addLayer(bufferLayer, 20);
		panel.getMap().addLayer(reachableLayer, 25);
		panel.getMap().addLayer(sourceLayer, 30);

		return panel;
	}

	@Override
	public DiGraphNode<ColoredNode, E> getLastSource() {
		return lastSource;
	}

	@Override
	public List<DiGraphNode<ColoredNode, E>> getSplitNodes() {
		return splitNodes;
	}

	@Override
	public DiGraph<ColoredNode, E> getColoredGraph() {
		return coloredGraph;
	}

	@Override
	public final DiGraph<IsoVertex, IsoEdge> getRoutingGraph() {
		return routingGraph;
	}
}