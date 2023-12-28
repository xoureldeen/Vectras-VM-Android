package com.vectras.vm.logger;

import android.os.Parcel;
import android.os.Parcelable;

public enum VMStatus implements Parcelable {
    V_STARTVM,
    V_STOPVM,
    UNKNOWN_LEVEL;

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VMStatus> CREATOR = new Creator<VMStatus>() {
        @Override
        public VMStatus createFromParcel(Parcel in) {
            return VMStatus.values()[in.readInt()];
        }

        @Override
        public VMStatus[] newArray(int size) {
            return new VMStatus[size];
        }
    };
}

