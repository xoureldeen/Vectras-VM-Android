#!/usr/bin/perl

use strict;
use warnings;

use Text::CSV;

my %names = (
    linux => [],
    osx => []
);

my %namecolumns = (
    linux => 0,
    osx => 2,
    win32 => 10,
    );

# Base data sources:
#
#  linux:     Linux: linux/input.h                                  (master set)
#    osx:      OS-X: Carbon/HIToolbox/Events.h                      (manually mapped)
# atset1:  AT Set 1: linux/drivers/input/keyboard/atkbd.c           (atkbd_set2_keycode + atkbd_unxlate_table)
# atset2:  AT Set 2: linux/drivers/input/keyboard/atkbd.c           (atkbd_set2_keycode)
# atset3:  AT Set 3: linux/drivers/input/keyboard/atkbd.c           (atkbd_set3_keycode)
#     xt:        XT: linux/drivers/input/keyboard/xt.c              (xtkbd_keycode)
#  xtkbd: Linux RAW: linux/drivers/char/keyboard.c                  (x86_keycodes)
#    usb:   USB HID: linux/drivers/hid/usbhid/usbkbd.c              (usb_kbd_keycode)
#  win32:     Win32: mingw32/winuser.h                              (manually mapped)
# xwinxt:   XWin XT: xorg-server/hw/xwin/{winkeybd.c,winkeynames.h} (xt + manually transcribed)
# xkbdxt:   XKBD XT: xf86-input-keyboard/src/at_scancode.c          (xt + manually transcribed)
#
# Derived data sources
#
#    xorgevdev: Xorg +  evdev: linux + an offset
#      xorgkbd: Xorg +    kbd: xkbdxt + an offset
#  xorgxquartz: Xorg +   OS-X: osx + an offset
#     xorgxwin: Xorg + Cygwin: xwinxt + an offset
#          rfb:   XT over RFB: xtkbd + special re-encoding of high bit

my @basemaps = qw(linux osx atset1 atset2 atset3 xt xtkbd usb win32 xwinxt xkbdxt android);
my @derivedmaps = qw(xorgevdev xorgkbd xorgxquartz xorgxwin rfb);
my @maps = (@basemaps, @derivedmaps);

my %maps;

foreach my $map (@maps) {
    $maps{$map} = [ [], [] ];
}
my %mapcolumns = (
    osx => 3,
    atset1 => 4,
    atset2 => 5,
    atset3 => 6,
    xt => 7,
    xtkbd => 8,
    usb => 9,
    win32 => 11,
    xwinxt => 12,
    xkbdxt => 13,
    android => 14,
    );

sub help {
    my $msg = shift;
    print $msg;
    print "\n";
    print "Valid keymaps are:\n";
    print "\n";
    foreach my $name (sort { $a cmp $b } keys %maps) {
    print "  $name\n";
    }
    print "\n";
    exit (1);
}

if ($#ARGV != 2) {
    help("syntax: $0 KEYMAPS SRCMAP DSTMAP\n");
}

my $keymaps = shift @ARGV;
my $src = shift @ARGV;
my $dst = shift @ARGV;

help("$src is not a known keymap\n") unless exists $maps{$src};
help("$dst is not a known keymap\n") unless exists $maps{$dst};


open CSV, $keymaps
    or die "cannot read $keymaps: $!";

my $csv = Text::CSV->new();
# Discard column headings
$csv->getline(\*CSV);

my $row;
while ($row = $csv->getline(\*CSV)) {
    my $linux = $row->[1];

    $linux = hex($linux) if $linux =~ /0x/;

    my $to = $maps{linux}->[0];
    my $from = $maps{linux}->[1];
    $to->[$linux] = $linux;
    $from->[$linux] = $linux;

    foreach my $name (keys %namecolumns) {
    my $col = $namecolumns{$name};
    my $val = $row->[$col];

    $val = "" unless defined $val;

    $names{$name}->[$linux] = $val;
    }

    foreach my $name (keys %mapcolumns) {
    my $col = $mapcolumns{$name};
    my $val = $row->[$col];

    next unless defined $val && $val ne "";
    $val = hex($val) if $val =~ /0x/;

    $to = $maps{$name}->[0];
        $from = $maps{$name}->[1];
    $to->[$linux] = $val;
    $from->[$val] = $linux;
    }

    # XXX there are some special cases in kbd to handle
    # Xorg KBD driver is the Xorg KBD XT codes offset by +8
    # The XKBD XT codes are the same as normal XT codes
    # for values <= 83, and completely made up for extended
    # scancodes :-(
    ($to, $from) = @{$maps{xorgkbd}};
    if (defined $maps{xkbdxt}->[0]->[$linux]) {
    $to->[$linux] = $maps{xkbdxt}->[0]->[$linux] + 8;
    $from->[$to->[$linux]] = $linux;
    }

    # Xorg evdev is simply Linux keycodes offset by +8
    ($to, $from) = @{$maps{xorgevdev}};
    $to->[$linux] = $linux + 8;
    $from->[$to->[$linux]] = $linux;

    # Xorg XQuartz is simply OS-X keycodes offset by +8
    ($to, $from) = @{$maps{xorgxquartz}};
    if (defined $maps{osx}->[0]->[$linux]) {
    $to->[$linux] = $maps{osx}->[0]->[$linux] + 8;
    $from->[$to->[$linux]] = $linux;
    }

    # RFB keycodes are XT kbd keycodes with a slightly
    # different encoding of 0xe0 scan codes. RFB uses
    # the high bit of the first byte, instead of the low
    # bit of the second byte.
    ($to, $from) = @{$maps{rfb}};
    my $xtkbd = $maps{xtkbd}->[0]->[$linux];
    if (defined $xtkbd) {
    $to->[$linux] = $xtkbd ? (($xtkbd & 0x100)>>1) | ($xtkbd & 0x7f) : 0;
    $from->[$to->[$linux]] = $linux;
    }

    # Xorg Cygwin is the Xorg Cygwin XT codes offset by +8
    # The Cygwin XT codes are the same as normal XT codes
    # for values <= 83, and completely made up for extended
    # scancodes :-(
    ($to, $from) = @{$maps{xorgxwin}};
    if (defined $maps{xwinxt}->[0]->[$linux]) {
    $to->[$linux] = $maps{xwinxt}->[0]->[$linux] + 8;
    $from->[$to->[$linux]] = $linux;
    }

#    print $linux, "\n";
}

close CSV;

my $srcmap = $maps{$src}->[1];
my $dstmap = $maps{$dst}->[0];

printf "static const guint16 keymap_%s2%s[] = {\n", $src, $dst;

for (my $i = 0 ; $i <= $#{$srcmap} ; $i++) {
    my $linux = $srcmap->[$i] || 0;
    my $j = $dstmap->[$linux];
    next unless $linux && $j;

    my $srcname = $names{$src}->[$linux] if exists $names{$src};
    my $dstname = $names{$dst}->[$linux] if exists $names{$dst};
    my $vianame = $names{linux}->[$linux] unless $src eq "linux" || $dst eq "linux";

    $srcname = "" unless $srcname;
    $dstname = "" unless $dstname;
    $vianame = "" unless $vianame;
    $srcname = " ($srcname)" if $srcname;
    $dstname = " ($dstname)" if $dstname;
    $vianame = " ($vianame)" if $vianame;

    my $comment;
    if ($src ne "linux" && $dst ne "linux") {
    $comment = sprintf "%d%s => %d%s via %d%s", $i, $srcname, $j, $dstname, $linux, $vianame;
    } else {
    $comment = sprintf "%d%s => %d%s", $i, $srcname, $j, $dstname;
    }

    my $data = sprintf "[0x%x] = 0x%x,", $i, $j;

    printf "  %-20s /* %s */\n", $data, $comment;
}

print "};\n";
