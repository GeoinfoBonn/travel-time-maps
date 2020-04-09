package graph.generic.LD.factory;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.TreeSet;

import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LineComparator;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.VisualizationEdge;

public class MinimumLinkLDFactory extends TurncostFactory {

	private PlanarGraph<ColoredNode, ?> coloredGraph;

	public MinimumLinkLDFactory(double offsetFactor, PlanarGraph<ColoredNode, ?> coloredGraph) {
		super(offsetFactor);
		this.coloredGraph = coloredGraph;
	}

	@Override
	public VisualizationEdge createEdgeData(DiGraphArc<Point2D, VisualizationEdge> incomingArc,
			DiGraphArc<Point2D, VisualizationEdge> outgoingArc, boolean connectsTwinNodes) {
		Point2D pred = incomingArc.getSource().getNodeData();
		Point2D actual = incomingArc.getTarget().getNodeData();
		Point2D target = outgoingArc.getTarget().getNodeData();

		if (actual.distance(new Point2D.Double(361808.3, 5623031.1)) < 0.3)
//				&& pred.distance(new Point2D.Double(360100.0, 5624152.5)) < 0.5
//				&& target.distance(new Point2D.Double(360100.0, 5624152.5)) < 0.5)
			System.out.println();

		double val = computeWeight(pred, actual, target);

		DiGraphNode<ColoredNode, ?> coloredNode = coloredGraph.getDiGraphNode(actual);
		if (coloredNode == null)
			return new VisualizationEdge(val, false, VisualizationEdge.GRID_LINE);

		// decide which arc is "greater" of incoming and outgoing arc, to be able to
		// only search in one direction later
		DiGraphArc<?, ?> low = incomingArc.getTwin(), high = outgoingArc;
		if (low == high)
			return new VisualizationEdge(val, false, VisualizationEdge.GRID_LINE); // U-turn

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Comparator<DiGraphArc<?, ?>> comparator = new LineComparator(false);
		if (comparator.compare(low, high) > 0) {
			low = outgoingArc;
			high = incomingArc.getTwin();
		}

		// sorted arcs by inclination, value == true means arc is part of the
		// visualization graph, value == false means arc is part of the (non-crossable)
		// colored graph
		TreeSet<DiGraphArc<?, ?>> arcs = new TreeSet<>(comparator);

		boolean unreachableNeighbour = false, reachableNeighbour = false;
		for (DiGraphArc<ColoredNode, ?> arc : coloredNode.getOutgoingArcs()) {
			arcs.add(arc);
			if (arc.getTarget().getNodeData().getColor() == Colored.UNREACHABLE)
				unreachableNeighbour = true;
			if (arc.getTarget().getNodeData().getColor() == Colored.REACHABLE)
				reachableNeighbour = true;
		}
		if (coloredNode.getNodeData().getColor() == Colored.REACHABLE && reachableNeighbour && unreachableNeighbour) // split
																														// node
			return new VisualizationEdge(val, false, VisualizationEdge.GRID_LINE);

		// fill set with arcs at point
//		for (DiGraphArc<?, ?> arc : incomingArc.getTarget().getOutgoingArcs()) {
//			arcs.put(arc, true);
//		}
		arcs.remove(low);
		arcs.remove(high);
		arcs.add(low);
		arcs.add(high);

		DiGraphArc<?, ?> curr = arcs.first();
		boolean inbetweenArc = false;
		boolean lowIsFirst = false, arcBetween = true;
		do { // run through whole set
			if (curr == low) {
				if (!inbetweenArc)
					lowIsFirst = true;
				inbetweenArc = false;
				continue;
			}

			if (curr == high) {
				if (!inbetweenArc)
					arcBetween = false;
				inbetweenArc = false;
				continue;
			}

			inbetweenArc = true;
		} while ((curr = arcs.higher(curr)) != null);

		boolean highIsLast = !inbetweenArc;

		if ((lowIsFirst && highIsLast) || !arcBetween)
			return new VisualizationEdge(val, false, VisualizationEdge.GRID_LINE);

//		System.out.println("Skipped edge " + pred + " -> " + actual + " -> " + target);
		return null;
	}
}
