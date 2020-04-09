package ipeio.tests;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import ipeio.api.IpeParser;
import ipeio.api.IpeWriter;
import ipeio.drawables.IpePath;
import ipeio.drawables.IpePoints;
import ipeio.api.IpeObject.Document;
import ipeio.api.IpeObject.Geometry;
import ipeio.api.IpeObject.ObjectFilter;

public class TestReader {

	public static void main(String[] args) throws XMLStreamException, IOException {
		
		// ------------------------------------
		
		IpeParser parser = new IpeParser();
		Document doc = parser.parseFile(new FileInputStream("/home/axel/Desktop/IpeReaderTest.ipe"));
		
		ObjectFilter filter = new ObjectFilter() {
			@Override
			public boolean collectGeometry(Geometry geometry) {
				if (geometry.getAttribute("stroke").equals("black"))
					return true;
				return false;
			}
		};
		
		List<Shape> shapes = doc.collectShapes();
		
		Point2D[] pointArray = new Point2D[shapes.size()];
		ArrayList<Point2D> pointList = new ArrayList<>();
		
		int i = 0;
		for (Shape shape : shapes) {
			PathIterator pi = shape.getPathIterator(null);
			double[] coords = new double[6];
			pi.currentSegment(coords);
			
			Point2D currentPoint = new Point2D.Double(coords[0],coords[1]);
			pointArray[i++] = currentPoint;
			pointList.add(currentPoint);
		}
		
		// ------------------------------------
		
		IpeWriter writer = new IpeWriter();
		writer.addDrawable(new IpePoints(pointList), Color.red);
		writer.addDrawable(new IpePath((Path2D) shapes.get(shapes.size()-2)), Color.blue);
		writer.addDrawable(new IpePath((Path2D) shapes.get(shapes.size()-1)), Color.blue);
		writer.write("/home/forsch/Desktop/pointsOUT_2.ipe");
		
		System.out.println("End.");
	}
}
