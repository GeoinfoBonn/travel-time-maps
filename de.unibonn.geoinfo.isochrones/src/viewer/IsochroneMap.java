package viewer;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import gisviewer.Map;
import gisviewer.Transformation;
import util.geometry.Envelope;

public class IsochroneMap extends Map {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1126970055185616774L;
	/**
	 * The IsochronePanel containing this MapDisplay.
	 */
	private IsochronePanel isoPanel;

	public IsochroneMap(IsochronePanel isoPanel) {
		super(null);
		this.isoPanel = isoPanel;
		this.addMouseWheelListener(new WheelZoomListener(this.isoPanel));
		this.addKeyListener(new KeyboardMoveZoomListener(this.isoPanel));
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		int r = e.getY();
		int c = e.getX();
		this.isoPanel.setXYLabelText(super.getTransformation().getX(c), super.getTransformation().getY(r));
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// Focus f�r KeyListener setzen
		this.requestFocusInWindow();
	}

	private Transformation zoomTransformation(int row, int column, Transformation myTransformation, boolean zoomIn) {
		double xCenter = myTransformation.getX(column);
		double yCenter = myTransformation.getY(row);
		if (zoomIn) {
			myTransformation.setM(1.25 * myTransformation.getM());
		} else {
			myTransformation.setM(0.8 * myTransformation.getM());
		}
		int rowTemp = myTransformation.getRow(yCenter);
		int columnTemp = myTransformation.getColumn(xCenter);
		myTransformation.setColumnOrigin(myTransformation.getColumnOrigin() + column - columnTemp);
		myTransformation.setRowOrigin(myTransformation.getRowOrigin() + row - rowTemp);
		return myTransformation;
	}

	public void fitBoxToDisplay(Envelope env) {
		this.fitBoxToDisplay(env.getxMin(), env.getyMin(), env.getxMax(), env.getyMax());
	}

	private class WheelZoomListener implements MouseWheelListener {
		private IsochronePanel isoPanel;

		public WheelZoomListener(IsochronePanel isoPanel) {
			this.isoPanel = isoPanel;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int row = e.getY();
			int column = e.getX();
			Transformation myTransformation = this.isoPanel.getMap().getTransformation();
			if (e.getWheelRotation() < 0) {
				myTransformation = zoomTransformation(row, column, myTransformation, true);
			} else {
				myTransformation = zoomTransformation(row, column, myTransformation, false);
			}
			this.isoPanel.getMap().setTransformation(myTransformation);
			this.isoPanel.repaint();
		}

	}

	private class KeyboardMoveZoomListener implements KeyListener {
		private IsochronePanel isoPanel;

		public KeyboardMoveZoomListener(IsochronePanel isoPanel) {
			this.isoPanel = isoPanel;
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			// Zentrum der Map
			Rectangle mapRectangle = this.isoPanel.getMap().getBounds();
			int row = (int) Math.round(mapRectangle.getSize().getHeight() / 2);
			int column = (int) Math.round(mapRectangle.getSize().getWidth() / 2);

			// Transformation berechnen
			Transformation myTransformation = this.isoPanel.getMap().getTransformation();
			switch (e.getKeyCode()) {
			case KeyEvent.VK_W:
				myTransformation.setRowOrigin(myTransformation.getRowOrigin() + 10);
				break;
			case KeyEvent.VK_S:
				myTransformation.setRowOrigin(myTransformation.getRowOrigin() - 10);
				break;
			case KeyEvent.VK_A:
				myTransformation.setColumnOrigin(myTransformation.getColumnOrigin() + 10);
				break;
			case KeyEvent.VK_D:
				myTransformation.setColumnOrigin(myTransformation.getColumnOrigin() - 10);
				break;
			case KeyEvent.VK_Q:
				myTransformation = zoomTransformation(row, column, myTransformation, true);
				break;
			case KeyEvent.VK_E:
				myTransformation = zoomTransformation(row, column, myTransformation, false);
				break;
			default:
				return;
			}
			// Transformation an Panel �bergeben
			this.isoPanel.getMap().setTransformation(myTransformation);
			this.isoPanel.repaint();
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

	}

}
