package ipeio.api;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.List;
 
import util.geometry.Envelope;

public abstract class IpeDrawable {

	private String currLayer = "alpha";

	public static final String MARKER_CIRCLE = "circle";
	public static final String MARKER_DISK = "disk";
	public static final String MARKER_BOX = "box";
	public static final String MARKER_SQUARE = "quare";
	public static final String MARKER_CROSS = "cross";

	public static final String SIZE_TINY = "tiny";
	public static final String SIZE_SMALL = "small";
	public static final String SIZE_NORMAL = "normal";
	public static final String SIZE_LARGE = "large";

	public static final byte ARROW_FORWARD = 1;
	public static final byte ARROW_BACKWARD = 2;
	public static final byte ARROW_BOTH = 3;

	public abstract String toIpeString(IpeTransformation t, Color c);

	public abstract Envelope getBoundingBox();

	public abstract List<String> layerList();

	protected String ipeMarker(IpeTransformation t, Color color, Point2D pos, String type, String size) {
		StringBuilder sb = new StringBuilder();

		sb.append("<use");
		sb.append(" layer=\"" + currLayer + "\"");
		sb.append(" name=\"mark/" + type + "(sx)\"");
		sb.append(" size=\"" + size + "\"");
		sb.append(" stroke=\"" + IpeWriter.colorToIPE(color, IpeWriter.IPE_DECIMAL_FORMAT) + "\"");
		sb.append(" pos=\"" + IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleX(pos.getX())) + " "
				+ IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleY(pos.getY())) + "\"");
		sb.append("/>\n");

		return sb.toString();
	}

	protected String ipeLine(IpeTransformation t, Color color, Line2D line, byte arrowType, String arrowSize,
			int penWidth) {
		StringBuilder sb = new StringBuilder();

		sb.append("<path");
		sb.append(" layer=\"" + currLayer + "\"");
		sb.append(" stroke=\"" + IpeWriter.colorToIPE(color, IpeWriter.IPE_DECIMAL_FORMAT) + "\"");
		sb.append(" pen=\"" + penWidth + "\"");
		sb.append(arrow(arrowType, arrowSize));
		sb.append(">\n");
		sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleX(line.getX1())) + " "
				+ IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleY(line.getY1())) + " m\n");
		sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleX(line.getX2())) + " "
				+ IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleY(line.getY2())) + " l\n");
		sb.append("</path>\n");

		return sb.toString();
	}

	protected String ipePath(IpeTransformation t, Color color, Path2D path, byte arrowType, String arrowSize,
			int penWidth) {
		StringBuilder sb = new StringBuilder();

		PathIterator pi = path.getPathIterator(t);

		sb.append("<path");
		sb.append(" layer=\"" + currLayer + "\"");
		sb.append(" stroke=\"" + IpeWriter.colorToIPE(color, IpeWriter.IPE_DECIMAL_FORMAT) + "\"");
		sb.append(" pen=\"" + penWidth + "\"");
		sb.append(arrow(arrowType, arrowSize));
		sb.append(">\n");

		int status = -1;
		double[] coords = new double[6];
		while (!pi.isDone()) {
			status = pi.currentSegment(coords);

			switch (status) {
			case PathIterator.SEG_MOVETO:
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[0]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[1]) + " " + " m\n");
				break;
				
			case PathIterator.SEG_LINETO:
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[0]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[1]) + " " + " l\n");
				break;
				
			case PathIterator.SEG_CLOSE:
				sb.append("h\n");
				break;
				
			case PathIterator.SEG_QUADTO:
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[0]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[1]) + "\n");
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[2]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[3]) + " c\n");
				break;
				
			case PathIterator.SEG_CUBICTO:
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[0]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[1]) + "\n");
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[2]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[3]) + "\n");
				sb.append(IpeWriter.IPE_DECIMAL_FORMAT.format(coords[4]) + " "
						+ IpeWriter.IPE_DECIMAL_FORMAT.format(coords[5]) + " c\n");
				break;

			default:
				System.err.println("Type " + status + " not implemented yet.");
				break;
			}
			
			pi.next();
		}

		sb.append("</path>\n");

		return sb.toString();
	}

	protected String ipeLabel(IpeTransformation t, Color color, Point2D position, String text, String textSize) {
		StringBuilder sb = new StringBuilder();

		sb.append("<text");
		sb.append(" layer=\"" + currLayer + "\"");
		sb.append(" transformations=\"translations\" type=\"label\"");
		sb.append(" size=\"" + textSize + "\"");
		sb.append(" stroke=\"" + IpeWriter.colorToIPE(color, IpeWriter.IPE_DECIMAL_FORMAT) + "\"");
		sb.append(" pos=\"" + IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleX(position.getX())) + " "
				+ IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleY(position.getY())) + "\"");
		sb.append(">");
		sb.append(text);
		sb.append("</text>\n");

		return sb.toString();
	}

	protected String ipeMinipage(IpeTransformation t, Color color, Point2D position, int width, String text,
			String textSize) {
		StringBuilder sb = new StringBuilder();

		sb.append("<text");
		sb.append(" layer=\"" + currLayer + "\"");
		sb.append(" transformations=\"translations\" type=\"minipage\"");
		sb.append(" width=\"" + width + "\"");
		sb.append(" size=\"" + textSize + "\"");
		sb.append(" stroke=\"" + IpeWriter.colorToIPE(color, IpeWriter.IPE_DECIMAL_FORMAT) + "\"");
		sb.append(" pos=\"" + IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleX(position.getX())) + " "
				+ IpeWriter.IPE_DECIMAL_FORMAT.format(t.scaleY(position.getY())) + "\"");
		sb.append(">");
		sb.append(text);
		sb.append("</text>\n");

		return sb.toString();
	}

	protected void changeLayer(String newLayer) {
		this.currLayer = newLayer;
	}

	private String arrow(byte arrowType, String arrowSize) {
		String arrow = "";
		if ((arrowType & ARROW_FORWARD) != 0)
			arrow += " arrow=\"ptarc/" + arrowSize + "\"";
		if ((arrowType & ARROW_BACKWARD) != 0)
			arrow += " rarrow=\"fptarc/" + arrowSize + "\"";
		return arrow;
	}
}
