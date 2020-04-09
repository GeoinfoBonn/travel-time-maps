package graph.generic.LD.factory;

import java.awt.geom.Point2D;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.LD.LinearDualCreator.LinearDualFactory;
import graph.types.VisualizationEdge;
import main.AbstractMain;
import util.tools.Util;

public class TurncostFactory
		implements LinearDualFactory<DiGraph<Point2D, VisualizationEdge>, Point2D, VisualizationEdge> {

	private double offsetFactor;
	public static final int WEIGHT_UTURN = 10000;

	public TurncostFactory(double offsetFactor) {
		this.offsetFactor = offsetFactor;
	}

	@Override
	public Point2D createNodeData(DiGraphArc<Point2D, VisualizationEdge> arc, int iteration) {
		Point2D source = arc.getSource().getNodeData();
		Point2D target = arc.getTarget().getNodeData();

		if (source.distance(target) < 1e-6) {
			if (arc.getId() % 2 == 0) // TODO better solution for avoiding same-location-nodes if super graph contains
				// arc with source==target
				return new Point2D.Double((source.getX() + (iteration + 1) * offsetFactor), source.getY());
			return new Point2D.Double((source.getX() - (iteration + 1) * offsetFactor), source.getY());
		}

		// Point2D location = new Point2D.Double((source.getX() + target.getX()) / 2,
		// (source.getY() + target.getY()) / 2);
		Point2D location = new Point2D.Double((1.25 * source.getX() + target.getX()) / 2.25,
				(1.25 * source.getY() + target.getY()) / 2.25);

		double orthX = target.getY() - source.getY();
		double orthY = source.getX() - target.getX();
		double orthNorm = Math.sqrt(Math.pow(orthX, 2) + Math.pow(orthY, 2));
		double x = location.getX() + (iteration + 1) * offsetFactor * orthX / orthNorm;
		double y = location.getY() + (iteration + 1) * offsetFactor * orthY / orthNorm;
		location.setLocation(x, y);

		return new Point2D.Double(location.getX(), location.getY());
	}

	@Override
	public VisualizationEdge createEdgeData(DiGraphArc<Point2D, VisualizationEdge> incomingArc,
			DiGraphArc<Point2D, VisualizationEdge> outgoingArc, boolean connectsTwinNodes) {
		Point2D pred = incomingArc.getSource().getNodeData();
		Point2D actual = incomingArc.getTarget().getNodeData();
		Point2D target = outgoingArc.getTarget().getNodeData();

		double val = computeWeight(pred, actual, target);
		return new VisualizationEdge(val, false, (byte) 0);
	}

	@Override
	public DiGraph<Point2D, VisualizationEdge> createGraph() {
		return new DiGraph<>();
	}

	protected double computeWeight(Point2D pred, Point2D actual, Point2D target) {
		double val = TurncostFactory.computeTurncosts(pred, actual, target);

		double nonOctiMalus = TurncostFactory.isOcti(pred, actual) ? 1 : AbstractMain.NON_OCTI_MALUS;
		val += pred.distance(actual) * AbstractMain.DISTANCE_FACTOR * nonOctiMalus;
		return val;
	}

	public static boolean isOcti(Point2D from, Point2D to) {
		return isOcti(Util.getInclination(from, to));
	}

	public static boolean isOcti(double inclination) {
		if (inclination < 0)
			inclination += 2 * Math.PI;
		inclination = inclination / Math.PI * 180;
		inclination = Math.round(inclination * 100) / 100.0;
		return (inclination % 45) == 0;
	}

	public static double computeTurncosts(Point2D pred, Point2D actual, Point2D target) {
		double winkel1 = Util.getInclination(pred, actual);
		double winkel2 = Util.getInclination(actual, target);
		double diffWinkel = Math.abs(winkel1 - winkel2); // Winkel im Wertebereich [0,2*pi] (Knick 1/4*pi = 3/4*pi
		// nur nach links statt rechts)

		double diffReduced = diffWinkel > Math.PI ? 2 * Math.PI - diffWinkel : diffWinkel; // Winkel im Wertebereich
		// [0,pi] (keine
		// Unterscheidung der
		if (diffReduced < (2.0 / 180.0 * Math.PI))
			return 0; // geradeaus ist 0

		if (!AbstractMain.WEIGHT_TURNS)
			return 1;

		if (diffReduced > (178.0 / 180.0 * Math.PI))
			return TurncostFactory.WEIGHT_UTURN;

		return Math.round(diffReduced / Math.PI * 4); // oktilineare Richtungen mit 1, 2, 3, 4(, WEIGHT_UTURN für
		// u-turn) bestraft
	}

	/**
	 * @return 1 if path <code>pred</code> -> <code>actual</code> ->
	 *         <code>target</code> forms a right turn, -1 if it forms a left turn, 0
	 *         in case of a straight line
	 */
	public static int direction(Point2D pred, Point2D actual, Point2D target) {
		if (computeTurncosts(pred, actual, target) == 0)
			return 0;

		double dx = actual.getX();
		double dy = actual.getY();

		double p0_x = pred.getX() - dx;
		double p1_x = actual.getX() - dx;
		double p2_x = target.getX() - dx;
		double p0_y = pred.getY() - dy;
		double p1_y = actual.getY() - dy;
		double p2_y = target.getY() - dy;

		double vx = p1_x - p0_x;
		double vy = p1_y - p0_y;
		double normV = Math.sqrt(Math.pow(vx, 2) + Math.pow(vy, 2));
		Point2D v = new Point2D.Double(vx / normV, vy / normV);

		double wx = p2_x - p1_x;
		double wy = p2_y - p1_y;
		double normW = Math.sqrt(Math.pow(wx, 2) + Math.pow(wy, 2));
		Point2D w = new Point2D.Double(wx / normW, wy / normW);

		Point2D u = new Point2D.Double(v.getY(), -v.getX());

		double direction = w.getX() * u.getX() + w.getY() * u.getY();
		if (direction > 0) {
			return 1;
		} else if (direction < 0) {
			return -1;
		} else if (Double.isNaN(direction)) {
			System.err.println("Not a Number!");
		} else {
			System.err.println("Should never happen due to test in first line of method!");
		}
		return 0;
	}
}
