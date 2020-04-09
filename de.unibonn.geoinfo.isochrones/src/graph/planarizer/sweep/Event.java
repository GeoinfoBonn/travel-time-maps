package graph.planarizer.sweep;

import java.util.HashSet;
import java.util.LinkedList;

public class Event {
	
	private LinkedList<LineSegment> segmentsBegin;
	private LinkedList<LineSegment> segmentsEnd;
	private HashSet<LineSegment> segmentsCross;
	private Point eventPoint;
	
	public Event(Point eventPoint) {
		this.segmentsBegin = new LinkedList<LineSegment>();
		this.segmentsEnd = new LinkedList<LineSegment>();
		this.segmentsCross = new HashSet<LineSegment>();
		this.eventPoint = eventPoint;
	}

	public LinkedList<LineSegment> getSegmentsBegin() {
		return segmentsBegin;
	}
	public LinkedList<LineSegment> getSegmentsEnd() {
		return segmentsEnd;
	}
	public HashSet<LineSegment> getSegmentsCross() {
		return segmentsCross;
	}
	
	public Point getEventPoint() {
		return eventPoint;
	}	
}