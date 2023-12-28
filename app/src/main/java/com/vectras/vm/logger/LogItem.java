/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.vectras.vm.logger;

import android.os.Parcel;
import android.os.Parcelable;

import com.vectras.vm.R;
import android.content.Context;
import java.util.Locale;
import java.util.UnknownFormatConversionException;
import java.util.FormatFlagsConversionMismatchException;
import android.annotation.SuppressLint;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import android.content.pm.PackageManager;
import java.io.ByteArrayInputStream;
import android.content.pm.Signature;
import java.security.MessageDigest;
import java.util.Arrays;
import android.content.pm.PackageInfo;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Created by arne on 24.04.16.
 */
public class LogItem implements Parcelable {
    
	private Object[] mArgs = null;
    private String mMessage = null;
	private int mResourceId;
    // Default log priority
    VectrasStatus.LogLevel mLevel = VectrasStatus.LogLevel.INFO;
    private long logtime = System.currentTimeMillis();
    private int mVerbosityLevel = -1;
	
	public LogItem(int resId, Object... args) {
        mResourceId = resId;
		mArgs = args;
    }
	
	public LogItem(VectrasStatus.LogLevel loglevel, int verblevel, String msg) {
        mLevel = loglevel;
        mMessage = msg;
		mVerbosityLevel = verblevel;
    }

    public LogItem(VectrasStatus.LogLevel level, int resId, Object... args) {
        mLevel = level;
		mResourceId = resId;
		mArgs = args;
    }
	
	public LogItem(VectrasStatus.LogLevel loglevel, String msg) {
        mLevel = loglevel;
        mMessage = msg;
    }


    public LogItem(VectrasStatus.LogLevel loglevel, int ressourceId) {
        mResourceId = ressourceId;
        mLevel = loglevel;
    }
	
	@Override
    public String toString() {
        return getString(null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeArray(mArgs);
        dest.writeString(mMessage);
		dest.writeInt(mResourceId);
        dest.writeInt(mLevel.getInt());
        
        dest.writeLong(logtime);
    }

    public LogItem(Parcel in) {
        mArgs = in.readArray(Object.class.getClassLoader());
        mMessage = in.readString();
		mResourceId = in.readInt();
        mLevel = VectrasStatus.LogLevel.getEnumByValue(in.readInt());
        logtime = in.readLong();
    }

    public static final Creator<LogItem> CREATOR
            = new Creator<LogItem>() {
        public LogItem createFromParcel(Parcel in) {
            return new LogItem(in);
        }

        public LogItem[] newArray(int size) {
            return new LogItem[size];
        }
    };

    public VectrasStatus.LogLevel getLogLevel() {
        return mLevel;
    }

    public long getLogtime() {
        return logtime;
    }
	
	public String getMessage() {
		return mMessage;
	}

	public String getString(Context c) {
        try {
            if (mMessage != null) {
                return mMessage;
            } else {
                if (c != null) {
					if (mResourceId == R.string.app_name)
                        return getAppInfoString(c);
                    else if (mArgs == null)
                        return c.getString(mResourceId);
                    else
                        return c.getString(mResourceId, mArgs);
                } else {
                    String str = String.format(Locale.ENGLISH, "Log (no context) resid %d", mResourceId);
                    if (mArgs != null)
                        str += join("|", mArgs);

                    return str;
                }
            }
        } catch (UnknownFormatConversionException e) {
            if (c != null)
                throw new UnknownFormatConversionException(e.getLocalizedMessage() + getString(null));
            else
                throw e;
        } catch (java.util.FormatFlagsConversionMismatchException e) {
            if (c != null)
                throw new FormatFlagsConversionMismatchException(e.getLocalizedMessage() + getString(null), e.getConversion());
            else
                throw e;
        }
    }
	
	//private String listb = "";
	
	// The lint is wrong here
    @SuppressLint("StringFormatMatches")
    private String getAppInfoString(Context c) {
        c.getPackageManager();
        String apksign = "error getting package signature";

        String version = "error getting version";
        try {
            @SuppressLint("PackageManagerGetSignatures")
			Signature raw = c.getPackageManager().getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(raw.toByteArray()));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();
            if (!Arrays.equals(digest, VectrasStatus.oficialkey) && !Arrays.equals(digest, VectrasStatus.oficialdebugkey))
                apksign = "";
            PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = String.format("%s Projeto %d", packageinfo.versionName, packageinfo.versionCode);

        } catch (PackageManager.NameNotFoundException | CertificateException |
				NoSuchAlgorithmException ignored) {
        }

       /* Object[] argsext = Arrays.copyOf(mArgs, mArgs.length);
        argsext[argsext.length - 1] = apksign;
        argsext[argsext.length - 2] = version;*/

        return c.getString(R.string.app_name, version, apksign);

    }
	
	// TextUtils.join will cause not macked exeception in tests ....
    public static String join(CharSequence delimiter, Object[] tokens) {
        StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (Object token : tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }
	
	public int getVerbosityLevel() {
        if (mVerbosityLevel == -1) {
            // Hack:
            // For message not from OpenVPN, report the status level as log level
            return mLevel.getInt();
        }
        return mVerbosityLevel;
    }
}
