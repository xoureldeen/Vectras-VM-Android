package au.com.darkside.xserver;

import java.io.IOException;

/**
 * Handle X events.
 *
 * @author Matthew Kwan
 */
public class EventCode {
    public static final byte KeyPress = 2;
    public static final byte KeyRelease = 3;
    public static final byte ButtonPress = 4;
    public static final byte ButtonRelease = 5;
    public static final byte MotionNotify = 6;
    public static final byte EnterNotify = 7;
    public static final byte LeaveNotify = 8;
    public static final byte FocusIn = 9;
    public static final byte FocusOut = 10;
    public static final byte KeymapNotify = 11;
    public static final byte Expose = 12;
    public static final byte GraphicsExposure = 13;
    public static final byte NoExposure = 14;
    public static final byte VisibilityNotify = 15;
    public static final byte CreateNotify = 16;
    public static final byte DestroyNotify = 17;
    public static final byte UnmapNotify = 18;
    public static final byte MapNotify = 19;
    public static final byte MapRequest = 20;
    public static final byte ReparentNotify = 21;
    public static final byte ConfigureNotify = 22;
    public static final byte ConfigureRequest = 23;
    public static final byte GravityNotify = 24;
    public static final byte ResizeRequest = 25;
    public static final byte CirculateNotify = 26;
    public static final byte CirculateRequest = 27;
    public static final byte PropertyNotify = 28;
    public static final byte SelectionClear = 29;
    public static final byte SelectionRequest = 30;
    public static final byte SelectionNotify = 31;
    public static final byte ColormapNotify = 32;
    public static final byte ClientMessage = 33;
    public static final byte MappingNotify = 34;

    public static final int MaskKeyPress = 0x00000001;
    public static final int MaskKeyRelease = 0x00000002;
    public static final int MaskButtonPress = 0x00000004;
    public static final int MaskButtonRelease = 0x00000008;
    public static final int MaskEnterWindow = 0x00000010;
    public static final int MaskLeaveWindow = 0x00000020;
    public static final int MaskPointerMotion = 0x00000040;
    public static final int MaskPointerMotionHint = 0x00000080;
    public static final int MaskButton1Motion = 0x00000100;
    public static final int MaskButton2Motion = 0x00000200;
    public static final int MaskButton3Motion = 0x00000400;
    public static final int MaskButton4Motion = 0x00000800;
    public static final int MaskButton5Motion = 0x00001000;
    public static final int MaskButtonMotion = 0x00002000;
    public static final int MaskKeymapState = 0x00004000;
    public static final int MaskExposure = 0x00008000;
    public static final int MaskVisibilityChange = 0x00010000;
    public static final int MaskStructureNotify = 0x00020000;
    public static final int MaskResizeRedirect = 0x00040000;
    public static final int MaskSubstructureNotify = 0x00080000;
    public static final int MaskSubstructureRedirect = 0x00100000;
    public static final int MaskFocusChange = 0x00200000;
    public static final int MaskPropertyChange = 0x00400000;
    public static final int MaskColormapChange = 0x00800000;
    public static final int MaskOwnerGrabButton = 0x01000000;

    public static final int MaskAllPointer = MaskButtonPress | MaskButtonRelease | MaskPointerMotion | MaskPointerMotionHint | MaskButton1Motion | MaskButton2Motion | MaskButton3Motion | MaskButton4Motion | MaskButton5Motion | MaskButtonMotion;

    /**
     * Write an event header.
     *
     * @param client The client to write to.
     * @param code   The event code.
     * @param arg    Optional first argument.
     * @throws IOException
     */
    private static void writeHeader(Client client, byte code, int arg) throws IOException {
        InputOutput io = client.getInputOutput();

        io.writeByte((byte) code);
        io.writeByte((byte) arg);
        io.writeShort((short) (client.getSequenceNumber() & 0xffff));
    }

    /**
     * Send a key press event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param keycode     The code of the key that was pressed.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @throws IOException
     */
    public static void sendKeyPress(Client client, int timestamp, int keycode, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, KeyPress, keycode);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) 1);    // Same screen.
            io.writePadBytes(1);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a key release event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param keycode     The code of the key that was released.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @throws IOException
     */
    public static void sendKeyRelease(Client client, int timestamp, int keycode, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, KeyRelease, keycode);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) 1);    // Same screen.
            io.writePadBytes(1);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a button press event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param button      The button that was pressed.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @throws IOException
     */
    public static void sendButtonPress(Client client, int timestamp, int button, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ButtonPress, button);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) 1);    // Same screen.
            io.writePadBytes(1);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a button release event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param button      The button that was released.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @throws IOException
     */
    public static void sendButtonRelease(Client client, int timestamp, int button, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ButtonRelease, button);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) 1);    // Same screen.
            io.writePadBytes(1);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a motion notify event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param detail      0=Normal, 1=Hint.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @throws IOException
     */
    public static void sendMotionNotify(Client client, int timestamp, int detail, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, MotionNotify, detail);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) 1);    // Same screen.
            io.writePadBytes(1);    // Unused.
        }
        io.flush();
    }

    /**
     * Send an enter notify event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param detail      0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear, 4=NonlinearVirtual.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @param mode        0=Normal, 1=Grab, 2=Ungrab.
     * @param focus       Is the event window the focus window or an inferior of it?
     * @throws IOException
     */
    public static void sendEnterNotify(Client client, int timestamp, int detail, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state, int mode, boolean focus) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, EnterNotify, detail);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) mode);    // Mode.
            io.writeByte((byte) (focus ? 3 : 2));    // Same screen, focus.
        }
        io.flush();
    }

    /**
     * Send a leave notify event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param detail      0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *                    4=NonlinearVirtual.
     * @param root        The root window of the event window.
     * @param eventWindow The window interested in the event.
     * @param child       Child of event window, ancestor of source. Can be null.
     * @param rootX       Pointer root X coordinate at the time of the event.
     * @param rootY       Pointer root Y coordinate at the time of the event.
     * @param eventX      Pointer X coordinate relative to event window.
     * @param eventY      Pointer Y coordinate relative to event window.
     * @param state       Bitmask of the buttons and modifier keys.
     * @param mode        0=Normal, 1=Grab, 2=Ungrab.
     * @param focus       Is the event window the focus window or an inferior of it?
     * @throws IOException
     */
    public static void sendLeaveNotify(Client client, int timestamp, int detail, Window root, Window eventWindow, Window child, int rootX, int rootY, int eventX, int eventY, int state, int mode, boolean focus) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, LeaveNotify, detail);
            io.writeInt(timestamp);    // Time.
            io.writeInt(root.getId());    // Root.
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(child == null ? 0 : child.getId());    // Child.
            io.writeShort((short) rootX);    // Root X.
            io.writeShort((short) rootY);    // Root Y.
            io.writeShort((short) eventX);    // Event X.
            io.writeShort((short) eventY);    // Event Y.
            io.writeShort((short) state);    // State.
            io.writeByte((byte) mode);    // Mode.
            io.writeByte((byte) (focus ? 3 : 2));    // Same screen, focus.
        }
        io.flush();
    }

    /**
     * Send a focus in event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param detail      0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *                    4=NonlinearVirtual, 5=Pointer, 6=PinterRoot, 7=None.
     * @param eventWindow The window interested in the event.
     * @param mode        0=Normal, 1=Grab, 2=Ungrab, 3=WhileGrabbed.
     * @throws IOException
     */
    public static void sendFocusIn(Client client, int timestamp, int detail, Window eventWindow, int mode) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, FocusIn, detail);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeByte((byte) mode);    // Mode.
            io.writePadBytes(23);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a focus out event.
     *
     * @param client      The client to write to.
     * @param timestamp   Time in milliseconds since last server reset.
     * @param detail      0=Ancestor, 1=Virtual, 2=Inferior, 3=Nonlinear,
     *                    4=NonlinearVirtual, 5=Pointer, 6=PinterRoot, 7=None.
     * @param eventWindow The window interested in the event.
     * @param mode        0=Normal, 1=Grab, 2=Ungrab, 3=WhileGrabbed.
     * @throws IOException
     */
    public static void sendFocusOut(Client client, int timestamp, int detail, Window eventWindow, int mode) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, FocusOut, detail);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeByte((byte) mode);    // Mode.
            io.writePadBytes(23);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a keymap notify event.
     *
     * @param client The client to write to.
     * @param keys   A bit vector of the logical state of the keyboard.
     * @throws IOException
     */
    public static void sendKeymapNotify(Client client, byte[] keys) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            io.writeByte(KeymapNotify);
            io.writeBytes(keys, 0, 31);    // Keys.
        }
        io.flush();
    }

    /**
     * Send an expose event.
     *
     * @param client The client to write to.
     * @param window The window where the exposure occurred.
     * @param x      Left of the exposed rectangle.
     * @param y      Top of the exposed rectangle.
     * @param width  Width of the exposed rectangle.
     * @param height Height of the exposed rectangle.
     * @param count  Number of rectangles remaining in the exposed region.
     * @throws IOException
     */
    public static void sendExpose(Client client, Window window, int x, int y, int width, int height, int count) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, Expose, 0);
            io.writeInt(window.getId());    // Event.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writeShort((short) count);    // Count.
            io.writePadBytes(14);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a graphics exposure event.
     *
     * @param client      The client to write to.
     * @param drawable    The drawable where the exposure occurred.
     * @param majorOpcode The graphics request that caused the event.
     *                    Either CopyArea or CopyPlane.
     * @param x           Left of the exposed rectangle.
     * @param y           Top of the exposed rectangle.
     * @param width       Width of the exposed rectangle.
     * @param height      Height of the exposed rectangle.
     * @param count       Number of rectangles remaining in the exposed region.
     * @throws IOException
     */
    public static void sendGraphicsExposure(Client client, Resource drawable, byte majorOpcode, int x, int y, int width, int height, int count) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, GraphicsExposure, 0);
            io.writeInt(drawable.getId());    // Drawable.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writeShort((short) 0);    // Minor opcode.
            io.writeShort((short) count);    // Count.
            io.writeByte((byte) majorOpcode);    // Major opcode.
            io.writePadBytes(11);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a no exposure event.
     *
     * @param client      The client to write to.
     * @param drawable    The drawable where no exposure exposure occurred.
     * @param majorOpcode The graphics request that caused the event.
     *                    Either CopyArea or CopyPlane.
     * @throws IOException
     */
    public static void sendNoExposure(Client client, Resource drawable, byte majorOpcode) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, NoExposure, 0);
            io.writeInt(drawable.getId());    // Drawable.
            io.writeShort((short) 0);    // Minor opcode.
            io.writeByte((byte) majorOpcode);    // Major opcode.
            io.writePadBytes(21);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a visibility notify event.
     *
     * @param client The client to write to.
     * @param window The window where the exposure occurred.
     * @param state  0=Unobscured, 1=PartiallyObscured, 2=FullyObscured.
     * @throws IOException
     */
    public static void sendVisibilityNotify(Client client, Window window, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, VisibilityNotify, 0);
            io.writeInt(window.getId());    // Event.
            io.writeByte((byte) state);    // State.
            io.writePadBytes(23);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a create notify event.
     *
     * @param client           The client to write to.
     * @param parent           The parent of the window that was created.
     * @param window           The window that was created.
     * @param x                X position of the created window.
     * @param y                Y position of the created window.
     * @param width            Width of the created window.
     * @param height           Height of the created window.
     * @param borderWidth      Border width of the created window.
     * @param overrideRedirect Does the created window use override redirect?
     * @throws IOException
     */
    public static void sendCreateNotify(Client client, Window parent, Window window, int x, int y, int width, int height, int borderWidth, boolean overrideRedirect) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, CreateNotify, 0);
            io.writeInt(parent.getId());    // Parent.
            io.writeInt(window.getId());    // Window.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writeShort((short) borderWidth);    // Border width.
            io.writeByte((byte) (overrideRedirect ? 1 : 0));
            io.writePadBytes(9);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a destroy notify event.
     *
     * @param client      The client to write to.
     * @param eventWindow The window where the event was generated.
     * @param window      The window that was destroyed.
     * @throws IOException
     */
    public static void sendDestroyNotify(Client client, Window eventWindow, Window window) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, DestroyNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writePadBytes(20);    // Unused.
        }
        io.flush();
    }

    /**
     * Send an unmap notify event.
     *
     * @param client        The client to write to.
     * @param eventWindow   The window where the event was generated.
     * @param window        The window that was unmapped.
     * @param fromConfigure True if event was caused by parent being resized.
     * @throws IOException
     */
    public static void sendUnmapNotify(Client client, Window eventWindow, Window window, boolean fromConfigure) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, UnmapNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writeByte((byte) (fromConfigure ? 1 : 0));    // From configure.
            io.writePadBytes(19);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a map notify event.
     *
     * @param client           The client to write to.
     * @param eventWindow      The window where the event was generated.
     * @param window           The window that was mapped.
     * @param overrideRedirect True if the window uses override redirect.
     * @throws IOException
     */
    public static void sendMapNotify(Client client, Window eventWindow, Window window, boolean overrideRedirect) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, MapNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writeByte((byte) (overrideRedirect ? 1 : 0));
            io.writePadBytes(19);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a map request event.
     *
     * @param client      The client to write to.
     * @param eventWindow The window where the event was generated.
     * @param window      The window that was mapped.
     * @throws IOException
     */
    public static void sendMapRequest(Client client, Window eventWindow, Window window) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, MapRequest, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writePadBytes(20);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a reparent notify event.
     *
     * @param client           The client to write to.
     * @param eventWindow      The window where the event was generated.
     * @param window           The window that has been rerooted.
     * @param parent           The window's new parent.
     * @param x                X position of the window relative to the new parent.
     * @param y                Y position of the window relative to the new parent.
     * @param overrideRedirect Does the window use override redirect?
     * @throws IOException
     */
    public static void sendReparentNotify(Client client, Window eventWindow, Window window, Window parent, int x, int y, boolean overrideRedirect) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ReparentNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writeInt(parent.getId());    // Parent.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeByte((byte) (overrideRedirect ? 1 : 0));
            io.writePadBytes(11);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a configure notify event.
     *
     * @param client           The client to write to.
     * @param eventWindow      The window where the event was generated.
     * @param window           The window that was changed.
     * @param aboveSibling     The sibling window beneath it. May be null.
     * @param x                X position of the window relative to its parent.
     * @param y                Y position of the window relative to its parent.
     * @param width            Width of the window.
     * @param height           Height of the window.
     * @param borderWidth      Border width of the window.
     * @param overrideRedirect Does the window use override redirect?
     * @throws IOException
     */
    public static void sendConfigureNotify(Client client, Window eventWindow, Window window, Window aboveSibling, int x, int y, int width, int height, int borderWidth, boolean overrideRedirect) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ConfigureNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writeInt(aboveSibling == null ? 0 : aboveSibling.getId());
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writeShort((short) borderWidth);    // Border width.
            io.writeByte((byte) (overrideRedirect ? 1 : 0));
            io.writePadBytes(5);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a configure request event.
     *
     * @param client      The client to write to.
     * @param stackMode   0=Above, 1=Below, 2=TopIf, 3=BottomIf, 4=Opposite
     * @param parent      The parent of the window.
     * @param window      The window that the ConfigureWindow was issued to.
     * @param sibling     The sibling window beneath it. May be null.
     * @param x           X position of the window.
     * @param y           Y position of the window.
     * @param width       Width of the window.
     * @param height      Height of the window.
     * @param borderWidth Border width of the window.
     * @param valueMask   Components specified in the ConfigureWindow request.
     * @throws IOException
     */
    public static void sendConfigureRequest(Client client, int stackMode, Window parent, Window window, Window sibling, int x, int y, int width, int height, int borderWidth, int valueMask) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ConfigureRequest, stackMode);
            io.writeInt(parent.getId());    // Parent.
            io.writeInt(window.getId());    // Window.
            io.writeInt(sibling == null ? 0 : sibling.getId());    // Sibling.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writeShort((short) borderWidth);    // Border width.
            io.writeShort((short) valueMask);    // Value mask.
            io.writePadBytes(4);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a gravity notify event.
     *
     * @param client      The client to write to.
     * @param eventWindow The window where the event was generated.
     * @param window      The window that was moved because parent changed size.
     * @param x           X position of the window relative to the parent.
     * @param y           Y position of the window relative to the parent.
     * @throws IOException
     */
    public static void sendGravityNotify(Client client, Window eventWindow, Window window, int x, int y) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, GravityNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writeShort((short) x);    // X.
            io.writeShort((short) y);    // Y.
            io.writePadBytes(16);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a resize request event.
     *
     * @param client The client to write to.
     * @param window A client is attempting to resize this window.
     * @param width  Requested width of the window.
     * @param height Requested height of the window.
     * @throws IOException
     */
    public static void sendResizeRequest(Client client, Window window, int width, int height) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ResizeRequest, 0);
            io.writeInt(window.getId());    // Window.
            io.writeShort((short) width);    // Width.
            io.writeShort((short) height);    // Height.
            io.writePadBytes(20);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a circulate notify event.
     *
     * @param client      The client to write to.
     * @param eventWindow The window where the event was generated.
     * @param window      The window that was restacked.
     * @param place       0=Top, 1=Bottom.
     * @throws IOException
     */
    public static void sendCirculateNotify(Client client, Window eventWindow, Window window, int place) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, CirculateNotify, 0);
            io.writeInt(eventWindow.getId());    // Event.
            io.writeInt(window.getId());    // Window.
            io.writePadBytes(4);    // Unused.
            io.writeByte((byte) place);    // Place.
            io.writePadBytes(15);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a circulate request event.
     *
     * @param client The client to write to.
     * @param parent The parent of the window.
     * @param window The window that needs to be restacked.
     * @param place  0=Top, 1=Bottom.
     * @throws IOException
     */
    public static void sendCirculateRequest(Client client, Window parent, Window window, int place) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, CirculateRequest, 0);
            io.writeInt(parent.getId());    // Parent.
            io.writeInt(window.getId());    // Window.
            io.writePadBytes(4);    // Unused.
            io.writeByte((byte) place);    // Place.
            io.writePadBytes(15);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a property notify event.
     *
     * @param client    The client to write to.
     * @param window    The window being changed
     * @param atom      The property being changed.
     * @param timestamp Time in milliseconds when the property was changed.
     * @param state     0=NewValue, 1=Deleted.
     * @throws IOException
     */
    public static void sendPropertyNotify(Client client, Window window, Atom atom, int timestamp, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, PropertyNotify, 0);
            io.writeInt(window.getId());    // Window.
            io.writeInt(atom.getId());    // Atom.
            io.writeInt(timestamp);    // Time.
            io.writeByte((byte) state);    // State.
            io.writePadBytes(15);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a selection clear event.
     *
     * @param client    The client to write to.
     * @param timestamp Last-change time for the selection.
     * @param window    Previous owner of the selection.
     * @param atom      The selection.
     * @throws IOException
     */
    public static void sendSelectionClear(Client client, int timestamp, Window window, Atom atom) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, SelectionClear, 0);
            io.writeInt(timestamp);    // Time.
            io.writeInt(window.getId());    // Owner.
            io.writeInt(atom.getId());    // Selection.
            io.writePadBytes(16);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a selection request event.
     *
     * @param client    The client to write to.
     * @param timestamp Last-change time for the selection.
     * @param owner     Current owner of the selection.
     * @param requestor Window requesting change of selection.
     * @param selection The selection whose owner changed.
     * @param target    Target atom.
     * @param property  Property atom. May be null.
     * @throws IOException
     */
    public static void sendSelectionRequest(Client client, int timestamp, Window owner, Window requestor, Atom selection, Atom target, Atom property) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, SelectionRequest, 0);
            io.writeInt(timestamp);    // Time.
            io.writeInt(owner.getId());    // Owner.
            io.writeInt(requestor.getId());    // Requestor.
            io.writeInt(selection.getId());    // Selection.
            io.writeInt(target.getId());    // Target.
            io.writeInt(property == null ? 0 : property.getId());
            io.writePadBytes(4);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a selection notify event.
     *
     * @param client    The client to write to.
     * @param timestamp Last-change time for the selection.
     * @param requestor Window requesting change of selection.
     * @param selection The selection whose owner changed.
     * @param target    Target atom.
     * @param property  Property atom. May be null.
     * @throws IOException
     */
    public static void sendSelectionNotify(Client client, int timestamp, Window requestor, Atom selection, Atom target, Atom property) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, SelectionNotify, 0);
            io.writeInt(timestamp);    // Time.
            io.writeInt(requestor.getId());    // Requestor.
            io.writeInt(selection.getId());    // Selection.
            io.writeInt(target.getId());    // Target.
            io.writeInt(property == null ? 0 : property.getId());
            io.writePadBytes(8);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a colormap notify event.
     *
     * @param client The client to write to.
     * @param window The window the colormap belongs to.
     * @param cmap   The colormap. May be null.
     * @param isNew  True=colormap changed, False=colormap un/installed.
     * @param state  0=Uninstalled, 1=Installed.
     * @throws IOException
     */
    public static void sendColormapNotify(Client client, Window window, Colormap cmap, boolean isNew, int state) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, ColormapNotify, 0);
            io.writeInt(window.getId());    // Window.
            io.writeInt(cmap == null ? 0 : cmap.getId());    // Colormap.
            io.writeByte((byte) (isNew ? 1 : 0));    // New.
            io.writeByte((byte) state);    // State.
            io.writePadBytes(18);    // Unused.
        }
        io.flush();
    }

    /**
     * Send a mapping notify event.
     *
     * @param client       The client to write to.
     * @param request      0=Modifier, 1=Keyboard, 2=Pointer.
     * @param firstKeycode Start of altered keycodes if request=Keyboard.
     * @param count        Size of altered keycode range if request=Keyboard.
     * @throws IOException
     */
    public static void sendMappingNotify(Client client, int request, int firstKeycode, int count) throws IOException {
        InputOutput io = client.getInputOutput();

        synchronized (io) {
            writeHeader(client, MappingNotify, 0);
            io.writeByte((byte) request);    // Request.
            io.writeByte((byte) firstKeycode);    // First keycode.
            io.writeByte((byte) count);    // Count.
            io.writePadBytes(25);    // Unused.
        }
        io.flush();
    }
}