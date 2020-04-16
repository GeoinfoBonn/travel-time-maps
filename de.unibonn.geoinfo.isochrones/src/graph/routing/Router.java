package graph.routing;

import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphNode;
import graph.types.ColoredNode;
import graph.types.GeofabrikData;
import graph.types.IsoEdge;
import graph.types.IsoVertex;
import graph.types.WalkingData;

public interface Router<V, E extends WalkingData> {

	public void run(DiGraphNode<V, E> originalSource, int maxDistance);

	public void run(DiGraphNode<V, E> originalSource, long time, long bufferTime);

	public List<DiGraphNode<ColoredNode, E>> getSplitNodes();

	public DiGraph<ColoredNode, E> getColoredGraph();

	public DiGraph<IsoVertex, IsoEdge> getRoutingGraph();

	public DiGraphNode<ColoredNode, E> getLastSource();

	public void setStarttime(long starttime);

	public static interface Factory<E_iso, E_road> {
		E_road createEdgeData(E_road data);

		E_iso createIsoEdgeData(E_road data);
	}

	public static final Factory<GeofabrikData, GeofabrikData> GEOFABRIK_FACTORY = new Factory<>() {
		@Override
		public GeofabrikData createEdgeData(GeofabrikData data) {
			return new GeofabrikData(data);
		}

		@Override
		public GeofabrikData createIsoEdgeData(GeofabrikData data) {
			GeofabrikData isoData = new GeofabrikData(data);
			isoData.setValue(data.getValueAsTime());
			isoData.valueIsDistance(false);
			return isoData;
		}
	};

}
