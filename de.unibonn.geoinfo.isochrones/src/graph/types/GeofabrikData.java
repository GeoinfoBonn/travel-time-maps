package graph.types;

public class GeofabrikData extends RoadEdge implements WalkingData {

	String osmId;
	Integer code;
	String fclass;
	byte oneway;
	Integer maxspeed;
	Integer layer;
	boolean bridge;
	boolean tunnel;

	boolean valueIsDistance = true;

	public GeofabrikData(double gemetricDist, String osmId, Integer code, String fclass, byte oneway, Integer maxspeed,
			Integer layer, boolean bridge, boolean tunnel) {
		super(gemetricDist);
		this.osmId = osmId;
		this.code = code;
		this.fclass = fclass;
		this.oneway = oneway;
		this.maxspeed = maxspeed;
		this.layer = layer;
		this.bridge = bridge;
		this.tunnel = tunnel;
	}

	public GeofabrikData(double gemetricDist) {
		super(gemetricDist);
	}

	public GeofabrikData(double gemetricDist, String fclass) {
		this(gemetricDist);
		this.fclass = fclass;
	}

	public GeofabrikData(GeofabrikData copy) {
		this(copy.getValueAsDist(), copy.getOsmId(), copy.getCode(), copy.fclass(), copy.getOneway(),
				copy.getMaxspeed(), copy.getLayer(), copy.isBridge(), copy.isTunnel());
	}

	public String fclass() {
		return fclass;
	}

	public String getOsmId() {
		return osmId;
	}

	public Integer getCode() {
		return code;
	}

	public byte getOneway() {
		return oneway;
	}

	public Integer getMaxspeed() {
		return maxspeed;
	}

	public Integer getLayer() {
		return layer;
	}

	public boolean isBridge() {
		return bridge;
	}

	public boolean isTunnel() {
		return tunnel;
	}

	public void valueIsDistance(boolean vid) {
		this.valueIsDistance = vid;
	}

	public boolean isValueDistance() {
		return valueIsDistance;
	}

	@Override
	public double getValueAsTime() {
		if (valueIsDistance)
			return getValue() / WalkingData.WALKING_SPEED;
		else
			return getValue();
	}

	@Override
	public double getValueAsDist() {
		if (valueIsDistance)
			return getValue();
		else
			return getValue() * WalkingData.WALKING_SPEED;
	}
}
