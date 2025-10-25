package au.com.darkside.xserver;

/**
 * This class records details of a passive button grab.
 *
 * @author Matthew Kwan
 */
public class PassiveButtonGrab {
    private final Client _grabClient;
    private final Window _grabWindow;
    private final byte _button;
    private final int _modifiers;
    private final boolean _ownerEvents;
    private final int _eventMask;
    private final boolean _pointerSynchronous;
    private final boolean _keyboardSynchronous;
    private final Window _confineWindow;
    private final Cursor _cursor;

    /**
     * Constructor.
     *
     * @param grabClient          The grabbing client.
     * @param grabWindow          The grab window.
     * @param button              The button being grabbed, or 0 for any.
     * @param modifiers           The modifier mask, or 0x8000 for any.
     * @param ownerEvents         Owner-events flag.
     * @param eventMask           Selected pointer events.
     * @param pointerSynchronous  Are pointer events synchronous?
     * @param keyboardSynchronous Are keyboard events synchronous?
     * @param confineWindow       Confine the cursor to this window. Can be null.
     * @param cursor              The cursor to use during the grab. Can be null.
     */
    public PassiveButtonGrab(Client grabClient, Window grabWindow, byte button, int modifiers, boolean ownerEvents, int eventMask, boolean pointerSynchronous, boolean keyboardSynchronous, Window confineWindow, Cursor cursor) {
        _grabClient = grabClient;
        _grabWindow = grabWindow;
        _button = button;
        _modifiers = modifiers;
        _ownerEvents = ownerEvents;
        _eventMask = eventMask;
        _pointerSynchronous = pointerSynchronous;
        _keyboardSynchronous = keyboardSynchronous;
        _confineWindow = confineWindow;
        _cursor = cursor;
    }

    /**
     * Does the event trigger the passive grab?
     *
     * @param button Currently-pressed buttons and modifiers.
     * @return True if the event matches.
     */
    public boolean matchesEvent(int buttons) {
        if (_button != 0 && (buttons & 0xff00) != (0x80 << _button)) return false;

        if (_modifiers != 0x8000 && (buttons & 0xff) != _modifiers) return false;

        return true;
    }

    /**
     * Does this match the parameters of the grab?
     *
     * @param button    The button being grabbed, or 0 for any.
     * @param modifiers The modifier mask, or 0x8000 for any.
     * @return True if it matches the parameters.
     */
    public boolean matchesGrab(int button, int modifiers) {
        if (button != 0 && _button != 0 && button != _button) return false;

        if (modifiers != 0x8000 && _modifiers != 0x8000 && modifiers != _modifiers) return false;

        return true;
    }

    /**
     * Return the button.
     *
     * @return The button.
     */
    public byte getButton() {
        return _button;
    }

    /**
     * Return the modifier mask.
     *
     * @return The modifier mask.
     */
    public int getModifiers() {
        return _modifiers;
    }

    /**
     * Return the grab client.
     *
     * @return The grab client.
     */
    public Client getGrabClient() {
        return _grabClient;
    }

    /**
     * Return the grab window.
     *
     * @return The grab window.
     */
    public Window getGrabWindow() {
        return _grabWindow;
    }

    /**
     * Return the owner-events flag.
     *
     * @return The owner-events flag.
     */
    public boolean getOwnerEvents() {
        return _ownerEvents;
    }

    /**
     * Return the pointer events mask.
     *
     * @return The pointer events mask.
     */
    public int getEventMask() {
        return _eventMask;
    }

    /**
     * Return whether pointer events are synchronous.
     *
     * @return Whether pointer events are synchronous.
     */
    public boolean getPointerSynchronous() {
        return _pointerSynchronous;
    }

    /**
     * Return whether pointer events are synchronous.
     *
     * @return Whether pointer events are synchronous.
     */
    public boolean getKeyboardSynchronous() {
        return _keyboardSynchronous;
    }

    /**
     * Return the confine window.
     *
     * @return The confine window.
     */
    public Window getConfineWindow() {
        return _confineWindow;
    }

    /**
     * Return the cursor.
     *
     * @return The cursor.
     */
    public Cursor getCursor() {
        return _cursor;
    }
}