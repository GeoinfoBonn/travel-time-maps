package graph.planarizer.sweep;

import java.util.Comparator;

public class LineSegmentComparator implements Comparator<LineSegment> {

	public Point sweepLinePosition;
	private static final double EPS = 1e-6;

	public LineSegmentComparator(Point sweepLinePosition) {
		this.sweepLinePosition = sweepLinePosition;
	}

	public void setSweepLinePosition(Point sweepLinePosition) {
		this.sweepLinePosition = sweepLinePosition;
	}

	@Override
	public int compare(LineSegment l1, LineSegment l2) {

		double y1 = l1.getY(sweepLinePosition);
		double y2 = l2.getY(sweepLinePosition);
		if (Math.abs(y2 - y1) > EPS) {
			return Double.compare(y1, y2);
		}

		// y1 == y2
		double m1 = l1.getSlope();
		double m2 = l2.getSlope();
		if (y1 <= sweepLinePosition.getY() + EPS) {
			return Double.compare(m1, m2);
		}
		if (y1 > sweepLinePosition.getY()) {
			return Double.compare(m2, m1);
		}
		return 0;
	}
}
