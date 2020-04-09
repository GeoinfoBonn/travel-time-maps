package main;

import java.awt.Color;

public class ColorStyle {

    public static final ColorStyle ORIGINAL = new ColorStyle(Color.GREEN, Color.RED, Color.BLUE);
    public static final ColorStyle PAPER = new ColorStyle(Color.decode("#91bfdb"), Color.decode("#fc8d59"),
	    Color.decode("#ffffbf"));

    private Color reachable, unreachable, buffer;
    private Color[] weights;

    public ColorStyle(Color reachable, Color unreachable, Color buffer) {
	this(reachable, unreachable, buffer, new Color[] { Color.decode("#1a9641"), Color.decode("#a6d96a"),
		Color.decode("#fdae61"), Color.decode("#d7191c") });
    }

    public ColorStyle(Color reachable, Color unreachable, Color buffer, Color[] weights) {
	this.reachable = reachable;
	this.unreachable = unreachable;
	this.buffer = buffer;
	this.weights = weights;
    }

    public Color reachable() {
	return reachable;
    }

    public Color unreachable() {
	return unreachable;
    }

    public Color buffer() {
	return buffer;
    }

    public Color weight(int i) {
	return weights[i];
    }
}
