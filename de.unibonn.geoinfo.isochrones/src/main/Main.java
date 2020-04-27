package main;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import com.vividsolutions.jump.io.IllegalParametersException;

import isochrone.BoundaryFace;
import isochrone.FaceIdentifier.FaceFactory;
import isochrone.IsoFace;
import isochrone.IsochroneCreator;
import isochrone.MinimumDistFace;
import isochrone.OctilinearFace;
import isochrone.Timezone;
import viewer.ResultFrame;

public class Main extends AbstractMain {

	public static void main(String[] args) throws IllegalParametersException, Exception {
		if (containsOptionalArg(args, IDENTIFIER.HELP)) {
			printHelp();
			return;
		}

		assert ASSERTION_ENABLED = true;
		read = new boolean[args.length];

		DEBUG = containsOptionalArg(args, IDENTIFIER.DEBUG);
		VERBOSE = containsOptionalArg(args, IDENTIFIER.VERBOSE_OUTPUT);
		SHOW_RESULTS = containsOptionalArg(args, IDENTIFIER.SHOW_RESULTS);

		INDIVIDUAL_RESULTS = containsOptionalArg(args, IDENTIFIER.CALCULATE_INDIVIDUAL_ZONES);
		USE_PARALLEL_PROCESSING = !containsOptionalArg(args, IDENTIFIER.DISABLE_PARALLEL_PROCESSING);

		checkArguments(args, new String[] { IDENTIFIER.ROAD_DATA_PATH, IDENTIFIER.GTFS_DATA_PATH });
		ROAD = new File(getOptionalArg(args, IDENTIFIER.ROAD_DATA_PATH).get());
		GTFS = new File(getOptionalArg(args, IDENTIFIER.GTFS_DATA_PATH).get());

		getOptionalArg(args, IDENTIFIER.START_ID).ifPresent(AbstractMain::createStartIds);

		if (containsOptionalArg(args, IDENTIFIER.OUTPUT_PATH))
			OUTPUT_DIRECTORY = new File(getOptionalArg(args, IDENTIFIER.OUTPUT_PATH).get());
		else
			OUTPUT_DIRECTORY = new File("output/");
		if (OUTPUT_DIRECTORY.mkdirs())
			if (AbstractMain.DEBUG)
				System.out.println(OUTPUT_DIRECTORY + " created");
			else if (AbstractMain.DEBUG)
				System.out.println(OUTPUT_DIRECTORY + " not created (already present?)");

		getOptionalArg(args, IDENTIFIER.VISUALIZATION_TYPE).ifPresent(AbstractMain::createTypes);
		getOptionalArg(args, IDENTIFIER.ROAD_FILTER).ifPresent(x -> FILTER_ROADS = Byte.parseByte(x));

		if (containsOptionalArg(args, IDENTIFIER.STATS_DIR)) {
			STATS_DIRECTORY = new File(getOptionalArg(args, IDENTIFIER.STATS_DIR).get());
		} else {
			STATS_DIRECTORY = OUTPUT_DIRECTORY;
		}

		KEEP_MOTORWAY = containsOptionalArg(args, IDENTIFIER.KEEP_MOTORWAY);

		getOptionalArg(args, IDENTIFIER.STARTTIME).ifPresent(x -> STARTTIME = Long.parseLong(x));
		if (containsOptionalArg(args, IDENTIFIER.TIMEZONES))
			createTimezones(getOptionalArg(args, IDENTIFIER.TIMEZONES).get(),
					getOptionalArg(args, IDENTIFIER.LINEAR_ZONES));
		if (containsOptionalArg(args, IDENTIFIER.DILATION)) {
			String dil = getOptionalArg(args, IDENTIFIER.DILATION).get();
			char lastChar = dil.charAt(dil.length() - 1);
			String val = dil.substring(0, dil.length() - 1);
			switch (lastChar) {
			case 'm':
				DILATION_FACTOR = Double.parseDouble(val);
				break;
			case 'a':
				DILATION_VALUE = Long.parseLong(val);
				break;
			default:
				System.err.println(
						"Unknown type for dilation factor! Use 'm' for multiplicative and 'a' for additive at the end of the value.");
			}
		}

		getOptionalArg(args, IDENTIFIER.MAX_DOR).ifPresent(x -> MAX_DoR = Integer.parseInt(x));
		ITERATE_DoR = !containsOptionalArg(args, IDENTIFIER.DoNot_ITERATE_DOR);

		getOptionalArg(args, IDENTIFIER.TIMED_BUFFER_FACTOR)
				.ifPresent(x -> TIMED_BUFFER_FACTOR = Double.parseDouble(x));

		getOptionalArg(args, IDENTIFIER.FIND_NODES_FILE).ifPresent(x -> FIND_NODE_FILE = new File(x));

		if (SHOW_RESULTS)
			GUI = new ResultFrame();

		long starttime = System.currentTimeMillis();
		System.setErr(new PrintStream(System.err) {
			@Override
			public void println(String x) {
				super.println(time() + " " + AbstractMain.identify() + " " + x);
			}

			private String time() {
				double time = System.currentTimeMillis() - starttime;
				String unit = "ms";

				if (time > 1000) {
					time /= 1000.0;
					unit = "s";

					if (time > 60) {
						time /= 60.0;
						unit = "min";

						if (time > 60) {
							time /= 60.0;
							unit = "h";
						}
					}
				}

				return String.format("%4.0f%-3s", time, unit);
			}
		});

		if (LOG_LEVEL > 1)
			System.setOut(new PrintStream(System.out) {
				@Override
				public void println(String x) {
					super.println(time() + " " + AbstractMain.identify() + " " + x);
				}

				private String time() {
					double time = System.currentTimeMillis() - starttime;
					String unit = "ms";

					if (time > 1000) {
						time /= 1000.0;
						unit = "s";

						if (time > 60) {
							time /= 60.0;
							unit = "min";

							if (time > 60) {
								time /= 60.0;
								unit = "h";
							}
						}
					}

					return String.format("%4.0f%-3s", time, unit);
				}
			});

		IsochroneCreator creator = new IsochroneCreator(ROAD, GTFS);

		if (FIND_NODE_FILE != null) {
			getRoadNodeIds(FIND_NODE_FILE, creator);
			System.exit(0);
		}

		if (START_IDS != null) {
			logConfig(args);
			File startpoints = new File(OUTPUT_DIRECTORY, "startpoints.csv");
			if (startpoints.exists())
				if (startpoints.delete() && AbstractMain.VERBOSE)
					System.out.println("Deleted previous startpoint file.");

			OutputWriter ow = new OutputWriter(STATS_DIRECTORY);
			ResultSet rs;

			long bufferTime;
			Timezone<Point2D> timezone;
			FaceFactory<?> faceFactory;

			int originalMaxDoR = MAX_DoR;
			double originalNonOctiMalus = NON_OCTI_MALUS;
			boolean originalWeightTurns = WEIGHT_TURNS;

			for (int startId : START_IDS) {
				for (byte type : VISUALIZATION_TYPES) {
					switch (type) {
					case OCTILINEAR:
						faceFactory = OctilinearFace.FACTORY;
						MAX_DoR = originalMaxDoR;
						NON_OCTI_MALUS = originalNonOctiMalus;
						WEIGHT_TURNS = originalWeightTurns;
						break;
					case BOUNDARY:
						faceFactory = BoundaryFace.FACTORY;
						MAX_DoR = -1;
						NON_OCTI_MALUS = 1;
						WEIGHT_TURNS = false;
						break;
					case MIN_LINK:
						faceFactory = MinimumDistFace.FACTORY;
						MAX_DoR = -1;
						NON_OCTI_MALUS = 1;
						WEIGHT_TURNS = false;
						break;
					case TIMED_BUFFER:
						faceFactory = null;
						break;
					default:
						System.err.println("Unknown visualization type! " + type);
						continue;
					}
					for (long time : TIMEZONES) {
						rs = new ResultSet(RunConfig.getCurrentRunConfig(startId, time, type));
						try {
							bufferTime = (long) (time * DILATION_FACTOR) + DILATION_VALUE;

							timezone = creator.createIsochrone(startId, STARTTIME, time, bufferTime, faceFactory);
							rs.setStopwatch(creator.getLastTiming());
							rs.setTimezone(timezone);

							if (!AbstractMain.INDIVIDUAL_RESULTS)
								IsoFace.setPolygonLimit(timezone);

							ow.writeResult(rs);
						} catch (Exception e) {
//						rs.addToMessage(e.getMessage());
							String message = "Error: " + type + ", " + startId + ", " + time + ", " + e.getMessage();
							System.err.println(message);
							System.out.println(message);
							e.printStackTrace();
						}
					}
				}
			}

			System.out.println("Done. Time needed for everything: " + (System.currentTimeMillis() - starttime) / 1000.0
					+ " seconds.");
		}
	}

	public static void getRoadNodeIds(File positions, IsochroneCreator creator) {
		System.out.println("Searching node indices:");
		try (BufferedReader br = new BufferedReader(new FileReader(positions))) {
			String line = br.readLine(); // header
			String[] words;
			Point2D position;
			while ((line = br.readLine()) != null) {
				words = line.split(",");
				position = new Point2D.Double(Double.parseDouble(words[0]), Double.parseDouble(words[1]));

				var node = creator.getRoadNode(position);
				node.ifPresentOrElse(x -> System.out.println("  found " + x.getId()),
						() -> System.out.println("  node not found"));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
