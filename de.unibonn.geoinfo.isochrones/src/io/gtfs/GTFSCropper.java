package io.gtfs;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import main.AbstractMain;
import util.geometry.Envelope;
import util.tools.Util;

public class GTFSCropper {

	private File stopFile, stopTimesFile, transferFile, tripsFile, routesFile, agencyFile, calendarFile;
	private File outputDir;

	public GTFSCropper(final File inputDir, final File outputDir, final Envelope boundingBox) {
		this.setFiles(inputDir);
		this.outputDir = outputDir;
		if (outputDir.mkdir()) {
			if (AbstractMain.VERBOSE) {
				System.out.println(outputDir + " created");
			}
		} else {
			if (AbstractMain.VERBOSE) {
				System.out.println(outputDir + " not created (already present?)");
			}
		}

		this.cropToBoundingbox(boundingBox);
		System.out.println("GTFSCropper done!");
	}

	public void setFiles(final File inputDir) {
		this.stopFile = new File(inputDir + File.separator + "stops.txt");
		this.stopTimesFile = new File(inputDir + File.separator + "stop_times.txt");
		this.transferFile = new File(inputDir + File.separator + "transfers.txt");
		this.tripsFile = new File(inputDir + File.separator + "trips.txt");
		this.routesFile = new File(inputDir + File.separator + "routes.txt");
		this.agencyFile = new File(inputDir + File.separator + "agency.txt");
		this.calendarFile = new File(inputDir + File.separator + "calendar.txt");
	}

	public void cropToBoundingbox(Envelope env) {
		Set<Integer> stopsToRemove = processStops(env);
		processTransfers(stopsToRemove);
		Set<Integer> remainingTrips = processStopTimes(stopsToRemove);
		Set<Integer> remainingServices = new HashSet<>();
		Set<Integer> remainingRoutes = processTrips(remainingTrips, remainingServices);
		Set<Integer> remainingAgencies = processRoutes(remainingRoutes);
		processCalendar(remainingServices);
		processAgencies(remainingAgencies);
	}

	private void processCalendar(final Set<Integer> remainingServices) {
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(calendarFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "calendar.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int serviceId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				serviceId = Integer.parseInt(fields[0]);
				if (remainingServices.contains(serviceId)) {
					bw.write(line);
					bw.newLine();
				} else {
					removedCount++;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed services: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
	}

	private void processAgencies(final Set<Integer> remainingAgencies) {
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(agencyFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "agency.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int agencyId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				agencyId = Integer.parseInt(fields[0]);
				if (remainingAgencies.contains(agencyId)) {
					bw.write(line);
					bw.newLine();
				} else {
					removedCount++;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed agencies: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
	}

	private Set<Integer> processRoutes(final Set<Integer> remainingRoutes) {
		Set<Integer> remainingAgencies = new HashSet<>();
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(routesFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "routes.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int routeId, agencyId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				routeId = Integer.parseInt(fields[0]);
				agencyId = Integer.parseInt(fields[1]);
				if (remainingRoutes.contains(routeId)) {
					remainingAgencies.add(agencyId);
					bw.write(line);
					bw.newLine();
				} else {
					removedCount++;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed routes: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
		return remainingAgencies;
	}

	private Set<Integer> processTrips(final Set<Integer> remainingTrips, Set<Integer> remainingServices) {
		Set<Integer> remainingRoutes = new HashSet<>();
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(tripsFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "trips.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int routeId, tripId, serviceId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				routeId = Integer.parseInt(fields[0]);
				serviceId = Integer.parseInt(fields[1]);
				tripId = fields[2].hashCode();
				if (remainingTrips.contains(tripId)) {
					remainingRoutes.add(routeId);
					remainingServices.add(serviceId);
					bw.write(line);
					bw.newLine();
				} else {
					removedCount++;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed trips: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
		return remainingRoutes;
	}

	private void processTransfers(final Set<Integer> stopsToRemove) {
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(transferFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "transfers.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int fromId, toId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				fromId = Integer.parseInt(fields[0]);
				toId = Integer.parseInt(fields[1]);
				if (stopsToRemove.contains(fromId) || stopsToRemove.contains(toId)) {
					removedCount++;
				} else {
					bw.write(line);
					bw.newLine();
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed transfers: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
	}

	private Set<Integer> processStopTimes(final Set<Integer> stopsToRemove) {
		Set<Integer> remainingTrips = new HashSet<>();
		int count = 0;
		int removedCount = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(stopTimesFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "stop_times.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int stopId, tripId;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				tripId = fields[0].hashCode();
				stopId = Integer.parseInt(fields[3]);
				if (!stopsToRemove.contains(stopId)) {
					remainingTrips.add(tripId);
					bw.write(line);
					bw.newLine();
				} else {
					removedCount++;
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Removed stop_times: " + removedCount + "/" + count + " ("
				+ String.format("%5.2f", removedCount * 100.0 / count) + "%)");
		return remainingTrips;
	}

	public Set<Integer> processStops(final Envelope env) {
		int count = 0;
		Set<Integer> stopsToRemove = new HashSet<>();
		try (BufferedReader br = new BufferedReader(new FileReader(stopFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(outputDir + File.separator + "stops.txt"))) {
			String line = br.readLine(); // header
			if (line != null) {
				bw.write(line); // write header
				bw.newLine();
			}
			int stopId;
			double lon, lat;
			Point2D utm;
			String[] fields;
			while ((line = br.readLine()) != null && !line.isBlank()) {
				fields = line.split("(?=(?:(?:[^\"]*\"){2})*[^\"]*$)(?![^{]*})(?![^\\[]*\\]),");
				stopId = Integer.parseInt(fields[0]);
				lon = Double.parseDouble(fields[5]);
				lat = Double.parseDouble(fields[4]);
				utm = Util.lonlat2utm(lon, lat);
				if (!env.contains(utm)) {
					stopsToRemove.add(stopId);
				} else {
					bw.write(line);
					bw.newLine();
				}
				count++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Removed stops: " + stopsToRemove.size() + "/" + count + " ("
				+ String.format("%5.2f", stopsToRemove.size() * 100.0 / count) + "%)");
		return stopsToRemove;
	}

	public static void main(String[] args) {
		checkArguments(args, new String[] { "-i", "-o", "-bbox" });

		File inputDir = new File(getOptionalArg(args, "-i")); // input directory
		File outputDir = new File(getOptionalArg(args, "-o")); // output directory

		String[] bbIn = getOptionalArg(args, "-bbox").trim().split(",");
		if (bbIn.length != 4)
			throw new IllegalArgumentException("Bounding box needs to be given by: '[xMin],[xMax],[yMin],[yMax]'");

		double[] bbox = Arrays.stream(bbIn).mapToDouble(Double::parseDouble).toArray();
		Envelope bb = new Envelope(bbox[0], bbox[1], bbox[2], bbox[3]);

		new GTFSCropper(inputDir, outputDir, bb);
	}

	protected static boolean containsOptionalArg(String[] args, String identifier) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals(identifier)) {
				return true;
			}
		}
		return false;
	}

	public static String getOptionalArg(String[] args, String identifier) {
		for (int i = 0; i < args.length - 1; i++) {
			if (args[i].equals(identifier)) {
				return args[i + 1];
			}
		}
		return null;
	}

	protected static void checkArguments(String[] args, String[] obligatory) {
		for (String identifier : obligatory) {
			if (getOptionalArg(args, identifier) == null) {
				throw new InvalidParameterException(identifier + " must be set");
			}
		}
	}
}
