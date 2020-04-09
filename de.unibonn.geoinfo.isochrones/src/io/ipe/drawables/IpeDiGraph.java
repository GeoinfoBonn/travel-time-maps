package io.ipe.drawables;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import ipeio.api.IpeDrawable;
import ipeio.api.IpeTransformation;
import util.geometry.Envelope;

/**
 * Wrapper for DiGraph (including SuperGraph) to be used by the IpeWriter to
 * print the graph into an ipe-document.
 * 
 * @author forsch
 *
 */
public class IpeDiGraph extends IpeDrawable {

	DiGraph<?, ?> graph;
	boolean isSuperGraph;

	String arcLayerName;
	String weightLayerName;
	String nodeLayerName;

	int minNodeId;
	int maxNodeId;

	public IpeDiGraph(DiGraph<?, ?> graph, String name, int minNodeId, int maxNodeId) {
		isSuperGraph = false;

		if (!(graph.getNode(0).getNodeData() instanceof Point2D)) {
			if (!(graph.getNode(0).getNodeData() instanceof DiGraphNode)) {
				throw new IllegalArgumentException(
						"Input graph is not a super graph nor does it have node data of type Point2D");
			}
			if (!(((DiGraphNode<?, ?>) graph.getNode(0).getNodeData()).getNodeData() instanceof Point2D)) {
				throw new IllegalArgumentException(
						"Input graph is not a super graph nor does it have node data of type Point2D");
			} else {
				isSuperGraph = true;
			}
		}

		this.graph = graph;
		this.arcLayerName = name + "_arcs";
		this.weightLayerName = name + "_weights";
		this.nodeLayerName = name + "_nodes";
		this.minNodeId = minNodeId;
		this.maxNodeId = maxNodeId;
	}

	public IpeDiGraph(DiGraph<?, ?> graph, String name) {
		this(graph, name, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public String toIpeString(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();
		sb.append(writeArcs(t, c));
		sb.append(writeWeights(t, c));
		sb.append(writeNodes(t, c));
		return sb.toString();
	}

	private String writeArcs(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(arcLayerName);
		ArrayList<Integer> visitedIds = new ArrayList<Integer>();
		for (DiGraphArc<?, ?> arc : graph.getArcs()) {
			if (!visitedIds.contains(arc.getId())
					&& (arc.getTwin() == null || !visitedIds.contains(arc.getTwin().getId())) && isInIdRange(arc)) {
				sb.append(writeArc(arc, t, c));

				visitedIds.add(arc.getId());
			}
		}

		return sb.toString();
	}

	private String writeArc(DiGraphArc<?, ?> arc, IpeTransformation t, Color c) {
		Point2D source, target;
		if (!isSuperGraph) {
			source = (Point2D) arc.getSource().getNodeData();
			target = (Point2D) arc.getTarget().getNodeData();
		} else {
			source = (Point2D) ((DiGraphNode<?, ?>) arc.getSource().getNodeData()).getNodeData();
			target = (Point2D) ((DiGraphNode<?, ?>) arc.getTarget().getNodeData()).getNodeData();
		}

		byte arrowType = ARROW_FORWARD;
		if (arc.getTwin() != null) {
			arrowType = ARROW_BOTH;
		}

		StringBuilder sb = new StringBuilder();

		sb.append(ipeLine(t, c, new Line2D.Double(source, target), arrowType, SIZE_SMALL, 1));

		return sb.toString();
	}

	private String writeWeights(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(weightLayerName);
		ArrayList<Integer> visitedIds = new ArrayList<Integer>();
		for (DiGraphArc<?, ?> arc : graph.getArcs()) {
			if (!visitedIds.contains(arc.getId())
					&& (arc.getTwin() == null || !visitedIds.contains(arc.getTwin().getId())) && isInIdRange(arc)) {
				sb.append(writeWeight(arc, t, c));
				visitedIds.add(arc.getId());
			}
		}

		return sb.toString();
	}

	private String writeWeight(DiGraphArc<?, ?> arc, IpeTransformation t, Color c) {
		Point2D source, target;
		if (!isSuperGraph) {
			source = (Point2D) arc.getSource().getNodeData();
			target = (Point2D) arc.getTarget().getNodeData();
		} else {
			source = (Point2D) ((DiGraphNode<?, ?>) arc.getSource().getNodeData()).getNodeData();
			target = (Point2D) ((DiGraphNode<?, ?>) arc.getTarget().getNodeData()).getNodeData();
		}

		Point2D midpoint = new Point2D.Double((source.getX() + target.getX()) / 2, (source.getY() + target.getY()) / 2);

		String text = arc.getArcData().toString();

		if (arc.getTwin() != null) {
			text += ";" + arc.getTwin().getArcData();
		}

		StringBuilder sb = new StringBuilder();

		sb.append(ipeLabel(t, c, midpoint, text, SIZE_TINY));

		return sb.toString();
	}

	private String writeNodes(IpeTransformation t, Color c) {
		StringBuilder sb = new StringBuilder();

		changeLayer(nodeLayerName);
		for (DiGraphNode<?, ?> node : graph.getNodes()) {
			if (isInIdRange(node))
				sb.append(writeNode(node, t, c));
		}

		return sb.toString();
	}

	private String writeNode(DiGraphNode<?, ?> node, IpeTransformation t, Color c) {
		Point2D position;
		position = (Point2D) node.getNodeData();

		StringBuilder sb = new StringBuilder();

		sb.append(ipeLabel(t, c, position, "" + node.getId(), SIZE_NORMAL));
		sb.append(ipeMarker(t, c, position, MARKER_DISK, SIZE_NORMAL));

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
		return ret;
	}

}
