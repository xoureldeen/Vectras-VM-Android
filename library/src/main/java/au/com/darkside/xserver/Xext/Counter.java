package au.com.darkside.xserver.Xext;

public class Counter {
	public int COUNTER;
	public long value;
	public long firstTimestamp;
	public long firstTimestampNano;

	/**
	 * Constructor.
	 *
	 * @param pmajorOpcode	Major opcode of the extension, or zero.
	 * @param pfirstEvent	Base event type code, or zero.
	 * @param pfirstError	Base error code, or zero.
	 */
	public Counter (int ID, long initVal) {
		COUNTER=ID;
		value=initVal;
		firstTimestamp=System.currentTimeMillis();
		firstTimestampNano=System.nanoTime();
	}

	public long getValue() {
		return System.currentTimeMillis()-firstTimestamp;
	}
}
