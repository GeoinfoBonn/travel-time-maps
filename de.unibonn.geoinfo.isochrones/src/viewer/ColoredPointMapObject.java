package viewer;

import java.awt.Color;
import java.awt.Graphics2D;

import gisviewer.PointMapObject;
import gisviewer.Transformation;
import graph.types.Colored;
import graph.types.ColoredNode;
import main.AbstractMain;

public class ColoredPointMapObject extends PointMapObject {

	private int color;

	public ColoredPointMapObject(ColoredNode node) {
		super(node);
		this.color = node.getColor();
	}

	@Override
	public void draw(Graphics2D g, Transformation t) {
		Color c = g.getColor();
		if (color == Colored.REACHABLE)
			g.setColor(AbstractMain.COLOR_STYLE.reachable());
		else if (color == Colored.BUFFER)
			g.setColor(AbstractMain.COLOR_STYLE.buffer());
		else
			g.setColor(AbstractMain.COLOR_STYLE.unreachable());
		g.fillOval(t.getColumn(myPoint.getX()) - 2, t.getRow(myPoint.getY()) - 2, 5, 5);
		g.setColor(c);
	}
}
