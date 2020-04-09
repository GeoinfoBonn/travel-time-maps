package graph.planarizer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;

import gisviewer.LineMapObject;
import gisviewer.ListLayer;
import gisviewer.PointMapObject;
import graph.generic.DiGraph.DiGraphArc;
import graph.generic.GeometricGraph;
import viewer.IsochronePanel;
import viewer.ResultFrame;

public abstract class Planarizer<V extends Point2D, E> {

	protected GeometricGraph<V, E> inputGraph;
	protected PlanarGraph<V, E> planarGraph;

	protected Map<Point2D, List<DiGraphArc<V, E>>> crossPoints;
	protected Map<DiGraphArc<V, E>, List<Point2D>> crossedLines;

	protected PlanarizerFactory<V, E> factory;

	public void setInputGraph(GeometricGraph<V, E> g) {
		this.inputGraph = g;
	}

	public PlanarGraph<V, E> getPlanarGraph() {
		return planarGraph;
	}

	public abstract void planarize();

	public Map<Point2D, List<DiGraphArc<V, E>>> getCrosspoints() {
		return crossPoints;
	}

	public Map<DiGraphArc<V, E>, List<Point2D>> getCrossedLines() {
		return crossedLines;
	}

	public interface PlanarizerFactory<V, E> {
		V createNodeData(double x, double y);

		E createEdgeData(double dist);
	}

	public IsochronePanel showPlanarization(ResultFrame frame, String title) {
		return showPlanarization(frame, title, false);
	}

	public IsochronePanel showPlanarization(ResultFrame frame, String title, boolean withEdges) {
		IsochronePanel panel = new IsochronePanel(frame);

		ListLayer crosspointLayer = new ListLayer(Color.RED);
		ListLayer crossedEdgesLayer = new ListLayer(Color.GREEN);

		DiGraphArc<V, E> edge;
		for (Point2D crosspoint : this.getCrosspoints().keySet()) {
			crosspointLayer.add(new PointMapObject(crosspoint));

			if (withEdges) {
				edge = getCrosspoints().get(crosspoint).get(0);
				crossedEdgesLayer
						.add(new LineMapObject(edge.getSource().getNodeData(), edge.getTarget().getNodeData()));
				edge = getCrosspoints().get(crosspoint).get(1);
				crossedEdgesLayer
						.add(new LineMapObject(edge.getSource().getNodeData(), edge.getTarget().getNodeData()));
			}
		}

		panel.getMap().addLayer(frame.getRoadLayer(), 5);
		panel.getMap().addLayer(crossedEdgesLayer, 6);
		panel.getMap().addLayer(crosspointLayer, 10);

		return (IsochronePanel) frame.addTab(title, panel);
	}
}
