/* config.h.  Generated from config.h.in by configure.  */
/* config.h.in.  Generated from configure.ac by autoheader.  */

/* Define if building universal (internal helper macro) */
/* #undef AC_APPLE_UNIVERSAL_BUILD */

/* always defined to indicate that i18n is enabled */
#define ENABLE_NLS 1

/* Enable compile-time and run-time bounds-checking, and some warnings. */
     #if !defined _FORTIFY_SOURCE &&  defined __OPTIMIZE__ && __OPTIMIZE__
     # define _FORTIFY_SOURCE 2
     #endif
    

/* GETTEXT package name */
#define GETTEXT_PACKAGE "spice-gtk"

/* Define to 1 if you have the <arpa/inet.h> header file. */
#define HAVE_ARPA_INET_H 1

/* Define to 1 if you have the `bind_textdomain_codeset' function. */
#define HAVE_BIND_TEXTDOMAIN_CODESET 1

/* Define to 1 if you have the `clearenv' function. */
#define HAVE_CLEARENV 1

/* Define to 1 if you have the `dcgettext' function. */
#define HAVE_DCGETTEXT 1

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* Define if the GNU gettext() function is already present or preinstalled. */
#define HAVE_GETTEXT 1

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define if your <locale.h> file defines LC_MESSAGES. */
#define HAVE_LC_MESSAGES 1

/* Define to 1 if you have the <locale.h> header file. */
#define HAVE_LOCALE_H 1

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Define to 1 if you have the <netinet/in.h> header file. */
#define HAVE_NETINET_IN_H 1

/* Define if you have a polkit with polkit_authority_get_sync() */
/* #undef HAVE_POLKIT_AUTHORITY_GET_SYNC */

/* Define if you have a polkit with
   polkit_authorization_result_get_dismissed() */
/* #undef HAVE_POLKIT_AUTHORIZATION_RESULT_GET_DISMISSED */

/* Have Quartz? */
/* #undef HAVE_QUARTZ */

/* Have xrandr? */
#undef HAVE_RANDR

/* whether Cyrus SASL is available for authentication */
#undef HAVE_SASL

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/ipc.h> header file. */
#define HAVE_SYS_IPC_H 1

/* Define to 1 if you have the <sys/shm.h> header file. */
#undef HAVE_SYS_SHM_H

/* Define to 1 if you have the <sys/socket.h> header file. */
#define HAVE_SYS_SOCKET_H 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <termios.h> header file. */
#define HAVE_TERMIOS_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Have Win32? */
/* #undef HAVE_WINDOWS */

/* Have x11? */
#undef HAVE_X11

/* Define to 1 if you have the <X11/XKBlib.h> header file. */
#undef HAVE_X11_XKBLIB_H

/* Define to the sub-directory in which libtool stores uninstalled libraries.
   */
#define LT_OBJDIR ".libs/"

/* Define to 1 if your C compiler doesn't accept -c and -o together. */
/* #undef NO_MINUS_C_MINUS_O */

/* Name of package */
#define PACKAGE "spice-gtk"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT "spice-devel@lists.freedesktop.org"

/* Define to the full name of this package. */
#define PACKAGE_NAME "spice-gtk"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "spice-gtk 0.22"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "spice-gtk"

/* Define to the home page for this package. */
#define PACKAGE_URL ""

/* Define to the version of this package. */
#define PACKAGE_VERSION "0.22"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Define if supporting dbus */
/* #undef USE_DBUS */

/* Define if supporting gudev */
/* #undef USE_GUDEV */

/* Define if libusb has hotplug support */
#define USE_LIBUSB_HOTPLUG 1

/* Define if supporting polkit */
/* #undef USE_POLKIT */

/* Define if supporting smartcard proxying */
/* #undef USE_SMARTCARD */

/* Define if supporting usbredir proxying */
#define USE_USBREDIR 1

/* Version number of package */
#define VERSION "0.22"

/* Have GStreamer? */
/* #undef WITH_GSTAUDIO */

/* Whether to use gthread coroutine impl */
#undef WITH_GTHREAD

/* Have pulseaudio? */
#undef WITH_PULSE

/* Whether to use ucontext coroutine impl */
#undef WITH_UCONTEXT

/* Define if compiling with usb.ids support */
#undef WITH_USBIDS

/* Whether to use fiber coroutine impl */
#undef WITH_WINFIBER

/* Use X11 backend? */
/* #undef WITH_X11 */

/* Define WORDS_BIGENDIAN to 1 if your processor stores words with the most
   significant byte first (like Motorola and SPARC, unlike Intel). */
#if defined AC_APPLE_UNIVERSAL_BUILD
# if defined __BIG_ENDIAN__
#  define WORDS_BIGENDIAN 1
# endif
#else
# ifndef WORDS_BIGENDIAN
/* #  undef WORDS_BIGENDIAN */
# endif
#endif
