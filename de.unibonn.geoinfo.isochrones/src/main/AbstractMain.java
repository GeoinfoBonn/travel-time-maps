package main;

import java.io.File;
import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import viewer.ResultFrame;

public abstract class AbstractMain {

	public static final byte OCTILINEAR = 0;
	public static final byte BOUNDARY = 1;
	public static final byte MIN_LINK = 2;
	public static final byte TIMED_BUFFER = 3;

	public static boolean ASSERTION_ENABLED = false;

	public static boolean DEBUG = false;
	public static boolean VERBOSE = false;
	public static boolean SHOW_RESULTS = false;

	public static final int LOG_LEVEL = 2;

	public static int[] START_IDS;
	public static long STARTTIME = Long.valueOf(35999);
	public static Long[] TIMEZONES = { 1800l };
	public static boolean INDIVIDUAL_RESULTS = false;

	public static boolean USE_PARALLEL_PROCESSING = true;

	public static byte[] VISUALIZATION_TYPES = { OCTILINEAR };
	public static double DILATION_FACTOR = 0.0;
	public static long DILATION_VALUE = 0l;
	public static int MAX_DoR = 32;
	public static boolean ITERATE_DoR = true;
	public static byte FILTER_ROADS = 0;
	public static double TIMED_BUFFER_FACTOR = 0.5;

	public static File OUTPUT_DIRECTORY;
	public static File STATS_DIRECTORY;
	public static File ROAD;
	public static File GTFS;
	public static boolean KEEP_MOTORWAY = true;

	public static boolean WEIGHT_TURNS = true;
	public static double NON_OCTI_MALUS = 10000; // no final as it is change in case of no-octilinear visualization
	public static final double DISTANCE_FACTOR = 0.01;
	public static final double FACE_BOUNDARY_BUFFER = 0.05;
	public static final int DEFAULT_TRANSFER_TIME = 120;
	public static final ColorStyle COLOR_STYLE = ColorStyle.PAPER;

	protected static boolean[] read;

	protected static File FIND_NODE_FILE = null;

	public static ResultFrame GUI;

	protected static final class IDENTIFIER {

		// other launches
		public static final String FIND_NODES_FILE = "-f";

		// general
		public static final String HELP = "-h";
		public static final String DEBUG = "-d";
		public static final String VERBOSE_OUTPUT = "-v";
		public static final String SHOW_RESULTS = "-r";

		// launch config
		public static final String CALCULATE_INDIVIDUAL_ZONES = "-i";
		public static final String DISABLE_PARALLEL_PROCESSING = "-noPP";
		public static final String START_ID = "-s";
		public static final String VISUALIZATION_TYPE = "-type";
		public static final String COLOR_STYLE = "-c";

		// data
		public static final String OUTPUT_PATH = "-o";
		public static final String STATS_DIR = "-os";
		public static final String ROAD_DATA_PATH = "-dr";
		public static final String GTFS_DATA_PATH = "-dg";

		// run configuration
		public static final String STARTTIME = "-st";
		public static final String TIMEZONES = "-t";
		public static final String LINEAR_ZONES = "-lz";
		public static final String DILATION = "-dil";
		public static final String MAX_DOR = "-maxDoR";
		public static final String DoNot_ITERATE_DOR = "-noIterDoR";
		public static final String ROAD_FILTER = "-filter";
		public static final String TIMED_BUFFER_FACTOR = "-tbf";
		public static final String KEEP_MOTORWAY = "-km";

		public static Iterator<String> iterator() {
			return new Iterator<String>() {
				Field[] fields = IDENTIFIER.class.getFields();
				int i = 0;

				@Override
				public boolean hasNext() {
					return i < fields.length;
				}

				@Override
				public String next() {
					String ret;
					try {
						ret = (String) fields[i].get(null);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new java.util.NoSuchElementException();
					}
					++i;
					return ret;
				}
			};
		}

	}

	protected static void checkArguments(String[] args, String[] obligatory) {
		for (String identifier : obligatory) {
			if (getOptionalArg(args, identifier) == null) {
				throw new InvalidParameterException(identifier + " must be set");
			}
		}
	}

	public static Optional<String> getOptionalArg(String[] args, String identifier) {
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals(identifier)) {
				read[i] = true;
				read[i + 1] = true;
				return Optional.of(args[i + 1]);
			}
		}
		return Optional.empty();
	}

	protected static boolean containsOptionalArg(String[] args, String identifier) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(identifier)) {
				read[i] = true;
				return true;
			}
		}
		return false;
	}

	protected static void printUnreadArgs(String args[]) {
		System.out.println("Unread args: ");
		for (int i = 0; i < args.length; ++i) {
			if (!read[i]) {
				System.out.println("  " + i + ": " + args[i]);
			}
		}
	}

	public static String getHelpText(String id) {
		switch (id) {
		case IDENTIFIER.HELP:
			return IDENTIFIER.HELP + "\t\t" + "-\t" + "-\t" + "Display this text for help.";
		case IDENTIFIER.VERBOSE_OUTPUT:
			return IDENTIFIER.VERBOSE_OUTPUT + "\t\t" + "-\t" + "-\t" + "Set for verbose output during run time.";
		case IDENTIFIER.DEBUG:
			return IDENTIFIER.DEBUG + "\t\t" + "-\t" + "-\t" + "Enables Debug output with even more graphs/sysos.";
		case IDENTIFIER.SHOW_RESULTS:
			return IDENTIFIER.SHOW_RESULTS + "\t\t" + "-\t" + "-\t" + "Set to show results in GUI.";
		case IDENTIFIER.DISABLE_PARALLEL_PROCESSING:
			return IDENTIFIER.DISABLE_PARALLEL_PROCESSING + "\t\t" + "-\t" + "-\t"
					+ "Parallel processing of faces is enabled by default. In case this causes errors it can be diables by this flag.";
		case IDENTIFIER.START_ID:
			return IDENTIFIER.START_ID + "\t\t" + "int\t" + "-\t"
					+ "Enables fast-mode. Select start node in road graph by its ID. If no start id is given, start node can be selected on map.";
		case IDENTIFIER.VISUALIZATION_TYPE:
			return IDENTIFIER.VISUALIZATION_TYPE + "\t\t" + "-\t" + "-\t"
					+ "Set to disable calculation of octilinear timezones. Boundary of faces is used for visualization instead.";
		case IDENTIFIER.OUTPUT_PATH:
			return IDENTIFIER.OUTPUT_PATH + "\t\t" + "string\t" + "m\t" + "File path of output folder. Default: ."
					+ File.separator + "output" + File.separator + "[start_id]" + File.separator;
		case IDENTIFIER.ROAD_DATA_PATH:
			return IDENTIFIER.ROAD_DATA_PATH + "\t\t" + "string\t" + "m\t" + "Input shapefile for road network data.";
		case IDENTIFIER.GTFS_DATA_PATH:
			return IDENTIFIER.GTFS_DATA_PATH + "\t\t" + "string\t" + "m\t" + "Input directory with GTFS data.";
		default:
			return "unknown identifier";
		}
	}

	protected static void printHelp() {
		Iterator<String> it = IDENTIFIER.iterator();
		while (it.hasNext()) {
			System.out.println(getHelpText(it.next()));
		}
	}

	protected static void createTimezones(String timezones, Optional<String> linear) {
		String[] values = timezones.split(",");
		TIMEZONES = new Long[values.length];
		for (int i = 0; i < values.length; ++i)
			TIMEZONES[i] = Long.parseLong(values[i].trim());
		Arrays.sort(TIMEZONES, Collections.reverseOrder());

		if (linear.isPresent()) {
			int numberZones = Integer.parseInt(linear.get().trim());
			long maxTime = TIMEZONES[0];
			TIMEZONES = new Long[numberZones];
			for (int i = numberZones; i > 0; --i) {
				TIMEZONES[numberZones - i] = (i * maxTime) / numberZones;
				System.out.println("Timezone added: " + TIMEZONES[numberZones - i] + "s");
			}
		}
	}

	protected static void createStartIds(String ids) {
		String[] values = ids.split(",");
		START_IDS = new int[values.length];
		for (int i = 0; i < values.length; ++i)
			START_IDS[i] = Integer.parseInt(values[i].trim());
	}

	protected static void createTypes(String types) {
		String[] values = types.split(",");
		VISUALIZATION_TYPES = new byte[values.length];
		for (int i = 0; i < values.length; ++i)
			VISUALIZATION_TYPES[i] = Byte.parseByte(values[i].trim());
		Arrays.sort(VISUALIZATION_TYPES);
	}

	protected static void logConfig(String args[]) {
		System.out.println();
		Main m = new Main();
		for (Field field : m.getClass().getSuperclass().getDeclaredFields()) {
			String name = field.getName();
			Object value = null;
			try {
				value = field.get(m);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
			System.out.printf("%s = %s%n", name, value);
		}
		System.out.println();
		printUnreadArgs(args);
		System.out.println();
	}

	/**
	 * Get the current line number.
	 * 
	 * @return int - Current line number.
	 */
	protected static int identifyLineNumber() {
		return Thread.currentThread().getStackTrace()[4].getLineNumber();
	}

	protected static String identifyClassName() {
		return Thread.currentThread().getStackTrace()[4].getClassName();
	}

	protected static String identify() {
		return String.format("[%-35s",
				String.format("%s:%04d]", AbstractMain.identifyClassName(), AbstractMain.identifyLineNumber()));
	}
}
