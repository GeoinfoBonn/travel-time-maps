package viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import gisviewer.ListLayer;
import gisviewer.Map;
import gisviewer.PointMapObject;
import graph.generic.DiGraph.DiGraphNode;

public class IsochronePanel extends JPanel {

	private static final long serialVersionUID = -1213534859561103030L;

	/**
	 * the map displayed in this frame
	 */
	private IsochroneMap myMap;

	/**
	 * the label displaying the x-coordinate
	 */
	private JLabel xLabel;

	/**
	 * the label displaying the y-coordinate
	 */
	private JLabel yLabel;

	private JPanel infoPanel;

	private transient ListLayer positionLayerR;
	private transient ListLayer positionLayerG;
	private transient ListLayer positionLayerB;
	private transient ListLayer positionLayerY;

	private JTextField insertPosition;
	private JButton markPositionR;
	private JButton markPositionG;
	private JButton markPositionB;
	private JButton markPositionY;
	private JButton clearMarks;

	private JButton zoomToSplitnodes;

	private JLabel lblSelectedNode;

	private transient ResultFrame superFrame;

	private transient ActionListener markPositionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			String posString = insertPosition.getText().trim();
			Pattern r = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
			Matcher m = r.matcher(posString);
			if (!m.find()) {
				System.err.println("Wrong input coordinate format");
				return;
			}

			double x = Double.parseDouble(m.group(1));
			double y = Double.parseDouble(m.group(3));

			if (e.getSource() == markPositionR) {
				positionLayerR.add(new PointMapObject(new Point2D.Double(x, y)));
				myMap.addLayer(positionLayerR, 1000);
			} else if (e.getSource() == markPositionG) {
				positionLayerG.add(new PointMapObject(new Point2D.Double(x, y)));
				myMap.addLayer(positionLayerG, 1001);
			} else if (e.getSource() == markPositionB) {
				positionLayerB.add(new PointMapObject(new Point2D.Double(x, y)));
				myMap.addLayer(positionLayerB, 1002);
			} else if (e.getSource() == markPositionY) {
				positionLayerY.add(new PointMapObject(new Point2D.Double(x, y)));
				myMap.addLayer(positionLayerY, 1003);
			}

//			invalidate();
//			myMap.invalidate();
//			superFrame.frame.invalidate();
//			superFrame.jTabpane.invalidate();
//
//			revalidate();
//			myMap.revalidate();
//			superFrame.frame.revalidate();
//			superFrame.jTabpane.revalidate();
//
//			validate();
//			myMap.validate();
//			superFrame.frame.validate();
//			superFrame.jTabpane.validate();
//
//			updateUI();
//			myMap.updateUI();
//			superFrame.jTabpane.updateUI();
//
//			repaint();
//			myMap.repaint();
//			superFrame.frame.repaint();
//			superFrame.jTabpane.repaint();

			myMap.fitBoxToDisplay(x - 100, y - 100, x + 100, y + 100);
		}
	};

	public IsochronePanel(ResultFrame superFrame) {
		this.superFrame = superFrame;

		setLayout(new BorderLayout());

		// labels showing the world coordinates of the mouse pointer
		this.xLabel = new JLabel();
		this.yLabel = new JLabel();

		this.lblSelectedNode = new JLabel("Not clicked yet.");

		this.positionLayerR = new ListLayer(Color.RED);
		this.positionLayerG = new ListLayer(Color.GREEN);
		this.positionLayerB = new ListLayer(Color.BLUE);
		this.positionLayerY = new ListLayer(Color.YELLOW);

		this.insertPosition = new HintTextField("Insert position: x, y");

		this.markPositionR = new JButton("Mark");
		this.markPositionR.setBackground(Color.RED);
		this.markPositionR.addActionListener(markPositionListener);

		this.markPositionG = new JButton("Mark");
		this.markPositionG.setBackground(Color.GREEN);
		this.markPositionG.addActionListener(markPositionListener);

		this.markPositionB = new JButton("Mark");
		this.markPositionB.setForeground(Color.WHITE);
		this.markPositionB.setBackground(Color.BLUE);
		this.markPositionB.addActionListener(markPositionListener);

		this.markPositionY = new JButton("Mark");
		this.markPositionY.setBackground(Color.YELLOW);
		this.markPositionY.addActionListener(markPositionListener);

		this.clearMarks = new JButton("Clear Marks");
		this.clearMarks.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				positionLayerR = new ListLayer(Color.RED);
				positionLayerG = new ListLayer(Color.GREEN);
				positionLayerB = new ListLayer(Color.BLUE);
				positionLayerY = new ListLayer(Color.YELLOW);
				myMap.removeLayer(1000);
				myMap.removeLayer(1001);
				myMap.removeLayer(1002);
				myMap.removeLayer(1003);
				myMap.fitMapToDisplay();
			}
		});

		this.zoomToSplitnodes = new JButton("Zoom to Extend");
		this.zoomToSplitnodes.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (superFrame.getEnvelope() != null)
					myMap.fitBoxToDisplay(superFrame.getEnvelope());
				else
					System.out.println("Envelope not set yet. Be patient!");
			}
		});

		this.myMap = new IsochroneMap(this);

		// when fitting the content to the map extend, use some empty space at the
		// boundary
		this.myMap.setFrameRatio(0.1);

		this.infoPanel = new JPanel(new GridLayout(1, 2));

		JPanel coordinatePanel = new JPanel(new GridLayout(2, 1));
		coordinatePanel.add(this.xLabel);
		coordinatePanel.add(this.yLabel);
		this.infoPanel.add(coordinatePanel);

		JPanel markPanel = new JPanel(new GridLayout(1, 4));
		markPanel.add(markPositionR);
		markPanel.add(markPositionG);
		markPanel.add(markPositionB);
		markPanel.add(markPositionY);

		JPanel navigationPanel = new JPanel(new GridLayout(3, 2));
		navigationPanel.add(new JLabel());
		navigationPanel.add(insertPosition);
		navigationPanel.add(zoomToSplitnodes);
		navigationPanel.add(markPanel);
		navigationPanel.add(lblSelectedNode);
		navigationPanel.add(clearMarks);

		this.infoPanel.add(navigationPanel);
		this.infoPanel.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(), "Map Coordinates", 0, 0));

		// some GUI attributes
		this.myMap.fitMapToDisplay();

		this.infoPanel.setMinimumSize(new Dimension(200, 80));
		this.infoPanel.setPreferredSize(new Dimension(200, 80));

		this.add(BorderLayout.NORTH, this.infoPanel);
		this.add(BorderLayout.CENTER, this.myMap);

		myMap.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double x = myMap.getTransformation().getX(e.getX());
				double y = myMap.getTransformation().getY(e.getY());
				double[] k = { x, y };
				DiGraphNode<Point2D, ?> start = superFrame.getNearestRoadNode(k);
				if (start != null)
					lblSelectedNode.setText("selected Node: " + start.getId());
				else
					lblSelectedNode.setText("Seems like the road graph is not initialized for ResultFrame.");
			}
		});
	}

	/**
	 * setter method used to update the coordinates displayed as text
	 * 
	 * @param x: the x-coordinate that is displayed
	 * @param y: the y-coordinate that is displayed
	 */
	public void setXYLabelText(double x, double y) {
		this.xLabel.setText(" x = " + x);
		this.xLabel.repaint();
		this.yLabel.setText(" y = " + y);
		this.yLabel.repaint();

	}

	public ResultFrame getSuperFrame() {
		return superFrame;
	}

	/**
	 * the map displayed in this frame
	 * 
	 * @return the map
	 */
	public Map getMap() {
		return this.myMap;
	}

	public void createPanelTime(long max_time) {
		Color[] color = { Color.GREEN, Color.CYAN, Color.BLUE, Color.magenta, Color.RED };
		int time = (int) (max_time / 60) / 5;
		JPanel panelTime = new JPanel(new GridLayout(5, 1));
		for (int i = 0; i < color.length; i++) {
			JLabel lbl = new JLabel(time * i + " min" + " - " + time * (i + 1) + " min");
			lbl.setForeground(color[i]);
			panelTime.add(lbl);
		}
		this.infoPanel.add(panelTime);
	}

	public static IsochronePanel showRoadGraph(ResultFrame frame, String title) {
		IsochronePanel panel = new IsochronePanel(frame);
		panel.getMap().addLayer(frame.getRoadLayer(), 5);
		return (IsochronePanel) frame.addTab(title, panel);
	}
}
