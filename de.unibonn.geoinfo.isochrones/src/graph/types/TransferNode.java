/**
 * 
 */
package graph.types;

import java.util.Date;

public class TransferNode extends PublicTransportNode {

    /**
     * @param lat
     * @param lon
     * @param name
     * @param id
     * @param time
     * @param tripId
     * @param stopSequence
     */
    public TransferNode(Date time, String tripId, int stopSequence) {
	super(time, tripId, stopSequence);
    }

    public TransferNode(String name, int id, Date time, String tripId, int stopSequence) {
	super(name, id, time, tripId, stopSequence);
    }

}
