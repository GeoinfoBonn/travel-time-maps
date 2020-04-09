package io.csv;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PointToCSV {

	public static void write(File file, Point2D point, boolean append) {
		write(file, point, null, null, append);
	}

	public static <D> void write(File file, Point2D point, D data, DataFactory<D> factory, boolean append) {

		List<Point2D> p = new LinkedList<>();
		p.add(point);

		List<D> d = null;
		if (data != null) {
			d = new LinkedList<>();
			d.add(data);
		}

		write(file, p, d, factory, append);
	}

	public static void write(File file, List<Point2D> points, boolean append) {
		write(file, points, null, null, append);
	}

	public static <D> void write(File file, List<Point2D> points, List<D> data, DataFactory<D> factory,
			boolean append) {

		boolean writeHeader = !append || !file.exists();

		int n = points.size();
		if (data != null && data.size() != n)
			throw new IllegalArgumentException("points and data need to have same size");

		if (data != null && factory == null)
			throw new IllegalArgumentException("factory is null");

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, append))) {
			if (writeHeader) {
				bw.write("x,y");
				if (factory != null) {
					for (String title : factory.getColumTitles()) {
						bw.write("," + title);
					}
				}
				bw.newLine();
			}

			Point2D p;
			D d;
			for (int i = 0; i < n; ++i) {
				p = points.get(i);
				bw.write(p.getX() + "," + p.getY());
				if (data != null) {
					d = data.get(i);
					for (String s : factory.getDataTerms(d))
						bw.write("," + s);
					bw.newLine();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static interface DataFactory<D> {
		public String[] getColumTitles();

		public String[] getDataTerms(D in);
	}

}
