package isochrone;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jump.io.IllegalParametersException;

import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import gisviewer.MapObject;
import gisviewer.PointMapObject;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.generic.GraphFilterer;
import graph.generic.GraphFilterer.ArcFilter;
import graph.generic.GraphFilterer.GraphFactory;
import graph.generic.LD.factory.TurncostFactory;
import graph.planarier.union.UnionPlanarizer;
import graph.planarizer.NodeInserter;
import graph.planarizer.PlanarGraph;
import graph.planarizer.Planarizer;
import graph.planarizer.Planarizer.PlanarizerFactory;
import graph.planarizer.sweep.SweepPlanarizer;
import graph.routing.MultiModalRouter;
import graph.routing.Router;
import graph.types.Colored;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.RoadGraph;
import io.csv.PointToCSV;
import io.kml.KmlPolygon;
import io.shp.GeofabrikFactory;
import io.shp.ShapeFileReader;
import isochrone.FaceIdentifier.FaceFactory;
import main.AbstractMain;
import tools.Stopwatch;
import util.geometry.Envelope;
import viewer.EdgeMapObject;
import viewer.FaceMapObjectWithId;
import viewer.IsochronePanel;
import viewer.PolygonMapObject;
import viewer.ResultFrame;

public class IsochroneCreator {

	private RoadGraph<Point2D, GeofabrikData> roadGraph;
	private PlanarGraph<Point2D, GeofabrikData> planarGraph;
	private Router<Point2D, GeofabrikData> router;
	private KmlPolygon kml_saver;

	private String lastType;
	private Stopwatch lastTiming;

	private static ListLayer reachableEdges;
	private static ListLayer unreachableEdges;
	private static ListLayer bufferEdges;

	private File currentOutputDir;

	private IdGenerator idGenerator;

	/**
	 * Creates an instance of the IsochroneCreator.
	 * 
	 * @param roadShape
	 * @param gtfsDir
	 * @throws IllegalParametersException
	 * @throws Exception
	 */
	public IsochroneCreator(File roadShape, File gtfsDir) throws IllegalParametersException, Exception {
		idGenerator = new IdGenerator();

		loadRoadGraph(roadShape);

		if (AbstractMain.FILTER_ROADS > 0) {
			RoadGraph<Point2D, GeofabrikData> filtered = filterRoadGraph();
			planarizeGraph(filtered);
		} else {
			planarizeGraph(roadGraph);
		}

		router = new MultiModalRouter<>(roadGraph, gtfsDir, Router.GEOFABRIK_FACTORY);
	}

	private void loadRoadGraph(File roadShape) {
		try {
			GeofabrikFactory gff = new GeofabrikFactory();
			roadGraph = ShapeFileReader.importFromSHP(roadShape, gff);
			ShapeFileReader.reduceToBiggestComponent(roadGraph);

			if (AbstractMain.SHOW_RESULTS) {
				AbstractMain.GUI.initializeRoadLayer(roadGraph);
				IsochronePanel.showRoadGraph(AbstractMain.GUI, "Road Graph");
			}

		} catch (IllegalParametersException e) {
			System.err.println("IllegalParametersException while loading road shapefile.");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Exception while loading road shapefile.");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private RoadGraph<Point2D, GeofabrikData> filterRoadGraph() {

		ArcFilter<GeofabrikData> filter_01 = new ArcFilter<GeofabrikData>() {
			@Override
			public boolean keep(GeofabrikData edgeData) {
				if (edgeData.fclass().equals("bridleway"))
					return false;
				if (edgeData.fclass().equals("path"))
					return false;
				if (edgeData.fclass().equals("track"))
					return false;
				if (edgeData.fclass().equals("track_grade1"))
					return false;
				if (edgeData.fclass().equals("track_grade2"))
					return false;
				if (edgeData.fclass().equals("track_grade3"))
					return false;
				if (edgeData.fclass().equals("track_grade4"))
					return false;
				if (edgeData.fclass().equals("track_grade5"))
					return false;
				return true;
			}
		};

		ArcFilter<GeofabrikData> filter_02 = new ArcFilter<GeofabrikData>() {
			@Override
			public boolean keep(GeofabrikData edgeData) {
				if (edgeData.fclass().equals("footway"))
					return false;
				if (edgeData.fclass().equals("pedestrian"))
					return false;
				if (edgeData.fclass().equals("unknown"))
					return false;
				if (edgeData.fclass().equals("unclassified"))
					return false;
				if (edgeData.fclass().equals("steps"))
					return false;
				return true;
			}
		};

		ArcFilter<GeofabrikData> filter = null;
		if (AbstractMain.FILTER_ROADS == 1)
			filter = filter_01;
		if (AbstractMain.FILTER_ROADS == 2)
			filter = GraphFilterer.joinedArcFilter(new ArcFilter[] { filter_01, filter_02 });

		RoadGraph<Point2D, GeofabrikData> filtered = GraphFilterer.filterArcs(roadGraph, filter, new GraphFactory<>() {

			@Override
			public Point2D createNodeData(Point2D in) {
				return new Point2D.Double(in.getX(), in.getY());
			}

			@Override
			public GeofabrikData createEdgeData(GeofabrikData in) {
				return new GeofabrikData(in.getValue(), in.fclass());
			}

			@Override
			public RoadGraph<Point2D, GeofabrikData> createGraph() {
				return new RoadGraph<Point2D, GeofabrikData>(roadGraph.getQuadTree().getEnvelope());
			}

			@Override
			public GeofabrikData createEdgeData(double weight) {
				return new GeofabrikData(weight);
			}
		}, false);

		if (AbstractMain.SHOW_RESULTS)
			AbstractMain.GUI.initializeRoadLayer(filtered);

		return filtered;
	}

	private void planarizeGraph(RoadGraph<Point2D, GeofabrikData> graphToPlanarize) {
		long start = 0;
		if (AbstractMain.VERBOSE) {
			System.out.println("Starting planarization...");
			start = System.currentTimeMillis();
		}

		PlanarizerFactory<Point2D, GeofabrikData> planFac = new PlanarizerFactory<>() {

			@Override
			public Point2D createNodeData(double x, double y) {
				return new Point2D.Double(x, y);
			}

			@Override
			public GeofabrikData createEdgeData(double dist) {
				return new GeofabrikData(dist);
			}

		};

		boolean useUnion = false;
		Planarizer<Point2D, GeofabrikData> planarizer;
		if (useUnion)
			planarizer = new UnionPlanarizer<>(planFac, true);
		else
			planarizer = new SweepPlanarizer<>(planFac);
		planarizer.setInputGraph(graphToPlanarize);
		planarizer.planarize();
		planarGraph = planarizer.getPlanarGraph();

		if (AbstractMain.VERBOSE)
			System.out.println("Planarization finished: #crosspoints (" + (useUnion ? "union" : "sweep") + ") = "
					+ planarGraph.getCrossPoints().size() + ", running time = "
					+ (System.currentTimeMillis() - start) / 1000.0 + "s");

		if (AbstractMain.SHOW_RESULTS) {
			if (AbstractMain.DEBUG)
				planarizer.showPlanarization(AbstractMain.GUI, "Planarized Graph");
			planarizer.showPlanarization(AbstractMain.GUI, "Planarized Graph with Edges", true);
		}
	}

	private void route(int startid, long starttime, long time, long bufferTime, Stopwatch sw) {
		long routeTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Computing traveltimes...");
		DiGraphNode<Point2D, GeofabrikData> roadSource = roadGraph.getNode(startid);
		router.setStarttime(starttime);
		router.run(roadSource, time, bufferTime);

		routeTime = System.currentTimeMillis() - routeTime;
		sw.add("travelTimes", routeTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Travel times computed. (" + routeTime / 1000.0 + "s)");
	}

	private PlanarGraph<ColoredNode, GeofabrikData> createColoredPlanarGraph(Stopwatch sw) {
		long time = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Creating visualization graph...");

		NodeInserter<Point2D, GeofabrikData> ni = new NodeInserter<>(planarGraph);
		ni.setArcDataSplitter(NodeInserter.GEOFABRIK_SPLITTER);

		ni.colorGraph(router.getColoredGraph());
		List<DiGraphNode<ColoredNode, GeofabrikData>> newSplitNodes = ni.insertSplitNodes();

		if (AbstractMain.SHOW_RESULTS) {
			AbstractMain.GUI.setEnvelope(router.getSplitNodes());
		}

		PlanarGraph<ColoredNode, GeofabrikData> result = ni.getResultGraph();

		fixOuterColorAsUnreachable(result);

		if (AbstractMain.SHOW_RESULTS) {
			showColoredGraph(AbstractMain.GUI, "Colored Graph", result, router.getLastSource().getNodeData(),
					newSplitNodes);
//			showColoredGraph(AbstractMain.GUI, "Colored Graph", result, router.getLastSource().getNodeData());
		}

		if (AbstractMain.ASSERTION_ENABLED) {
			for (DiGraphNode<ColoredNode, GeofabrikData> node : result.getNodes()) {
				assert node.getNodeData().getColor() != Colored.UNDEFINED;
				assert node.getNodeData().getColor() != Colored.BUFFER;
			}
		}

		time = System.currentTimeMillis() - time;
		sw.add("visualizationGraph", time);
		if (AbstractMain.VERBOSE)
			System.out.println("Visualization graph created. (" + time / 1000.0 + "s)");

		return result;
	}

	private void fixOuterColorAsUnreachable(PlanarGraph<ColoredNode, GeofabrikData> graph) {
		ColoredNode data;
		Envelope env = graph.getEnvelope();
		for (DiGraphNode<ColoredNode, GeofabrikData> node : graph.getNodes()) {
			data = node.getNodeData();
			if (data.x < env.getxMin() + 1e-2) {
				data.setReachability(Colored.UNREACHABLE, -1);
				data.fixReachability();
				continue;
			}

			if (data.x > env.getxMax() - 1e-2) {
				data.setReachability(Colored.UNREACHABLE, -1);
				data.fixReachability();
				continue;
			}

			if (data.y < env.getyMin() + 1e-2) {
				data.setReachability(Colored.UNREACHABLE, -1);
				data.fixReachability();
				continue;
			}

			if (data.y > env.getyMax() - 1e-2) {
				data.setReachability(Colored.UNREACHABLE, -1);
				data.fixReachability();
				continue;
			}
		}
	}

	private Timezone<Point2D> createTimezoneFaces(PlanarGraph<ColoredNode, GeofabrikData> planarColoredGraph,
			FaceFactory<?> factory, Stopwatch sw) {
		FaceIdentifier<?, GeofabrikData> faceIdentifier = new FaceIdentifier<>(planarColoredGraph, sw);
		List<List<IsoFace>> faces = faceIdentifier.identifyFaces(factory, sw, idGenerator);

		if (AbstractMain.SHOW_RESULTS)
			showFaces(AbstractMain.GUI, "Faces", faces);

		IsoMap iMap = new IsoMap(faces, sw, idGenerator);

		if (AbstractMain.SHOW_RESULTS)
			showFaceGraphs(AbstractMain.GUI, "Lines", faces, planarColoredGraph);

		Timezone<Point2D> timezone = iMap.route(planarColoredGraph, factory, sw);

		if (AbstractMain.SHOW_RESULTS)
			showResultRouting(AbstractMain.GUI, "Route", planarColoredGraph, timezone);

		Map<IsoPolygon<Point2D>, Set<Integer>> needInnerComponents = timezone.needsInnerComponents(planarColoredGraph,
				sw);
		for (Entry<IsoPolygon<Point2D>, Set<Integer>> poly : needInnerComponents.entrySet()) {

			faceIdentifier.recolor(poly.getValue(), sw);

			if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG) {
				showColoredArcs(AbstractMain.GUI, "Recolored", planarColoredGraph);
			}

			faces = faceIdentifier.identifyFaces(factory, sw, idGenerator, true);

			if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG)
				showFaces(AbstractMain.GUI, "Inner Faces", faces);

			iMap = new IsoMap(faces, sw, idGenerator);

			if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG)
				showFaceGraphs(AbstractMain.GUI, "Inner lines " + poly.getKey().getComponentId(), faces,
						planarColoredGraph);

			Timezone<Point2D> timezoneInner = iMap.route(planarColoredGraph, factory, sw);

			if (AbstractMain.SHOW_RESULTS && AbstractMain.DEBUG)
				showResultRouting(AbstractMain.GUI, "Inner route " + poly.getKey().getComponentId(), planarColoredGraph,
						timezoneInner);

			for (IsoPolygon<Point2D> inner : timezoneInner.getPolyList()) {
				poly.getKey().addInnerRing(inner.getOuterRing());
			}
		}

		faceIdentifier.restoreColor(sw);

		return timezone;
	}

	private Timezone<Point2D> createTimezoneBuffer(PlanarGraph<ColoredNode, GeofabrikData> planarColoredGraph,
			Stopwatch sw) {

		GeometryFactory gf = new GeometryFactory();

		IsoBufferedCreator creator = new IsoBufferedCreator(AbstractMain.TIMED_BUFFER_FACTOR);

		Timezone<Point2D> zone = new Timezone<>(idGenerator);
		LinkedList<Polygon> buffers = new LinkedList<>();
		Coordinate[] coordinates;
		ColoredNode source, target;

		long rtime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Processing arcs...");
		Set<Integer> processedArcs = new HashSet<>();
		for (DiGraphArc<ColoredNode, ?> arc : planarColoredGraph.getArcs()) {
			if (processedArcs.contains(arc.getId()))
				continue;

			source = arc.getSource().getNodeData();
			target = arc.getTarget().getNodeData();

			if (Colored.edgeColor(source.getColor(), target.getColor()) == Colored.REACHABLE) {
				try {
					coordinates = creator.createBeltdrive(source, source.getRemainingDist(), target,
							target.getRemainingDist());

					if (coordinates[0].distance(coordinates[coordinates.length - 1]) > 1e-9)
						System.err.println("LineString not closed: " + coordinates[0] + " -> " + coordinates[1]);

					buffers.add(gf.createPolygon(coordinates));

				} catch (NullPointerException e) {
//					System.err.println("  Arc " + arc.getId() + ": " + source + " -> " + target);
				}

				processedArcs.add(arc.getId());
				if (arc.getTwin() != null)
					processedArcs.add(arc.getTwin().getId());
			}
		}
		rtime = System.currentTimeMillis() - rtime;
		sw.add("faceProcessing", rtime);
		if (AbstractMain.VERBOSE)
			System.out.println("Arcs processed. (" + rtime / 1000.0 + "s)");

		if (AbstractMain.VERBOSE)
			System.out.println("Unioning arc beltdrives to zone...");
		rtime = System.currentTimeMillis();
		GeometryCollection geom = new GeometryCollection(buffers.toArray(new Polygon[buffers.size()]), gf);
		Geometry union = geom.union();
		if (union instanceof Polygon) {
			Polygon poly = (Polygon) union;
			zone.addPolygon(jtsPolygonToIsoPolygon(poly));
		} else if (union instanceof GeometryCollection) {
			GeometryCollection collection = (GeometryCollection) union;
			Geometry g;
			for (int i = 0; i < collection.getNumGeometries(); ++i) {
				g = union.getGeometryN(i);
				if (g instanceof Polygon) {
					Polygon poly = (Polygon) g;
					zone.addPolygon(jtsPolygonToIsoPolygon(poly));
				} else {
					System.err.println("union inner type not done yet: " + g.getClass().getName());
				}
			}
		} else {
			System.err.println("union type not done yet: " + geom.getClass().getName());
		}

		rtime = System.currentTimeMillis() - rtime;
		sw.add("faceRouting", rtime);
		if (AbstractMain.VERBOSE)
			System.out.println("Union done. (" + rtime / 1000.0 + "s)");

		return zone;
	}

	private Timezone<Point2D> createTimezone(PlanarGraph<ColoredNode, GeofabrikData> planarColoredGraph, long time,
			FaceFactory<?> factory, Stopwatch sw) {
		Timezone<Point2D> timezone;
		if (factory != null)
			timezone = createTimezoneFaces(planarColoredGraph, factory, sw);
		else
			timezone = createTimezoneBuffer(planarColoredGraph, sw);
		timezone.setTime(time);
		return timezone;
	}

	public Timezone<Point2D> createIsochrone(int startid, long starttime, long time, long bufferTime,
			FaceFactory<?> factory) {
		currentOutputDir = new File(AbstractMain.OUTPUT_DIRECTORY + File.separator + startid + File.separator);
		if (currentOutputDir.mkdir())
			System.out.println(currentOutputDir + " created");

		if (factory != null)
			lastType = factory.getName();
		else
			lastType = FaceFactory.TIMED_BUFFER;

		PointToCSV.write(new File(currentOutputDir, "startpoint.csv"), roadGraph.getNode(startid).getNodeData(), false);
		PointToCSV.write(new File(AbstractMain.OUTPUT_DIRECTORY, "startpoints.csv"),
				roadGraph.getNode(startid).getNodeData(), startid, new PointToCSV.DataFactory<Integer>() {

					@Override
					public String[] getColumTitles() {
						return new String[] { "startid" };
					}

					@Override
					public String[] getDataTerms(Integer in) {
						return new String[] { in.toString() };
					}
				}, true);

		if (AbstractMain.VERBOSE)
			System.out.println("Starting timezone " + time + " for start node " + startid);

		Stopwatch sw = new Stopwatch();
		long alg_starttime = System.currentTimeMillis();

		route(startid, starttime, time, bufferTime, sw);

		PlanarGraph<ColoredNode, GeofabrikData> planarColoredGraph = createColoredPlanarGraph(sw);

		Timezone<Point2D> timezone = createTimezone(planarColoredGraph, time, factory, sw);
		timezone.setType(lastType);

		IsochronePanel endresultPanel = null;
		if (AbstractMain.SHOW_RESULTS)
			endresultPanel = showEndresult(AbstractMain.GUI, "Result " + lastType + " " + time, planarColoredGraph,
					timezone);

		try {
			validateResult(timezone, planarColoredGraph, endresultPanel, currentOutputDir, sw);
		} catch (TopologyException ex) {
			System.err.println("TopologyException while validating! " + ex.getMessage());
		}

		// export as KML
		File successDir = new File(currentOutputDir + File.separator + "success");
		File failDir = new File(currentOutputDir + File.separator + "fail");
		File kmlDir;
		if (timezone.isSuccess()) {
			if (successDir.mkdir() && AbstractMain.VERBOSE)
				System.out.println(successDir + " created.");
			kmlDir = successDir;
		} else {
			if (failDir.mkdir() && AbstractMain.VERBOSE)
				System.out.println(failDir + " created.");
			kmlDir = failDir;
		}
		kml_saver = new KmlPolygon(
				kmlDir + File.separator + "Iso" + lastType + "_time_" + String.format("%05d", time) + "s");
		kml_saver.saveSingleZone(timezone);

		// save timing
		System.out.println(sw);
		lastTiming = sw;
		System.out
				.println("Timezone finished in " + (System.currentTimeMillis() - alg_starttime) / 1000.0 + "seconds.");

		return timezone;
	}

	public void validateResult(Timezone<Point2D> resultZone, PlanarGraph<ColoredNode, GeofabrikData> resultRouting,
			IsochronePanel endresultPanel, File directory, Stopwatch sw) {
		long validateTime = System.currentTimeMillis();
		if (AbstractMain.VERBOSE)
			System.out.println("Validating Result...");

		int fp = 0, tp = 0, fn = 0, tn = 0;

		var fps = new LinkedList<Point2D>();
		var fns = new LinkedList<Point2D>();

		final int constFactor = 10000000;
		int[] validationResult = resultRouting.getNodes().parallelStream().mapToInt((node) -> {
			int color = node.getNodeData().getColor();
			if (resultZone.covers(node.getNodeData())) {
				if (color == Colored.REACHABLE) {
					return (1 * constFactor + node.getId()); // tp
				}
				if (color == Colored.UNREACHABLE) {
					return (2 * constFactor + node.getId()); // fp
				}
			} else {
				if (color == Colored.UNREACHABLE) {
					return (3 * constFactor + node.getId()); // tn
				}
				if (color == Colored.REACHABLE) {
					return (4 * constFactor + node.getId()); // fn
				}
			}
			return 0;
		}).toArray();

		int k;// , x;
		int id;
		for (int i : validationResult) {
			k = 0;
			if (i >= 4 * constFactor) {
				k = 4;
				id = i - k * constFactor;
				fns.add(resultRouting.getNode(id).getNodeData());
				fn++;
			} else if (i >= 3 * constFactor) {
				k = 3;
				tn++;
			} else if (i >= 2 * constFactor) {
				k = 2;
				id = i - k * constFactor;
				fps.add(resultRouting.getNode(id).getNodeData());
				fp++;
			} else if (i >= 1 * constFactor) {
				k = 1;
				tp++;
			} else {
				System.err.println("Error in validation step.");
			}
		}

		if (fn > 0 || fp > 0) {
			try (BufferedWriter bw = new BufferedWriter(
					new FileWriter(new File(directory,
							"wrongPoints_" + lastType + String.format("_%05d", resultZone.getTime()) + ".csv")),
					32768)) {
				bw.write("type,x,y");
				bw.newLine();

				for (var p : fps) {
					bw.write("fp," + p.getX() + "," + p.getY());
					bw.newLine();
				}

				for (var n : fns) {
					bw.write("fn," + n.getX() + "," + n.getY());
					bw.newLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (AbstractMain.SHOW_RESULTS)
			showValidationResults(endresultPanel, "False Points", fps, fns);

		resultZone.setQualityMeasures(tp, fp, fn, tn);

		validateTime = System.currentTimeMillis() - validateTime;
		sw.add("validation", validateTime);
		if (AbstractMain.VERBOSE)
			System.out.println("Result validated. FP: " + fp + " FN: " + fn + " (" + validateTime / 1000.0 + "s)");
	}

	public IsochronePanel showValidationResults(IsochronePanel endresultPanel, String title, List<Point2D> fps,
			List<Point2D> fns) {
		ListLayer falsePositives = new ListLayer(Color.RED);
		ListLayer falseNegatives = new ListLayer(Color.MAGENTA);

		for (var p : fps) {
			falsePositives.add(new PointMapObject(p));
		}

		for (var n : fns) {
			falseNegatives.add(new PointMapObject(n));
		}

		endresultPanel.getMap().addLayer(falsePositives, 1000);
		endresultPanel.getMap().addLayer(falseNegatives, 1001);
		return endresultPanel;
	}

	public IsochronePanel showEndresult(ResultFrame frame, String title, DiGraph<ColoredNode, ?> graph,
			Timezone<Point2D> zone) {
		IsochronePanel panel = showColoredArcs(frame, title, graph);

		ListLayer isochroneLayer = new ListLayer(Color.GREEN);
		for (IsoPolygon<Point2D> p : zone.getPolyList()) {
			if (!p.getVisualizationPolygon().isEmpty()) {
				PolygonMapObject polyMap;
				Geometry viz = p.getVisualizationPolygon();
				if (viz instanceof Polygon) {
					polyMap = new PolygonMapObject((Polygon) viz);
					isochroneLayer.add(polyMap);
				} else if (viz instanceof MultiPolygon) {
					for (int i = 0; i < viz.getNumGeometries(); ++i) {
						polyMap = new PolygonMapObject((Polygon) ((MultiPolygon) viz).getGeometryN(i));
						isochroneLayer.add(polyMap);
					}
				} else {
					System.err.println("Unknonw geometry type: " + viz.getClass().getName());
				}
			}
		}

		panel.getMap().addLayer(isochroneLayer, 1);
		return panel;
	}

	public IsochronePanel showColoredGraph(ResultFrame frame, String title, DiGraph<ColoredNode, ?> graph,
			Point2D sourceLocation) {
		return showColoredGraph(frame, title, graph, sourceLocation, null);
	}

	public IsochronePanel showColoredGraph(ResultFrame frame, String title, DiGraph<ColoredNode, ?> graph,
			Point2D sourceLocation, List<DiGraphNode<ColoredNode, GeofabrikData>> splitNodes) {
		IsochronePanel panel = showColoredArcs(frame, title, graph);

		ListLayer sourceLayer = new ListLayer(Color.ORANGE);
		ListLayer splitNodeLayer = new ListLayer(AbstractMain.COLOR_STYLE.reachable());

		if (splitNodes != null)
			for (DiGraphNode<ColoredNode, GeofabrikData> splitnode : splitNodes) {
				splitNodeLayer.add(new PointMapObject(splitnode.getNodeData()));
			}

		sourceLayer.add(new PointMapObject(sourceLocation));

		panel.getMap().addLayer(splitNodeLayer, 25);
		panel.getMap().addLayer(sourceLayer, 30);

		return panel;
	}

	public static <V extends Point2D, E> IsochronePanel showFilteredGraph(ResultFrame frame,
			Set<DiGraphArc<V, E>> removedArcs) {
		IsochronePanel panel = new IsochronePanel(frame);

		ListLayer roadLayer = frame.getRoadLayer();
		ListLayer removedLayer = new ListLayer(Color.LIGHT_GRAY);

		for (var arc : removedArcs) {
			removedLayer.add(new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData()));
		}

		panel.getMap().addLayer(removedLayer, 35);
		panel.getMap().addLayer(roadLayer, 30);

		frame.addTab("Filtered", panel);

		return panel;
	}

	public static IsochronePanel showColoredArcs(ResultFrame frame, String title, DiGraph<ColoredNode, ?> graph) {
		IsochronePanel panel = new IsochronePanel(frame);

//		if (reachableEdges == null) {
		reachableEdges = new ListLayer(AbstractMain.COLOR_STYLE.reachable());
		unreachableEdges = new ListLayer(AbstractMain.COLOR_STYLE.unreachable());
		bufferEdges = new ListLayer(AbstractMain.COLOR_STYLE.buffer());

		ColoredNode source, target;
		MapObject mo;
		Set<DiGraphArc<ColoredNode, ?>> paintedArcs = new HashSet<>();
		for (DiGraphArc<ColoredNode, ?> arc : graph.getArcs()) {
			if (!paintedArcs.contains(arc)) {
				source = arc.getSource().getNodeData();
				target = arc.getTarget().getNodeData();
				mo = new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData());
				if (Colored.edgeColor(source.getColor(), target.getColor()) == Colored.REACHABLE) {
					reachableEdges.add(mo);
				} else if (Colored.edgeColor(source.getColor(), target.getColor()) == Colored.BUFFER) {
					bufferEdges.add(mo);
				} else if (Colored.edgeColor(source.getColor(), target.getColor()) == Colored.UNREACHABLE) {
					unreachableEdges.add(mo);
				} else {
					System.err.println("Undefined edge!");
				}

				paintedArcs.add(arc);
				if (arc.getTwin() != null)
					paintedArcs.add(arc.getTwin());
			}
		}
//		}

		panel.getMap().addLayer(unreachableEdges, 2);
		panel.getMap().addLayer(bufferEdges, 5);
		panel.getMap().addLayer(reachableEdges, 7);

		return (IsochronePanel) frame.addTab(title, panel);
	}

	public static <F extends IsoFace> IsochronePanel showFaceGraphs(ResultFrame frame, String title,
			List<List<F>> faces, DiGraph<ColoredNode, ?> graph) {
		IsochronePanel panel = showColoredArcs(frame, title, graph);

		if (faces == null)
			return panel;

		int i = 100;
		ListLayer layer;
		for (List<F> fList : faces) {
			for (F f : fList) {
				layer = f.getLinesAsMapLayer(Color.BLUE);
				panel.getMap().addLayer(layer, i++);
			}
		}

		return panel;
	}

	private static <F extends IsoFace> IsochronePanel showFaces(ResultFrame frame, String title, List<List<F>> faces) {
		if (faces == null || faces.size() == 0)
			return null;

		IsochronePanel mapPanel = new IsochronePanel(frame);

		// Create layer
		ListLayer faceLayer = new ListLayer(Color.BLUE);
		for (List<F> fList : faces) {
			for (F f : fList) {
				faceLayer.add(new FaceMapObjectWithId(f));
			}
		}

		// Add layer to map
		mapPanel.getMap().addLayer(frame.getRoadLayer(), 5);
		mapPanel.getMap().addLayer(faceLayer, 10);

		return (IsochronePanel) frame.addTab(title, mapPanel);
	}

	private IsochronePanel showResultRouting(ResultFrame frame, String title, DiGraph<ColoredNode, ?> graph,
			Timezone<Point2D> timezone) {
		IsochronePanel panel = showColoredArcs(frame, title, graph);

		ListLayer otherEdge = new ListLayer(Color.RED);
		ListLayer octiEdge = new ListLayer(Color.GREEN);

		for (List<Point2D> path : timezone.getPolyBoundaries()) {
			Iterator<Point2D> it = path.iterator();
			if (!it.hasNext())
				continue;
			Point2D node = it.next();
			while (it.hasNext()) {
				Point2D predNode = node;
				node = it.next();

				if (predNode == node)
					continue;

				EdgeMapObject edge = new EdgeMapObject(node, predNode);
				edge.setStrokeWidth(3);
				if (TurncostFactory.isOcti(predNode, node))
					octiEdge.add(edge);
				else
					otherEdge.add(edge);

			}
		}

		panel.getMap().addLayer(otherEdge, 10);
		panel.getMap().addLayer(octiEdge, 11);

		return panel;
	}

	public void showEndresult(ResultFrame frame, Timezone<Point2D> zone) {
		IsochronePanel mapPanel = new IsochronePanel(frame);
		mapPanel.setSize(500, 500);
		mapPanel.getMap().addLayer(frame.getRoadLayer(), 100);
		ListLayer isochroneLayer = new ListLayer(Color.GREEN);
		for (IsoPolygon<Point2D> p : zone.getPolyList()) {
			if (!p.getVisualizationPolygon().isEmpty()) {
				PolygonMapObject polyMap;
				Geometry viz = p.getVisualizationPolygon();
				if (viz instanceof Polygon) {
					polyMap = new PolygonMapObject((Polygon) viz);
					isochroneLayer.add(polyMap);
				} else if (viz instanceof MultiPolygon) {
					for (int i = 0; i < viz.getNumGeometries(); ++i) {
						polyMap = new PolygonMapObject((Polygon) ((MultiPolygon) viz).getGeometryN(i));
						isochroneLayer.add(polyMap);
					}
				} else {
					System.err.println("Unknonw geometry type: " + viz.getClass().getName());
				}
			}
		}
		mapPanel.getMap().addLayer(isochroneLayer, 900);
		frame.addTab("Resulting Zone", mapPanel);
	}

	public static <F extends IsoFace> IsochronePanel showPolygon(ResultFrame frame, String title, Polygon poly) {

		IsochronePanel mapPanel = new IsochronePanel(frame);

		// Create layer
		ListLayer faceLayer = new ListLayer(Color.BLUE);

		faceLayer.add(new PolygonMapObject(poly));

		// Add layer to map
		mapPanel.getMap().addLayer(frame.getRoadLayer(), 5);
		mapPanel.getMap().addLayer(faceLayer, 10);

		return (IsochronePanel) frame.addTab(title, mapPanel);
	}

	public Stopwatch getLastTiming() {
		return lastTiming;
	}

	public IsoPolygon<Point2D> jtsPolygonToIsoPolygon(Polygon jtsPoly) {
		List<Point2D> outerRing = Arrays.stream(jtsPoly.getExteriorRing().getCoordinates())
				.map(c -> new Point2D.Double(c.x, c.y)).collect(Collectors.toList());

		List<Point2D> innerRing;
		List<List<Point2D>> innerRings = new LinkedList<>();
		for (int i = 0; i < jtsPoly.getNumInteriorRing(); ++i) {
			innerRing = Arrays.stream(jtsPoly.getInteriorRingN(i).getCoordinates())
					.map(c -> new Point2D.Double(c.x, c.y)).collect(Collectors.toList());
			innerRings.add(innerRing);
		}

		return new IsoPolygon<Point2D>(outerRing, innerRings, idGenerator);
	}

	public Optional<DiGraphNode<Point2D, GeofabrikData>> getRoadNode(Point2D location) {
		return Optional.ofNullable(roadGraph.getDiGraphNode(location, 5));
	}
}
