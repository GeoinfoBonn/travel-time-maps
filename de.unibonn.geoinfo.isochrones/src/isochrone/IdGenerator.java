package isochrone;

public class IdGenerator {

	private int nextFaceId, nextTimezoneId, nextComponentId;

	public IdGenerator() {
		this.nextFaceId = 0;
		this.nextTimezoneId = 0;
		this.nextComponentId = 0;
	}

	public synchronized int nextFaceId() {
		return nextFaceId++;
	}

	public synchronized int nextTimezoneId() {
		return nextTimezoneId++;
	}

	public synchronized int nextComponentId() {
		return nextComponentId++;
	}
}
