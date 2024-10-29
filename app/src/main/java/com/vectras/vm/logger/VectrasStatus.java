package com.vectras.vm.logger;

import static android.provider.Settings.System.getString;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Vector;
import android.os.Build;
import com.vectras.vm.R;
import android.content.Intent;
import android.content.Context;
import android.os.Message;
import java.io.File;
import android.os.HandlerThread;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.util.Iterator;
import java.util.Locale;

public class VectrasStatus
{
	private static final LinkedList<LogItem> logbuffer;
	
	private static Vector<LogListener> logListener;
    private static Vector<StateListener> stateListener;
	
	private static VMStatus mLastLevel = VMStatus.V_STOPVM;
	
	private static String mLaststatemsg = "";
    private static String mLaststate = "NOPROCESS";
    private static int mLastStateresid = R.string.noproccesses;
    private static Intent mLastIntent = null;
	
	
	static final int MAXLOGENTRIES = 1000;

    public static boolean isVMActive() {
        return mLastLevel != VMStatus.V_STOPVM && !(mLastLevel == VMStatus.V_STOPVM);
    }
	
	public static String getLastState() {
		return mLaststate;
	}
	
	public static String getLastCleanLogMessage(Context c) {
        String message = mLaststatemsg;
        switch (mLastLevel) {
            case V_STARTVM:
                String[] parts = mLaststatemsg.split(",");
                if (parts.length >= 7)
                    message = String.format(Locale.US, "%s %s", parts[1], parts[6]);
                break;
        }

        while (message.endsWith(","))
            message = message.substring(0, message.length() - 1);

        String status = mLaststate;
        if (status.equals("NOPROCESS"))
            return message;

        String prefix = c.getString(mLastStateresid);
        if (mLastStateresid == R.string.unknownstate)
            message = status + message;
        if (message.length() > 0)
            prefix += ": ";

        return prefix + message;

    }
	
	
	public static enum LogLevel {
		
        INFO(2),
        ERROR(-2),
        WARNING(1),
        VERBOSE(3),
        DEBUG(4);

        protected int mValue;

        LogLevel(int value) {
            mValue = value;
        }

        public int getInt() {
            return mValue;
        }

        public static LogLevel getEnumByValue(int value) {
            switch (value) {
                case 2:
                    return INFO;
                case -2:
                    return ERROR;
                case 1:
                    return WARNING;
                case 3:
                    return VERBOSE;
                case 4:
                    return DEBUG;

                default:
                    return null;
            }
        }
    }
	
	// keytool -printcert -jarfile de.blinkt.openvpn_85.apk
	// tudo ok, certificado da Playstore
	static final byte[] oficialkey = {93, -72, 88, 103, -128, 115, -1, -47, 120, 113, 98, -56, 12, -56, 52, -62, 95, -2, -114, 95};
    // já atualizado, slipk certificado
	static final byte[] oficialdebugkey = {-41, 73, 58, 102, -81, -27, -120, 45, -56, -3, 53, -49, 119, -97, -20, -80, 65, 68, -72, -22};
	
	static {
        logbuffer = new LinkedList<>();
        logListener = new Vector<>();
        stateListener = new Vector<>();
		
		logInformation();
    }
	

    public synchronized static void clearLog() {
        logbuffer.clear();
		logInformation();
		logInfo("LOGS CLEARED!");
		
		for (LogListener li : logListener) {
			li.onClear();
		}
    }
	
	public synchronized static LogItem[] getlogbuffer() {

        // The stoned way of java to return an array from a vector
        // brought to you by eclipse auto complete
        return logbuffer.toArray(new LogItem[logbuffer.size()]);
    }
	
	private static void logInformation() {
		logInfo(R.string.app_name);
        logInfo(R.string.app_version);
        logInfo("MOBILE MODEL: " + Build.MODEL);
		logInfo("ANDROID VERSION: " + Build.VERSION.SDK_INT);
	}

	
	/**
	* Listeners
	*/
	
	public interface LogListener {
        void newLog(LogItem logItem);
		void onClear();
    }

    public interface StateListener {
        void updateState(String state, String logMessage, int localizedResId, VMStatus level, Intent intent);
    }
	
    public synchronized static void addLogListener(LogListener ll) {
        if (!logListener.contains(ll)) {
			logListener.add(ll);
		}
    }

    public synchronized static void removeLogListener(LogListener ll) {
        if (logListener.contains(ll)) {
			logListener.remove(ll);
		}
    }

    public synchronized static void addStateListener(StateListener sl) {
        if (!stateListener.contains(sl)) {
            stateListener.add(sl);
            if (mLaststate != null)
                sl.updateState(mLaststate, mLaststatemsg, mLastStateresid, mLastLevel, mLastIntent);
        }
    }
	
	public synchronized static void removeStateListener(StateListener sl) {
        if (stateListener.contains(sl)) {
			stateListener.remove(sl);
		}
    }

	
	/**
	* State
	*/
	
	public static final String
		V_STARTVM = "STARTING VM",
		V_STOPVM = "STOPPING VM";
	
	public static int getLocalizedState(String state) {
        switch (state) {
			case V_STARTVM:
                return R.string.startvm;
            case V_STOPVM:
                return R.string.stopvm;
        }
		return R.string.unknownstate;
    }

	private static VMStatus getLevel(String state) {
        String[] noreplyet = {V_STARTVM, V_STOPVM};
        String[] reply = {V_STARTVM, V_STOPVM};
        String[] startedvm = {V_STARTVM};
        String[] stoppedvm = {V_STOPVM};

        for (String x : noreplyet)
            if (state.equals(x))
                return VMStatus.V_STARTVM;

        for (String x : reply)
            if (state.equals(x))
                return VMStatus.V_STOPVM;

        for (String x : startedvm)
            if (state.equals(x))
                return VMStatus.V_STARTVM;

        for (String x : stoppedvm)
            if (state.equals(x))
                return VMStatus.V_STOPVM;

        return VMStatus.UNKNOWN_LEVEL;
    }
	
    public static void updateStateString(String state, String msg) {
        int rid = getLocalizedState(state);
        VMStatus level = getLevel(state);
        updateStateString(state, msg, rid, level);
    }

    public synchronized static void updateStateString(String state, String msg, int resid, VMStatus level)
    {
        updateStateString(state, msg, resid, level, null);
    }

    public synchronized static void updateStateString(String state, String msg, int resid, VMStatus level, Intent intent) {
        // Workound for OpenVPN doing AUTH and wait and being startedvm
        // Simply ignore these state
        /*if (mLastLevel == VMStatus.LEVEL_CONNECTED &&
				(state.equals(SSH_AUTHENTICATING))) {
            newLogItem(new LogItem((LogLevel.DEBUG), String.format("Ignoring SocksHttp Status in CONNECTED state (%s->%s): %s", state, level.toString(), msg)));
            return;
        }*/

        mLaststate = state;
        mLaststatemsg = msg;
        mLastStateresid = resid;
        mLastLevel = level;
        mLastIntent = intent;


        for (StateListener sl : stateListener) {
            sl.updateState(state, msg, resid, level, intent);
        }
		
        //newLogItem(new LogItem((LogLevel.DEBUG), String.format("SocksHttp Novo Status (%s->%s): %s",state,level.toString(),msg)));
    }

    
	/**
	* NewLog
	*/
	
    static void newLogItem(LogItem logItem) {
        newLogItem(logItem, false);
    }

    synchronized static void newLogItem(LogItem logItem, boolean cachedLine) {
        if (cachedLine) {
            logbuffer.addFirst(logItem);
        } else {
            logbuffer.addLast(logItem);
        }

        if (logbuffer.size() > MAXLOGENTRIES + MAXLOGENTRIES / 2) {
            while (logbuffer.size() > MAXLOGENTRIES)
                logbuffer.removeFirst();
        }

        for (LogListener ll : logListener) {
            ll.newLog(logItem);
        }
    }

	
	/**
	* Logger static methods
	*/
	
	public static void logException(String context, String e) {
        logException(LogLevel.ERROR, context, e);
    }
	
	public static void logException(LogLevel ll, String context, String e) {
        
		LogItem li;
		
		if (context != null)
			li = new LogItem(ll, String.format("%s: %s", context, e));
		else
			li = new LogItem(ll, String.format("Error: %s", e));

        newLogItem(li);
    }

	public static void logException(Exception e) {
        logException(LogLevel.ERROR, null, e.getMessage());
    }
	
	public static void logInfo(String message) {
        newLogItem(new LogItem(LogLevel.INFO, message));
    }

    public static void logDebug(String message) {
        newLogItem(new LogItem(LogLevel.DEBUG, message));
    }

    public static void logInfo(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.INFO, resourceId, args));
    }

    public static void logDebug(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.DEBUG, resourceId, args));
    }

    public static void logError(String msg) {
        newLogItem(new LogItem(LogLevel.ERROR, msg));
    }

    public static void logWarning(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.WARNING, resourceId, args));
    }

    public static void logWarning(String msg) {
        newLogItem(new LogItem(LogLevel.WARNING, msg));
    }

    public static void logError(int resourceId) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId));
    }

    public static void logError(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId, args));
    }

}
