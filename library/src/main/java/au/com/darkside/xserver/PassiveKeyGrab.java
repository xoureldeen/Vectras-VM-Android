package au.com.darkside.xserver;

/**
 * This class records details of a passive key grab.
 *
 * @author Matthew Kwan
 */
public class PassiveKeyGrab {
    private final Client _grabClient;
    private final Window _grabWindow;
    private final byte _key;
    private final int _modifiers;
    private final boolean _ownerEvents;
    private final boolean _pointerSynchronous;
    private final boolean _keyboardSynchronous;

    /**
     * Constructor.
     *
     * @param grabClient          The grabbing client.
     * @param grabWindow          The grab window.
     * @param key                 The key being grabbed, or 0 for any.
     * @param modifiers           The modifier mask, or 0x8000 for any.
     * @param ownerEvents         Owner-events flag.
     * @param pointerSynchronous  Are pointer events synchronous?
     * @param keyboardSynchronous Are keyboard events synchronous?
     */
    public PassiveKeyGrab(Client grabClient, Window grabWindow, byte key, int modifiers, boolean ownerEvents, boolean pointerSynchronous, boolean keyboardSynchronous) {
        _grabClient = grabClient;
        _grabWindow = grabWindow;
        _key = key;
        _modifiers = modifiers;
        _ownerEvents = ownerEvents;
        _pointerSynchronous = pointerSynchronous;
        _keyboardSynchronous = keyboardSynchronous;
    }

    /**
     * Does the event trigger the passive grab?
     *
     * @param key       The key that was pressed.
     * @param modifiers The current state of the modifiers.
     * @return True if the event matches.
     */
    public boolean matchesEvent(int key, int modifiers) {
        if (_key != 0 && _key != key) return false;

        if (_modifiers != 0x8000 && _modifiers != modifiers) return false;

        return true;
    }

    /**
     * Does this match the parameters of the grab?
     *
     * @param key       The key being grabbed, or 0 for any.
     * @param modifiers The modifier mask, or 0x8000 for any.
     * @return True if it matches the parameters.
     */
    public boolean matchesGrab(int key, int modifiers) {
        if (key != 0 && _key != 0 && key != _key) return false;

        if (modifiers != 0x8000 && _modifiers != 0x8000 && modifiers != _modifiers) return false;

        return true;
    }

    /**
     * Return the key code.
     *
     * @return The key code.
     */
    public byte getKey() {
        return _key;
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
}