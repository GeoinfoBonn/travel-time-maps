package graph.types;

public class PublicTransportEdge extends IsoEdge {

	public PublicTransportEdge(double weight, int type) {
		super(weight, type);
	}

	@Override
	public String toString() {
		return "VrsEdge[type=" + getType() + "; weight=" + getValue() + "]";
	}

}
