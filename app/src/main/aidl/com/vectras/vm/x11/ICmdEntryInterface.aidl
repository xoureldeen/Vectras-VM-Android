package com.vectras.vm.x11;

// This interface is used by utility on termux side.
interface ICmdEntryInterface {
    ParcelFileDescriptor getXConnection();
    ParcelFileDescriptor getLogcatOutput();
}