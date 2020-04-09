package main;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import isochrone.IsoPolygon;
import isochrone.Timezone;

public class OutputWriter {

	private File fileZone;
	private File fileComponent;

	private int nBins = 16;

	public OutputWriter(File statsDir) {
		this.fileZone = new File(statsDir, "stats_zone.csv");
		this.fileComponent = new File(statsDir, "stats_component.csv");
	}

	public void writeResult(ResultSet result) {
		if (result.getTimezone() == null)
			return;

		boolean writeHeader = !fileZone.exists();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileZone, true))) {
			if (writeHeader)
				writeZoneHeader();
			bw.write(generateZoneString(result));
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writeHeader = !fileComponent.exists();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileComponent, true))) {
			if (writeHeader)
				writeComponentHeader();
			bw.write(generateComponentsString(result));
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String generateZoneString(ResultSet rs) {
		StringBuffer sb = new StringBuffer();

		Timezone<Point2D> timezone = rs.getTimezone();

		sb.append(rs.getTimezone().getId() + ",");
		sb.append((rs.getTimezone().isSuccess() ? 1 : 0) + ",");
		sb.append(rs.getConfig().getStartId() + ",");
		sb.append(rs.getTimezone().getTime() + ",");
		sb.append(rs.getConfig().getStarttime() + ",");
		sb.append(rs.getConfig().getVisualizationType() + ",");
		sb.append(rs.getConfig().getFilter() + ",");
		sb.append(rs.getConfig().getMaxDoR() + ",");
		sb.append(rs.getConfig().iterDoR() + ",");
		sb.append(rs.getConfig().getDilationFactor() + ",");
		sb.append(rs.getConfig().getRoad() + ",");
		sb.append(rs.getConfig().getTrain() + ",");
		sb.append(timezone.getPerimeter() + ",");
		sb.append(timezone.getOctiPerimeter() + ",");
		sb.append(timezone.getArea() + ",");
		sb.append(timezone.getNumHoles() + ",");
		sb.append(timezone.getAreaHoles() + ",");
		sb.append(timezone.getNumTurns() + ",");
		sb.append(timezone.getTP() + ",");
		sb.append(timezone.getFP() + ",");
		sb.append(timezone.getTN() + ",");
		sb.append(timezone.getFN() + ",");
		sb.append(timezone.getPolyList().size() + ",");
		sb.append(rs.getStopwatch().getTotalRuntime() + ",");
		sb.append(rs.getStopwatch().getRuntime("travelTimes") + ",");
		sb.append(rs.getStopwatch().getRuntime("visualizationGraph") + ",");
		sb.append(rs.getStopwatch().getRuntime("dcel") + ",");
		sb.append(rs.getStopwatch().getRuntime("findSplitnodes") + ",");
		sb.append(rs.getStopwatch().getRuntime("seperateSplitnodes") + ",");
		sb.append(rs.getStopwatch().getRuntime("faceIdentification") + ",");
		sb.append(rs.getStopwatch().getRuntime("graphCombine") + ",");
		sb.append(rs.getStopwatch().getRuntime("faceProcessing") + ",");
		sb.append(rs.getStopwatch().getRuntime("faceRouting") + ",");
		sb.append(rs.getStopwatch().getRuntime("innerIdentification") + ",");
		sb.append(rs.getStopwatch().getRuntime("recolor") + ",");
		sb.append(rs.getStopwatch().getRuntime("validation") + ",");
		sb.append(histToString(timezone.outerAngleHistogramm(nBins)) + ",");

		return sb.toString();
	}

	private String generateZoneHeader() {
		String[] headers = { "zone_id", "success", "start_id", "zone_time", "start_time", "type", "filter", "maxDoR",
				"iterDoR", "dilFactor", "road_path", "gtfs_path", "perimeter", "perimeter_octi", "area", "num_holes",
				"area_holes", "turns", "tp", "fp", "tn", "fn", "num_components", "run_time", "t_travelTimes",
				"t_visGraph", "t_dcel", "t_findSplitnodes", "t_seperateSplitnodes", "t_faceIden", "t_graphCombine",
				"t_faceProc", "t_faceRoute", "t_innerIden", "t_recolor", "t_validate", binIndexToString(nBins) };
		return generateHeader(headers);
	}

	public void writeZoneHeader() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileZone, true))) {
			bw.write(generateZoneHeader());
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String generateComponentsString(ResultSet rs) {
		StringBuffer sb = new StringBuffer();

		Timezone<Point2D> timezone = rs.getTimezone();

		boolean first = true;
		for (IsoPolygon<Point2D> poly : rs.getTimezone().getPolyList()) {
			if (!first) {
				sb.append("\n");
			} else {
				first = false;
			}
			sb.append(timezone.getId() + ",");
			sb.append(poly.getPerimeter() + ",");
			sb.append(poly.getOctiPerimeter() + ",");
			sb.append(poly.getArea() + ",");
			sb.append(poly.getNumHoles() + ",");
			sb.append(poly.getAreaHoles() + ",");
			sb.append(poly.getNumTurns() + ",");
			sb.append(poly.getCompactness() + ",");
			sb.append(poly.getComplexity() + ",");
			sb.append(poly.getNotchNorm() + ",");
			sb.append(poly.getAmplitudeOfVibration() + ",");
			sb.append(poly.getFrequencyOfVibration() + ",");
			sb.append(poly.getConvexity() + ",");
			sb.append(poly.getMessage() + ",");
			sb.append(histToString(poly.outerAngleHistogramm(nBins)) + ",");
		}

		return sb.toString();

	}

	private String generateComponentHeader() {
		String[] headers = { "zone_id", "perimeter", "perimeter_octi", "area", "num_holes", "area_holes", "turns",
				"compactness", "complexity", "nn", "aov", "fov", "convexity", "message", binIndexToString(nBins) };
		return generateHeader(headers);
	}

	public void writeComponentHeader() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileComponent, true))) {
			bw.write(generateComponentHeader());
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String generateHeader(String[] headers) {
		if (headers == null || headers.length < 1)
			return "";

		StringBuffer sb = new StringBuffer();

		sb.append(headers[0]);
		for (int i = 1; i < headers.length; ++i) {
			sb.append(",");
			sb.append(headers[i]);
		}

		return sb.toString();
	}

	private String histToString(int[] bins) {
		StringBuilder sb = new StringBuilder();

		sb.append(bins[0]);
		for (int i = 1; i < bins.length; ++i) {
			sb.append(",");
			sb.append(bins[i]);
		}

		return sb.toString();
	}

	private String binIndexToString(int nBins) {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("bin%02d", 0));
		for (int i = 1; i < nBins + 1; ++i) {
			sb.append(",");
			sb.append(String.format("bin%02d", i));
		}

		return sb.toString();
	}

	@SuppressWarnings("unused")
	private String histToString(double[] bins) {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("%6.2f", bins[0]));
		for (int i = 1; i < bins.length; ++i) {
			sb.append(",");
//			sb.append(String.format("%6.2f", bins[i]));
			sb.append(bins[i]);
		}

		return sb.toString();
	}
}
