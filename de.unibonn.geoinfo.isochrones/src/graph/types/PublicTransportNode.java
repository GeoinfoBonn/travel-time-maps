package graph.types;

import java.util.Date;

public abstract class PublicTransportNode implements IsoVertex {
	// Attributes
	private String name = "";
	private int id = 0;
	protected Date time = null;
	private String tripId = "";
	private int stopSequence = 0;

	public PublicTransportNode(Date time, String tripId, int stopSequence) {
		this.time = time;
		this.tripId = tripId;
		this.stopSequence = stopSequence;
	}

	/**
	 * @param lat          Latitude
	 * @param lon          Longitude
	 * @param name         Name of the node
	 * @param id           Id of the node
	 * @param time         Time of the node
	 * @param tripId       Id of the trip
	 * @param stopSequence Number of stop in the trip
	 */
	public PublicTransportNode(String name, int id, Date time, String tripId, int stopSequence) {
		this.name = name;
		this.id = id;
		this.time = time;
		this.tripId = tripId;
		this.stopSequence = stopSequence;
	}

	/**
	 * 
	 * @return Attribute time
	 */
	public Date getTime() {
		return time;
	}

	/**
	 * 
	 * @param time Set a new time
	 */
	public void setTime(Date time) {
		this.time = time;
	}

	/**
	 * 
	 * @return TripId of the node
	 */
	public String getTripId() {
		return tripId;
	}

	/**
	 * @param tripId set a new trip ID
	 */
	public void setTripId(String tripId) {
		this.tripId = tripId;
	}

	/**
	 * @return the stopSequence
	 */
	public int getStopSequence() {
		return stopSequence;
	}

	/**
	 * @param stopSequence the stopSequence to set
	 */
	public void setStopSequence(int stopSequence) {
		this.stopSequence = stopSequence;
	}

	/**
	 * Method for sorting lists of VrsNodes
	 * 
	 * @param otherNode Other VrsNode
	 * @return Integer for sorting
	 */
	public int compareTo(PublicTransportNode otherNode) {
		if (this.time.before(otherNode.time))
			return -1;
		if (this.time.after(otherNode.time))
			return 1;
		if (this.getId() < otherNode.getId())
			return -1;
		if (this.getId() > otherNode.getId())
			return 1;
		return 0;
	}

	@Override
	public int compareTo(IsoVertex otherVertex) {
		if (otherVertex.getClass() == PublicTransportNode.class || otherVertex.getClass() == ArrivalNode.class
				|| otherVertex.getClass() == TransferNode.class) {
			return this.compareTo((PublicTransportNode) otherVertex);
		}
		if (this.id < otherVertex.getId()) {
			return -1;
		} else if (this.id > otherVertex.getId()) {
			return 1;
		}
		return 0;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getId() {
		return this.id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}
}
