package au.com.darkside.xserver.Xext;

public class SystemCounter {
	public final byte		resHi;
	public final byte		resLo;
	public int COUNTER;
	public short nameLength;
	public byte[] name;
	public int nameLengthInt;
	public int padLen;

	/**
	 * Constructor.
	 *
	 * @param pmajorOpcode	Major opcode of the extension, or zero.
	 * @param pfirstEvent	Base event type code, or zero.
	 * @param pfirstError	Base error code, or zero.
	 */
	public SystemCounter (int ID, int presHi, int presLo, String pname) {
		COUNTER=ID;
		resHi = (byte) presHi;
		resLo = (byte) presLo;
		name=pname.getBytes();
		nameLengthInt=name.length;
		nameLength=(short)nameLengthInt;
		padLen=2;
	}
}
