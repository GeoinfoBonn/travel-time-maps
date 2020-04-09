package io.ipe.drawables;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.types.Colored;
import graph.types.ColoredNode;
import ipeio.api.IpeDrawable;
import ipeio.api.IpeTransformation;
import main.AbstractMain;
import util.geometry.Envelope;

public class IpeColoredGraph extends IpeDrawable {

	DiGraph<ColoredNode, ?> graph;

	String arcLayerName;
	String weightLayerName;
	String nodeLayerName;

	String arcIdLayerName;

	int minNodeId;
	int maxNodeId;

	public IpeColoredGraph(DiGraph<ColoredNode, ?> graph, String name, int minNodeId, int maxNodeId) {
		this.graph = graph;
		this.arcLayerName = name + "_arcs";
		this.weightLayerName = name + "_weights";
		this.nodeLayerName = name + "_nodes";
		this.arcIdLayerName = name + "_arcId";
		this.minNodeId = minNodeId;
		this.maxNodeId = maxNodeId;
	}

	public IpeColoredGraph(DiGraph<ColoredNode, ?> graph, String name) {
		this(graph, name, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public String toIpeString(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();
		sb.append(writeArcs(t, c));
		sb.append(writeWeights(t, c));
		sb.append(writeNodes(t, c));
		sb.append(writeArcIds(t, c));
		return sb.toString();
	}

	private String writeArcs(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(arcLayerName);
		ArrayList<Integer> visitedIds = new ArrayList<Integer>();
		for (DiGraphArc<ColoredNode, ?> arc : graph.getArcs()) {
			if (!visitedIds.contains(arc.getId())
					&& (arc.getTwin() == null || !visitedIds.contains(arc.getTwin().getId())) && isInIdRange(arc)) {
				sb.append(writeArc(arc, t, c));

				visitedIds.add(arc.getId());
			}
		}

		return sb.toString();
	}

	private String writeArc(DiGraphArc<ColoredNode, ?> arc, IpeTransformation t, Color c) {
		Point2D source, target;
		source = arc.getSource().getNodeData();
		target = arc.getTarget().getNodeData();

		byte arrowType = ARROW_FORWARD;
		if (arc.getTwin() != null) {
			arrowType = ARROW_BOTH;
		}

		StringBuilder sb = new StringBuilder();

		sb.append(ipeLine(t, c, new Line2D.Double(source, target), arrowType, SIZE_TINY, 1));

		return sb.toString();
	}

	private String writeWeights(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(weightLayerName);
		ArrayList<Integer> visitedIds = new ArrayList<Integer>();
		for (DiGraphArc<ColoredNode, ?> arc : graph.getArcs()) {
			if (!visitedIds.contains(arc.getId())
					&& (arc.getTwin() == null || !visitedIds.contains(arc.getTwin().getId())) && isInIdRange(arc)) {
				sb.append(writeWeight(arc, t, c));
				visitedIds.add(arc.getId());
			}
		}

		return sb.toString();
	}

	private String writeWeight(DiGraphArc<ColoredNode, ?> arc, IpeTransformation t, Color c) {

		Point2D source, target;
		source = arc.getSource().getNodeData();
		target = arc.getTarget().getNodeData();

		Point2D midpoint = new Point2D.Double((source.getX() + target.getX()) / 2, (source.getY() + target.getY()) / 2);

		if (arc.getArcData() == null)
			return ipeLabel(t, c, midpoint, "no data", SIZE_TINY);

		String text = arc.getArcData().toString();

		if (arc.getTwin() != null) {
			text += ";" + arc.getTwin().getArcData();
		}

		StringBuilder sb = new StringBuilder();

		sb.append(ipeLabel(t, c, midpoint, text, SIZE_TINY));

		return sb.toString();
	}

	private String writeArcIds(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(arcIdLayerName);
		for (DiGraphNode<ColoredNode, ?> node : graph.getNodes()) {
			if (isInIdRange(node))
				sb.append(writeArcIds(node, t, c));
		}

		return sb.toString();
	}

	private String writeArcIds(DiGraphNode<ColoredNode, ?> node, IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < node.getOutgoingArcs().size(); ++i) {
			DiGraphArc<ColoredNode, ?> arc = node.getOutgoingArcs().get(i);
			sb.append(writeOAId(arc, i, t, c));
		}

		if (node.getId() == 3)
			System.out.println();
		for (int i = 0; i < node.getIncomingArcs().size(); ++i) {
			DiGraphArc<ColoredNode, ?> arc = node.getIncomingArcs().get(i);
			sb.append(writeIAId(arc, i, t, c));
		}

		return sb.toString();
	}

	private String writeOAId(DiGraphArc<ColoredNode, ?> arc, int index, IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		double dist = 20;
		Point2D source = arc.getSource().getNodeData();
		Point2D target = arc.getTarget().getNodeData();

		double length = source.distance(target);
		double x = source.getX() + (target.getX() - source.getX()) / length * dist;
		double y = source.getY() + (target.getY() - source.getY()) / length * dist;
		Point2D position = new Point2D.Double(x, y);

		sb.append(ipeLabel(t, c, position, "" + index, SIZE_NORMAL));

		return sb.toString();
	}

	private String writeIAId(DiGraphArc<ColoredNode, ?> arc, int index, IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		double dist = 10;
		Point2D source = arc.getSource().getNodeData();
		Point2D target = arc.getTarget().getNodeData();

		double length = source.distance(target);
		double x = source.getX() + (target.getX() - source.getX()) / length * dist;
		double y = source.getY() + (target.getY() - source.getY()) / length * dist;
		Point2D position = new Point2D.Double(x, y);

		sb.append(ipeLabel(t, c, position, "" + index, SIZE_NORMAL));

		return sb.toString();
	}

	private String writeNodes(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(nodeLayerName);
		for (DiGraphNode<ColoredNode, ?> node : graph.getNodes()) {
			if (isInIdRange(node))
				sb.append(writeNode(node, t, c));
		}

		return sb.toString();
	}

	private String writeNode(DiGraphNode<ColoredNode, ?> node, IpeTransformation t, Color c) {
		Point2D position;
		position = node.getNodeData();

		StringBuilder sb = new StringBuilder();

		Color curr;
		switch (node.getNodeData().getColor()) {
		case Colored.REACHABLE:
			curr = AbstractMain.COLOR_STYLE.reachable();
			break;
		case Colored.UNREACHABLE:
			curr = AbstractMain.COLOR_STYLE.unreachable();
			break;
		case Colored.BUFFER:
			curr = AbstractMain.COLOR_STYLE.buffer();
			break;
		default:
			curr = Color.BLACK;
		}

		sb.append(ipeLabel(t, c, position, "" + node.getId(), SIZE_NORMAL));
		sb.append(ipeMarker(t, curr, position, MARKER_DISK, SIZE_NORMAL));

		return sb.toString();
	}

	private boolean isInIdRange(DiGraphArc<?, ?> arc) {
		return isInIdRange(arc.getSource()) && isInIdRange(arc.getTarget());
	}

	private boolean isInIdRange(DiGraphNode<?, ?> node) {
		return node.getId() >= minNodeId && node.getId() <= maxNodeId;
	}

	@Override
	public Envelope getBoundingBox() {
		Envelope bb = new Envelope();

		Point2D position;
		for (DiGraphNode<?, ?> node : graph.getNodes()) {
			if (isInIdRange(node)) {
				position = (Point2D) node.getNodeData();
				bb.expandToInclude(position.getX(), position.getY());
			}
		}
		return bb;
	}

	@Override
	public List<String> layerList() {
		List<String> ret = new ArrayList<>();
		ret.add(arcLayerName);
		ret.add(weightLayerName);
		ret.add(nodeLayerName);
		ret.add(arcIdLayerName);
		return ret;
	}

}
