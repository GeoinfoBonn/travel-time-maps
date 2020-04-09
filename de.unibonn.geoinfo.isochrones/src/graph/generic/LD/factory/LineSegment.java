package graph.generic.LD.factory;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.TreeSet;

import graph.generic.DiGraph.DiGraphArc;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;

public class LineSegment extends Line2D.Double implements Comparable<LineSegment> {

	private static final long serialVersionUID = -2801015341267119124L;
	public static final int INCOMING_ARC = 1;
	public static final int OUTGOING_ARC = 2;
	public static final int INCOMING_OVERLAY = 3;
	public static final int OUTGOING_OVERLAY = 4;

	int type;

	public LineSegment(DiGraphArc<?, ?> arc, int type) {
		boolean poiIsSource;
		if (type == INCOMING_ARC || type == INCOMING_OVERLAY) {
			poiIsSource = false;
		} else if (type == OUTGOING_ARC || type == OUTGOING_OVERLAY) {
			poiIsSource = true;
		} else {
			throw new IllegalArgumentException("Unknown type!");
		}

		if (poiIsSource)
			this.setLine((Point2D) arc.getSource().getNodeData(), (Point2D) arc.getTarget().getNodeData());
		else
			this.setLine((Point2D) arc.getTarget().getNodeData(), (Point2D) arc.getSource().getNodeData());

		this.type = type;
	}

	public LineSegment(Point2D source, Point2D target, int type) {
		boolean poiIsSource;
		if (type == INCOMING_ARC || type == INCOMING_OVERLAY) {
			poiIsSource = false;
		} else if (type == OUTGOING_ARC || type == OUTGOING_OVERLAY) {
			poiIsSource = true;
		} else {
			throw new IllegalArgumentException("Unknown type!");
		}

		if (poiIsSource)
			this.setLine(source, target);
		else
			this.setLine(target, source);

		this.type = type;
	}

	@Override
	public String toString() {
		String s = this.getP1() + " -> " + this.getP2() + ": ";
		if (type == INCOMING_ARC)
			s += "INCOMING_ARC";
		else if (type == INCOMING_OVERLAY)
			s += "INCOMING_OVERLAY";
		else if (type == OUTGOING_ARC)
			s += "OUTGOING_ARC";
		else if (type == OUTGOING_OVERLAY)
			s += "OUTGOING_OVERLAY";
		return s;
	}

	@Override
	public int compareTo(LineSegment o) {
		return java.lang.Double.compare(this.getInclination(), o.getInclination());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LineSegment) {
			LineSegment o = (LineSegment) obj;
			return java.lang.Double.compare(this.getInclination(), o.getInclination()) == 0;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getInclination());
	}

	public double getInclination() {
		double inc = 0;
		double Y = 0;
		double X = 0;

		Y = getP2().getY() - getP1().getY();
		X = getP2().getX() - getP1().getX();

		inc = Math.atan2(Y, X);

		if (inc >= Math.PI / 2 && inc <= Math.PI) {

			inc -= Math.PI / 2;
		} else {
			inc += 3 * Math.PI / 2;
		}

		return inc;
	}

	public int getType() {
		return type;
	}

	public static TreeSet<LineSegment> compareSet(DiGraphArc<Point2D, VisualizationEdge> incomingArc,
			DiGraphArc<Point2D, VisualizationEdge> outgoingArc,
			List<DiGraphArc<ColoredNode, GeofabrikData>> overlayIncoming,
			List<DiGraphArc<ColoredNode, GeofabrikData>> overlayOutgoing) {
		if (incomingArc == null && outgoingArc == null) {
			throw new IllegalArgumentException("Either incoming or outgoing arc must be non-null");
		}

		if (outgoingArc != null && incomingArc != null && outgoingArc.getSource() != incomingArc.getTarget())
			throw new IllegalArgumentException("Incoming and outgoing arc are not adjacent!");

		Point2D pointOfInterest;
		if (incomingArc != null)
			pointOfInterest = incomingArc.getTarget().getNodeData();
		else
			pointOfInterest = outgoingArc.getSource().getNodeData();

		if (overlayIncoming.stream().filter(x -> x.getTarget().getNodeData().getX() != pointOfInterest.getX()
				|| x.getTarget().getNodeData().getY() != pointOfInterest.getY()).count() > 0)
			throw new IllegalArgumentException("Not all incoming arcs are adjacent to input.");

		if (overlayOutgoing.stream().filter(x -> x.getSource().getNodeData().getX() != pointOfInterest.getX()
				|| x.getSource().getNodeData().getY() != pointOfInterest.getY()).count() > 0)
			throw new IllegalArgumentException("Not all outgoing arcs are adjacent to input.");

		TreeSet<LineSegment> segments = new TreeSet<>();
		if (incomingArc != null)
			segments.add(new LineSegment(incomingArc, LineSegment.INCOMING_ARC));
		if (outgoingArc != null)
			segments.add(new LineSegment(outgoingArc, LineSegment.OUTGOING_ARC));
		overlayIncoming.forEach(x -> segments.add(new LineSegment(x, LineSegment.INCOMING_OVERLAY)));
		overlayOutgoing.forEach(x -> segments.add(new LineSegment(x, LineSegment.OUTGOING_OVERLAY)));

		return segments;
	}

	public static TreeSet<LineSegment> compareSet2(DiGraphArc<Point2D, VisualizationEdge> incomingArc,
			DiGraphArc<Point2D, VisualizationEdge> outgoingArc, List<ColoredNode> overlayIncomingSource,
			List<ColoredNode> overlayOutgoingTarget) {
		if (incomingArc == null && outgoingArc == null) {
			throw new IllegalArgumentException("Either incoming or outgoing arc must be non-null");
		}

		if (outgoingArc != null && incomingArc != null && outgoingArc.getSource() != incomingArc.getTarget())
			throw new IllegalArgumentException("Incoming and outgoing arc are not adjacent!");

		Point2D pointOfInterest;
		if (incomingArc != null)
			pointOfInterest = incomingArc.getTarget().getNodeData();
		else
			pointOfInterest = outgoingArc.getSource().getNodeData();

		TreeSet<LineSegment> segments = new TreeSet<>();
		if (incomingArc != null)
			segments.add(new LineSegment(incomingArc, LineSegment.INCOMING_ARC));
		if (outgoingArc != null)
			segments.add(new LineSegment(outgoingArc, LineSegment.OUTGOING_ARC));
		overlayIncomingSource
				.forEach(x -> segments.add(new LineSegment(x, pointOfInterest, LineSegment.INCOMING_OVERLAY)));
		overlayOutgoingTarget
				.forEach(x -> segments.add(new LineSegment(pointOfInterest, x, LineSegment.OUTGOING_OVERLAY)));

		return segments;
	}

	public static int crossingDirection(TreeSet<LineSegment> segments) {
		if (!crosses(segments))
			return 0;

		boolean incFound = false;
		boolean outFound = false;
		for (Iterator<LineSegment> i = segments.iterator(); i.hasNext();) {
			LineSegment segment = i.next();

			if (segment.getType() == LineSegment.INCOMING_ARC) {
				incFound = true;
			}

			if (segment.getType() == LineSegment.OUTGOING_ARC) {
				outFound = true;
			}

			if (incFound && outFound) {
				break;
			}

			// from reachable node
			if (segment.getType() == LineSegment.INCOMING_OVERLAY) {
				if (incFound)
					return -1;
			}

			// to unreachable node
			if (segment.getType() == LineSegment.OUTGOING_OVERLAY) {
				if (outFound)
					return -1;
			}

		}

		return 1;
	}

	public static boolean crosses(TreeSet<LineSegment> segments) {

		boolean first = true;
		boolean compareArcFirst = false;
		boolean compareArcLast = false;
		boolean lastWasCompareArc = false;
		boolean compareArcsAreNeighbors = false;
		for (Iterator<LineSegment> i = segments.iterator(); i.hasNext();) {
			LineSegment segment = i.next();

			if (segment.getType() <= LineSegment.OUTGOING_ARC) {
				if (first) {
					compareArcFirst = true;
				}

				if (!i.hasNext()) {
					compareArcLast = true;
				}

				if (lastWasCompareArc) {
					compareArcsAreNeighbors = true;
					break;
				}

				lastWasCompareArc = true;
			} else {
				lastWasCompareArc = false;
			}

			first = false;
		}

		if (compareArcFirst && compareArcLast) {
			compareArcsAreNeighbors = true;
		}

		if (compareArcsAreNeighbors)
			return false;
		return true;
	}

	public static int arrivalDirection(TreeSet<LineSegment> segments) {
		boolean prevWasIn = false;
		OptionalInt prev = OptionalInt.empty();
		OptionalInt next = OptionalInt.empty();
		OptionalInt finalPrev = OptionalInt.empty();
		for (Iterator<LineSegment> i = segments.iterator(); i.hasNext();) {
			LineSegment segment = i.next();

			if (segment.getType() == LineSegment.INCOMING_ARC) {
				if (prev.isPresent())
					finalPrev = prev;
				prevWasIn = true;
				continue;
			}

			if (prevWasIn) {
				next = OptionalInt.of(segment.getType());
				if (finalPrev.isPresent())
					break;
			}

			if (!i.hasNext() && finalPrev.isEmpty()) {
				finalPrev = OptionalInt.of(segment.getType());
			}

			prev = OptionalInt.of(segment.getType());
			prevWasIn = false;
		}

		if (next.isEmpty())
			next = OptionalInt.of(segments.first().getType());

		if (prev.getAsInt() == next.getAsInt()) {
			System.err.println("Edge does not split reachable and unreachable part!");
			return 0;
		}

		if (prev.getAsInt() == LineSegment.INCOMING_OVERLAY) {
			return 1;
		} else if (prev.getAsInt() == LineSegment.OUTGOING_OVERLAY) {
			return -1;
		} else {
			System.err.println("ERROR");
			return -11111;
		}
	}

	public static int exitDirection(TreeSet<LineSegment> segments) {
		boolean prevWasOut = false;
		OptionalInt prev = OptionalInt.empty();
		OptionalInt next = OptionalInt.empty();
		OptionalInt finalPrev = OptionalInt.empty();
		for (Iterator<LineSegment> i = segments.iterator(); i.hasNext();) {
			LineSegment segment = i.next();

			if (segment.getType() == LineSegment.OUTGOING_ARC) {
				if (prev.isPresent())
					finalPrev = prev;
				prevWasOut = true;
				continue;
			}

			if (prevWasOut) {
				next = OptionalInt.of(segment.getType());
				if (finalPrev.isPresent())
					break;
			}

			if (!i.hasNext() && finalPrev.isEmpty()) {
				finalPrev = OptionalInt.of(segment.getType());
			}

			prev = OptionalInt.of(segment.getType());
			prevWasOut = false;
		}

		if (next.isEmpty())
			next = OptionalInt.of(segments.first().getType());

		if (prev.getAsInt() == next.getAsInt()) {
			System.err.println("Edge does not split reachable and unreachable part!");
			return 0;
		}

		if (prev.getAsInt() == LineSegment.INCOMING_OVERLAY) {
			return 1;
		} else if (prev.getAsInt() == LineSegment.OUTGOING_OVERLAY) {
			return -1;
		} else {
			System.err.println("ERROR");
			return -11111;
		}
	}
}