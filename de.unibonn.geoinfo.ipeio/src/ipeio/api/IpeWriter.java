package ipeio.api;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;

import ipeio.api.IpeTransformation;

public class IpeWriter {

	public static IPEDecimalFormat IPE_DECIMAL_FORMAT = new IPEDecimalFormat();

	protected IpeTransformation t;

	private final static String HEADER = "<?xml version=\"1.0\"?>\n" + " <!DOCTYPE ipe SYSTEM \"ipe.dtd\">\n"
			+ " <ipe version=\"70206\" creator=\"Ipe 7.2.7\">\n"
			+ " <info created=\"D:20170226182717\" modified=\"D:20170226182717\"/>\n";

	protected ArrayList<IpeStyle> styles;
	protected ArrayList<IpeDrawable> objects;
	protected ArrayList<Color> colors;
	protected TreeSet<String> layers;
	
	private String content;

	public IpeWriter() {
		objects = new ArrayList<IpeDrawable>();
		colors = new ArrayList<Color>();
		styles = new ArrayList<>();
		styles.add(IpeStyle.LARGESQUARED);
		layers = new TreeSet<>();
		
		content = "";

		t = new IpeTransformation(objects);
	}

	public void write(String path) throws IOException {
		File file = new File(path);
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write(HEADER);
		for (IpeStyle style : styles) {
			bw.write(style.toString());
		}
		bw.write("<page>\n");

		// layers
		for (String layer : layers) {
			bw.write("<layer name=\"" + layer + "\"/>\n");
		}

		String lastLayer = objects.get(0).layerList().get(0);
		StringBuilder view = new StringBuilder();
		for (IpeDrawable object : objects) {
			view.append("<view layers=\"");
			view.append(lastLayer + " ");
			for (String layer : object.layerList()) {
				view.append(layer + " ");
			}
			view.append("\" active=\"" + lastLayer + "\"/>\n");
		}

		bw.write(view.toString());

		// objects
		bw.write(content);
		for (int i = 0; i < objects.size(); ++i) {
			bw.write(objects.get(i).toIpeString(t, colors.get(i)));
		}
		bw.write("</page>\n</ipe>");
		bw.close();
		
		System.out.println("Ipe file written to: " + file.getAbsolutePath());
	}
	
	public void addDrawable(IpeDrawable drawable, Color color) {
		addDrawable(drawable, color, false);
	}
	
	public void addDrawable(IpeDrawable drawable, Color color, boolean addStatic) {
		this.objects.add(drawable);
		this.colors.add(color);
		this.layers.addAll(drawable.layerList());
		t = new IpeTransformation(objects);

		if (addStatic) {
			StringBuilder sb = new StringBuilder(content);
			sb.append(drawable.toIpeString(t, color));
			content = sb.toString();
		}
	}

	public String colorToIPE(Color c) {
		return IPE_DECIMAL_FORMAT.format(c.getRed() / 255.0) + " " + IPE_DECIMAL_FORMAT.format(c.getGreen() / 255.0)
				+ " " + IPE_DECIMAL_FORMAT.format(c.getBlue() / 255.0);
	}

	public static class IPEDecimalFormat extends DecimalFormat {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4510770158418580427L;

		public IPEDecimalFormat() {
			super("0.000");
			setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		}
	}

	public static String colorToIPE(Color c, DecimalFormat f) {
		double r = c.getRed() / 255.;
		double g = c.getGreen() / 255.;
		double b = c.getBlue() / 255.;
		return f.format(r) + " " + f.format(g) + " " + f.format(b);
	}
}