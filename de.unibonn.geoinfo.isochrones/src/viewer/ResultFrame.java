package viewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.DiGraph.DiGraphNode;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import util.geometry.Envelope;

public class ResultFrame {

	JFrame frame;
	JTabbedPane jTabpane;

	Component curr = null;
	Component prev = null;

	private Envelope currEnvelope = null;

	private ListLayer roadLayer = null;
	private KDTree<DiGraphNode<Point2D, ?>> roadNodes = null;

	public ResultFrame() {
		this("");
	}

	public ResultFrame(String maxtime) {
		this.frame = new JFrame("Result " + maxtime);
		this.frame.setSize(600, 600);
		this.frame.setVisible(true);

		this.jTabpane = new JTabbedPane();
		this.frame.add(jTabpane);

		this.jTabpane.addChangeListener(new Tabchanger(this));

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void setEnvelope(List<DiGraphNode<ColoredNode, GeofabrikData>> splitNodes) {
		this.currEnvelope = new Envelope();
		for (DiGraphNode<ColoredNode, GeofabrikData> node : splitNodes) {
			currEnvelope.expandToInclude(node.getNodeData().getX(), node.getNodeData().getY());
		}
	}

	public void setEnvelope(Envelope env) {
		this.currEnvelope = env;
	}

	public Envelope getEnvelope() {
		return currEnvelope;
	}

	public JPanel addTab(String title, JPanel panel) {
		this.jTabpane.addTab(title, panel);
		return panel;
	}

	public JTabbedPane getjTabpane() {
		return jTabpane;
	}

	public void setjTabpane(JTabbedPane jTabpane) {
		this.jTabpane = jTabpane;
	}

	public JFrame getFrame() {
		return this.frame;
	}

	public void initializeRoadLayer(DiGraph<Point2D, ?> g) {
		roadLayer = new ListLayer(Color.DARK_GRAY);

		Set<DiGraphArc<Point2D, ?>> paintedArcs = new HashSet<>();
		for (DiGraphArc<Point2D, ?> arc : g.getArcs()) {
			if (!paintedArcs.contains(arc)) {
				roadLayer.add(new LineMapObject(arc.getSource().getNodeData(), arc.getTarget().getNodeData()));
				paintedArcs.add(arc);
				if (arc.getTwin() != null)
					paintedArcs.add(arc.getTwin());
			}
		}

		roadNodes = new KDTree<>(2);
		try {
			for (DiGraphNode<Point2D, ?> n : g.getNodes()) {
				Point2D source = n.getNodeData();
				double[] k = { source.getX(), source.getY() };

				if (roadNodes.search(k) == null)
					this.roadNodes.insert(k, n);
				else
					System.out.println("Did not insert key. " + k[0] + " " + k[1]);
			}
		} catch (KeySizeException | KeyDuplicateException e) {
			e.printStackTrace();
		}
	}

	public DiGraphNode<Point2D, ?> getNearestRoadNode(double[] k) {
		try {
			return roadNodes.nearest(k);
		} catch (KeySizeException e) {
			e.printStackTrace();
			return null;
		} catch (NullPointerException e) {
			return null;
		}
	}

	public ListLayer getRoadLayer() {
		return roadLayer;
	}
}

class Tabchanger implements ChangeListener {
	private ResultFrame resultPanel;

	public Tabchanger(ResultFrame resultPanel) {
		this.resultPanel = resultPanel;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		JTabbedPane jTab = (JTabbedPane) e.getSource();

		if (jTab.getSelectedComponent() instanceof IsochronePanel) {
			if (resultPanel.prev != null) {
				// change from Isochrone to Isochrone
				resultPanel.prev = resultPanel.curr;
				resultPanel.curr = jTab.getSelectedComponent();
				IsochronePanel oldComponent = (IsochronePanel) resultPanel.prev;

				IsochronePanel currentComponent = (IsochronePanel) resultPanel.curr;
				currentComponent.getMap().setTransformation(oldComponent.getMap().getTransformation());

			} else {
				// first time an IsochronePanel is shown
				this.resultPanel.curr = jTab.getSelectedComponent();
				this.resultPanel.prev = jTab.getSelectedComponent();
			}
		}
	}
}

class OwnMouseListener implements MouseListener {
	private ResultFrame resultPanel;

	public OwnMouseListener(ResultFrame resultPanel) {
		this.resultPanel = resultPanel;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.resultPanel.getjTabpane().requestFocusInWindow();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

}
