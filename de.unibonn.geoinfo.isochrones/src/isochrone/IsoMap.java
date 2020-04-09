package isochrone;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import gisviewer.PointMapObject;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.DoubleWeight;
import graph.generic.LD.LDIterator;
import graph.generic.LD.LDVisitor;
import graph.generic.LD.LinearDualCreator;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.generic.LD.LinearDualCreator.LinearDualGraphIdentifier;
import graph.planarizer.PlanarGraph;
import graph.routing.Dijkstra;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import isochrone.FaceIdentifier.FaceFactory;
import main.AbstractMain;
import tools.Stopwatch;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public class IsoMap {

	private int numComponents;
	private List<DiGraph<Point2D, VisualizationEdge>> componentGraphs;
	private List<List<DiGraphNode<Point2D, VisualizationEdge>>> componentST;
	private List<String> componentMessage;
	private List<Set<DiGraphNode<Point2D, VisualizationEdge>>> componentSplits;

	private IdGenerator idGenerator;

	public IsoMap(List<List<IsoFace>> seperatedFaces, Stopwatch sw, IdGenerator idGen) {
		this.idGenerator = idGen;

		long faceProcessingTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Processing faces...");

		if (seperatedFaces == null || seperatedFaces.isEmpty())
			return;

		numComponents = seperatedFaces.size();
		componentGraphs = new ArrayList<>(numComponents);
		componentST = new ArrayList<>(numComponents);
		componentMessage = new ArrayList<>(numComponents);
		componentSplits = new ArrayList<>(numComponents);

		if (AbstractMain.USE_PARALLEL_PROCESSING) {
			List<Callable<String>> callables = new LinkedList<>();
			for (List<IsoFace> faceList : seperatedFaces) {
				for (IsoFace face : faceList) {
					callables.add(new Callable<>() {
						@Override
						public String call() {
							face.createVisualizationGraph();
							return face.getMessage();
						}
					});
				}
			}

			ExecutorService executor = Executors.newWorkStealingPool();
			try {
				List<Future<String>> futures = executor.invokeAll(callables);
				for (Future<String> future : futures) {
					future.get();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			executor.shutdown();

			faceProcessingTime = System.currentTimeMillis() - faceProcessingTime;
			sw.add("faceProcessing", faceProcessingTime);
			if (AbstractMain.VERBOSE)
				System.out.println("Faces processed. (" + faceProcessingTime / 1000.0 + "s)");

			long combineTime = System.currentTimeMillis();
			if (AbstractMain.VERBOSE)
				System.out.println("Combining face graphs...");
			List<DiGraphNode<Point2D, VisualizationEdge>> st;
			Set<DiGraphNode<Point2D, VisualizationEdge>> splits;
			for (List<IsoFace> faceList : seperatedFaces) {
				st = new LinkedList<>();
				splits = new HashSet<>();
				componentGraphs.add(combineFacesOrdered(faceList, st, splits));
				componentST.add(st);
				componentSplits.add(splits);
			}

			combineTime = System.currentTimeMillis() - combineTime;
			sw.add("graphCombine", combineTime);
			if (AbstractMain.VERBOSE)
				System.out.println("Face graphs combined. (" + combineTime / 1000.0 + "s)");

		} else {
			List<DiGraphNode<Point2D, VisualizationEdge>> st;
			Set<DiGraphNode<Point2D, VisualizationEdge>> splits;
			for (List<IsoFace> faceList : seperatedFaces) {
				for (IsoFace face : faceList) {
					face.createVisualizationGraph();
				}

				faceProcessingTime = System.currentTimeMillis() - faceProcessingTime;
				sw.add("faceProcessing", faceProcessingTime);
				if (AbstractMain.VERBOSE)
					System.out.println("Faces processed. (" + faceProcessingTime / 1000.0 + "s)");

				long combineTime = System.currentTimeMillis();
				if (AbstractMain.VERBOSE)
					System.out.println("Combining face graphs...");
				st = new LinkedList<>();
				splits = new HashSet<>();
				componentGraphs.add(this.combineFacesOrdered(faceList, st, splits));
				componentST.add(st);
				componentSplits.add(splits);

				combineTime = System.currentTimeMillis() - combineTime;
				sw.add("graphCombine", combineTime);
				if (AbstractMain.VERBOSE)
					System.out.println("Face graphs combined. (" + combineTime / 1000.0 + "s)");
			}
		}
	}

	public Timezone<Point2D> route(PlanarGraph<ColoredNode, GeofabrikData> coloredGraph, FaceFactory<?> faceFactory,
			Stopwatch sw) {
		long routingTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Routing...");
		DiGraph<Point2D, VisualizationEdge> componentGraph;
		Set<DiGraphNode<Point2D, VisualizationEdge>> componentSplit;
		DiGraphNode<Point2D, VisualizationEdge> originalSource;
		DiGraphNode<Point2D, VisualizationEdge> originalTarget;

		LinearDualCreator<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> ldc;
		DiGraph<Point2D, VisualizationEdge> graphR;

		Timezone<Point2D> timezone = new Timezone<>(idGenerator);
		List<Point2D> ring;
		String message;
		for (int i = 0; i < numComponents; ++i) {
			componentGraph = componentGraphs.get(i);
			componentSplit = componentSplits.get(i);
			originalSource = componentST.get(i).get(0);
			originalTarget = componentST.get(i).get(1);
			message = componentMessage.get(i);

			LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> ldFactory = faceFactory
					.getLDFactory(coloredGraph, componentSplit);

			ldc = new LinearDualCreator<>(componentGraph, ldFactory);
			graphR = ldc.getLinearDualGraph();

//			if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG)
//				showRoutingGraph(AbstractMain.GUI, graphR, "LD");

			DiGraphNode<Point2D, VisualizationEdge> source = graphR.addNode(originalSource.getNodeData());
			DiGraphNode<Point2D, VisualizationEdge> target = graphR.addNode(originalTarget.getNodeData());

			ring = this.createRing(graphR, originalSource, originalTarget, source, target, ldc.getIdentifier(),
					coloredGraph);

			if (ring.size() == 0)
				continue;

			if (ring.get(0).distance(ring.get(ring.size() - 1)) > 1e-10)
				ring.add(ring.get(0));

			timezone.addPolygon(ring, message);
		}

		routingTime = System.currentTimeMillis() - routingTime;
		sw.add("faceRouting", routingTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Routing done. (" + routingTime / 1000.0 + "s)");
		return timezone;
	}

	// Creating a circular path around the given graph and given start/end node
	private List<Point2D> createRing(DiGraph<Point2D, VisualizationEdge> graph,
			DiGraphNode<Point2D, VisualizationEdge> originalSource,
			DiGraphNode<Point2D, VisualizationEdge> originalTarget, DiGraphNode<Point2D, VisualizationEdge> source,
			DiGraphNode<Point2D, VisualizationEdge> target,
			LinearDualGraphIdentifier<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> ldi,
			PlanarGraph<ColoredNode, GeofabrikData> coloredGraph) {

		// Create an instance of Dijkstra
		Dijkstra<Point2D, VisualizationEdge> d = new Dijkstra<>(graph);

		// Run the Dijkstra with the given nodes
		LDVisitor visitor = new LDVisitor(target);
		LDIterator iterator = new LDIterator(originalSource, originalTarget, source, target, ldi);
		iterator.setColoredGraph(coloredGraph);
		d.run(source, visitor, iterator);

		// Receive the resulting path
		List<DiGraphNode<Point2D, VisualizationEdge>> path = d.getPath(target);

		LinkedList<Point2D> streetNodePath = new LinkedList<>();
		DiGraphArc<Point2D, VisualizationEdge> currArc = null;
		boolean first = true;
		for (DiGraphNode<Point2D, VisualizationEdge> ldNode : path) {
			if (ldNode == source) {
//				streetNodePath.add(originalSource.getNodeData());
			} else if (ldNode == target) {
//				streetNodePath.add(originalTarget.getNodeData());
			} else {
				currArc = ldi.getOriginalArc(ldNode);
				if (first) {
					streetNodePath.add(currArc.getSource().getNodeData());
					first = false;
				}
				streetNodePath.add(currArc.getTarget().getNodeData());
			}
		}

		if (streetNodePath.size() < 3) {
			String err = "Zone missing?";
			componentMessage.add(err);
			System.err.println(err);
			if (AbstractMain.SHOW_RESULTS) {
				var visitedNodes = visitor.getVisitedNodes();
				if (!visitedNodes.isEmpty()) {
					var visitedRoadLocations = new LinkedList<Point2D>();
					for (var node : visitedNodes) {
						var arc = ldi.getOriginalArc(node);
						if (arc != null)
							visitedRoadLocations.add(arc.getSource().getNodeData());
					}
					showVisitedNodes(AbstractMain.GUI, graph, visitedRoadLocations, "Visited nodes");
				}
			}
		}

		return streetNodePath;
	}

	private DiGraph<Point2D, VisualizationEdge> combineFacesOrdered(List<IsoFace> faces,
			List<DiGraphNode<Point2D, VisualizationEdge>> startEnde,
			Set<DiGraphNode<Point2D, VisualizationEdge>> splits) {
		if (faces.size() == 1) {
			IsoFace face = faces.get(0);
			startEnde.add(face.getInput());
			startEnde.add(face.getOutput());
			splits.add(face.getInput());
			splits.add(face.getOutput());
			componentMessage.add("[component " + componentMessage.size() + ":" + face.getMessage() + "]");
			return face.getVisualizationGraph();
		}

		String collectedMessage = null;

		// Creating the combined graph
		DiGraph<Point2D, VisualizationEdge> combinedGraph = new DiGraph<>();
		HashMap<Point2D, DiGraphNode<Point2D, VisualizationEdge>> inputMap = new HashMap<>();
		DiGraphNode<Point2D, VisualizationEdge> input = null;
		DiGraphNode<Point2D, VisualizationEdge> output = null;

		for (IsoFace face : faces) {
			if (!face.getMessage().isBlank()) {
				if (collectedMessage == null)
					collectedMessage = "[component " + componentMessage.size() + ":";
				else
					collectedMessage += ";";
				collectedMessage += face.getMessage();
			}

			// Lookup HashMap for referencing nodes
			Map<DiGraphNode<Point2D, VisualizationEdge>, DiGraphNode<Point2D, VisualizationEdge>> oldNewMap = new HashMap<>();

			DiGraph<Point2D, VisualizationEdge> faceGraph = face.getVisualizationGraph();

			// in case in- and output are the same, the routing graph needs to have two
			// separate nodes at this point
			boolean sameIO = face.getInput() == face.getOutput();
			if (sameIO && AbstractMain.DEBUG)
				System.out.println("Same IO");
			DiGraphNode<Point2D, VisualizationEdge> combinedInput = null, combinedOutput = null;

			for (var node : faceGraph.getNodes()) {
				if (node == face.getInput()) {
					if (inputMap.isEmpty()) {
						input = combinedGraph.addNode(node.getNodeData());
						startEnde.add(input);
						inputMap.put(node.getNodeData(), input);
					}

					oldNewMap.put(node, input);
					if (sameIO && combinedInput == null) {
						combinedInput = input;

						output = combinedGraph.addNode(node.getNodeData());
						combinedOutput = output;
					}
					splits.add(input);

				} else if (node == face.getOutput()) {
					output = combinedGraph.addNode(node.getNodeData());
					oldNewMap.put(node, output);
					if (inputMap.containsKey(node.getNodeData())) {
						startEnde.add(output);
					}
					splits.add(output);
				} else {
					DiGraphNode<Point2D, VisualizationEdge> n = combinedGraph.addNode(node.getNodeData());
					oldNewMap.put(node, n);
				}
			}
			input = output;
			// Iterate through all node of the face graph
			for (var a : faceGraph.getArcs()) {
				// Getting the new node corresponding to the old source node
				DiGraphNode<Point2D, VisualizationEdge> source = oldNewMap.get(a.getSource());
				// Getting the new node corresponding to the old target node
				DiGraphNode<Point2D, VisualizationEdge> target = oldNewMap.get(a.getTarget());
				// Creating the arc in the combined graph

//				// use separate nodes as in- and output are the same
				if (sameIO && a.getSource() == face.getOutput())
					source = combinedInput;
				if (sameIO && a.getTarget() == face.getInput())
					target = combinedOutput;

				combinedGraph.addArc(source, target, a.getArcData());
			}
		}

		if (startEnde.size() < 2) {
			StringBuilder sb = new StringBuilder();
			sb.append("StartEnde not set correctly. " + faces.get(0).getInput());
			for (var face : faces)
				sb.append(face.getId() + " ");

			if (collectedMessage != null && !collectedMessage.isBlank())
				collectedMessage += sb.toString();
			else
				collectedMessage = "[component " + componentMessage.size() + ": " + sb.toString() + "]";
			System.err.println(collectedMessage);
		}

		if (collectedMessage != null && !collectedMessage.isBlank())
			componentMessage.add(collectedMessage + "]");
		else
			componentMessage.add("");
		return combinedGraph;
	}

	@SuppressWarnings("unused")
	private IsochronePanel showRoutingGraph(ResultFrame rf, DiGraph<Point2D, DoubleWeight> graphR, String title) {
		IsochronePanel panel = new IsochronePanel(rf);

		ListLayer ld = new ListLayer(Color.BLUE);
		for (DiGraphArc<Point2D, DoubleWeight> arc : graphR.getArcs())
			ld.add(new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData()));

		panel.getMap().addLayer(rf.getRoadLayer(), 100);
		panel.getMap().addLayer(ld, 102);

		rf.addTab(title, panel);

		return panel;
	}

	private IsochronePanel showVisitedNodes(ResultFrame rf, DiGraph<Point2D, ?> graph, List<Point2D> locations,
			String title) {
		IsochronePanel panel = new IsochronePanel(rf);

//		ListLayer arcs = new ListLayer(Color.BLUE);
		ListLayer visited = new ListLayer(Color.GREEN);
		for (Point2D location : locations)
			visited.add(new PointMapObject(location));

//		for (var arc : graph.getArcs()) {
//			arcs.add(new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData()));
//		}
//
//		ListLayer visArcs = new ListLayer(Color.GRAY);
//		for (var cgraph : componentGraphs) {
//			for (var arc : cgraph.getArcs()) {
//				visArcs.add(new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData()));
//			}
//		}

		panel.getMap().addLayer(rf.getRoadLayer(), 100);
//		panel.getMap().addLayer(visArcs, 101);
//		panel.getMap().addLayer(arcs, 102);
		panel.getMap().addLayer(visited, 103);

		rf.addTab(title, panel);

		return panel;
	}
}
