package au.com.darkside.xserver;

import android.util.SparseArray;

/**
 * All the X request codes.
 *
 * @author Matthew Kwan
 */
public class RequestCode {

    public static class AllowEventsMode {

        public static final int AsyncPointer = 0;
        public static final int SyncPointer = 1;
        public static final int ReplayPointer = 2;
        public static final int AsyncKeyboard = 3;
        public static final int SyncKeyboard = 4;
        public static final int ReplayKeyboard = 5;
        public static final int AsyncBoth = 6;
        public static final int SyncBoth = 7;

        private static SparseArray<String> mNames = new SparseArray<String>();

        public static String toString(int mode) {
            String name = mNames.get(mode);
            return name != null ? name : "unknown mode";
        }

        private static void initialize() {
            mNames.put(AsyncPointer, "AsyncPointer");
            mNames.put(SyncPointer, "SyncPointer");
            mNames.put(ReplayPointer, "ReplayPointer");
            mNames.put(AsyncKeyboard, "AsyncKeyboard");
            mNames.put(SyncKeyboard, "SyncKeyboard");
            mNames.put(ReplayKeyboard, "ReplayKeyboard");
            mNames.put(AsyncBoth, "AsyncBoth");
            mNames.put(SyncBoth, "SyncBoth");
        }
    }

    public static final byte CreateWindow = 1;
    public static final byte ChangeWindowAttributes = 2;
    public static final byte GetWindowAttributes = 3;
    public static final byte DestroyWindow = 4;
    public static final byte DestroySubwindows = 5;
    public static final byte ChangeSaveSet = 6;
    public static final byte ReparentWindow = 7;
    public static final byte MapWindow = 8;
    public static final byte MapSubwindows = 9;
    public static final byte UnmapWindow = 10;
    public static final byte UnmapSubwindows = 11;
    public static final byte ConfigureWindow = 12;
    public static final byte CirculateWindow = 13;
    public static final byte GetGeometry = 14;
    public static final byte QueryTree = 15;
    public static final byte InternAtom = 16;
    public static final byte GetAtomName = 17;
    public static final byte ChangeProperty = 18;
    public static final byte DeleteProperty = 19;
    public static final byte GetProperty = 20;
    public static final byte ListProperties = 21;
    public static final byte SetSelectionOwner = 22;
    public static final byte GetSelectionOwner = 23;
    public static final byte ConvertSelection = 24;
    public static final byte SendEvent = 25;
    public static final byte GrabPointer = 26;
    public static final byte UngrabPointer = 27;
    public static final byte GrabButton = 28;
    public static final byte UngrabButton = 29;
    public static final byte ChangeActivePointerGrab = 30;
    public static final byte GrabKeyboard = 31;
    public static final byte UngrabKeyboard = 32;
    public static final byte GrabKey = 33;
    public static final byte UngrabKey = 34;
    public static final byte AllowEvents = 35;
    public static final byte GrabServer = 36;
    public static final byte UngrabServer = 37;
    public static final byte QueryPointer = 38;
    public static final byte GetMotionEvents = 39;
    public static final byte TranslateCoordinates = 40;
    public static final byte WarpPointer = 41;
    public static final byte SetInputFocus = 42;
    public static final byte GetInputFocus = 43;
    public static final byte QueryKeymap = 44;
    public static final byte OpenFont = 45;
    public static final byte CloseFont = 46;
    public static final byte QueryFont = 47;
    public static final byte QueryTextExtents = 48;
    public static final byte ListFonts = 49;
    public static final byte ListFontsWithInfo = 50;
    public static final byte SetFontPath = 51;
    public static final byte GetFontPath = 52;
    public static final byte CreatePixmap = 53;
    public static final byte FreePixmap = 54;
    public static final byte CreateGC = 55;
    public static final byte ChangeGC = 56;
    public static final byte CopyGC = 57;
    public static final byte SetDashes = 58;
    public static final byte SetClipRectangles = 59;
    public static final byte FreeGC = 60;
    public static final byte ClearArea = 61;
    public static final byte CopyArea = 62;
    public static final byte CopyPlane = 63;
    public static final byte PolyPoint = 64;
    public static final byte PolyLine = 65;
    public static final byte PolySegment = 66;
    public static final byte PolyRectangle = 67;
    public static final byte PolyArc = 68;
    public static final byte FillPoly = 69;
    public static final byte PolyFillRectangle = 70;
    public static final byte PolyFillArc = 71;
    public static final byte PutImage = 72;
    public static final byte GetImage = 73;
    public static final byte PolyText8 = 74;
    public static final byte PolyText16 = 75;
    public static final byte ImageText8 = 76;
    public static final byte ImageText16 = 77;
    public static final byte CreateColormap = 78;
    public static final byte FreeColormap = 79;
    public static final byte CopyColormapAndFree = 80;
    public static final byte InstallColormap = 81;
    public static final byte UninstallColormap = 82;
    public static final byte ListInstalledColormaps = 83;
    public static final byte AllocColor = 84;
    public static final byte AllocNamedColor = 85;
    public static final byte AllocColorCells = 86;
    public static final byte AllocColorPlanes = 87;
    public static final byte FreeColors = 88;
    public static final byte StoreColors = 89;
    public static final byte StoreNamedColor = 90;
    public static final byte QueryColors = 91;
    public static final byte LookupColor = 92;
    public static final byte CreateCursor = 93;
    public static final byte CreateGlyphCursor = 94;
    public static final byte FreeCursor = 95;
    public static final byte RecolorCursor = 96;
    public static final byte QueryBestSize = 97;
    public static final byte QueryExtension = 98;
    public static final byte ListExtensions = 99;
    public static final byte ChangeKeyboardMapping = 100;
    public static final byte GetKeyboardMapping = 101;
    public static final byte ChangeKeyboardControl = 102;
    public static final byte GetKeyboardControl = 103;
    public static final byte Bell = 104;
    public static final byte ChangePointerControl = 105;
    public static final byte GetPointerControl = 106;
    public static final byte SetScreenSaver = 107;
    public static final byte GetScreenSaver = 108;
    public static final byte ChangeHosts = 109;
    public static final byte ListHosts = 110;
    public static final byte SetAccessControl = 111;
    public static final byte SetCloseDownMode = 112;
    public static final byte KillClient = 113;
    public static final byte RotateProperties = 114;
    public static final byte ForceScreenSaver = 115;
    public static final byte SetPointerMapping = 116;
    public static final byte GetPointerMapping = 117;
    public static final byte SetModifierMapping = 118;
    public static final byte GetModifierMapping = 119;
    public static final byte NoOperation = 127;
    public static final byte ExtensionStart = -128;
    public static final byte ExtensionEnd = -1;

    static {
        AllowEventsMode.initialize();
    }
}
