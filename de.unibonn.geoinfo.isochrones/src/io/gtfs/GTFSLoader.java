package io.gtfs;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

import com.vividsolutions.jump.io.IllegalParametersException;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import graph.generic.DiGraph;
import graph.generic.DiGraph.DiGraphNode;
import graph.routing.DiGraphNodeComparator;
import graph.types.ArrivalNode;
import graph.types.DepartureNode;
import graph.types.IsoEdge;
import graph.types.IsoVertex;
import graph.types.PublicTransportEdge;
import graph.types.PublicTransportNode;
import graph.types.RoadNode;
import graph.types.TransferNode;
import main.AbstractMain;
import util.tools.Util;

public class GTFSLoader {

	private HashMap<Integer, IsoVertex> vertexMap; // all stops
	private HashMap<String, Integer> trips; // key: trip_id, value: service_id
	private HashMap<String, Integer> routeId; // key: trip_id, value: route_id
	private HashMap<Integer, String> routes; // key: route id, value: route_name
	private HashMap<Integer, Boolean[]> calendar = new HashMap<>();
	private HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> arrivalNodes;
	private HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> transferNodes;
	private HashMap<Integer, Integer> transferTimes = new HashMap<>();
	private HashMap<Integer, DiGraphNode<IsoVertex, IsoEdge>> nextStreetNode; // key: stop id, value: next street node
	private HashMap<DiGraphNode<IsoVertex, IsoEdge>, Integer> nextStop; // key: next street node, value: stop id

	private DiGraph<IsoVertex, IsoEdge> graph;

	public GTFSLoader(DiGraph<IsoVertex, IsoEdge> roadGraph) {
		this.graph = roadGraph;

		this.vertexMap = new HashMap<>();
		this.trips = new HashMap<>();
		this.routeId = new HashMap<>();
		this.routes = new HashMap<>();
		this.calendar = new HashMap<>();
		this.arrivalNodes = new HashMap<>();
		this.transferNodes = new HashMap<>();
		this.transferTimes = new HashMap<>();
		this.nextStreetNode = new HashMap<>();
		this.nextStop = new HashMap<>();
	}

	/**
	 * loads VRS data in GTFS format
	 * 
	 * @param graph
	 * @throws Exception
	 * @throws IllegalParametersException
	 */
	public void loadGTFS(File directory) throws IllegalParametersException, Exception {

		this.loadStop(directory);
		this.loadTrips(directory);
		this.loadRoutes(directory);
		this.loadCalendar(directory);
		this.loadStopTimes(directory);
		this.loadTransfers(directory);

		this.edgeTransferTransfer();

		this.vertexMap = null;
		this.trips = null;
		this.routeId = null;
		this.routes = null;
		this.calendar = null;
		this.arrivalNodes = null;

		// Connect Graphs
		// this.connectVrsStreet();
	}

	/**
	 * Loading stop names and positions.
	 * 
	 * @param graph Graph which will load the data
	 * @throws ParseException Exception, if parsing fails
	 * @author Jim
	 * @throws KeyDuplicateException
	 * @throws KeySizeException
	 */
	private void loadStop(File directory) throws ParseException, KeySizeException, KeyDuplicateException {
		if (AbstractMain.VERBOSE) {
			System.out.println();
			System.out.println("Start loading stops");
		}
		long starttime = System.currentTimeMillis();
		// Create and fill KDTree
		KDTree<DiGraphNode<IsoVertex, IsoEdge>> nextStreetNodeTree = new KDTree<>(2);
		for (DiGraphNode<IsoVertex, IsoEdge> v : graph.getNodes()) {
			double[] key = { ((RoadNode) v.getNodeData()).getX(), ((RoadNode) v.getNodeData()).getY() };
			if (nextStreetNodeTree.search(key) == null)
				nextStreetNodeTree.insert(key, v);
			else
				System.out.println("Did not insert key: " + key[0] + " " + key[1]);
		}

		// Read stops
		try {
			FileReader fr = new FileReader(directory + "/stops.txt");
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			br.readLine();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit;
				if (currentLine.equals("")) {
					continue;
				}
				String[] namesplit = currentLine.split("\"");

				if (namesplit.length != 1) {
					String[] firstPart = namesplit[0].split(",", 2);
					String[] middlePart = { namesplit[1] };
					String[] lastPart = namesplit[2].split(",");
					for (int i = 0; i < lastPart.length - 1; i++) {
						lastPart[i] = lastPart[i + 1];
					}
					String[] temp = Stream.concat(Arrays.stream(firstPart), Arrays.stream(middlePart))
							.toArray(String[]::new);
					lineSplit = Stream.concat(Arrays.stream(temp), Arrays.stream(lastPart)).toArray(String[]::new);

				} else {
					lineSplit = currentLine.split(",");
				}
				int id = Integer.parseInt(lineSplit[0]);
				Point2D asUTM = Util.lonlat2utm(Double.parseDouble(lineSplit[5]), Double.parseDouble(lineSplit[4]));

				RoadNode v = new RoadNode(asUTM.getX(), asUTM.getY(), lineSplit[2], id);
				double[] key = { v.getX(), v.getY() };

				this.vertexMap.put(id, v);

				DiGraphNode<IsoVertex, IsoEdge> nod = nextStreetNodeTree.nearest(key);

				((RoadNode) nod.getNodeData()).setNextStop(id);

				this.nextStreetNode.put(id, nod);
				this.nextStop.put(nod, id);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Finished loading stops");
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) / 1000 + "s");
		}
	}

	/**
	 * @author Peter
	 * @param trips
	 */
	private void loadTrips(File directory) {
		long starttime = System.currentTimeMillis();
		// Trips einlesen
		try {
			if (AbstractMain.VERBOSE)
				System.out.println("Start loading trips.");
			FileReader fr = new FileReader(directory + "/trips.txt");
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			br.readLine();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit = currentLine.split(",");
				int route_id = Integer.parseInt(lineSplit[0]);
				int service_id = Integer.parseInt(lineSplit[1]);
				String trip_id = lineSplit[2];
				this.trips.put(trip_id, service_id);
				this.routeId.put(trip_id, route_id);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Number of trips loaded: " + this.trips.size());
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
		}
	}

	/**
	 * @author Peter
	 * @param trips
	 */
	private void loadRoutes(File directory) {
		long starttime = System.currentTimeMillis();
		// Trips einlesen
		try {
			if (AbstractMain.VERBOSE)
				System.out.println("Start loading routes.");
			FileReader fr = new FileReader(directory + "/routes.txt");
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			br.readLine();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit = currentLine.split(",");
				int route_id = Integer.parseInt(lineSplit[0]);
				String route_name = lineSplit[2];
				if (route_name.isEmpty())
					route_name = lineSplit[3];
				this.routes.put(route_id, route_name);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Number of routes loaded: " + routes.size());
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
		}
	}

	/**
	 * @author Peter
	 * @param calendar
	 */
	private void loadCalendar(File directory) {
		long starttime = System.currentTimeMillis();
		// Trips einlesen
		try {
			if (AbstractMain.VERBOSE) {
				System.out.println();
				System.out.println("Start loading calendar.");
			}
			FileReader fr = new FileReader(directory + "/calendar.txt");
			BufferedReader br = new BufferedReader(fr);
			String currentLine;
			br.readLine();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit = currentLine.split(",");
				int service_id = Integer.parseInt(lineSplit[0]);
				Boolean[] days = new Boolean[7];
				for (int i = 0; i < days.length; i++) {
					int temp = Integer.parseInt(lineSplit[i + 1]);
					if (temp == 0)
						days[i] = false;
					else
						days[i] = true;
				}
				this.calendar.put(service_id, days);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Number of calendars loaded: " + this.calendar.size());
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
		}
	}

	/**
	 * Loading stop times using given names and coordinates
	 * 
	 * @param vertexMap Mapping the Id of the nodes to its vertices
	 * @param graph     Actual Graph, which loads stop times
	 * @throws ParseException Exception if parsing fails
	 * @author Peter
	 */
	private void loadStopTimes(File directory) {
		if (AbstractMain.VERBOSE) {
			System.out.println();
			System.out.println("Start loading stop times.");
		}
		long starttime = System.currentTimeMillis();
		try (BufferedReader br = new BufferedReader(new FileReader(directory + "/stop_times.txt"))) {
			String currentLine;
			br.readLine();

			/*
			 * Initialization
			 */
			SimpleDateFormat format = new SimpleDateFormat("kk:mm:ss");
			String tripId = "";

			LinkedList<Integer> stopId = new LinkedList<>();
			LinkedList<Date> arrivalTime = new LinkedList<>();
			LinkedList<Date> departureTime = new LinkedList<>();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit = currentLine.split(",");
				if (!tripId.equals(lineSplit[0]) && tripId != "") {
					erstelleTrip(tripId, arrivalTime, departureTime, stopId);
					arrivalTime.clear();
					departureTime.clear();
					stopId.clear();
				}
				tripId = lineSplit[0];
				arrivalTime.add(format.parse(lineSplit[1]));
				departureTime.add(format.parse(lineSplit[2]));
				stopId.add(Integer.parseInt(lineSplit[3]));
			}
			if (!arrivalTime.isEmpty()) {
				erstelleTrip(tripId, arrivalTime, departureTime, stopId);
				arrivalTime.clear();
				departureTime.clear();
				stopId.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) / 1000 + "s");
			System.out.println();
			System.out.println("Sortiere Nodes");
		}
		starttime = System.currentTimeMillis();
		this.sortList(this.arrivalNodes);
		this.sortList(this.transferNodes);
		if (AbstractMain.VERBOSE)
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
	}

	private void erstelleTrip(String tripId, LinkedList<Date> arrivalTime, LinkedList<Date> departureTime,
			LinkedList<Integer> stopId) {

		Boolean[] day = this.calendar.get(this.trips.get(tripId));

		for (int weekday = 0; weekday < day.length; weekday++) {
			if (!day[weekday])
				continue;
			long days = weekday * 86400L * 1000;
			Iterator<Date> arrTime = arrivalTime.iterator();
			Iterator<Date> depTime = departureTime.iterator();
			Iterator<Integer> id = stopId.iterator();
			erstelleTripDay(tripId, arrTime, depTime, id, days);
		}
	}

	private void erstelleTripDay(String tripId, Iterator<Date> arrTime, Iterator<Date> depTime, Iterator<Integer> id,
			long days) {
		int stopSequence = 0;
		DiGraphNode<IsoVertex, IsoEdge> departure = null;
		Date departureTime = null;
		while (id.hasNext()) {
			stopSequence++;
			Date arrivalTime = arrTime.next();
			Integer stopId = id.next();
			IsoVertex stop = this.vertexMap.get(stopId);

			int currentID = stop.getId();

			// ----------- Arrival Node -----------
			ArrivalNode arrival = new ArrivalNode(stop.getName(), currentID, new Date(arrivalTime.getTime() + days),
					tripId, stopSequence, this.routes.get(this.routeId.get(tripId)));

			if (!this.arrivalNodes.containsKey(currentID)) {
				this.arrivalNodes.put(currentID, new LinkedList<>());
			}

			if (nextStreetNode.containsKey(currentID)) {
				arrival.setNextStreetNode(nextStreetNode.get(currentID));
			} else {
				System.err.println("no next street node " + arrival);
			}

			DiGraphNode<IsoVertex, IsoEdge> arrivalNode = this.graph.addNode(arrival);
			this.arrivalNodes.get(currentID).add(arrivalNode);

			PublicTransportEdge e;
			/*
			 * If the new station in part of the route -> add edge between the departure
			 * visited before and the new arrival
			 */
			if (departure != null) {
				long time = (arrivalTime.getTime() - departureTime.getTime()) / 1000;
				if (time < 0)
					time += 86400;
				e = new PublicTransportEdge(time, -1);
				this.graph.addArc(departure, arrivalNode, e);
			}

			// ----------- TransferNode -----------
			departureTime = depTime.next();
			TransferNode transfer = new TransferNode(stop.getName(), currentID,
					new Date(departureTime.getTime() + days), tripId, stopSequence);

			if (!this.transferNodes.containsKey(currentID)) {
				this.transferNodes.put(currentID, new LinkedList<>());
			}

			DiGraphNode<IsoVertex, IsoEdge> transferNode = this.graph.addNode(transfer);
			this.transferNodes.get(currentID).add(transferNode);

			// ----------- Update departure node -----------
			DepartureNode dep = new DepartureNode(stop.getName(), currentID, new Date(departureTime.getTime() + days),
					tripId, stopSequence);
			departure = this.graph.addNode(dep);

			// Adding the Edge for direct connection
			long time = (departureTime.getTime() - arrivalTime.getTime()) / 1000;
			if (time < 0) {
				time += 86400;
			}
			e = new PublicTransportEdge(time, -1);
			this.graph.addArc(arrivalNode, departure, e);

			// Adding Edge between transfer and departure
			PublicTransportEdge n = new PublicTransportEdge(0, 0);
			this.graph.addArc(transferNode, departure, n);
		}
	}

	/**
	 * Loading Transfers and adding them into the Graph
	 * 
	 * @param graph
	 * @param arrivalNodes
	 * @param transferNodes
	 * @throws ParseException
	 */
	private void loadTransfers(File directory) throws ParseException {
		if (AbstractMain.VERBOSE) {
			System.out.println();
			System.out.println("Start loading transfers.");
		}
		long starttime = System.currentTimeMillis();
		int counterTransfers = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(directory + "/transfers.txt"))) {
			String currentLine;
			br.readLine();
			while ((currentLine = br.readLine()) != null) {
				String[] lineSplit = currentLine.split(",");
				int fromStop = Integer.parseInt(lineSplit[0]);
				int toStop = Integer.parseInt(lineSplit[1]);
				int transferType = Integer.parseInt(lineSplit[2]);
				int transferTime;

				try {
					transferTime = Integer.parseInt(lineSplit[3]);
				} catch (Exception e) {
					transferTime = AbstractMain.DEFAULT_TRANSFER_TIME;
				}

				if (fromStop == toStop) {
					this.transferTimes.put(fromStop, transferTime);
					continue;
				}

				// Get lists of all transfers and arrivals
				LinkedList<DiGraphNode<IsoVertex, IsoEdge>> arrivals = this.arrivalNodes.get(fromStop);
				LinkedList<DiGraphNode<IsoVertex, IsoEdge>> transfers = this.transferNodes.get(toStop);

				Iterator<DiGraphNode<IsoVertex, IsoEdge>> it = transfers.iterator();
				for (DiGraphNode<IsoVertex, IsoEdge> beginNode : arrivals) {
					// Extract and cast the arrival node
					PublicTransportNode b = (PublicTransportNode) beginNode.getNodeData();
					// Calculate starttime
					long startTime = (b.getTime().getTime() + transferTime * 1000l) % 1000 * 86400 * 7;
					/*
					 * Iterate through the transfer nodes, starting at the last seen point (do not
					 * reset i -> 0) This is possible by sorting the node by time
					 */
					while (true) {
						// Modulo for starting at the beginning again
						// FIXME: Transferknoten am naechsten Tag nehmen/naechste Woche
						if (!it.hasNext()) {
							it = transfers.iterator();
						}
						DiGraphNode<IsoVertex, IsoEdge> endNode = it.next();
						PublicTransportNode end = (PublicTransportNode) endNode.getNodeData();

						// Calculate endtime
						long endTime = end.getTime().getTime();
						/*
						 * if starttime + transfer time <= departure of the next connection, it can be
						 * reached -> arc added
						 */
						if (startTime <= endTime) {
							// Create a new edge
							IsoEdge e = new PublicTransportEdge(transferTime, transferType);
							// add the new edge to the graph
							this.graph.addArc(beginNode, endNode, e);
							counterTransfers++;
							// if one edge is found, the search do not have to continue
							break;
						}
					}
				}

			}
			if (AbstractMain.VERBOSE) {
				System.out.println("Number of Arrival-Transfer-edges: " + counterTransfers);
				System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creating Edges between TransferNode
	 * 
	 * @param sortedTransfers
	 * @param graph
	 * 
	 * @author Jim
	 */
	private void edgeTransferTransfer() {
		int counterTransfer = 0;
		long starttime = System.currentTimeMillis();
		// Iterate over all Stations
		for (LinkedList<DiGraphNode<IsoVertex, IsoEdge>> station : this.transferNodes.values()) {
			Iterator<DiGraphNode<IsoVertex, IsoEdge>> it = station.iterator();
			DiGraphNode<IsoVertex, IsoEdge> aktuell = it.next();
			DiGraphNode<IsoVertex, IsoEdge> neu;
			boolean kanteEingefuegt = false;
			// Create Edges between TransferNodes
			while (it.hasNext()) {
				neu = it.next();
				long t = ((PublicTransportNode) station.getFirst().getNodeData()).getTime().getTime()
						- ((PublicTransportNode) neu.getNodeData()).getTime().getTime() + 1000 * 7 * 86400;
				if (t < 0 && kanteEingefuegt == false) {
					t = ((PublicTransportNode) station.getFirst().getNodeData()).getTime().getTime()
							- ((PublicTransportNode) aktuell.getNodeData()).getTime().getTime() + 1000 * 7 * 86400;
					t /= 1000;
					IsoEdge e = new PublicTransportEdge(t, 0);
					this.graph.addArc(aktuell, station.getFirst(), e);
					kanteEingefuegt = true;
				}
				long time = ((PublicTransportNode) neu.getNodeData()).getTime().getTime()
						- ((PublicTransportNode) aktuell.getNodeData()).getTime().getTime();
				IsoEdge e = new PublicTransportEdge(time / 1000.0, 0);
				this.graph.addArc(aktuell, neu, e);
				aktuell = neu;
				counterTransfer++;
			}
		}
		if (AbstractMain.VERBOSE) {
			System.out.println("Number of transfer-transfer-edges: " + counterTransfer);
			System.out.println("Elapsed time: " + (System.currentTimeMillis() - starttime) + "ms");
		}
	}

	/**
	 * Rearranges the nodes to arrival and transfer lists for each station.
	 * Additionally returns the ID of the node in the DiGraph
	 * 
	 * @param nodeMap
	 * @param sortedNodes
	 * @param idMap
	 * 
	 * @author Peter
	 */
	private void sortList(Map<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> nodeMap) {
		// Create Lists
		for (LinkedList<DiGraphNode<IsoVertex, IsoEdge>> l : nodeMap.values()) {
			// Sort Lists by date
			Collections.sort(l, new DiGraphNodeComparator());
		}
	}

	public HashMap<Integer, LinkedList<DiGraphNode<IsoVertex, IsoEdge>>> getTransferNodes() {
		return transferNodes;
	}

	public HashMap<Integer, Integer> getTransferTimes() {
		return transferTimes;
	}
}
