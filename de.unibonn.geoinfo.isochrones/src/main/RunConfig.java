package main;

import java.io.File;

public class RunConfig {
	private int startId;
	private File road;
	private File train;

	// run configuration
	private byte visualizationType;
	private long starttime;
	private long timezone;
	private double distanceFactor;
	private double dilationFactor;
	private double faceBoundaryBuffer;
	private int defaultTransferTime;
	private double nonOctiMalus;
	private int maxDoR;
	private boolean iterDoR;
	private byte filter;

	private int numberOfZone;

	public RunConfig(int startId, File road, File train, byte visualizationType, long starttime, long timezone,
			double distanceFactor, double dilationFactor, double faceBoundaryBuffer, int maxDoR,
			int defaultTransferTime, double nonOctiMalus, int numberOfZone, boolean iterDoR, byte filter) {
		super();
		this.startId = startId;
		this.road = road;
		this.train = train;
		this.visualizationType = visualizationType;
		this.starttime = starttime;
		this.timezone = timezone;
		this.distanceFactor = distanceFactor;
		this.dilationFactor = dilationFactor;
		this.faceBoundaryBuffer = faceBoundaryBuffer;
		if (visualizationType == AbstractMain.OCTILINEAR)
			this.maxDoR = maxDoR;
		else
			this.maxDoR = -1;
		this.defaultTransferTime = defaultTransferTime;
		this.nonOctiMalus = nonOctiMalus;
		this.numberOfZone = numberOfZone;
		this.iterDoR = iterDoR;
		this.filter = filter;
	}

	public static RunConfig getCurrentRunConfig(int startId, long zoneTime, byte visualizationType) {
		int numberOfZone = 0;
		if (!AbstractMain.INDIVIDUAL_RESULTS && AbstractMain.TIMEZONES.length > 1)
			for (int i = 0; i < AbstractMain.TIMEZONES.length; ++i)
				if (AbstractMain.TIMEZONES[i] == zoneTime)
					numberOfZone = i;

		return new RunConfig(startId, AbstractMain.ROAD, AbstractMain.GTFS, visualizationType, AbstractMain.STARTTIME,
				zoneTime, AbstractMain.DISTANCE_FACTOR, AbstractMain.DILATION_FACTOR, AbstractMain.FACE_BOUNDARY_BUFFER,
				AbstractMain.MAX_DoR, AbstractMain.DEFAULT_TRANSFER_TIME, AbstractMain.NON_OCTI_MALUS, numberOfZone,
				AbstractMain.ITERATE_DoR, AbstractMain.FILTER_ROADS);
	}

	public int getStartId() {
		return startId;
	}

	public File getRoad() {
		return road;
	}

	public File getTrain() {
		return train;
	}

	public byte getVisualizationType() {
		return visualizationType;
	}

	public long getStarttime() {
		return starttime;
	}

	public long getTimezone() {
		return timezone;
	}

	public double getDistanceFactor() {
		return distanceFactor;
	}

	public double getDilationFactor() {
		return dilationFactor;
	}

	public double getFaceBoundaryBuffer() {
		return faceBoundaryBuffer;
	}

	public int getMaxDoR() {
		return maxDoR;
	}

	public int getDefaultTransferTime() {
		return defaultTransferTime;
	}

	public double getNonOctiMalus() {
		return nonOctiMalus;
	}

	public int getNumberOfZone() {
		return numberOfZone;
	}

	public boolean iterDoR() {
		return iterDoR;
	}

	public byte getFilter() {
		return filter;
	}
}
