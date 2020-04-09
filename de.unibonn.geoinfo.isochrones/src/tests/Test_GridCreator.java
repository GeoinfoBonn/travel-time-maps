package tests;

import java.awt.Color;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import isochrone.GridCreator;
import util.geometry.Envelope;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public class Test_GridCreator {

	private static MultiLineString GRID;

	public static void main(String[] args) {
		Envelope bbox = new Envelope(0, 4, 0, 5);
		bbox = new Envelope(354000.0000000000582077, 376618.7741306821117178, 5607400.9674360034987330,
				5641316.3429054161533713);

		GridCreator gc = new GridCreator(bbox);

		GRID = gc.createGrid(2, GridCreator.OCTILINEAR);

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				createAndShowUI();
			}
		});
	}

	public static void createAndShowUI() {
		ResultFrame rf = new ResultFrame();

		IsochronePanel panel = new IsochronePanel(rf);

		ListLayer layer = new ListLayer(Color.DARK_GRAY);

		for (int i = 0; i < GRID.getNumGeometries(); ++i) {
			LineString line = (LineString) GRID.getGeometryN(i);
			layer.add(new LineMapObject(new Point2D.Double(line.getCoordinateN(0).x, line.getCoordinateN(0).y),
					new Point2D.Double(line.getCoordinateN(1).x, line.getCoordinateN(1).y)));
		}

		panel.getMap().addLayer(layer, 10);

		rf.addTab("Grid", panel);
	}
}
