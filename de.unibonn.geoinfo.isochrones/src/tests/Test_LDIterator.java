package tests;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import graph.generic.DiGraph.DiGraphNode;
import graph.generic.LD.factory.SplitNodeFactory;
import graph.planarizer.PlanarGraph;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.VisualizationEdge;
import ipeio.api.IpeObject.Document;
import ipeio.api.IpeObject.Geometry;
import ipeio.api.IpeObject.ObjectFilter;
import ipeio.api.IpeParser;
import util.geometry.Envelope;

public class Test_LDIterator {

	public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
		IpeReader reader = new IpeReader(
				new File("/home/forsch/Documents/002_Isochrones/tests/Test_SplitNodeFactoryLD.ipe"));

		String[] layers = { "test_01", "test_02", "test_03", "test_04", "test_05", "test_06", "test_07" };

		for (String layer : layers) {
			var input = reader.readInputArcs(layer);
			var splitNode = input.getNodes().stream().filter(x -> x.inDegree() + x.outDegree() > 1).findFirst().get();
			System.out.println("split node " + splitNode);
			var overlay = reader.readOverlayArcs(layer);
			var splitnodes = new HashSet<DiGraphNode<Point2D, VisualizationEdge>>();
			splitnodes.add(splitNode);

			SplitNodeFactory snf = new SplitNodeFactory(0.1, overlay, splitnodes);
			System.out.println(layer + " " + snf.createEdgeData(splitNode.getIncomingArcs().get(0),
					splitNode.getOutgoingArcs().get(0), false));
			System.out.println();
		}
	}

	public static class IpeReader {

		File input;

		public IpeReader(File input) {
			this.input = input;
		}

		public PlanarGraph<ColoredNode, GeofabrikData> readOverlayArcs(String layer)
				throws FileNotFoundException, XMLStreamException {
			IpeParser parser = new IpeParser();

			Document doc = parser.parseFile(new FileInputStream(input));

			List<Shape> arcShapesR = doc.collectShapes(new ObjectFilter() {
				@Override
				public boolean collectGeometry(Geometry geometry) {
					if (geometry.belongsTo(layer) && geometry.getType().equals("path"))
						if (geometry.getAttribute("stroke").equals("reachable"))
							return true;
					return false;
				}
			});

			List<Shape> arcShapesU = doc.collectShapes(new ObjectFilter() {
				@Override
				public boolean collectGeometry(Geometry geometry) {
					if (geometry.belongsTo(layer) && geometry.getType().equals("path"))
						if (geometry.getAttribute("stroke").equals("unreachable"))
							return true;
					return false;
				}
			});

			var arcShapes = new ArrayList<Shape>();
			arcShapes.addAll(arcShapesU);
			arcShapes.addAll(arcShapesR);

			var graph = new PlanarGraph<ColoredNode, GeofabrikData>(getEnvelope(arcShapes));

			// step 1: add all nodes to graph
			for (Shape shape : arcShapesR) {

				PathIterator pi = shape.getPathIterator(null);
				double[] coords = new double[6];

				while (!pi.isDone()) {
					pi.currentSegment(coords);
					graph.addNode(new ColoredNode(coords[0], coords[1], Colored.REACHABLE, 0));
					pi.next();
				}
			}

			for (Shape shape : arcShapesU) {

				PathIterator pi = shape.getPathIterator(null);
				double[] coords = new double[6];

				while (!pi.isDone()) {
					pi.currentSegment(coords);
					graph.addNode(new ColoredNode(coords[0], coords[1], Colored.UNREACHABLE, -1));
					pi.next();
				}
			}

			for (Shape shape : arcShapes) {
				PathIterator pi = shape.getPathIterator(null);
				double[] coords = new double[6];

				DiGraphNode<ColoredNode, GeofabrikData> source = null, target = null;

				while (!pi.isDone()) {
					int type = pi.currentSegment(coords);
					if (type == PathIterator.SEG_MOVETO) {
						source = graph.getDiGraphNode(coords[0], coords[1]);
					} else if (type == PathIterator.SEG_LINETO) {
						target = graph.getDiGraphNode(coords[0], coords[1]);

						// create arcs
						graph.addArc(source, target,
								new GeofabrikData(source.getNodeData().distance(target.getNodeData())));
//									graph.addArc(target, source, new DistanceData(1));

					} else {
						System.out.println("Unknown SEG_TYPE: " + type);
					}
					pi.next();
				}
			}

			return graph;
		}

		public PlanarGraph<Point2D, VisualizationEdge> readInputArcs(String layer)
				throws FileNotFoundException, XMLStreamException {

			IpeParser parser = new IpeParser();

			Document doc = parser.parseFile(new FileInputStream(input));
			List<Shape> arcShapes = doc.collectShapes(new ObjectFilter() {
				@Override
				public boolean collectGeometry(Geometry geometry) {
					if (geometry.belongsTo(layer) && geometry.getType().equals("path"))
						if (geometry.getAttribute("stroke").equals("black"))
							return true;
					return false;
				}
			});

			var graph = new PlanarGraph<Point2D, VisualizationEdge>(getEnvelope(arcShapes));

			// step 1: add all nodes to graph
			for (Shape shape : arcShapes) {
				PathIterator pi = shape.getPathIterator(null);
				double[] coords = new double[6];

				while (!pi.isDone()) {
					pi.currentSegment(coords);
					graph.addNode(new Point2D.Double(coords[0], coords[1]));
					pi.next();
				}
			}

			for (Shape shape : arcShapes) {
				PathIterator pi = shape.getPathIterator(null);
				double[] coords = new double[6];

				DiGraphNode<Point2D, VisualizationEdge> source = null, target = null;

				while (!pi.isDone()) {
					int type = pi.currentSegment(coords);
					if (type == PathIterator.SEG_MOVETO) {
						source = graph.getDiGraphNode(coords[0], coords[1]);
					} else if (type == PathIterator.SEG_LINETO) {
						target = graph.getDiGraphNode(coords[0], coords[1]);

						// create arcs
						graph.addArc(source, target, new VisualizationEdge(
								source.getNodeData().distance(target.getNodeData()), false, (byte) 0));
//						graph.addArc(target, source, new DistanceData(1));

					} else {
						System.out.println("Unknown SEG_TYPE: " + type);
					}
					pi.next();
				}
			}

			return graph;

		}

		private Envelope getEnvelope(List<Shape> shapes) {
			Envelope env = new Envelope();

			for (Shape shape : shapes) {
				PathIterator pi = shape.getPathIterator(null);

				double[] coords = new double[6];
				while (!pi.isDone()) {
					pi.currentSegment(coords);
					env.expandToInclude(coords[0], coords[1]);
					pi.next();
				}
			}

			return env;
		}

	}
}
