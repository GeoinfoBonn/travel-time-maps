package util.structures;

import java.security.InvalidParameterException;
import java.util.ArrayList;

import util.geometry.Envelope;

public class QuadTree<V> {

	private QTNode root;

	private double eps = 1e-9;

	int size = 0;
	int qt_depth = 0;

	/**
	 * Constructor for a quad tree managing objects in the euclidean plane.
	 * 
	 * @param e Bounded region managed by this quad tree.
	 */
	public QuadTree(Envelope e) {
		root = new QTNode(e);

		if (!e.isBounded()) {
			throw new InvalidParameterException("Only bounded regions are allowed.");
		}

		root.depth = 0;
		qt_depth = 0;
	}

	/**
	 * Constructor for a quad tree managing objects in the euclidean plane. When an
	 * element at some coordinates (x, y) is requested, it returns the closest
	 * element at most eps from (x, y).
	 * 
	 * @param e   Bounded region managed by this quad tree.
	 * @param eps Precision of this quad tree concerning requests.
	 */
	public QuadTree(Envelope e, double eps) {
		this(e);
		this.eps = eps;
	}

	/**
	 * Returns the number of elements in this tree.
	 * 
	 * @return size of this tree.
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns the depth of this tree.
	 * 
	 * @return depth of this tree.
	 */
	public int depth() {
		return qt_depth;
	}

	/**
	 * Returns the coordinates of the extent of the first level of this quad tree.
	 * 
	 * @return [minX,maxX,minY,maxY]
	 */
	public double[] getExtent() {
		return new double[] { root.e.getxMin(), root.e.getxMax(), root.e.getyMin(), root.e.getyMax() };
	}

	/**
	 * Adds the specified element to this quadtree if it is not already present.
	 * More formally, adds the specified element e to this set if the set contains
	 * no element e2 such that (e==null ? e2==null : e.equals(e2)). If this quadtree
	 * already contains the element, the call leaves the set unchanged and returns
	 * false.
	 * 
	 * @param v element to be added to this quadtree
	 * @param x x-coordinate of v
	 * @param y y-coordinate of v
	 * @return true if (x,y) is managed by this quadtree and it did not already
	 *         contain the specified element
	 * @throws NullPointerException if the specified element is null
	 */
	public boolean add(V v, double x, double y) throws NullPointerException {
		if (v == null) {
			throw new NullPointerException();
		}
		CoordinateContainer ccv = new CoordinateContainer(v, x, y);
		if (!root.withinEnv(ccv)) {
			return false;
		}

		if (size == 0) {
			root.ccv = ccv;
			++size;
			return true;
		}
		return root.add(ccv);
	}

	/**
	 * 
	 * 
	 * @param x
	 * @param y
	 * @param v
	 */
	public void removeNode(double x, double y, V v) {
		QTNode candidate = findQTNode(new CoordinateContainer(v, x, y));
		if (candidate.ccv.v.equals(v)) {
			candidate.ccv = null;
		}
	}

	/**
	 * 
	 * 
	 * @param ccv
	 * @return
	 */
	private QTNode findQTNode(CoordinateContainer ccv) {
		QTNode qtn = root.findQTNode(ccv.x, ccv.y);
		if (qtn.ccv.v.equals(ccv.v)) {
			return qtn;
		}
		return null;
	}

	/**
	 * Returns the element close to the given coordinates. More precisely, it
	 * returns the element contained in the quadtree node containing (x,y) if its
	 * distance to (x,y) is at most this.eps.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public V getNode(double x, double y) {
		return getNode(x, y, eps);
	}

	public V getNode(double x, double y, double eps) {
		return root.getNode(x, y, eps);
	}

	public ArrayList<V> getNodes(Envelope env) {
		ArrayList<V> nodes = new ArrayList<V>();

		visitQuadTree(new QuadTreeVertexEnvelopeVisitor<V>() {

			@Override
			public boolean visit(V v, Envelope e, double x, double y) {
				if (!e.intersects(env)) {
					return false;
				}
				if (v != null) {
					if ((env.getxMin() <= x && x <= env.getxMax()) && (env.getyMin() <= y && y <= env.getyMax())) {
						nodes.add(v);
					}
				}
				return true;
			}
		});

		return nodes;
	}

	public double getPrecision() {
		return eps;
	}

	public void setPrecision(double eps) {
		this.eps = eps;
	}

	public void visitQuadTree(QuadTreeVisitor<V> qtv) {
		if (root != null)
			root.visit(qtv);
	}

	public interface QuadTreeVisitor<V> {
	}

	public interface QuadTreeVertexVisitor<V> extends QuadTreeVisitor<V> {
		public boolean visit(V v, double x, double y);
	}

	public interface QuadTreeVertexEnvelopeVisitor<V> extends QuadTreeVisitor<V> {
		public boolean visit(V v, Envelope e, double x, double y);
	}

	public void reset(Envelope env) {
		root = new QTNode(env);

		if (!env.isBounded()) {
			throw new InvalidParameterException("Only bounded regions are allowed.");
		}

		root.depth = 0;
		qt_depth = 0;
	}

	private class QTNode {

		private Envelope e;
		private double centerX;
		private double centerY;

		private CoordinateContainer ccv;

		private static final int QT_NE = 0;
		private static final int QT_NW = 1;
		private static final int QT_SW = 2;
		private static final int QT_SE = 3;

		private ArrayList<QTNode> children;

		private int depth;

		private QTNode(Envelope e, QTNode parent) {
			this.e = e;
			centerX = (e.getxMax() + e.getxMin()) / 2;
			centerY = (e.getyMax() + e.getyMin()) / 2;

			// this.parent = parent;

			if (parent != null)
				depth = parent.depth + 1;
			if (qt_depth < depth) {
				qt_depth = depth;
			}
			children = new ArrayList<QTNode>(4);
			for (int i = 0; i < 4; i++)
				children.add(i, null);
		}

		private QTNode(Envelope e) {
			this(e, null);
		}

		public void visit(QuadTreeVisitor<V> qtv) {
			boolean goOn = true;
			if (ccv != null) {
				if (qtv instanceof QuadTreeVertexEnvelopeVisitor) {
					goOn = ((QuadTreeVertexEnvelopeVisitor<V>) qtv).visit(ccv.v, e, ccv.x, ccv.y);
				}
				if (qtv instanceof QuadTreeVertexVisitor) {
					goOn = ((QuadTreeVertexVisitor<V>) qtv).visit(ccv.v, ccv.x, ccv.y);
				}
			} else {
				if (qtv instanceof QuadTreeVertexEnvelopeVisitor) {
					goOn = ((QuadTreeVertexEnvelopeVisitor<V>) qtv).visit(null, e, Double.NaN, Double.NaN);
				}
			}
			if (goOn) {
				for (QTNode qtn : children) {
					if (qtn != null)
						qtn.visit(qtv);
				}
			}
		}

		public boolean add(CoordinateContainer ccv) {
			assert (withinEnv(ccv));

			// find quadrant to continue search for proper qtnode
			int loc = locate(ccv.x, ccv.y);
			QTNode qtn = children.get(loc);

			assert (qtn == null || qtn.withinEnv(ccv));

			if (qtn == null) { // search has arrived at lowest level of this quad tree with respect to thegiven
				// coordinates
				if (this.ccv == null) { // this qtnode has at least one child but not the one interesting for the given
					// ccv
					qtn = new QTNode(createSubEnvelope(loc), this);
					qtn.ccv = ccv;
					this.children.set(loc, qtn);
					++size;
					return true;
				} else { // qtnode has no children
					if (this.ccv.equals(ccv)) { // already contained in quad tree
						return false;
					}
					if (this.ccv.x == ccv.x && this.ccv.y == ccv.y) {// position full
						System.err.println(this.ccv + " within this tree and " + ccv + " have the same coordinates");
						return false;
					}

					// this qtnode's ccv is relocated in child node
					int locThis = locate(this.ccv.x, this.ccv.y);
					qtn = new QTNode(createSubEnvelope(locThis), this);
					qtn.ccv = this.ccv;
					this.ccv = null;
					this.children.set(locThis, qtn);

					if (locThis != loc) {// this qtnode's former ccv and the given ccv are located in differen child
						// nodes
						qtn = new QTNode(createSubEnvelope(loc), this);
						qtn.ccv = ccv;
						this.children.set(loc, qtn);
						++size;
						return true;
					}
				}
			}
			return qtn.add(ccv);
		}

		/**
		 * Checks whether this qtnode is responsible for the given ccv in any respect.
		 * [used for assert]
		 * 
		 * @param ccv
		 * @return true, if ccv lies within the envelope of this qtnode, false
		 *         otherwise.
		 */
		private boolean withinEnv(CoordinateContainer ccv) {
			if (e.getxMin() > ccv.x)
				return false;
			if (e.getxMax() < ccv.x)
				return false;
			if (e.getyMin() > ccv.y)
				return false;
			if (e.getyMax() < ccv.y)
				return false;
			return true;
		}

		/**
		 * Method creates the suitable envelope for this qtnode's child at the given
		 * location.
		 * 
		 * @param loc index of quadrant
		 * @return Suitable subenvelope of this qtnode's envelope
		 */
		private Envelope createSubEnvelope(int loc) {
			switch (loc) {
			case QT_NE:
				return new Envelope(centerX, e.getxMax(), centerY, e.getyMax());
			case QT_SE:
				return new Envelope(centerX, e.getxMax(), e.getyMin(), centerY);
			case QT_NW:
				return new Envelope(e.getxMin(), centerX, centerY, e.getyMax());
			case QT_SW:
				return new Envelope(e.getxMin(), centerX, e.getyMin(), centerY);
			default:
				return null;
			}
		}

		/**
		 * Determines the index of the quadrant in which (x,y) is located
		 * 
		 * @param x
		 * @param y
		 * @return Constant QT_SE, QT_SW, QT_NE or QT_NW depending on the location of
		 *         (x,y) in this qtnode's envelope
		 */
		private int locate(double x, double y) {
			if (x < centerX) { // w
				if (y < centerY) { // s
					return QT_SW;
				} else { // n
					return QT_NW;
				}
			} else { // e
				if (y < centerY) { // s
					return QT_SE;
				} else { // n
					return QT_NE;
				}
			}
		}

		/**
		 * This method returns a node close to the given (x,y). More precisely, it
		 * returns the element of the qtnode containing (x,y) if it is at most eps from
		 * (x,y).
		 * 
		 * @param x
		 * @param y
		 * @param eps
		 * @return
		 */
		public V getNode(double x, double y, double eps) {
			QTNode child = children.get(locate(x, y));
			if (child == null) {
				if (this.ccv != null) {
					if (ccv.isNearBy(x, y, eps)) {
						return ccv.v;
					}
				}
				return null;
			}
			return child.getNode(x, y, eps);
		}

		public QTNode findQTNode(double x, double y) {
			QTNode child = children.get(locate(x, y));
			if (child == null) {
				return this;
			}
			return child.findQTNode(x, y);
		}

		@Override
		public String toString() {
			String s = "O";
			if (ccv != null) {
				s = "X";
			}
			return s + "[x:" + e.getxMin() + "--" + e.getxMax() + "; y:" + e.getyMin() + "--" + e.getyMax() + "]";
		}

	}

	private class CoordinateContainer {
		private V v;
		private double x;
		private double y;

		private CoordinateContainer(V v, double x, double y) {
			this.v = v;
			this.x = x;
			this.y = y;
		}

		private boolean isNearBy(double x, double y, double eps) {
			if (Math.abs(this.x - x) <= eps) {
				if (Math.abs(this.y - y) <= eps) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return v.toString() + " (" + x + "," + y + ")";
		}
	}

	public boolean handles(Envelope env) {
		return this.root.e.contains(env);
	}

	public Envelope getEnvelope() {
		return root.e;
	}

}
