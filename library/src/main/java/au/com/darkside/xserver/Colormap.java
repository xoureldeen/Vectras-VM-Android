package au.com.darkside.xserver;

import android.graphics.Color;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;

/**
 * An X TrueColor colormap.
 *
 * @author Matthew Kwan
 */
public class Colormap extends Resource {
    private boolean _installed = false;
    private final ScreenView _screen;

    private static Hashtable<String, Integer> _colorNames = null;
    private static final String[] _names = {"snow", "ghost white", "ghostwhite", "white smoke", "whitesmoke", "gainsboro", "floral white", "floralwhite", "old lace", "oldlace", "linen", "antique white", "antiquewhite", "papaya whip", "papayawhip", "blanched almond", "blanchedalmond", "bisque", "peach puff", "peachpuff", "navajo white", "navajowhite", "moccasin", "cornsilk", "ivory", "lemon chiffon", "lemonchiffon", "seashell", "honeydew", "mint cream", "mintcream", "azure", "alice blue", "aliceblue", "lavender", "lavender blush", "lavenderblush", "misty rose", "mistyrose", "white", "black", "dark slate gray", "darkslategray", "dark slate grey", "darkslategrey", "dim gray", "dimgray", "dim grey", "dimgrey", "slate gray", "slategray", "slate grey", "slategrey", "light slate gray", "lightslategray", "light slate grey", "lightslategrey", "gray", "grey", "light grey", "lightgrey", "light gray", "lightgray", "midnight blue", "midnightblue", "navy", "navy blue", "navyblue", "cornflower blue", "cornflowerblue", "dark slate blue", "darkslateblue", "slate blue", "slateblue", "medium slate blue", "mediumslateblue", "light slate blue", "lightslateblue", "medium blue", "mediumblue", "royal blue", "royalblue", "blue", "dodger blue", "dodgerblue", "deep sky blue", "deepskyblue", "sky blue", "skyblue", "light sky blue", "lightskyblue", "steel blue", "steelblue", "light steel blue", "lightsteelblue", "light blue", "lightblue", "powder blue", "powderblue", "pale turquoise", "paleturquoise", "dark turquoise", "darkturquoise", "medium turquoise", "mediumturquoise", "turquoise", "cyan", "light cyan", "lightcyan", "cadet blue", "cadetblue", "medium aquamarine", "mediumaquamarine", "aquamarine", "dark green", "darkgreen", "dark olive green", "darkolivegreen", "dark sea green", "darkseagreen", "sea green", "seagreen", "medium sea green", "mediumseagreen", "light sea green", "lightseagreen", "pale green", "palegreen", "spring green", "springgreen", "lawn green", "lawngreen", "green", "chartreuse", "medium spring green", "mediumspringgreen", "green yellow", "greenyellow", "lime green", "limegreen", "yellow green", "yellowgreen", "forest green", "forestgreen", "olive drab", "olivedrab", "dark khaki", "darkkhaki", "khaki", "pale goldenrod", "palegoldenrod", "light goldenrod yellow", "lightgoldenrodyellow", "light yellow", "lightyellow", "yellow", "gold", "light goldenrod", "lightgoldenrod", "goldenrod", "dark goldenrod", "darkgoldenrod", "rosy brown", "rosybrown", "indian red", "indianred", "saddle brown", "saddlebrown", "sienna", "peru", "burlywood", "beige", "wheat", "sandy brown", "sandybrown", "tan", "chocolate", "firebrick", "brown", "dark salmon", "darksalmon", "salmon", "light salmon", "lightsalmon", "orange", "dark orange", "darkorange", "coral", "light coral", "lightcoral", "tomato", "orange red", "orangered", "red", "hot pink", "hotpink", "deep pink", "deeppink", "pink", "light pink", "lightpink", "pale violet red", "palevioletred", "maroon", "medium violet red", "mediumvioletred", "violet red", "violetred", "magenta", "violet", "plum", "orchid", "medium orchid", "mediumorchid", "dark orchid", "darkorchid", "dark violet", "darkviolet", "blue violet", "blueviolet", "purple", "medium purple", "mediumpurple", "thistle", "snow1", "snow2", "snow3", "snow4", "seashell1", "seashell2", "seashell3", "seashell4", "antiquewhite1", "antiquewhite2", "antiquewhite3", "antiquewhite4", "bisque1", "bisque2", "bisque3", "bisque4", "peachpuff1", "peachpuff2", "peachpuff3", "peachpuff4", "navajowhite1", "navajowhite2", "navajowhite3", "navajowhite4", "lemonchiffon1", "lemonchiffon2", "lemonchiffon3", "lemonchiffon4", "cornsilk1", "cornsilk2", "cornsilk3", "cornsilk4", "ivory1", "ivory2", "ivory3", "ivory4", "honeydew1", "honeydew2", "honeydew3", "honeydew4", "lavenderblush1", "lavenderblush2", "lavenderblush3", "lavenderblush4", "mistyrose1", "mistyrose2", "mistyrose3", "mistyrose4", "azure1", "azure2", "azure3", "azure4", "slateblue1", "slateblue2", "slateblue3", "slateblue4", "royalblue1", "royalblue2", "royalblue3", "royalblue4", "blue1", "blue2", "blue3", "blue4", "dodgerblue1", "dodgerblue2", "dodgerblue3", "dodgerblue4", "steelblue1", "steelblue2", "steelblue3", "steelblue4", "deepskyblue1", "deepskyblue2", "deepskyblue3", "deepskyblue4", "skyblue1", "skyblue2", "skyblue3", "skyblue4", "lightskyblue1", "lightskyblue2", "lightskyblue3", "lightskyblue4", "slategray1", "slategray2", "slategray3", "slategray4", "lightsteelblue1", "lightsteelblue2", "lightsteelblue3", "lightsteelblue4", "lightblue1", "lightblue2", "lightblue3", "lightblue4", "lightcyan1", "lightcyan2", "lightcyan3", "lightcyan4", "paleturquoise1", "paleturquoise2", "paleturquoise3", "paleturquoise4", "cadetblue1", "cadetblue2", "cadetblue3", "cadetblue4", "turquoise1", "turquoise2", "turquoise3", "turquoise4", "cyan1", "cyan2", "cyan3", "cyan4", "darkslategray1", "darkslategray2", "darkslategray3", "darkslategray4", "aquamarine1", "aquamarine2", "aquamarine3", "aquamarine4", "darkseagreen1", "darkseagreen2", "darkseagreen3", "darkseagreen4", "seagreen1", "seagreen2", "seagreen3", "seagreen4", "palegreen1", "palegreen2", "palegreen3", "palegreen4", "springgreen1", "springgreen2", "springgreen3", "springgreen4", "green1", "green2", "green3", "green4", "chartreuse1", "chartreuse2", "chartreuse3", "chartreuse4", "olivedrab1", "olivedrab2", "olivedrab3", "olivedrab4", "darkolivegreen1", "darkolivegreen2", "darkolivegreen3", "darkolivegreen4", "khaki1", "khaki2", "khaki3", "khaki4", "lightgoldenrod1", "lightgoldenrod2", "lightgoldenrod3", "lightgoldenrod4", "lightyellow1", "lightyellow2", "lightyellow3", "lightyellow4", "yellow1", "yellow2", "yellow3", "yellow4", "gold1", "gold2", "gold3", "gold4", "goldenrod1", "goldenrod2", "goldenrod3", "goldenrod4", "darkgoldenrod1", "darkgoldenrod2", "darkgoldenrod3", "darkgoldenrod4", "rosybrown1", "rosybrown2", "rosybrown3", "rosybrown4", "indianred1", "indianred2", "indianred3", "indianred4", "sienna1", "sienna2", "sienna3", "sienna4", "burlywood1", "burlywood2", "burlywood3", "burlywood4", "wheat1", "wheat2", "wheat3", "wheat4", "tan1", "tan2", "tan3", "tan4", "chocolate1", "chocolate2", "chocolate3", "chocolate4", "firebrick1", "firebrick2", "firebrick3", "firebrick4", "brown1", "brown2", "brown3", "brown4", "salmon1", "salmon2", "salmon3", "salmon4", "lightsalmon1", "lightsalmon2", "lightsalmon3", "lightsalmon4", "orange1", "orange2", "orange3", "orange4", "darkorange1", "darkorange2", "darkorange3", "darkorange4", "coral1", "coral2", "coral3", "coral4", "tomato1", "tomato2", "tomato3", "tomato4", "orangered1", "orangered2", "orangered3", "orangered4", "red1", "red2", "red3", "red4", "deeppink1", "deeppink2", "deeppink3", "deeppink4", "hotpink1", "hotpink2", "hotpink3", "hotpink4", "pink1", "pink2", "pink3", "pink4", "lightpink1", "lightpink2", "lightpink3", "lightpink4", "palevioletred1", "palevioletred2", "palevioletred3", "palevioletred4", "maroon1", "maroon2", "maroon3", "maroon4", "violetred1", "violetred2", "violetred3", "violetred4", "magenta1", "magenta2", "magenta3", "magenta4", "orchid1", "orchid2", "orchid3", "orchid4", "plum1", "plum2", "plum3", "plum4", "mediumorchid1", "mediumorchid2", "mediumorchid3", "mediumorchid4", "darkorchid1", "darkorchid2", "darkorchid3", "darkorchid4", "purple1", "purple2", "purple3", "purple4", "mediumpurple1", "mediumpurple2", "mediumpurple3", "mediumpurple4", "thistle1", "thistle2", "thistle3", "thistle4", "gray0", "grey0", "gray1", "grey1", "gray2", "grey2", "gray3", "grey3", "gray4", "grey4", "gray5", "grey5", "gray6", "grey6", "gray7", "grey7", "gray8", "grey8", "gray9", "grey9", "gray10", "grey10", "gray11", "grey11", "gray12", "grey12", "gray13", "grey13", "gray14", "grey14", "gray15", "grey15", "gray16", "grey16", "gray17", "grey17", "gray18", "grey18", "gray19", "grey19", "gray20", "grey20", "gray21", "grey21", "gray22", "grey22", "gray23", "grey23", "gray24", "grey24", "gray25", "grey25", "gray26", "grey26", "gray27", "grey27", "gray28", "grey28", "gray29", "grey29", "gray30", "grey30", "gray31", "grey31", "gray32", "grey32", "gray33", "grey33", "gray34", "grey34", "gray35", "grey35", "gray36", "grey36", "gray37", "grey37", "gray38", "grey38", "gray39", "grey39", "gray40", "grey40", "gray41", "grey41", "gray42", "grey42", "gray43", "grey43", "gray44", "grey44", "gray45", "grey45", "gray46", "grey46", "gray47", "grey47", "gray48", "grey48", "gray49", "grey49", "gray50", "grey50", "gray51", "grey51", "gray52", "grey52", "gray53", "grey53", "gray54", "grey54", "gray55", "grey55", "gray56", "grey56", "gray57", "grey57", "gray58", "grey58", "gray59", "grey59", "gray60", "grey60", "gray61", "grey61", "gray62", "grey62", "gray63", "grey63", "gray64", "grey64", "gray65", "grey65", "gray66", "grey66", "gray67", "grey67", "gray68", "grey68", "gray69", "grey69", "gray70", "grey70", "gray71", "grey71", "gray72", "grey72", "gray73", "grey73", "gray74", "grey74", "gray75", "grey75", "gray76", "grey76", "gray77", "grey77", "gray78", "grey78", "gray79", "grey79", "gray80", "grey80", "gray81", "grey81", "gray82", "grey82", "gray83", "grey83", "gray84", "grey84", "gray85", "grey85", "gray86", "grey86", "gray87", "grey87", "gray88", "grey88", "gray89", "grey89", "gray90", "grey90", "gray91", "grey91", "gray92", "grey92", "gray93", "grey93", "gray94", "grey94", "gray95", "grey95", "gray96", "grey96", "gray97", "grey97", "gray98", "grey98", "gray99", "grey99", "gray100", "grey100", "dark grey", "darkgrey", "dark gray", "darkgray", "dark blue", "darkblue", "dark cyan", "darkcyan", "dark magenta", "darkmagenta", "dark red", "darkred", "light green", "lightgreen"};
    private static final int[] _colors = {0xfffafa, 0xf8f8ff, 0xf8f8ff, 0xf5f5f5, 0xf5f5f5, 0xdcdcdc, 0xfffaf0, 0xfffaf0, 0xfdf5e6, 0xfdf5e6, 0xfaf0e6, 0xfaebd7, 0xfaebd7, 0xffefd5, 0xffefd5, 0xffebcd, 0xffebcd, 0xffe4c4, 0xffdab9, 0xffdab9, 0xffdead, 0xffdead, 0xffe4b5, 0xfff8dc, 0xfffff0, 0xfffacd, 0xfffacd, 0xfff5ee, 0xf0fff0, 0xf5fffa, 0xf5fffa, 0xf0ffff, 0xf0f8ff, 0xf0f8ff, 0xe6e6fa, 0xfff0f5, 0xfff0f5, 0xffe4e1, 0xffe4e1, 0xffffff, 0x000000, 0x2f4f4f, 0x2f4f4f, 0x2f4f4f, 0x2f4f4f, 0x696969, 0x696969, 0x696969, 0x696969, 0x708090, 0x708090, 0x708090, 0x708090, 0x778899, 0x778899, 0x778899, 0x778899, 0xbebebe, 0xbebebe, 0xd3d3d3, 0xd3d3d3, 0xd3d3d3, 0xd3d3d3, 0x191970, 0x191970, 0x000080, 0x000080, 0x000080, 0x6495ed, 0x6495ed, 0x483d8b, 0x483d8b, 0x6a5acd, 0x6a5acd, 0x7b68ee, 0x7b68ee, 0x8470ff, 0x8470ff, 0x0000cd, 0x0000cd, 0x4169e1, 0x4169e1, 0x0000ff, 0x1e90ff, 0x1e90ff, 0x00bfff, 0x00bfff, 0x87ceeb, 0x87ceeb, 0x87cefa, 0x87cefa, 0x4682b4, 0x4682b4, 0xb0c4de, 0xb0c4de, 0xadd8e6, 0xadd8e6, 0xb0e0e6, 0xb0e0e6, 0xafeeee, 0xafeeee, 0x00ced1, 0x00ced1, 0x48d1cc, 0x48d1cc, 0x40e0d0, 0x00ffff, 0xe0ffff, 0xe0ffff, 0x5f9ea0, 0x5f9ea0, 0x66cdaa, 0x66cdaa, 0x7fffd4, 0x006400, 0x006400, 0x556b2f, 0x556b2f, 0x8fbc8f, 0x8fbc8f, 0x2e8b57, 0x2e8b57, 0x3cb371, 0x3cb371, 0x20b2aa, 0x20b2aa, 0x98fb98, 0x98fb98, 0x00ff7f, 0x00ff7f, 0x7cfc00, 0x7cfc00, 0x00ff00, 0x7fff00, 0x00fa9a, 0x00fa9a, 0xadff2f, 0xadff2f, 0x32cd32, 0x32cd32, 0x9acd32, 0x9acd32, 0x228b22, 0x228b22, 0x6b8e23, 0x6b8e23, 0xbdb76b, 0xbdb76b, 0xf0e68c, 0xeee8aa, 0xeee8aa, 0xfafad2, 0xfafad2, 0xffffe0, 0xffffe0, 0xffff00, 0xffd700, 0xeedd82, 0xeedd82, 0xdaa520, 0xb8860b, 0xb8860b, 0xbc8f8f, 0xbc8f8f, 0xcd5c5c, 0xcd5c5c, 0x8b4513, 0x8b4513, 0xa0522d, 0xcd853f, 0xdeb887, 0xf5f5dc, 0xf5deb3, 0xf4a460, 0xf4a460, 0xd2b48c, 0xd2691e, 0xb22222, 0xa52a2a, 0xe9967a, 0xe9967a, 0xfa8072, 0xffa07a, 0xffa07a, 0xffa500, 0xff8c00, 0xff8c00, 0xff7f50, 0xf08080, 0xf08080, 0xff6347, 0xff4500, 0xff4500, 0xff0000, 0xff69b4, 0xff69b4, 0xff1493, 0xff1493, 0xffc0cb, 0xffb6c1, 0xffb6c1, 0xdb7093, 0xdb7093, 0xb03060, 0xc71585, 0xc71585, 0xd02090, 0xd02090, 0xff00ff, 0xee82ee, 0xdda0dd, 0xda70d6, 0xba55d3, 0xba55d3, 0x9932cc, 0x9932cc, 0x9400d3, 0x9400d3, 0x8a2be2, 0x8a2be2, 0xa020f0, 0x9370db, 0x9370db, 0xd8bfd8, 0xfffafa, 0xeee9e9, 0xcdc9c9, 0x8b8989, 0xfff5ee, 0xeee5de, 0xcdc5bf, 0x8b8682, 0xffefdb, 0xeedfcc, 0xcdc0b0, 0x8b8378, 0xffe4c4, 0xeed5b7, 0xcdb79e, 0x8b7d6b, 0xffdab9, 0xeecbad, 0xcdaf95, 0x8b7765, 0xffdead, 0xeecfa1, 0xcdb38b, 0x8b7900, 0xfffacd, 0xeee9bf, 0xcdc9a5, 0x8b8970, 0xfff8dc, 0xeee8cd, 0xcdc8b1, 0x8b8878, 0xfffff0, 0xeeeee0, 0xcdcdc1, 0x8b8b83, 0xf0fff0, 0xe0eee0, 0xc1cdc1, 0x838b83, 0xfff0f5, 0xeee0e5, 0xcdc1c5, 0x8b8386, 0xffe4e1, 0xeed5d2, 0xcdb7b5, 0x8b7d7b, 0xf0ffff, 0xe0eeee, 0xc1cdcd, 0x838b8b, 0x836fff, 0x7a67ee, 0x6959cd, 0x473c8b, 0x4876ff, 0x436eee, 0x3a5fcd, 0x27408b, 0x0000ff, 0x0000ee, 0x0000cd, 0x00008b, 0x1e90ff, 0x1c86ee, 0x1874cd, 0x104e8b, 0x63b8ff, 0x5cacee, 0x4f94cd, 0x36648b, 0x00bfff, 0x00b2ee, 0x009acd, 0x00688b, 0x87ceff, 0x7ec0ee, 0x6ca6cd, 0x4a708b, 0xb0e2ff, 0xa4d3ee, 0x8db6cd, 0x607b8b, 0xc6e2ff, 0xb9d3ee, 0x9fb6cd, 0x6c7b8b, 0xcae1ff, 0xbcd2ee, 0xa2b5cd, 0x6e7b8b, 0xbfefff, 0xb2dfee, 0x9ac0cd, 0x68838b, 0xe0ffff, 0xd1eeee, 0xb4cdcd, 0x7a8b8b, 0xbbffff, 0xaeeeee, 0x96cdcd, 0x668b8b, 0x98f5ff, 0x8ee5ee, 0x7ac5cd, 0x53868b, 0x00f5ff, 0x00e5ee, 0x00c5cd, 0x00868b, 0x00ffff, 0x00eeee, 0x00cdcd, 0x008b8b, 0x97ffff, 0x8deeee, 0x79cdcd, 0x528b8b, 0x7fffd4, 0x76eec6, 0x66cdaa, 0x458b74, 0xc1ffc1, 0xb4eeb4, 0x9bcd9b, 0x698b69, 0x54ff9f, 0x4eee94, 0x43cd80, 0x2e8b00, 0x9aff9a, 0x90ee90, 0x7ccd7c, 0x548b00, 0x00ff7f, 0x00ee76, 0x00cd66, 0x008b00, 0x00ff00, 0x00ee00, 0x00cd00, 0x008b00, 0x7fff00, 0x76ee00, 0x66cd00, 0x458b00, 0xc0ff00, 0xb3ee00, 0x9acd00, 0x698b00, 0xcaff70, 0xbcee68, 0xa2cd00, 0x6e8b00, 0xfff68f, 0xeee685, 0xcdc673, 0x8b8600, 0xffec8b, 0xeedc82, 0xcdbe70, 0x8b8100, 0xffffe0, 0xeeeed1, 0xcdcdb4, 0x8b8b7a, 0xffff00, 0xeeee00, 0xcdcd00, 0x8b8b00, 0xffd700, 0xeec900, 0xcdad00, 0x8b7500, 0xffc100, 0xeeb400, 0xcd9b00, 0x8b6900, 0xffb900, 0xeead00, 0xcd9500, 0x8b6500, 0xffc1c1, 0xeeb4b4, 0xcd9b9b, 0x8b6969, 0xff6a6a, 0xee6300, 0xcd5500, 0x8b3a00, 0xff8200, 0xee7900, 0xcd6800, 0x8b4700, 0xffd39b, 0xeec591, 0xcdaa7d, 0x8b7300, 0xffe7ba, 0xeed8ae, 0xcdba96, 0x8b7e66, 0xffa500, 0xee9a00, 0xcd8500, 0x8b5a00, 0xff7f00, 0xee7600, 0xcd6600, 0x8b4500, 0xff3000, 0xee2c00, 0xcd2600, 0x8b1a00, 0xff4000, 0xee3b00, 0xcd3300, 0x8b2300, 0xff8c69, 0xee8200, 0xcd7000, 0x8b4c00, 0xffa07a, 0xee9572, 0xcd8100, 0x8b5700, 0xffa500, 0xee9a00, 0xcd8500, 0x8b5a00, 0xff7f00, 0xee7600, 0xcd6600, 0x8b4500, 0xff7200, 0xee6a00, 0xcd5b00, 0x8b3e00, 0xff6300, 0xee5c00, 0xcd4f00, 0x8b3600, 0xff4500, 0xee4000, 0xcd3700, 0x8b2500, 0xff0000, 0xee0000, 0xcd0000, 0x8b0000, 0xff1493, 0xee1289, 0xcd1076, 0x8b0a00, 0xff6eb4, 0xee6aa7, 0xcd6090, 0x8b3a62, 0xffb5c5, 0xeea9b8, 0xcd919e, 0x8b636c, 0xffaeb9, 0xeea2ad, 0xcd8c95, 0x8b5f65, 0xff82ab, 0xee799f, 0xcd6889, 0x8b4700, 0xff34b3, 0xee30a7, 0xcd2990, 0x8b1c00, 0xff3e96, 0xee3a8c, 0xcd3278, 0x8b2200, 0xff00ff, 0xee00ee, 0xcd00cd, 0x8b008b, 0xff83fa, 0xee7ae9, 0xcd69c9, 0x8b4789, 0xffbbff, 0xeeaeee, 0xcd96cd, 0x8b668b, 0xe066ff, 0xd15fee, 0xb452cd, 0x7a378b, 0xbf3eff, 0xb23aee, 0x9a32cd, 0x68228b, 0x9b30ff, 0x912cee, 0x7d26cd, 0x551a8b, 0xab82ff, 0x9f79ee, 0x8968cd, 0x5d478b, 0xffe1ff, 0xeed2ee, 0xcdb5cd, 0x8b7b8b, 0x000000, 0x000000, 0x030303, 0x030303, 0x050505, 0x050505, 0x080808, 0x080808, 0x0a0a0a, 0x0a0a0a, 0x0d0d0d, 0x0d0d0d, 0x0f0f0f, 0x0f0f0f, 0x121212, 0x121212, 0x141414, 0x141414, 0x171717, 0x171717, 0x1a1a1a, 0x1a1a1a, 0x1c1c1c, 0x1c1c1c, 0x1f1f1f, 0x1f1f1f, 0x212121, 0x212121, 0x242424, 0x242424, 0x262626, 0x262626, 0x292929, 0x292929, 0x2b2b2b, 0x2b2b2b, 0x2e2e2e, 0x2e2e2e, 0x303030, 0x303030, 0x333333, 0x333333, 0x363636, 0x363636, 0x383838, 0x383838, 0x3b3b3b, 0x3b3b3b, 0x3d3d3d, 0x3d3d3d, 0x404040, 0x404040, 0x424242, 0x424242, 0x454545, 0x454545, 0x474747, 0x474747, 0x4a4a4a, 0x4a4a4a, 0x4d4d4d, 0x4d4d4d, 0x4f4f4f, 0x4f4f4f, 0x525252, 0x525252, 0x545454, 0x545454, 0x575757, 0x575757, 0x595959, 0x595959, 0x5c5c5c, 0x5c5c5c, 0x5e5e5e, 0x5e5e5e, 0x616161, 0x616161, 0x636363, 0x636363, 0x666666, 0x666666, 0x696969, 0x696969, 0x6b6b6b, 0x6b6b6b, 0x6e6e6e, 0x6e6e6e, 0x707070, 0x707070, 0x737373, 0x737373, 0x757575, 0x757575, 0x787878, 0x787878, 0x7a7a7a, 0x7a7a7a, 0x7d7d7d, 0x7d7d7d, 0x7f7f7f, 0x7f7f7f, 0x828282, 0x828282, 0x858585, 0x858585, 0x878787, 0x878787, 0x8a8a8a, 0x8a8a8a, 0x8c8c8c, 0x8c8c8c, 0x8f8f8f, 0x8f8f8f, 0x919191, 0x919191, 0x949494, 0x949494, 0x969696, 0x969696, 0x999999, 0x999999, 0x9c9c9c, 0x9c9c9c, 0x9e9e9e, 0x9e9e9e, 0xa1a1a1, 0xa1a1a1, 0xa3a3a3, 0xa3a3a3, 0xa6a6a6, 0xa6a6a6, 0xa8a8a8, 0xa8a8a8, 0xababab, 0xababab, 0xadadad, 0xadadad, 0xb0b0b0, 0xb0b0b0, 0xb3b3b3, 0xb3b3b3, 0xb5b5b5, 0xb5b5b5, 0xb8b8b8, 0xb8b8b8, 0xbababa, 0xbababa, 0xbdbdbd, 0xbdbdbd, 0xbfbfbf, 0xbfbfbf, 0xc2c2c2, 0xc2c2c2, 0xc4c4c4, 0xc4c4c4, 0xc7c7c7, 0xc7c7c7, 0xc9c9c9, 0xc9c9c9, 0xcccccc, 0xcccccc, 0xcfcfcf, 0xcfcfcf, 0xd1d1d1, 0xd1d1d1, 0xd4d4d4, 0xd4d4d4, 0xd6d6d6, 0xd6d6d6, 0xd9d9d9, 0xd9d9d9, 0xdbdbdb, 0xdbdbdb, 0xdedede, 0xdedede, 0xe0e0e0, 0xe0e0e0, 0xe3e3e3, 0xe3e3e3, 0xe5e5e5, 0xe5e5e5, 0xe8e8e8, 0xe8e8e8, 0xebebeb, 0xebebeb, 0xededed, 0xededed, 0xf0f0f0, 0xf0f0f0, 0xf2f2f2, 0xf2f2f2, 0xf5f5f5, 0xf5f5f5, 0xf7f7f7, 0xf7f7f7, 0xfafafa, 0xfafafa, 0xfcfcfc, 0xfcfcfc, 0xffffff, 0xffffff, 0xa9a9a9, 0xa9a9a9, 0xa9a9a9, 0xa9a9a9, 0x00008b, 0x00008b, 0x008b8b, 0x008b8b, 0x8b008b, 0x8b008b, 0x8b0000, 0x8b0000, 0x90ee90, 0x90ee90};

    /**
     * Constructor.
     *
     * @param id      The colormap ID.
     * @param xServer The X server.
     * @param client  The client issuing the request.
     */
    public Colormap(int id, XServer xServer, Client client, ScreenView screen) {
        super(COLORMAP, id, xServer, client);

        _screen = screen;
    }

    /**
     * Return the value of a black pixel.
     *
     * @return The value of a black pixel.
     */
    public int getBlackPixel() {
        return Color.BLACK;
    }

    /**
     * Return the value of a white pixel.
     *
     * @return The value of a white pixel.
     */
    public int getWhitePixel() {
        return Color.WHITE;
    }

    /**
     * Has the colormap been installed?
     *
     * @return True if the colormap has been installed.
     */
    public boolean getInstalled() {
        return _installed;
    }

    /**
     * Set whether the colormap has been installed.
     *
     * @param installed Whether the colormap has been installed.
     */
    public void setInstalled(boolean installed) {
        _installed = installed;
        if (_installed) _screen.addInstalledColormap(this);
        else _screen.removeInstalledColormap(this);
    }

    /**
     * Initialize the color name hash table.
     */
    private static void initializeColorNames() {
        if (_colorNames != null) return;

        _colorNames = new Hashtable<String, Integer>();
        for (int i = 0; i < _names.length; i++)
            _colorNames.put(_names[i], _colors[i]);
    }

    /**
     * Construct a color from 16-bit RGB components.
     *
     * @param r The red component.
     * @param g The green component.
     * @param b The blue component.
     * @return
     */
    public static int fromParts16(int r, int g, int b) {
        return 0xff000000 | ((r & 0xff00) << 8) | (g & 0xff00) | ((b & 0xff00) >> 8);
    }

    /**
     * Process an X request relating to this colormap.
     *
     * @param client         The remote client.
     * @param opcode         The request's opcode.
     * @param arg            Optional first argument.
     * @param bytesRemaining Bytes yet to be read in the request.
     * @throws IOException
     */
    @Override
    public void processRequest(Client client, byte opcode, byte arg, int bytesRemaining) throws IOException {
        InputOutput io = client.getInputOutput();

        switch (opcode) {
            case RequestCode.FreeColormap:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else if (_id != _screen.getDefaultColormap().getId()) {
                    _xServer.freeResource(_id);
                    if (_client != null) _client.freeResource(this);
                }
                break;
            case RequestCode.InstallColormap:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    setInstalled(true);
                }
                break;
            case RequestCode.UninstallColormap:
                if (bytesRemaining != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    setInstalled(false);
                }
                break;
            case RequestCode.AllocColor:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int r = io.readShort();    // Red.
                    int g = io.readShort();    // Green.
                    int b = io.readShort();    // Blue.
                    int color = fromParts16(r, g, b);

                    io.readSkip(2);    // Unused.
                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(0);    // Reply length.
                        io.writeShort((short) r);    // Red.
                        io.writeShort((short) g);    // Green.
                        io.writeShort((short) b);    // Blue.
                        io.writePadBytes(2);    // Unused.
                        io.writeInt(color);    // Pixel.
                        io.writePadBytes(12);    // Unused.
                    }
                    io.flush();
                }
                break;
            case RequestCode.AllocNamedColor:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int n = io.readShort();    // Length of name.
                    int pad = -n & 3;

                    io.readSkip(2);    // Unused.
                    bytesRemaining -= 4;
                    if (bytesRemaining != n + pad) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                    } else {
                        byte[] bytes = new byte[n];

                        io.readBytes(bytes, 0, n);    // Name.
                        io.readSkip(pad);    // Unused.

                        initializeColorNames();

                        String name = new String(bytes).toLowerCase(Locale.US);

                        if (_colorNames.containsKey(name)) {
                            int color = _colorNames.get(name);

                            synchronized (io) {
                                Util.writeReplyHeader(client, (byte) 0);
                                io.writeInt(0);    // Reply length.
                                io.writeInt(color);    // Pixel.

                                int r = (color & 0xff0000) >> 16;
                                int g = (color & 0xff00) >> 8;
                                int b = color & 0xff;

                                // Exact red/green/blue.
                                io.writeShort((short) (r | (r << 8)));
                                io.writeShort((short) (g | (g << 8)));
                                io.writeShort((short) (b | (b << 8)));
                                // Visual red/green/blue.
                                io.writeShort((short) (r | (r << 8)));
                                io.writeShort((short) (g | (g << 8)));
                                io.writeShort((short) (b | (b << 8)));
                                io.writePadBytes(8);    // Unused.
                            }
                            io.flush();
                        } else {
                            ErrorCode.write(client, ErrorCode.Name, opcode, 0);
                        }
                    }
                }
                break;
            case RequestCode.AllocColorCells:
                if (bytesRemaining != 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Cannot allocate from a TrueColor colormap.
                    io.readShort();    // Number of colors.
                    io.readShort();    // Number of planes.

                    ErrorCode.write(client, ErrorCode.Access, opcode, 0);
                }
                break;
            case RequestCode.AllocColorPlanes:
                if (bytesRemaining != 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Cannot allocate from a TrueColor colormap.
                    io.readShort();    // Colors.
                    io.readShort();    // Reds.
                    io.readShort();    // Greens.
                    io.readShort();    // Blues.

                    ErrorCode.write(client, ErrorCode.Access, opcode, 0);
                }
                break;
            case RequestCode.FreeColors:
                if (bytesRemaining < 4 || (bytesRemaining & 3) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Cannot modify a TrueColor colormap.
                    io.readSkip(bytesRemaining);
                }
                break;
            case RequestCode.StoreColors:
                if ((bytesRemaining % 12) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Cannot modify a TrueColor colormap.
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Access, opcode, 0);
                }
                break;
            case RequestCode.StoreNamedColor:
                if (bytesRemaining < 8) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {    // Cannot modify a TrueColor colormap.
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Access, opcode, 0);
                }
                break;
            case RequestCode.QueryColors:
                if ((bytesRemaining & 3) != 0) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int n = bytesRemaining / 4;
                    int[] pixels = new int[n];

                    for (int i = 0; i < n; i++)
                        pixels[i] = io.readInt();

                    synchronized (io) {
                        Util.writeReplyHeader(client, (byte) 0);
                        io.writeInt(n * 2);    // Reply length.
                        io.writeShort((short) n);    // Number of RGBs.
                        io.writePadBytes(22);    // Unused.

                        for (int i = 0; i < n; i++) {
                            int color = pixels[i];
                            int r = (color & 0xff0000) >> 16;
                            int g = (color & 0xff00) >> 8;
                            int b = color & 0xff;

                            io.writeShort((short) (r | (r << 8)));    // Red.
                            io.writeShort((short) (g | (g << 8)));    // Green.
                            io.writeShort((short) (b | (b << 8)));    // Blue.
                            io.writePadBytes(2);    // Unused.
                        }
                    }
                    io.flush();
                }
                break;
            case RequestCode.LookupColor:
                if (bytesRemaining < 4) {
                    io.readSkip(bytesRemaining);
                    ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                } else {
                    int n = io.readShort();    // Length of name.
                    int pad = -n & 3;

                    io.readSkip(2);    // Unused.
                    bytesRemaining -= 4;
                    if (bytesRemaining != n + pad) {
                        io.readSkip(bytesRemaining);
                        ErrorCode.write(client, ErrorCode.Length, opcode, 0);
                    } else {
                        byte[] bytes = new byte[n];

                        io.readBytes(bytes, 0, n);    // Name.
                        io.readSkip(pad);    // Unused.

                        initializeColorNames();

                        String name = new String(bytes).toLowerCase(Locale.US);

                        if (_colorNames.containsKey(name)) {
                            int color = _colorNames.get(name);

                            synchronized (io) {
                                Util.writeReplyHeader(client, (byte) 0);
                                io.writeInt(0);    // Reply length.

                                int r = (color & 0xff0000) >> 16;
                                int g = (color & 0xff00) >> 8;
                                int b = color & 0xff;

                                // Exact red/green/blue.
                                io.writeShort((short) (r | (r << 8)));
                                io.writeShort((short) (g | (g << 8)));
                                io.writeShort((short) (b | (b << 8)));
                                // Visual red/green/blue.
                                io.writeShort((short) (r | (r << 8)));
                                io.writeShort((short) (g | (g << 8)));
                                io.writeShort((short) (b | (b << 8)));
                                io.writePadBytes(12);    // Unused.
                            }
                            io.flush();
                        } else {
                            ErrorCode.write(client, ErrorCode.Name, opcode, 0);
                        }
                    }
                }
                break;
            default:
                io.readSkip(bytesRemaining);
                ErrorCode.write(client, ErrorCode.Implementation, opcode, 0);
                break;
        }
    }

    /**
     * Process a CreateColormap request.
     *
     * @param xServer The X server.
     * @param client  The client issuing the request.
     * @param id      The ID of the colormap to create.
     * @param alloc   Allocate colours; 0=None, 1=All.
     * @throws IOException
     */
    public static void processCreateColormapRequest(XServer xServer, Client client, int id, int alloc) throws IOException {
        InputOutput io = client.getInputOutput();
        int wid = io.readInt();    // Window.
        int vid = io.readInt();    // Visual.
        Resource r = xServer.getResource(wid);

        if (alloc != 0) {    // Only TrueColor supported.
            ErrorCode.write(client, ErrorCode.Match, RequestCode.CreateColormap, id);
        } else if (r == null || r.getType() != Resource.WINDOW) {
            ErrorCode.write(client, ErrorCode.Window, RequestCode.CreateColormap, wid);
        } else if (vid != xServer.getRootVisual().getId()) {
            ErrorCode.write(client, ErrorCode.Match, RequestCode.CreateColormap, wid);
        } else {
            ScreenView s = ((Window) r).getScreen();
            Colormap cmap = new Colormap(id, xServer, client, s);

            xServer.addResource(cmap);
            client.addResource(cmap);
        }
    }

    /**
     * Process a CopyColormapAndFree request.
     *
     * @param client The client issuing the request.
     * @param id     The ID of the colormap to create.
     * @throws IOException
     */
    public void processCopyColormapAndFree(Client client, int id) throws IOException {
        Colormap cmap = new Colormap(id, _xServer, client, _screen);

        // Nothing to copy, nothing to free.
        _xServer.addResource(cmap);
        client.addResource(cmap);
    }
}