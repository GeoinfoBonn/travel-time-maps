package util.tools;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import util.geometry.Envelope;

public class Util {

	public static final int ORIENATION_VERTICAL = 0;
	public static final int ORIENATION_DIAG_DOWN = 1;
	public static final int ORIENATION_HORIZONTAL = 2;
	public static final int ORIENATION_DIAG_UP = 3;

	/**
	 * Simple PointComparator for comparing two Point2D.Double. It is sorting the
	 * points along x-axis. If x is equal the sorting extends to the y-axis.
	 * 
	 * @author prott
	 *
	 */
	public static class PointComparator implements Comparator<Point2D>, Serializable {

		private static final long serialVersionUID = -1068221579856278192L;

		@Override
		public int compare(Point2D o1, Point2D o2) {
			if (Math.abs(o1.getX() - o2.getX()) < 1e-12 && Math.abs(o1.getY() - o2.getY()) < 1e-12)
				return 0;
			if (Double.compare(o1.getX(), o2.getX()) == -1) {
				return -1;
			} else if (Double.compare(o1.getX(), o2.getX()) == 1) {
				return 1;
			} else {
				if (Double.compare(o1.getY(), o2.getY()) == -1) {
					return -1;
				} else if (Double.compare(o1.getY(), o2.getY()) == 1) {
					return 1;
				}
			}
			return 0;
		}
	}

	public static boolean pointIsOnLine(Point2D v, Point2D s, Point2D t) {
		return pointIsOnLine(v, s, t, 1e-6);
	}

	public static boolean pointIsOnLine(Point2D v, Point2D s, Point2D t, double eps) {
		if ((s.getX() <= v.getX()) && (v.getX() <= t.getX()) || (s.getX() >= v.getX()) && (v.getX() >= t.getX())) {
			if ((s.getY() <= v.getY()) && (v.getY() <= t.getY()) || (s.getY() >= v.getY()) && (v.getY() >= t.getY())) {
				double dist = Math.abs(
						s.getY() + ((v.getX() - s.getX()) / (t.getX() - s.getX())) * (t.getY() - s.getY()) - v.getY());
//				if (dist < 1)
//					System.out.println("Dist " + String.format("%10.8f", dist));
				if (dist < eps) {
					return true;
				}
			}
		}
		return false;
	}

	public static double getInclination(Point2D source, Point2D target) {
		double inc = 0;

		double Y = target.getY() - source.getY();
		double X = target.getX() - source.getX();

		if (source == target) {
			return 0;
		}

		inc = Math.atan2(Y, X);

		if (inc >= Math.PI / 2 && inc <= Math.PI) {

			inc -= Math.PI / 2;
		} else {
			inc += 3 * Math.PI / 2;
		}

		return inc;
	}

	public static int closestOrientation(Point2D source, Point2D target) {
		double angle = Util.getInclination(source, target);
		return closestOrientation(angle);
	}

	public static int closestOrientation(double angle) {
		return (int) Math.round(angle / Math.PI * 4) % 4;
	}

	public static Point2D normedVector(Point2D p1, Point2D p2) {
		double dx = p2.getX() - p1.getX();
		double dy = p2.getY() - p1.getY();
		double norm = Math.sqrt(dx * dx + dy * dy);
		dx /= norm;
		dy /= norm;
		return new Point2D.Double(dx, dy);
	}

	public static Point2D lonlat2utm(double lon, double lat) {
		return lonlat2utm(lon, lat, true);
	}

	public static Point2D lonlat2utm(double lon, double lat, boolean offsetE) {
		// GRS80-Ellipsoid
		double g0 = 111132.952547; // m/rad
		double g2 = -16038.5088; // m
		double g4 = 16.8326; // m
		double g6 = -0.0220; // m
		double a = 6378137.00;
		double b = 6356752.314;
		double rho = 180 / Math.PI;
		double c = Math.pow(a, 2) / b;
		double m = 0.9996;
		double L0 = 9; // in Grad (f�r Deutschland: 3,9 oder 15)
		double E0 = ((L0 + 3) / 6 + 30.5) * 1e6;

		double nu = (Math.pow(a, 2) - Math.pow(b, 2)) / Math.pow(b, 2) * Math.pow(Math.cos(lat * Math.PI / 180), 2);
		double t = Math.tan(lat * Math.PI / 180);
		double N_s = c / Math.sqrt(1 + nu);
		double dL = lon - L0;
		double G = g0 * lat + g2 * Math.sin(2 * lat * Math.PI / 180) + g4 * Math.sin(4 * lat * Math.PI / 180)
				+ g6 * Math.sin(6 * lat * Math.PI / 180);
		double eins = m / rho * N_s * Math.cos(lat * Math.PI / 180);
		double drei = m / (6 * Math.pow(rho, 3)) * N_s * Math.pow(Math.cos(lat * Math.PI / 180), 3)
				* (1 - Math.pow(t, 2) + nu);
		double fuenf = m / (120 * Math.pow(rho, 5)) * N_s * Math.pow(Math.cos(lat * Math.PI / 180), 5)
				* (5 - 18 * Math.pow(t, 2) + Math.pow(t, 4) + nu * (14 - 58 * Math.pow(t, 2)));
		double zwei = m / (2 * Math.pow(rho, 2)) * N_s * Math.pow(Math.cos(lat * Math.PI / 180), 2) * t;
		double vier = m / (24 * Math.pow(rho, 4)) * N_s * Math.pow(Math.cos(lat * Math.PI / 180), 4) * t
				* (5 - Math.pow(t, 2) + 9 * nu);
		double sechs = m / (720 * Math.pow(rho, 6)) * N_s * Math.pow(Math.cos(lat * Math.PI / 180), 6) * t
				* (61 - 58 * Math.pow(t, 2) + Math.pow(t, 4));
		double E = E0 + eins * dL + drei * Math.pow(dL, 3) + fuenf * Math.pow(dL, 5);
		double N = m * G + zwei * Math.pow(dL, 2) + vier * Math.pow(dL, 4) + sechs * Math.pow(dL, 6);
		// E: erste zwei Ziffern = Zone

		if (offsetE) {
			E -= 32e6;
		}

		return new Point2D.Double(E, N);
	}

	public static Point2D utm2lonlat(double x, double y) {
		return utm2lonlat(x, y, 32, 'U');
	}

	public static Point2D utm2lonlat(double x, double y, int zone, char letter) {
		double latitude;
		double longitude;
		int Zone = zone;
		char Letter = letter;
		double Easting = x > 32e6 ? x - 32e6 : x;
		double Northing = y;
		double Hem;
		if (Letter > 'M')
			Hem = 'N';
		else
			Hem = 'S';
		double north;
		if (Hem == 'S')
			north = Northing - 10000000;
		else
			north = Northing;
		latitude = (north / 6366197.724 / 0.9996
				+ (1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
						- 0.006739496742 * Math.sin(north / 6366197.724 / 0.9996)
								* Math.cos(north / 6366197.724 / 0.9996) * (Math
										.atan(Math
												.cos(Math
														.atan((Math
																.exp((Easting - 500000) / (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))))
																		* (1 - 0.006739496742 * Math.pow((Easting
																				- 500000)
																				/ (0.9996 * 6399593.625 / Math.sqrt(
																						(1 + 0.006739496742 * Math.pow(
																								Math.cos(north
																										/ 6366197.724
																										/ 0.9996),
																								2)))),
																				2) / 2 * Math
																						.pow(Math.cos(north
																								/ 6366197.724 / 0.9996),
																								2)
																				/ 3))
																- Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))))
																		* (1 - 0.006739496742 * Math.pow((Easting
																				- 500000)
																				/ (0.9996 * 6399593.625 / Math.sqrt(
																						(1 + 0.006739496742 * Math.pow(
																								Math.cos(north
																										/ 6366197.724
																										/ 0.9996),
																								2)))),
																				2) / 2 * Math
																						.pow(Math.cos(north
																								/ 6366197.724 / 0.9996),
																								2)
																				/ 3)))
																/ 2
																/ Math.cos((north - 0.9996 * 6399593.625 * (north
																		/ 6366197.724 / 0.9996
																		- 0.006739496742 * 3 / 4 * (north
																				/ 6366197.724 / 0.9996
																				+ Math.sin(2
																						* north / 6366197.724 / 0.9996)
																						/ 2)
																		+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3
																				* (3 * (north / 6366197.724
																						/ 0.9996
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								/ 2)
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(
																										Math.cos(north
																												/ 6366197.724
																												/ 0.9996),
																										2))
																				/ 4
																		- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27
																				* (5 * (3
																						* (north / 6366197.724 / 0.9996
																								+ Math.sin(2 * north
																										/ 6366197.724
																										/ 0.9996) / 2)
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(
																										Math.cos(north
																												/ 6366197.724
																												/ 0.9996),
																										2))
																						/ 4
																						+ Math.sin(2 * north
																								/ 6366197.724 / 0.9996)
																								* Math.pow(Math
																										.cos(north
																												/ 6366197.724
																												/ 0.9996),
																										2)
																								* Math.pow(Math.cos(
																										north / 6366197.724
																												/ 0.9996),
																										2))
																				/ 3))
																		/ (0.9996 * 6399593.625 / Math.sqrt(
																				(1 + 0.006739496742 * Math.pow(Math.cos(
																						north / 6366197.724 / 0.9996),
																						2))))
																		* (1 - 0.006739496742 * Math.pow((Easting
																				- 500000)
																				/ (0.9996 * 6399593.625 / Math.sqrt(
																						(1 + 0.006739496742 * Math.pow(
																								Math.cos(north
																										/ 6366197.724
																										/ 0.9996),
																								2)))),
																				2) / 2
																				* Math.pow(
																						Math.cos(north
																								/ 6366197.724 / 0.9996),
																						2))
																		+ north / 6366197.724 / 0.9996)))
												* Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
														- 0.006739496742 * 3 / 4 * (north / 6366197.724
																/ 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3
																* (north / 6366197.724 / 0.9996
																		+ Math.sin(2 * north / 6366197.724 / 0.9996)
																				/ 2)
																+ Math.sin(
																		2 * north / 6366197.724 / 0.9996)
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))
																/ 4
														- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3
																* (north / 6366197.724 / 0.9996
																		+ Math.sin(2 * north / 6366197.724 / 0.9996)
																				/ 2)
																+ Math.sin(2 * north / 6366197.724 / 0.9996) * Math
																		.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
																/ 4
																+ Math.sin(2 * north / 6366197.724 / 0.9996) * Math
																		.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
																		* Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2))
																/ 3))
														/ (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2))))
														* (1 - 0.006739496742
																* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2)))),
																		2)
																/ 2
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														+ north / 6366197.724 / 0.9996))
										- north / 6366197.724 / 0.9996)
								* 3 / 2)
						* (Math.atan(Math
								.cos(Math.atan((Math
										.exp((Easting - 500000)
												/ (0.9996 * 6399593.625
														/ Math.sqrt((1 + 0.006739496742
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742
														* Math.pow(
																(Easting - 500000) / (0.9996 * 6399593.625
																		/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																				Math.cos(north / 6366197.724 / 0.9996),
																				2)))),
																2)
														/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))
										- Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742
														* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
																2)
														/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)))
										/ 2
										/ Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
												- 0.006739496742 * 3 / 4
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3 * (3
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 4
												- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5 * (3
														* (north / 6366197.724 / 0.9996
																+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 4
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
														/ 3))
												/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
												* (1 - 0.006739496742 * Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2) / 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												+ north / 6366197.724 / 0.9996)))
								* Math.tan((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
										- 0.006739496742 * 3 / 4
												* (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
										+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3
												* (3 * (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 4
										- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5
												* (3 * (north / 6366197.724 / 0.9996
														+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
														+ Math.sin(2 * north / 6366197.724 / 0.9996)
																* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 4
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
												/ 3))
										/ (0.9996 * 6399593.625 / Math.sqrt((1 + 0.006739496742
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
										* (1 - 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										+ north / 6366197.724 / 0.9996))
								- north / 6366197.724 / 0.9996))
				* 180 / Math.PI;
		latitude = Math.round(latitude * 10000000);
		latitude = latitude / 10000000;
		longitude = Math
				.atan((Math
						.exp((Easting - 500000) / (0.9996 * 6399593.625 / Math
								.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1
										- 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3))
						- Math.exp(-(Easting - 500000) / (0.9996 * 6399593.625 / Math
								.sqrt((1 + 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))) * (1
										- 0.006739496742
												* Math.pow(
														(Easting - 500000) / (0.9996 * 6399593.625
																/ Math.sqrt((1 + 0.006739496742 * Math.pow(
																		Math.cos(north / 6366197.724 / 0.9996), 2)))),
														2)
												/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2) / 3)))
						/ 2
						/ Math.cos((north - 0.9996 * 6399593.625 * (north / 6366197.724 / 0.9996
								- 0.006739496742 * 3 / 4
										* (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
								+ Math.pow(0.006739496742 * 3 / 4, 2) * 5 / 3
										* (3 * (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 4
								- Math.pow(0.006739496742 * 3 / 4, 3) * 35 / 27 * (5
										* (3 * (north / 6366197.724 / 0.9996
												+ Math.sin(2 * north / 6366197.724 / 0.9996) / 2)
												+ Math.sin(2 * north / 6366197.724 / 0.9996)
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 4
										+ Math.sin(2 * north / 6366197.724 / 0.9996)
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)
												* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
										/ 3))
								/ (0.9996 * 6399593.625 / Math.sqrt((1
										+ 0.006739496742 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))))
								* (1 - 0.006739496742
										* Math.pow((Easting - 500000) / (0.9996 * 6399593.625
												/ Math.sqrt((1 + 0.006739496742
														* Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2)))),
												2)
										/ 2 * Math.pow(Math.cos(north / 6366197.724 / 0.9996), 2))
								+ north / 6366197.724 / 0.9996))
				* 180 / Math.PI + Zone * 6 - 183;
		longitude = Math.round(longitude * 10000000);
		longitude = longitude / 10000000;
		return new Point2D.Double(longitude, latitude);
	}

	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	@SafeVarargs
	public static <T> T[] concatAll(T[] first, T[]... rest) {
		int totalLength = first.length;
		for (T[] array : rest) {
			totalLength += array.length;
		}
		T[] result = Arrays.copyOf(first, totalLength);
		int offset = first.length;
		for (T[] array : rest) {
			System.arraycopy(array, 0, result, offset, array.length);
			offset += array.length;
		}
		return result;
	}

	/**
	 * Shoots a ray starting from <code>direction</code> passing through
	 * <code>split</code> and returns the crossing point of this ray with the
	 * bounding box. The point <code>split</code> must be inside the given bounding
	 * box.
	 * 
	 * @param split     point inside bounding box
	 * @param direction point defining the direction of the ray
	 * @param bbox      bounding box
	 * @return crossing point of ray and bounding box
	 */
	public static Point2D castRay(Point2D split, Point2D direction, Envelope bbox) {
		if (!bbox.contains(split))
			throw new IllegalArgumentException("Point split must be inside the bounding box!");

		// direction of ray
		double dx = split.getX() - direction.getX();
		double dy = split.getY() - direction.getY();

		// relevant bounds of bbox
		double X = dx > 0 ? bbox.getxMax() : bbox.getxMin();
		double Y = dy > 0 ? bbox.getyMax() : bbox.getyMin();

		// distance of split to relevant bounds
		double Dx = X - split.getX();
		double Dy = Y - split.getY();

		// how often does dx fit into distance
		double nx = Math.abs(Dx / dx);
		double ny = Math.abs(Dy / dy);
		double n = Math.min(nx, ny);

		// calculate new point
		double x = split.getX() + n * dx;
		double y = split.getY() + n * dy;

		return new Point2D.Double(x, y);
	}

	/**
	 * Calculates a new Point2d that lies left of the path defined by (in-mid-out).
	 * Assuming that polygon boundaries are given by points in counter clockwise
	 * order, this results in a point being inside the polygon.
	 * 
	 * In case the two vectors defined by [in-mid] and [mid-out] are the same (back
	 * and forth), two points are returned instead of one, forming a box around the
	 * point mid.
	 * 
	 * @param inc
	 * @param mid
	 * @param out
	 * @param d   distance the point is moved into the polygon
	 * @return one Point2D in regular case, two Point2Ds in case that vectors are
	 *         the same
	 */
	public static Point2D[] movePointIntoPolygon(Point2D in, Point2D mid, Point2D out, double d) {
		double inc1 = Util.getInclination(in, mid);
		double inc2 = Util.getInclination(mid, out);
		double dInc = inc2 - inc1;
		dInc = dInc < 0 ? dInc + 2 * Math.PI : dInc % (2 * Math.PI);

		Point2D vIn = Util.normedVector(in, mid);
		Point2D vOut = Util.normedVector(mid, out);

		double eps = 0.01; // 0.2 [rad] = 11.46 [deg]

		if (Math.abs(dInc - Math.PI) < eps) { // == pi
			Point2D ret1 = new Point2D.Double(mid.getX() + vIn.getX() * d - vIn.getY() * d,
					mid.getY() + vIn.getY() * d + vIn.getX() * d);
			Point2D ret2 = new Point2D.Double(mid.getX() + vIn.getX() * d + vIn.getY() * d,
					mid.getY() + vIn.getY() * d - vIn.getX() * d);
			return new Point2D[] { ret1, ret2 };
		} else if (Math.abs(dInc) < eps) { // == 0
			Point2D ret = new Point2D.Double(mid.getX() - vOut.getY() * d, mid.getY() + vOut.getX() * d);
			return new Point2D[] { ret };
		}

		// in case dInc is not pi or 0, it must be scaled by sin(dInc)^-1
		double d_ = d / Math.sin(dInc);
		Point2D ret = new Point2D.Double(mid.getX() + vOut.getX() * d_ - vIn.getX() * d_,
				mid.getY() + vOut.getY() * d_ - vIn.getY() * d_);

		// if moved point is too far from midpoint, move it closer so the distance gets
		// at most 3*d
		if (ret.distance(mid) > 3 * d) {
			Point2D dir = Util.normedVector(mid, ret);
			ret = new Point2D.Double(mid.getX() + 3 * d * dir.getX(), mid.getY() + 3 * d * dir.getY());
		}

		return new Point2D[] { ret };
	}

	/**
	 * Samples points along the line beginning at <code>source</code> and ending in
	 * <code>target</code>. The points are sampled equidistant with the distance
	 * between them being at most <code>maxSamplingDistance</code>. The
	 * <code>source</code> node is not returned, but <code>target</code> is returned
	 * in the final points.
	 * 
	 * @param source
	 * @param target
	 * @param maxSamplingDistance
	 * @return
	 */
	public static List<Point2D> sampleAlongLine(Point2D source, Point2D target, double maxSamplingDistance) {
		List<Point2D> sampled = new LinkedList<>();

		double dist = source.distance(target);
		int nPoints = (int) (dist / maxSamplingDistance);

		// difference between two cosecutive points
		double dx = (target.getX() - source.getX()) / (nPoints + 1);
		double dy = (target.getY() - source.getY()) / (nPoints + 1);

		// current coordinates
		double x = source.getX();
		double y = source.getY();

		// add points to result
		for (int i = 0; i < nPoints; ++i) {
			// update coordinates
			x += dx;
			y += dy;
			// add new point
			sampled.add(new Point2D.Double(x, y));
		}
		sampled.add(target);

		return sampled;
	}
}
