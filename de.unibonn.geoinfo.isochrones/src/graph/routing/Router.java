package graph.routing;

import java.util.List;

import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphNode;
import graph.types.ColoredNode;
import graph.types.DistanceData;
import graph.types.GeofabrikData;
import graph.types.IsoEdge;
import graph.types.IsoVertex;
import graph.types.RoadEdge;
import graph.types.TimeData;
import graph.types.WalkingData;

public interface Router<V, E extends WalkingData> {

	public void run(DiGraphNode<V, E> originalSource, int maxDistance);

	public void run(DiGraphNode<V, E> originalSource, long time, long bufferTime);

	public List<DiGraphNode<ColoredNode, E>> getSplitNodes();

	public DiGraph<ColoredNode, E> getColoredGraph();

	public DiGraph<IsoVertex, IsoEdge> getRoutingGraph();

	public DiGraphNode<ColoredNode, E> getLastSource();

	public void setStarttime(long starttime);

	public static interface Factory<E extends WalkingData, E_iso, E_road> {
		E createEdgeData(E_road data);

		E_iso createIsoEdgeData(E_road data);
	}

	public static final Factory<TimeData, RoadEdge, GeofabrikData> TIME_FACTORY = new Factory<>() {

		@Override
		public TimeData createEdgeData(GeofabrikData data) {
			return new TimeData(data.getValueAsTime());
		}

		@Override
		public RoadEdge createIsoEdgeData(GeofabrikData data) {
			return new RoadEdge(data.getValueAsTime());
		}
	};

	public static final Factory<DistanceData, RoadEdge, GeofabrikData> DIST_FACTORY = new Factory<>() {

		@Override
		public DistanceData createEdgeData(GeofabrikData data) {
			return new DistanceData(data.getValueAsDist());
		}

		@Override
		public RoadEdge createIsoEdgeData(GeofabrikData data) {
			return new RoadEdge(data.getValueAsTime());
		}
	};

	public static final Factory<GeofabrikData, GeofabrikData, GeofabrikData> GEOFABRIK_FACTORY = new Factory<>() {
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
