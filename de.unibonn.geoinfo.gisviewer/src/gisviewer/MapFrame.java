package gisviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

/**
 * top-level frame that contains a map and a panel with the coordinates of the
 * mouse cursor
 * 
 * @author haunert
 */
public class MapFrame extends JFrame {

	/**
	 * the map displayed in this frame
	 */
	private Map myMap;

	/**
	 * the label displaying the x-coordinate
	 */
	private JLabel xLabel;

	/**
	 * the label displaying the y-coordinate
	 */
	private JLabel yLabel;

	private static final long serialVersionUID = 1L;

	/**
	 * constructor for crating an empty MapFrame
	 * 
	 * @param title:        the title displayed in the upper left corner of the
	 *                      frame
	 * @param isMainWindow: if set to true, the application will terminate if the
	 *                      frame is closed
	 */
	public MapFrame(String title, boolean isMainWindow) {

		super(title);

		setLayout(new BorderLayout());

		// labels showing the world coordinates of the mouse pointer
		xLabel = new JLabel();
		yLabel = new JLabel();

		myMap = new Map(this);

		// when fitting the content to the map extend, use some empty space at the
		// boundary
		myMap.setFrameRatio(0.1);

		JPanel myPanel = new JPanel();
		myPanel.setLayout(new GridLayout(2, 1));
		myPanel.add(xLabel);
		myPanel.add(yLabel);
		myPanel.setBorder(BorderFactory.createTitledBorder(new EtchedBorder(), "Map Coordinates", 0, 0));

		// some GUI attributes
		myMap.fitMapToDisplay();

		myPanel.setMinimumSize(new Dimension(200, 80));
		myPanel.setPreferredSize(new Dimension(200, 80));

		this.add(BorderLayout.NORTH, myPanel);
		this.add(BorderLayout.CENTER, myMap);

		// end program if the main window is closed
		if (isMainWindow) {
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				};
			});
		}
	}

	/**
	 * setter method used to update the coordinates displayed as text
	 * 
	 * @param x: the x-coordinate that is displayed
	 * @param y: the y-coordinate that is displayed
	 */
	public void setXYLabelText(double x, double y) {
		xLabel.setText(" x = " + x);
		xLabel.repaint();
		yLabel.setText(" y = " + y);
		yLabel.repaint();

	}

	/**
	 * the map displayed in this frame
	 * 
	 * @return the map
	 */
	public Map getMap() {
		return myMap;
	}
}
