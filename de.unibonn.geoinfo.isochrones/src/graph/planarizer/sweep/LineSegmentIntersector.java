package graph.planarizer.sweep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

public class LineSegmentIntersector {

	public static final boolean testing = false;
	public static final boolean plotToIpe = false;
	public static final boolean writeIntersectionFile = false;

	public static List<Event> getIntersections(Iterable<LineSegment> segs) {

		double xMean = 0;
		double yMean = 0;
		int counter = 0;
		for (LineSegment seg : segs) {
			xMean += seg.getP1().getX();
			xMean += seg.getP2().getX();
			yMean += seg.getP1().getY();
			yMean += seg.getP2().getY();
			counter++;
		}
		xMean /= (2 * counter);
		yMean /= (2 * counter);

		List<Event> intersections = new LinkedList<>();

		// initialization of event queue
		TreeMap<Point, Event> events = new TreeMap<Point, Event>();
		for (LineSegment seg : segs) {
			seg.reduce(xMean, yMean);
			Event eBegin = events.get(seg.getP1());
			if (eBegin == null) {
				eBegin = new Event(seg.getP1());
				events.put(seg.getP1(), eBegin);
			}
			eBegin.getSegmentsBegin().add(seg);

			Event eEnd = events.get(seg.getP2());
			if (eEnd == null) {
				eEnd = new Event(seg.getP2());
				events.put(seg.getP2(), eEnd);
			}
			eEnd.getSegmentsEnd().add(seg);
		}

		LineSegmentComparator statusComparator = new LineSegmentComparator(events.firstKey());
		TreeSet<LineSegment> status = new TreeSet<LineSegment>(statusComparator);

		// sweep through event queue
		while (!events.isEmpty()) {
			Entry<Point, Event> e = events.pollFirstEntry();
			Event event = e.getValue();
			if (!event.getSegmentsCross().isEmpty()) {
				intersections.add(event);
			}

			if (testing) {
				// --- output for testing only ---
				System.out.println("Event at point " + e.getKey());
				System.out.println("----------------------------");
				if (!event.getSegmentsBegin().isEmpty()) {
					System.out.print("Beginning segments: ");
					for (LineSegment seg : event.getSegmentsBegin()) {
						System.out.print(" " + seg);
						if (Integer.parseInt(seg.getName()) == 43)
							System.out.print("");
					}
					System.out.println();
				}
				if (!event.getSegmentsEnd().isEmpty()) {
					System.out.print("Ending segments:    ");
					for (LineSegment seg : event.getSegmentsEnd())
						System.out.print(" " + seg);
					System.out.println();
				}
				if (!event.getSegmentsCross().isEmpty()) {
					System.out.print("Crossing segments:  ");
					for (LineSegment seg : event.getSegmentsCross())
						System.out.print(" " + seg);
					System.out.println();
				}
				System.out.println("****************************");
				// --- output for testing only ---
			}

			LineSegment lastSegRemoved = null;

			// remove ending segments from status
			for (LineSegment seg : event.getSegmentsEnd()) {
				lastSegRemoved = seg;
				status.remove(seg);
			}
			// remove crossing segments from status
			for (LineSegment seg : event.getSegmentsCross()) {
				lastSegRemoved = seg;
				status.remove(seg);
			}

			// add new intersection between floor and ceiling
			if (lastSegRemoved != null) {
				LineSegment ceiling = status.ceiling(lastSegRemoved);
				LineSegment floor = status.floor(lastSegRemoved);
				addCrossingEvent(events, ceiling, floor, event.getEventPoint());
			}

			// advance sweep line
			statusComparator.setSweepLinePosition(event.getEventPoint());

			LineSegment firstSegAdded = null;
			if (!event.getSegmentsCross().isEmpty())
				firstSegAdded = event.getSegmentsCross().iterator().next();
			if (!event.getSegmentsBegin().isEmpty())
				firstSegAdded = event.getSegmentsBegin().getFirst();
			if (firstSegAdded != null) {
				LineSegment floor = status.floor(firstSegAdded);
				LineSegment ceiling = status.ceiling(firstSegAdded);

				// add crossing segments to status\
				status.addAll(event.getSegmentsCross());

				// add beginning segments to status
				status.addAll(event.getSegmentsBegin());

				if (floor != null) {
					LineSegment firstNewSegment = status.higher(floor);
					addCrossingEvent(events, firstNewSegment, floor, event.getEventPoint());
				}

				if (ceiling != null) {
					LineSegment lastNewSegment = status.lower(ceiling);
					addCrossingEvent(events, lastNewSegment, ceiling, event.getEventPoint());
				}
			}

		}

		if (writeIntersectionFile) {
			try (BufferedWriter bw = new BufferedWriter(new FileWriter("./intersections.csv"))) {

				for (Event e : intersections) {
					String s = e.getSegmentsCross() + "; " + (e.getEventPoint().getX() - 3.2e7) + "; "
							+ e.getEventPoint().getY() + System.lineSeparator();
					bw.write(s);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (Event e : intersections) {
			e.getEventPoint().restore(xMean, yMean);
		}
		return intersections;
	}

	public static boolean addCrossingEvent(TreeMap<Point, Event> events, LineSegment seg1, LineSegment seg2,
			Point currentPoint) {
		if (seg1 != null && seg2 != null) {
			Point p = seg1.getIntersection(seg2);
			if (p != null && p.compareTo(currentPoint) > 0) {
				Event eNew = events.get(p);
				if (eNew == null) {
					eNew = new Event(p);
					events.put(p, eNew);
				}
				eNew.getSegmentsCross().add(seg1);
				eNew.getSegmentsCross().add(seg2);
				return true;
			}
		}
		return false;
	}
}
