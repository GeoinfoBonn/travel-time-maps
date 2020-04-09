package graph.routing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import graph.generic.DiGraph.DiGraphNode;
import graph.routing.Dijkstra.NodeIterator;
import graph.types.ArrivalNode;
import graph.types.IsoEdge;
import graph.types.IsoVertex;
import graph.types.PublicTransportNode;
import graph.types.RoadNode;
import graph.types.TransferNode;
import main.AbstractMain;

public class OwnIterator implements NodeIterator<IsoVertex, IsoEdge> {

	Dijkstra<IsoVertex, IsoEdge> dij;

	private HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> transferNodes;
	private HashMap<Integer, Integer> transferTimes;

	public OwnIterator(final HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> transferNodes,
			final HashMap<Integer, Integer> transferTimes, Dijkstra<IsoVertex, IsoEdge> dijkstra) {
		this.dij = dijkstra;
		this.transferNodes = transferNodes;
		this.transferTimes = transferTimes;
	}

	@Override
	public Iterator<DiGraphNode<IsoVertex, IsoEdge>> getIterator(DiGraphNode<IsoVertex, IsoEdge> s) {
		LinkedList<DiGraphNode<IsoVertex, IsoEdge>> a = new LinkedList<>();
		if (s.getNodeData() instanceof RoadNode && ((RoadNode) s.getNodeData()).isNextToStop()) {
//			if (((RoadNode) s.getNodeData()).getNextStopId() == 9601) {
//				System.out.println("Roisdorf");
//			}
			DiGraphNode<IsoVertex, IsoEdge> nextTransfer = getNextTransfer((RoadNode) s.getNodeData(),
					(long) this.dij.getDistance(s));
			if (nextTransfer != null)
				a.add(nextTransfer);
		} else if (s.getNodeData() instanceof ArrivalNode) {
			DiGraphNode<IsoVertex, IsoEdge> street = ((ArrivalNode) s.getNodeData()).getNextStreetNode();
			if (dij.stamps[street.getId()] != dij.currentStamp || dij.pred[street.getId()] == null)
				a.add(street);
		}
		if (a.size() == 1)
			if (a.getFirst() == null)
				a.removeFirst();
		return a.iterator();
	}

	@Override
	public double getWeightOfCurrentArc(DiGraphNode<IsoVertex, IsoEdge> s, DiGraphNode<IsoVertex, IsoEdge> t) {
		Objects.requireNonNull(s);
		Objects.requireNonNull(t);
		if (s.getNodeData() instanceof RoadNode && t.getNodeData() instanceof TransferNode) {
			TransferNode tr = (TransferNode) t.getNodeData();
			double transferTime = tr.getTime().getTime() / 1000.0; // in seconds
			double time = transferTime - dij.getCurrDist() + getTransferTime(tr.getId());
			// Transferzeit muss zwischen 0 und einer Woche liegen
			return (time + 7 * 86400) % (7 * 86400);
		}
		if (s.getNodeData() instanceof ArrivalNode && t.getNodeData() instanceof RoadNode) {
			long time = getTransferTime(s.getNodeData().getId());
			return time;
		}
		return 0;
	}

	/**
	 * 
	 * @return transfertime in seconds
	 */
	public long getTransferTime(int stop_id) {
		if (this.transferTimes.containsKey(stop_id)) {
			return (long) this.transferTimes.get(stop_id) / 2;
		} else {
			return (long) AbstractMain.DEFAULT_TRANSFER_TIME / 2;
		}
	}

	/**
	 * 
	 * @param arrId
	 * @param time  in ms
	 * @return
	 * @throws OutOfStreetNetworkException
	 */
	public DiGraphNode<IsoVertex, IsoEdge> getNextTransfer(RoadNode arr, long time) {
		int arrId = arr.getNextStopId();
		long transTime = this.getTransferTime(arrId);
		LinkedList<DiGraphNode<IsoVertex, IsoEdge>> t = this.transferNodes.get(arrId);

		for (DiGraphNode<IsoVertex, IsoEdge> d : t) {
			double transferTime = ((PublicTransportNode) d.getNodeData()).getTime().getTime() / 1000.0; // in seconds
			if (time < transferTime + transTime) {
				return d;
			}
		}
		return t.getFirst();
	}
}
