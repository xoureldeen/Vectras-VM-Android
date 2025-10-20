package au.com.darkside.xserver;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.io.IOException;

/**
 * This class handles an X keyboard.
 *
 * @author Matthew Kwan
 */
public class Keyboard {
    private int _minimumKeycode;
    private int _numKeycodes;
    private byte _keysymsPerKeycode = 3;
    private int[] _keyboardMapping = null;
    private byte _keycodesPerModifier = 8;
    private byte[] _keymap = new byte[32];
	private byte[] _modifierMapping = new byte[] {
		KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT, 0, 0, 0, 0, 0, 0,  // 1
		0, 0, 0, 0, 0, 0, 0, 0, // 2
		KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT, 0, 0, 0, 0, 0, 0, // 4
		KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT, 0, 0, 0, 0, 0, 0, // 8
		0, 0, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, // 20
		KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT, 0, 0, 0, 0, 0, 0, // 40
		0, 0, 0, 0, 0, 0, 0, 0  // 80
    };

    private static final int DefaultBellPercent = 50;
    private int _bellPercent = DefaultBellPercent;
    private static final int DefaultBellPitch = 400;
    private int _bellPitch = DefaultBellPitch;
    private static final int DefaultBellDuration = 100;
    private int _bellDuration = DefaultBellDuration;
    private short[] _bellBuffer = null;
    private boolean _bellBufferFilled = false;
    private AudioTrack _audioTrack = null;

    private static final int SAMPLE_RATE = 11025;
    private static final int AttrKeyClickPercent = 0;
    private static final int AttrBellPercent = 1;
    private static final int AttrBellPitch = 2;
    private static final int AttrBellDuration = 3;
    private static final int AttrLed = 4;
    private static final int AttrLedMode = 5;
    private static final int AttrKey = 6;
    private static final int AttrAutoRepeatMode = 7;

    /**
     * Constructor.
     */
    Keyboard() {
        final byte kpk = _keysymsPerKeycode;
        int min = 255;
        int max = 0;
        int idx = 0;
        int[] map = new int[256 * kpk];
        KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.FULL);
        // Some devices don't have keyboard with full type, and the Android OS uses
        // the KeyCharacterMap.BUILT_IN_KEYBOARD (value is 0) as the fallback keyboard
        // type. If the phone/tablet doesn't have built in keyboard, the returned
        // KeyCharacterMap is empty, and it will causes the ArrayOutOfBounds exception
        // when initializing keyboard mapping.
        if (kcm.getKeyboardType() != KeyCharacterMap.FULL) {
            kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        }

        for (int i = 0; i < 256; i++) {
            int c1 = kcm.get(i, 0);
            int c2 = kcm.get(i, KeyEvent.META_SHIFT_ON);
            int c3 = kcm.get(i, KeyEvent.META_ALT_ON);

            map[idx++] = c1;
            map[idx++] = c2;
            map[idx++] = c3;

            if (c1 != 0 || c2 != 0 || c3 != 0) {
                if (i < min) min = i;
                if (i > max) max = i;
            }
        }

        if (max == 0) min = 0;

        if (max < KeyEvent.KEYCODE_DEL) max = KeyEvent.KEYCODE_DEL;

        _minimumKeycode = min;
        _numKeycodes = max - min + 1;
        if (_numKeycodes > 248) _numKeycodes = 248;

        _keyboardMapping = new int[kpk * _numKeycodes];
        System.arraycopy(map, min * kpk, _keyboardMapping, 0, _keyboardMapping.length);

        // Keycode translation table
        _keyboardMapping[(KeyEvent.KEYCODE_FORWARD_DEL - min) * kpk] = 0xffff; // del
        _keyboardMapping[(KeyEvent.KEYCODE_DEL - min) * kpk] = 0xff08; // backspace
        _keyboardMapping[(KeyEvent.KEYCODE_ALT_LEFT - min) * kpk] = 0xffe9;
        _keyboardMapping[(KeyEvent.KEYCODE_ALT_RIGHT - min) * kpk] = 0xfe03;
        _keyboardMapping[(KeyEvent.KEYCODE_CTRL_LEFT - min) * kpk] = 0xffe3;
        _keyboardMapping[(KeyEvent.KEYCODE_CTRL_RIGHT - min) * kpk] = 0xffe4;
        _keyboardMapping[(KeyEvent.KEYCODE_ENTER - min) * kpk] = 0xff0d; // enter/return
        _keyboardMapping[(KeyEvent.KEYCODE_DPAD_UP - min) * kpk] = 0xff52;
        _keyboardMapping[(KeyEvent.KEYCODE_DPAD_DOWN - min) * kpk] = 0xff54;
        _keyboardMapping[(KeyEvent.KEYCODE_DPAD_LEFT - min) * kpk] = 0xff51;
        _keyboardMapping[(KeyEvent.KEYCODE_DPAD_RIGHT - min) * kpk] = 0xff53;
        _keyboardMapping[(KeyEvent.KEYCODE_PAGE_UP - min) * kpk] = 0xff55;
        _keyboardMapping[(KeyEvent.KEYCODE_PAGE_DOWN - min) * kpk] = 0xff56;
        _keyboardMapping[(KeyEvent.KEYCODE_MOVE_HOME - min) * kpk] = 0xff50;
        _keyboardMapping[(KeyEvent.KEYCODE_MOVE_END - min) * kpk] = 0xff57;
        _keyboardMapping[(KeyEvent.KEYCODE_ESCAPE - min) * kpk] = 0xff1b;
        _keyboardMapping[(KeyEvent.KEYCODE_F1 - min) * kpk] = 0xffbe;
        _keyboardMapping[(KeyEvent.KEYCODE_F2 - min) * kpk] = 0xffbf;
        _keyboardMapping[(KeyEvent.KEYCODE_F3 - min) * kpk] = 0xffc0;
        _keyboardMapping[(KeyEvent.KEYCODE_F4 - min) * kpk] = 0xffc1;
        _keyboardMapping[(KeyEvent.KEYCODE_F5 - min) * kpk] = 0xffc2;
        _keyboardMapping[(KeyEvent.KEYCODE_F6 - min) * kpk] = 0xffc3;
        _keyboardMapping[(KeyEvent.KEYCODE_F7 - min) * kpk] = 0xffc4;
        _keyboardMapping[(KeyEvent.KEYCODE_F8 - min) * kpk] = 0xffc5;
        _keyboardMapping[(KeyEvent.KEYCODE_F9 - min) * kpk] = 0xffc6;
        _keyboardMapping[(KeyEvent.KEYCODE_F10 - min) * kpk] = 0xffc7;
        _keyboardMapping[(KeyEvent.KEYCODE_F11 - min) * kpk] = 0xffc8;
        _keyboardMapping[(KeyEvent.KEYCODE_F12 - min) * kpk] = 0xffc9;
        _keyboardMapping[(KeyEvent.KEYCODE_INSERT - min) * kpk] = 0xff63;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_0 - min) * kpk] = 0xffb0;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_1 - min) * kpk] = 0xffb1;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_2 - min) * kpk] = 0xffb2;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_3 - min) * kpk] = 0xffb3;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_4 - min) * kpk] = 0xffb4;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_5 - min) * kpk] = 0xffb5;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_6 - min) * kpk] = 0xffb6;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_7 - min) * kpk] = 0xffb7;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_8 - min) * kpk] = 0xffb8;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_9 - min) * kpk] = 0xffb9;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_ADD - min) * kpk] = 0xffab;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_COMMA - min) * kpk] = 0xffac;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_DIVIDE - min) * kpk] = 0xffaf;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_DOT - min) * kpk] = 0xffae;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_ENTER - min) * kpk] = 0xff8d;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_EQUALS - min) * kpk] = 0xffbd;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_MULTIPLY - min) * kpk] = 0xffaa;
        _keyboardMapping[(KeyEvent.KEYCODE_NUMPAD_SUBTRACT - min) * kpk] = 0xffad;
        _keyboardMapping[(KeyEvent.KEYCODE_NUM_LOCK - min) * kpk] = 0xff7f;
        _keyboardMapping[(KeyEvent.KEYCODE_META_LEFT - min) * kpk] = 0xffeb; // Windows / Super key
        _keyboardMapping[(KeyEvent.KEYCODE_META_RIGHT - min) * kpk] = 0xffec; // Windows / Super key
        _keyboardMapping[(KeyEvent.KEYCODE_TAB - min) * kpk] = 0xff09;
        _keyboardMapping[(KeyEvent.KEYCODE_BREAK - min) * kpk] = 0xff13;
        _keyboardMapping[(KeyEvent.KEYCODE_SCROLL_LOCK - min) * kpk] = 0xff14;
        _keyboardMapping[(KeyEvent.KEYCODE_SYSRQ - min) * kpk] = 0xff61; // print screen
        _keyboardMapping[(KeyEvent.KEYCODE_SHIFT_LEFT - min) * kpk] = 0xffe1;
        _keyboardMapping[(KeyEvent.KEYCODE_SHIFT_RIGHT - min) * kpk] = 0xffe2;
    }

    /**
     * Translate an Android keycode to an X keycode.
     *
     * @param keycode The Android keycode.
     * @return The corresponding X keycode.
     */
    public int translateToXKeycode(int keycode) {
        if (_minimumKeycode < 8) return keycode + 8 - _minimumKeycode;
        else return keycode;
    }

    /**
     * Return the minimum keycode.
     *
     * @return The minimum keycode.
     */
    public int getMinimumKeycode() {
        if (_minimumKeycode < 8) return 8;
        else return _minimumKeycode;
    }

    /**
     * Return the minimum keycode diff.
     *
     * @return The minimum keycode.
     */
    public int getMinimumKeycodeDiff() {
        if (_minimumKeycode < 8) return 8 - _minimumKeycode;
        else return 0;
    }

    /**
     * Return the maximum keycode.
     *
     * @return The maximum keycode.
     */
    public int getMaximumKeycode() {
        return getMinimumKeycode() + _numKeycodes - 1;
    }

    /**
     * Return the keymap for keycodes 8-255.
     *
     * @return The keymap for keycodes 8-255.
     */
    public byte[] getKeymap() {
        byte[] keymap = new byte[31];

        System.arraycopy(_keymap, 1, keymap, 0, 31);

        return keymap;
    }

    /**
     * Update the keymap when a key is pressed or released.
     *
     * @param keycode The keycode of the key.
     * @param pressed True if pressed, false if released.
     */
    public void updateKeymap(int keycode, boolean pressed) {
        if (keycode < 0 || keycode > 255) return;

        int offset = keycode / 8;
        byte mask = (byte) (1 << (keycode & 7));

        if (pressed) _keymap[offset] |= mask;
        else _keymap[offset] &= ~mask;
    }

    /**
     * Process an X request relating to this keyboard.
     *
     * @param xServer        The X server.
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    public void processRequest(XServer xServer, Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.QueryKeymap:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(2);    // Reply length.
                        io.writeBytes(_keymap, 0, 32);    // Keys.
                    }
                    io.flush();
                }
                break;
            case RequestCode.ChangeKeyboardMapping:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    byte keycodeCount = arg;
                    byte keycode = (byte) io.readByte();    // First code.
                    byte kspkc = (byte) io.readByte();    // Keysyms per code.

                    io.readSkip(2);    // Unused.
                    bytesRemaining -= 4;

                    if (bytesRemaining != keycodeCount * kspkc * 4) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                    } else if (kspkc > _keysymsPerKeycode) {
                        io.readSkip (bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Value, opcode, 0);
                    } else {
                        int numKeycodes = keycode + keycodeCount;
                        if (numKeycodes > _numKeycodes ) {
                            int [] newKeyboardMapping = new int[(numKeycodes - getMinimumKeycode ()) * _keysymsPerKeycode];
                            System.arraycopy(_keyboardMapping, 0, newKeyboardMapping, 0, (_numKeycodes - getMinimumKeycode ()) * _keysymsPerKeycode);
                            _keyboardMapping = newKeyboardMapping;
                            _numKeycodes = numKeycodes;
                        }

                        for (int key = 0; key < keycodeCount; key++)
                            for (int sym = 0; sym < kspkc; sym++)
                                _keyboardMapping[(keycode - getMinimumKeycode () + key) * _keysymsPerKeycode + sym] = io.readInt ();	// Keysyms.

                        xServer.sendMappingNotify(1, keycode, keycodeCount);
                    }
                }
                break;
            case RequestCode.GetKeyboardMapping:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int keycode = io.readByte();    // First code.
                    int count = io.readByte();    // Count.
                    int length = count * _keysymsPerKeycode;
                    int offset = (keycode - getMinimumKeycode()) * _keysymsPerKeycode;

                    io.readSkip(2);    // Unused.

                    synchronized (io) {
                        Util.writeReplyHeader(client, _keysymsPerKeycode);
                        io.writeInt(length);    // Reply length.
                        io.writePadBytes(24);    // Unused.

                        for (int i = 0; i < length; i++) {
                            int n = i + offset;

                            if (n < 0 || n >= _keyboardMapping.length)
                                io.writeInt(0);    // No symbol.
                            else io.writeInt(_keyboardMapping[n]);
                        }
                    }
                    io.flush();
                }
                break;
            case RequestCode.ChangeKeyboardControl:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int valueMask = io.readInt();    // Value mask.
                    int nbits = Util.bitcount(valueMask);

                    bytesRemaining -= 4;
                    if (bytesRemaining != nbits * 4) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                    } else {
                        for (int i = 0; i < 23; i++)
                            if ((valueMask & (1 << i)) != 0) processValue(io, i);
                    }
                }
                break;
            case RequestCode.GetKeyboardControl:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    synchronized (io) {
                        Util.writeReplyHeader(client, _keysymsPerKeycode);
                        io.writeInt(5);    // Reply length.
                        io.writeInt(0);    // LED mask.
                        io.writeByte((byte) 0);    // Key click percent.
                        io.writeByte((byte) _bellPercent);    // Bell volume.
                        io.writeShort((short) _bellPitch);    // Bell pitch Hz.
                        io.writeShort((short) _bellDuration);
                        io.writePadBytes(2);    // Unused.
                        io.writePadBytes(32);    // Auto repeats. Ignored.
                    }
                    io.flush();
                }
                break;
            case RequestCode.SetModifierMapping:
                if (bytesRemaining != 8 * arg) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Not supported. Always fails.
					int diff = getMinimumKeycodeDiff();
					int status = 0;

					byte[] modifierMapping = new byte[bytesRemaining];
					for (int i = 0; i < bytesRemaining ; i++) {
						int keycode = io.readByte();
						int offset = keycode / 8;
						byte mask = (byte) (1 << (keycode & 7));
						if ((_keymap[offset] & mask) == mask)
							status = 2; // If a modifier is pressed
						modifierMapping[i] = (byte) (keycode - diff);
					}

					if (status == 0) {
						_modifierMapping = modifierMapping;
						xServer.sendMappingNotify (0, 0, 0);
					}
                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) status);
                        io.writeInt(0);    // Reply length.
                        io.writePadBytes(24);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.GetModifierMapping:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    final byte kpm = _keycodesPerModifier;
                    byte[] map = null;

                    if (kpm > 0) {
                        int diff = getMinimumKeycodeDiff();

                        map = new byte[kpm * 8];
                        for (int i = 0; i < map.length; i++)
                            if (_modifierMapping[i] == 0) map[i] = 0;
                            else map[i] = (byte) (_modifierMapping[i] + diff);
                    }

                    synchronized (io) {
                        Util.writeReplyHeader(client, kpm);
                        io.writeInt(kpm * 2);    // Reply length.
                        io.writePadBytes(24);    // Unused.

                        if (map != null) io.writeBytes(map, 0, map.length);
                    }
                    io.flush();
                }
                break;
            case RequestCode.Bell:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    playBell((byte) arg);
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Get modifier state.
     *
     * @return Modifier state According to pressed keys (keymap) and modifiermapping.
     */
    public int getState() {
        int diff = getMinimumKeycodeDiff();
        int state = 0;

        for (int m = 0 ; m < 8; m++) {
            for (int i = 0 ; i < 8 && _modifierMapping[m * 8 + i] != 0xff ; i++) {
                byte		keycode = (byte) (_modifierMapping[m * 8 + i] + diff);
                int			offset = keycode / 8;
                byte		mask = (byte) (1 << (keycode & 7));
                if ((_keymap[offset] & mask) == mask)
                    state |= 1 << m;
            }
        }

        return state;
    }


    /**
     * Play a beep.
     *
     * @param percent Volume relative to base volume, [-100, 100]
     */
    private void playBell(int percent) {
        int volume;

        if (percent < 0) {
            volume = _bellPercent + _bellPercent * percent / 100;
            _bellBufferFilled = false;
        } else if (percent > 0) {
            volume = _bellPercent - _bellPercent * percent / 100 + percent;
            _bellBufferFilled = false;
        } else {
            volume = _bellPercent;
        }

        if (_bellBuffer == null) {
            _bellBuffer = new short[SAMPLE_RATE * _bellDuration / 1000];
            _bellBufferFilled = false;

        }

        if (!_bellBufferFilled) {
            double vol = 32767.0 * (double) volume / 100.0;
            double dt = _bellPitch * 2.0 * Math.PI / SAMPLE_RATE;

            for (int i = 0; i < _bellBuffer.length; i++)
                _bellBuffer[i] = (short) (vol * Math.sin((double) i * dt));

            _bellBufferFilled = true;
        }

        if (_audioTrack != null) {
            _audioTrack.stop();
            _audioTrack.release();
        }

        _audioTrack = new AudioTrack(AudioManager.STREAM_SYSTEM, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 2 * _bellBuffer.length, AudioTrack.MODE_STATIC);

        _audioTrack.write(_bellBuffer, 0, _bellBuffer.length);
        _audioTrack.play();
    }

    /**
     * Process a single keyboard attribute value.
     *
     * @param io      The input/output stream.
     * @param maskBit The mask bit of the attribute.
     * @throws IOException
     */
    private void processValue(InputOutput io, int maskBit) throws IOException {
        switch (maskBit) {
            case AttrKeyClickPercent:
                io.readByte();    // Not implemented.
                io.readSkip(3);
                break;
            case AttrBellPercent:
                _bellPercent = (byte) io.readByte();
                if (_bellPercent < 0) _bellPercent = DefaultBellPercent;
                io.readSkip(3);
                _bellBufferFilled = false;
                break;
            case AttrBellPitch:
                _bellPitch = (short) io.readShort();
                if (_bellPitch < 0) _bellPitch = DefaultBellPitch;
                io.readSkip(2);
                _bellBufferFilled = false;
                break;
            case AttrBellDuration:
                _bellDuration = (short) io.readShort();
                if (_bellDuration < 0) _bellDuration = DefaultBellDuration;
                io.readSkip(2);
                _bellBuffer = null;
                break;
            case AttrLed:
                io.readByte();    // Not implemented.
                io.readSkip(3);
                break;
            case AttrLedMode:
                io.readByte();    // Not implemented.
                io.readSkip(3);
                break;
            case AttrKey:
                io.readByte();    // Not implemented.
                io.readSkip(3);
                break;
            case AttrAutoRepeatMode:
                io.readByte();    // Not implemented.
                io.readSkip(3);
                break;
        }
    }
}