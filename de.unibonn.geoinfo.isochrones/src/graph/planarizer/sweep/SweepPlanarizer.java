package graph.planarizer.sweep;

import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LineComparator;
import graph.planarizer.PlanarGraph;
import graph.planarizer.Planarizer;
import util.geometry.Envelope;

public class SweepPlanarizer<V extends Point2D, E> extends Planarizer<V, E> {

	private Map<LineSegment, DiGraphArc<V, E>> arcMap;

	/**
	 * Default constructor
	 */
	public SweepPlanarizer(PlanarizerFactory<V, E> factory) {
		this.factory = factory;
		this.arcMap = new HashMap<>();
	}

	/**
	 * 
	 * @param oldGraph
	 * @param marker
	 * 
	 */
	@Override
	public void planarize() {
//		long start = System.currentTimeMillis();
		this.copyGraph(inputGraph);
		this.preprocessArcs();
		this.intersect();
//		System.out.println("Schnittpunkte Intersector: " + crossPoints.size() + " Zeit: "
//				+ (System.currentTimeMillis() - start) / 1000.0 + "s");
	}

	public void intersect() {
		this.crossPoints = new HashMap<>();
		this.crossedLines = new HashMap<>();
		List<Event> crossings = LineSegmentIntersector.getIntersections(arcMap.keySet());
		Map<LineSegment, List<DiGraphArc<V, E>>> arcMapList = new HashMap<>();

		DiGraphNode<V, E> eventNode;
		Point2D eventLocation;
		Point eventPoint;
		for (Event cross : crossings) {
			eventPoint = cross.getEventPoint();
			eventLocation = new Point2D.Double(eventPoint.getX(), eventPoint.getY());
			for (LineSegment ls : cross.getSegmentsCross()) {
				eventNode = planarGraph.addNode(factory.createNodeData(eventPoint.getX(), eventPoint.getY()));
				if (eventNode != null) {
					crossPoints.put(eventLocation, new LinkedList<>());
				} else {
					eventNode = planarGraph.getDiGraphNode(eventLocation);
				}
				crossPoints.get(eventLocation).add(arcMap.get(ls));

				if (!crossedLines.containsKey(arcMap.get(ls)))
					crossedLines.put(arcMap.get(ls), new LinkedList<>());
				crossedLines.get(arcMap.get(ls)).add(eventLocation);

				DiGraphArc<V, E> cutArc = null;
				List<DiGraphArc<V, E>> possibleArcs = null;

				if (!arcMapList.containsKey(ls)) {
					possibleArcs = new LinkedList<>();
					arcMapList.put(ls, possibleArcs);
					cutArc = arcMap.get(ls);
					planarGraph.removeArc(cutArc);
					planarGraph.removeArc(cutArc.getTwin());
					possibleArcs.add(planarGraph.addArc(cutArc.getSource(), eventNode,
							factory.createEdgeData(cutArc.getSource().getNodeData().distance(eventLocation))));
					possibleArcs.add(planarGraph.addArc(eventNode, cutArc.getTarget(),
							factory.createEdgeData(eventLocation.distance(cutArc.getTarget().getNodeData()))));
					planarGraph.addArc(eventNode, cutArc.getSource(),
							factory.createEdgeData(cutArc.getSource().getNodeData().distance(eventLocation)));
					planarGraph.addArc(cutArc.getTarget(), eventNode,
							factory.createEdgeData(eventLocation.distance(cutArc.getTarget().getNodeData())));

				} else {
					possibleArcs = arcMapList.get(ls);
					for (DiGraphArc<V, E> checkArc : possibleArcs) {
						LineSegment line = createLineSegment(checkArc);
						if (eventPoint.isGreaterThan(line.getP1()) && eventPoint.isLessThan(line.getP2())
								&& line.getP1().isLessThan(line.getP2())) {
							cutArc = checkArc;
							break;
						}
					}

					if (cutArc == null) {
//						System.err.println("Cut arc == null");
						continue;
					}

					this.planarGraph.removeArc(cutArc);
					this.planarGraph.removeArc(cutArc.getTwin());
					possibleArcs.remove(cutArc);

					possibleArcs.add(this.planarGraph.addArc(cutArc.getSource(), eventNode,
							factory.createEdgeData(cutArc.getSource().getNodeData().distance(eventLocation))));
					possibleArcs.add(this.planarGraph.addArc(eventNode, cutArc.getTarget(),
							factory.createEdgeData(eventLocation.distance(cutArc.getTarget().getNodeData()))));

					this.planarGraph.addArc(eventNode, cutArc.getSource(),
							factory.createEdgeData(cutArc.getSource().getNodeData().distance(eventLocation)));
					this.planarGraph.addArc(cutArc.getTarget(), eventNode,
							factory.createEdgeData(eventLocation.distance(cutArc.getTarget().getNodeData())));
				}
			}
		}

		planarGraph.setCrosspoints(crossPoints);
		planarGraph.setCrossedLines(crossedLines);

		planarGraph.updateIDs();
		planarGraph.sort(new LineComparator<>(false), new LineComparator<>(true));
	}

	public void writeCrosspointsToTxt(String name) {
		try (FileWriter csvWriter = new FileWriter(name + ".csv")) {
			csvWriter.append("x, y\n");
			for (Point2D node : this.crossPoints.keySet()) {
				csvWriter.append(Double.toString(node.getX()));
				csvWriter.append(", ");
				csvWriter.append(Double.toString(node.getY()));
				csvWriter.append("\n");
			}
			csvWriter.flush();
			csvWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	private void copyGraph(DiGraph<V, E> input) {
		Envelope env = new Envelope();
		for (DiGraphNode<V, E> node : input.getNodes()) {
			env.expandToInclude(node.getNodeData().getX(), node.getNodeData().getY());
		}

		planarGraph = new PlanarGraph<>(env);

		HashMap<V, DiGraphNode<V, E>> nodeMap = new HashMap<>();
		DiGraphNode<V, E> planarNode;
		for (DiGraphNode<V, E> n : input.getNodes()) {
			planarNode = planarGraph.addNode(factory.createNodeData(n.getNodeData().getX(), n.getNodeData().getY()));

			if (planarNode == null)
				planarNode = planarGraph.getDiGraphNode(n.getNodeData());

			nodeMap.put(n.getNodeData(), planarNode);
		}

		for (DiGraphArc<V, E> arc : input.getArcs()) {
			planarGraph.addArc(nodeMap.get(arc.getSource().getNodeData()), nodeMap.get(arc.getTarget().getNodeData()),
					arc.getArcData());
		}
	}

	/**
	 * Reduces the amount of edges to edges from bottom left to top right
	 */
	private void preprocessArcs() {
		this.arcMap = new HashMap<>();
		for (DiGraphArc<V, E> a : planarGraph.getArcs()) {
			if ((a.getSource().getNodeData()).getX() > (a.getTarget().getNodeData()).getX())
				continue;
			else if ((a.getSource().getNodeData()).getX() == (a.getTarget().getNodeData()).getX())
				if ((a.getSource().getNodeData()).getY() > (a.getTarget().getNodeData()).getY())
					continue;
			createLineSegment(a);
		}
	}

	private LineSegment createLineSegment(DiGraphArc<V, E> arc) {
		DiGraphNode<V, E> source = arc.getSource();
		DiGraphNode<V, E> target = arc.getTarget();
		LineSegment ls = null;
		ls = new LineSegment(new Point((source.getNodeData()).getX(), (source.getNodeData()).getY()),
				new Point((target.getNodeData()).getX(), (target.getNodeData()).getY()), "" + arc.getId());
		arcMap.put(ls, arc);
		return ls;
	}
}
