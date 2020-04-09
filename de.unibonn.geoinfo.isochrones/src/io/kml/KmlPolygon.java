package io.kml;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

import isochrone.IsoPolygon;
import isochrone.Timezone;
import util.tools.Util;

public class KmlPolygon {

	// Attribute
	private String filePathAndName;
	private Document kmlDocument;
	private Element document;

	/*
	 * Constructors
	 */
	public KmlPolygon() {

	}

	public KmlPolygon(String file) {
		this.filePathAndName = file;
	}

	public KmlPolygon(String file, LinkedList<Timezone<Point2D>> timezones) {
		this.filePathAndName = file;
		this.initDocument();
		this.saveAllZones(timezones);
	}

	private void initDocument() {
		// Create kml document and set kml element as root
		Element kml = new Element("kml");
		kmlDocument = new Document(kml);

		// Start the document
		document = new Element("Document");
		kml.addContent(document);
	}

	private void addZoneToDocument(Timezone<Point2D> zone) {
		this.addZoneToDocument(zone, this.document);
	}

	private void addZoneToDocument(Timezone<Point2D> zone, Element document) {
		for (IsoPolygon<Point2D> poly : zone.getPolyList()) {
			for (Polygon p : poly.getOuterPolygons()) {
				addPolygonToDocument(p, zone.getTime(), document);
			}
		}
	}

	private void addPolygonToDocument(Polygon poly, long time, Element document) {
		// Placemark erstellen
		Element placemark = new Element("Placemark");
		document.addContent(placemark);

		Element name = new Element("name");
		placemark.addContent(name);
		name.addContent(time + "sec");

		// Using given styleUrl for different time zones
		Element styleURL = new Element("styleUrl");
		placemark.addContent(styleURL);
		styleURL.addContent("#poly" + time);

		// Create element for each polygon
		Element polygon = new Element("Polygon");
		placemark.addContent(polygon);

		// Create element for outerBoundary
		Element outerB = new Element("outerBoundaryIs");
		polygon.addContent(outerB);

		// Create element for LinearRing
		Element linearRing = new Element("LinearRing");
		outerB.addContent(linearRing);

		// Add coordinates-element
		linearRing.addContent(this.createCoordinatesElement(poly.getExteriorRing()));

		// Adding inner rings
		for (int i = 0; i < poly.getNumInteriorRing(); i++) {
			Element innerB = new Element("innerBoundaryIs");
			polygon.addContent(innerB);

			Element linRing = new Element("LinearRing");
			innerB.addContent(linRing);

			// Add coordinates-element
			linRing.addContent(this.createCoordinatesElement(poly.getInteriorRingN(i)));
		}
	}

	public void saveAllZones(List<Timezone<Point2D>> timezones) {
		this.initDocument();
		this.createStyles(timezones);

		for (Timezone<Point2D> zone : timezones) {
			this.addZoneToDocument(zone);
		}

		this.kmlErstellen();
	}

	public void saveSingleZone(Timezone<Point2D> zone) {
		this.initDocument();
		this.createSingleStyle(zone);
		this.addZoneToDocument(zone);
		this.kmlErstellen(kmlDocument, zone);
	}

	public void saveSingleZones(List<Timezone<Point2D>> timezones) {
		for (Timezone<Point2D> zone : timezones) {
			this.saveSingleZone(zone);
		}
	}

	private void createSingleStyle(Timezone<Point2D> zone) {
		// Create different styles for later use
		Element style = new Element("Style");
		document.addContent(style);
		style.setAttribute("id", "poly" + zone.getTime());
		Element pstyle = new Element("PolyStyle");
		style.addContent(pstyle);

		// Define the colors
		Element color = new Element("color");
		color.addContent(zone.getColor());
		pstyle.addContent(color);

		// Define if outer boundary is drawn in kml
		Element outline = new Element("outline");
		outline.addContent("0");
		pstyle.addContent(outline);
	}

	private void createStyles(Iterable<Timezone<Point2D>> zones) {
		// Different durations
		for (Timezone<Point2D> zone : zones) {
			this.createSingleStyle(zone);
		}
	}

	private Element createCoordinatesElement(LineString ring) {
		Element coordinates = new Element("coordinates");
		Point2D lonlat;

		for (Coordinate c : ring.getCoordinates()) {
			lonlat = utm2deg(c.x, c.y);
			coordinates.addContent(lonlat.getX() + "," + lonlat.getY() + "\n");
		}
		return coordinates;
	}

	/**
	 * Write the created kml-file to a file on disk
	 */
	public void kmlErstellen() {
		this.kmlErstellen(this.kmlDocument);
	}

	public void kmlErstellen(Timezone<Point2D> zone) {
		this.kmlErstellen(this.kmlDocument, zone);
	}

	public void kmlErstellen(Document kmlDocument) {
		// Create kml outputter
		XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		try {
			// Write kml-file
			FileWriter fw = new FileWriter(new File(this.filePathAndName + ".kml"), false);
			out.output(kmlDocument, fw);
			fw.close();
			System.out.println(this.filePathAndName + " erfolgreich erstellt.");
			// Catch errors
		} catch (IOException e) {
			System.err.println("IO-Fehler");
		} catch (Exception e) {
			System.err.println("Fehler");
		}
	}

	public void kmlErstellen(Document kmlDocument, Timezone<Point2D> zone) {
		// Create kml outputter
		XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
		try {
			String filename = this.filePathAndName;
			// Write kml-file
			FileWriter fw = new FileWriter(new File(filename + ".kml"), false);
			out.output(kmlDocument, fw);
			fw.close();
			System.out.println(filename + " erfolgreich erstellt.");
			// Catch errors
		} catch (IOException e) {
			System.err.println("IO-Fehler");
		} catch (Exception e) {
			System.err.println("Fehler");
		}
	}

	public static Point2D utm2deg(double x, double y) {
		return Util.utm2lonlat(x, y);
	}
}
