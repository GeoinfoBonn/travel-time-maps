package graph.types;

import java.awt.geom.Point2D;

public class RoadNode extends Point2D.Double implements IsoVertex {

	private String name;
	private int id;

	private int nextStop = -1;

	private static final long serialVersionUID = 708582318740278980L;

	public RoadNode(Point2D p) {
		this(p.getX(), p.getY(), "", -1);
	}

	public RoadNode(double x, double y, String name, int id) {
		this.setLocation(x, y);
		this.name = name;
		this.id = id;
	}

	public boolean isNextToStop() {
		return nextStop != -1;
	}

	public void setNextStop(int stopId) {
		this.nextStop = stopId;
	}

	public int getNextStopId() {
		return nextStop;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	@Override
	public int compareTo(IsoVertex otherVertex) {
		// TODO Auto-generated method stub
		return 0;
	}

}
