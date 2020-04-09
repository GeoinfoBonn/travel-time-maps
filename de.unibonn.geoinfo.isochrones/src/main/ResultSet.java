package main;

import java.awt.geom.Point2D;

import isochrone.Timezone;
import tools.Stopwatch;

public class ResultSet {

	private String message;

	private RunConfig config;

	private Timezone<Point2D> timezone;

	private Stopwatch sw;

	public ResultSet(RunConfig config) {
		this.config = config;
		this.message = "";
	}

	public Timezone<Point2D> getTimezone() {
		return timezone;
	}

	public void setTimezone(Timezone<Point2D> timezone) {
		this.timezone = timezone;
	}

	public Stopwatch getStopwatch() {
		return sw;
	}

	public void setStopwatch(Stopwatch sw) {
		this.sw = sw;
	}

	public String getMessage() {
		return message;
	}

	public void addToMessage(String additionalMessage) {
		if (!message.contentEquals("")) {
			message += ", ";
		}
		message += additionalMessage;
	}

	public RunConfig getConfig() {
		return config;
	}
}
