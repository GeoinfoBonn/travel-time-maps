package ipeio.api;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ipeio.api.IpeObject;
import ipeio.api.IpeObject.Document;
import ipeio.api.IpeObject.HasLayer;

public class IpeFactory {

	private StringBuffer log = new StringBuffer();

	public IpeObject create(String name, String content, Map<String, String> attributes, List<IpeObject> children,
			AffineTransform trans, boolean directTransform, String currentLayer) {
		IpeObject object = create(name, content, attributes, children, trans, directTransform);
		if (object != null && object instanceof HasLayer) {
			((HasLayer) object).setLayer(currentLayer);
		}
		return object;
	}

	private IpeObject create(String name, String content, Map<String, String> attributes, List<IpeObject> children,
			AffineTransform trans, boolean directTransform) {
		if (name.equals("page")) {
			return constructPage(name, attributes, children);
		} else if (name.equals("path")) {
			return constructPath(name, content.toString(), attributes, children, trans, directTransform);
		} else if (name.equals("ipe")) {
			return new Document(name, attributes, children);
		} else if (name.equals("group")) {
			return constructGroup(name, attributes, children, trans, directTransform);
		} else if (name.equals("use")) {
			return constructMark(name, attributes, children, trans, directTransform);
		} else if (name.equals("text") && attributes.containsKey("type") && attributes.get("type").equals("label")) {
			return constructLabel(name, attributes, children, trans, content, directTransform);
		} else {
			log.append("IpeFactory: object " + name + " is not supported.\n");
			return null;
		}
	}

	private IpeObject constructLabel(String name, Map<String, String> attributes, List<IpeObject> children,
			AffineTransform trans, String text, boolean directTransform) {

		double pos[] = convert(attributes.get("pos").split(" "));
		if (directTransform) {
			return new IpeObject.Label(name, attributes, text,
					trans.transform(new Point2D.Double(pos[0], pos[1]), null));
		}
		return new IpeObject.Label(name, attributes, text, new Point2D.Double(pos[0], pos[1]), trans);
	}

	private IpeObject constructGroup(String name, Map<String, String> attributes, List<IpeObject> children,
			AffineTransform trans, boolean directTransform) {
		if (directTransform) {
			return new IpeObject.Group(name, attributes, children, new AffineTransform());
		}
		return new IpeObject.Group(name, attributes, children, trans);
	}

	private IpeObject constructMark(String name, Map<String, String> attributes, List<IpeObject> children,
			AffineTransform trans, boolean directTransform) {
		if (attributes.containsKey("name") && attributes.get("name").startsWith("mark")) {
			double pos[] = convert(attributes.get("pos").split(" "));
			Shape shape = new Ellipse2D.Double(pos[0] - 2, pos[1] - 2, 4, 4);
			if (directTransform) {
				return new IpeObject.Geometry(name, attributes, trans.createTransformedShape(shape));
			}
			return new IpeObject.Geometry(name, attributes, shape, trans);
		}
		return null;
	}

	private IpeObject constructPage(String name, Map<String, String> objAttributes, List<IpeObject> children) {
		return new IpeObject.Page(name, objAttributes, children);
	}

	private Point2D toPoint(String[] words) {
		return new Point2D.Double(Double.parseDouble(words[0]), Double.parseDouble(words[1]));
	}

	private void moveTo(Path2D path, Point2D p) {
		path.moveTo(p.getX(), p.getY());
	}

	private void lineTo(Path2D path, Point2D p) {
		path.lineTo(p.getX(), p.getY());
	}

	private void quadTo(Path2D path, List<Point2D> bezierPoints) {
		path.quadTo(bezierPoints.get(0).getX(), bezierPoints.get(0).getY(), bezierPoints.get(1).getX(),
				bezierPoints.get(1).getY());
	}

	private void cubicTo(Path2D path, List<Point2D> bezierPoints) {
		path.curveTo(bezierPoints.get(0).getX(), bezierPoints.get(0).getY(), bezierPoints.get(1).getX(),
				bezierPoints.get(1).getY(), bezierPoints.get(2).getX(), bezierPoints.get(2).getY());
	}

	private double[] convert(String[] words) {
		double c[] = new double[words.length];
		for (int i = 0; i < words.length; i++) {
			c[i] = Double.parseDouble(words[i]);
		}
		return c;
	}

	private String[] restrict(String[] array, int begin, int end) {
		if (end <= 0)
			end = array.length + end;
		String[] res = new String[end - begin];
		int j = begin;
		for (int i = 0; i < res.length; i++) {
			res[i] = array[j];
			j++;
		}
		return res;
	}

	private boolean isNumeric(String input) {
		try {
			Double.parseDouble(input);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}

	private IpeObject constructPath(String name, String content, Map<String, String> attributes,
			List<IpeObject> children, AffineTransform trans, boolean directTransform) {

		String[] lines = content.split("\n");

		Path2D path = new Path2D.Double();
		Ellipse2D ellipse = null;
		List<Point2D> bezierPoints = new ArrayList<>();
		for (String line : lines) {
			if (line != null && !line.equals("")) {
				String words[] = line.split(" ");
				if (words.length == 0) {
					continue;
				}
				String last = words[words.length - 1];
				if (last.equals("m")) {
					moveTo(path, toPoint(words));
				} else if (last.equals("l")) {
					lineTo(path, toPoint(words));
				} else if (last.equals("h")) {
					path.closePath();
				} else if (last.equals("e")) {
					if (line.endsWith(" e")) { // found ellipse
						double[] c = convert(restrict(words, 0, -1));
						ellipse = new Ellipse2D.Double(c[4] - c[0], c[5] - c[3], c[0] * 2, c[3] * 2);
						path.append(ellipse, false);
					}
				} else if (isNumeric(last) && bezierPoints.size() < 2) {
					bezierPoints.add(toPoint(words));
				} else if (last.equals("c")) {
					bezierPoints.add(toPoint(words));
					if (bezierPoints.size() == 2) {
						quadTo(path, bezierPoints);
					} else {
						cubicTo(path, bezierPoints);
					}
					bezierPoints = new ArrayList<>();
				} else {
					log.append("command " + last + " not supported\n");
					return null;
				}
			}
		}
		Shape shape = ellipse == null ? path : ellipse;
		if (directTransform) {
			return new IpeObject.Geometry(name, attributes, trans.createTransformedShape(shape));
		}
		return new IpeObject.Geometry(name, attributes, shape, trans);
	}

	@Override
	public String toString() {
		return log.toString();
	}

}
