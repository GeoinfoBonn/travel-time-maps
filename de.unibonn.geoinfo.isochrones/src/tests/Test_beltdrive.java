package tests;

import java.awt.Color;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import gisviewer.ListLayer;
import isochrone.IsoBufferedCreator;
import viewer.IsochronePanel;
import viewer.PolygonMapObject;
import viewer.ResultFrame;

public class Test_beltdrive {

	public static void main(String[] args) {
		IsoBufferedCreator ibc = new IsoBufferedCreator(.125);

		Point2D c1 = new Point2D.Double(256, 160);
		double remDist1 = 32 * 8;
		Point2D c2 = new Point2D.Double(448, 256);
		double remDist2 = 64 * 8;

		Coordinate[] cStandard = ibc.createBeltdrive(c1, remDist1, c2, remDist2);

		Point2D c3 = new Point2D.Double(96, 256);
		Point2D c4 = new Point2D.Double(128, 384);
		double remDist = 48 * 8;

		Coordinate[] cSame = ibc.createBeltdrive(c3, remDist, c4, remDist);

		Point2D c5 = new Point2D.Double(112, 752);
		double remDist5 = 32 * 8;
		Point2D c6 = new Point2D.Double(400, 384);
		double remDist6 = 48 * 8;

		Coordinate[] cDeg = ibc.createBeltdrive(c5, remDist5, c6, remDist6);

		Point2D c7 = new Point2D.Double(336, 768);
		double remDist7 = 32 * 8;
		Point2D c8 = new Point2D.Double(512, 672);

		Coordinate[] cFinal = ibc.createBeltdrive(c7, remDist7, c8, 0);

		GeometryFactory gf = new GeometryFactory();

		ResultFrame rf = new ResultFrame();
		IsochronePanel panel = new IsochronePanel(rf);

		ListLayer ll = new ListLayer(Color.BLACK);
		ll.add(new PolygonMapObject(gf.createPolygon(cStandard)));
		ll.add(new PolygonMapObject(gf.createPolygon(cSame)));
		ll.add(new PolygonMapObject(gf.createPolygon(cDeg)));
		ll.add(new PolygonMapObject(gf.createPolygon(cFinal)));

		panel.getMap().addLayer(ll, 10);

		rf.addTab("Beltdrive", panel);
	}
}
