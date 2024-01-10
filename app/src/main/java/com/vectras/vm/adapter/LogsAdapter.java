package com.vectras.vm.adapter;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import com.vectras.vm.logger.LogItem;
import java.util.Collections;
import android.widget.TextView;
import android.content.Context;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import java.util.Vector;
import android.database.DataSetObserver;
import java.util.Date;
import android.text.format.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.os.Message;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import com.vectras.vm.R;
import android.text.Html;
import android.view.MotionEvent;
import com.vectras.vm.logger.VectrasStatus;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.logViewHolder>
	implements VectrasStatus.LogListener ,Handler.Callback,
		View.OnTouchListener
{
	private static final int MESSAGE_NEWLOG = 0;

	private static final int MESSAGE_CLEARLOG = 1;

	private static final int MESSAGE_NEWTS = 2;
	private static final int MESSAGE_NEWLOGLEVEL = 3;

	public static final int TIME_FORMAT_NONE = 0;
	public static final int TIME_FORMAT_SHORT = 1;
	public static final int TIME_FORMAT_ISO = 2;
	private static final int MAX_STORED_LOG_ENTRIES = 1000;

	private Vector<LogItem> allEntries = new Vector<>();

	private Vector<LogItem> currentLevelEntries = new Vector<LogItem>();

	private Handler mHandler;
	private Context mContext;
	private OnItemClickListener itemClickListener;
	private LinearLayoutManager mLinearLayoutManager;

	private Vector<AdapterDataObserver> observers = new Vector<>();

	private int mTimeFormat = -100;
	private int mLogLevel = 3;
	private boolean mLockAutoScroll = false;


	/**
	 * Interfaces
	 */

	public interface OnItemClickListener
	{
		void onItemClick(View view, int position, String logText);
		void onItemLongClick(View view, int position, String logText);
	}

	public class logViewHolder extends RecyclerView.ViewHolder
	{ 
		TextView textLog;

		logViewHolder(View itemView)
		{ 
			super(itemView);

			this.textLog = itemView.findViewById(R.id.textLog);
		} 
	}


	public LogsAdapter(LinearLayoutManager layoutManager,
		Context context)
	{ 
		this.mContext = context;
		this.mLinearLayoutManager = layoutManager;
		
		setLogLevel(VectrasStatus.LogLevel.DEBUG.getInt());
		
		initLogBuffer();
		if (mHandler == null)
		{
			mHandler = new Handler(this);
		}

		VectrasStatus.addLogListener(this);
	} 
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.itemClickListener = listener;
	}

	private void initLogBuffer()
	{
		allEntries.clear();
		Collections.addAll(allEntries, VectrasStatus.getlogbuffer());
		initCurrentMessages();
	}

	private void initCurrentMessages()
	{
		currentLevelEntries.clear();
		for (LogItem li : allEntries)
		{
			if (li.getLogLevel().getInt() <= mLogLevel)
				currentLevelEntries.add(li);
		}
	}

	@Override
	public logViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
	{
		Context context = parent.getContext(); 
		LayoutInflater inflater = LayoutInflater.from(context); 

		View logView = inflater.inflate(R.layout.list_item_log, 
			parent, false);
		logView.setOnTouchListener(this);

		return new logViewHolder(logView); 
	} 

	@Override
	public void onBindViewHolder(final logViewHolder viewHolder, 
		final int position)
	{
		final String text;

		try
		{
			LogItem logItem = currentLevelEntries.get(position);
			String msg = logItem.getString(mContext);
			String time = getTime(logItem, mTimeFormat);
			text = (!time.isEmpty() ? String.format("[%s] ", time) : "") + msg;
			viewHolder.textLog.setText(Html.fromHtml(text));
		}
		catch (Exception e)
		{
			VectrasStatus.logException(e);
			return;
		}

		viewHolder.textLog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				if (itemClickListener != null)
					itemClickListener.onItemClick(v, position, text);
			}
		});

		viewHolder.textLog.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v)
			{
				if (itemClickListener != null)
					itemClickListener.onItemLongClick(v, position, text);
				return true;
			}
		});
	}

	@Override
	public void registerAdapterDataObserver(RecyclerView.AdapterDataObserver observer)
	{
		super.registerAdapterDataObserver(observer);
		observers.add(observer);
	}

	@Override
	public void unregisterAdapterDataObserver(RecyclerView.AdapterDataObserver observer)
	{
		super.unregisterAdapterDataObserver(observer);
		observers.remove(observer);
	}

	@Override
	public int getItemCount() 
	{ 
		return currentLevelEntries.size();
	}

	@Override
	public long getItemId(int position)
	{
		return ((Object) currentLevelEntries.get(position)).hashCode();
	}

	public boolean isEmpty()
	{
		return currentLevelEntries.isEmpty();
	}

	@Override
	public void onAttachedToRecyclerView( 
		RecyclerView recyclerView) 
	{ 
		super.onAttachedToRecyclerView(recyclerView); 
	}

	@Override
	public boolean onTouch(View p1, MotionEvent event)
	{
		// aqui deveria pausar autoscroll
		/*int action = event.getAction();
		
		if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE){
            mLockAutoScroll = true;
			
            return true;
        }
		
		mLockAutoScroll = false;*/
		
		return false;
	}

	private String getTime(LogItem le, int time)
	{
		if (time != TIME_FORMAT_NONE)
		{
			Date d = new Date(le.getLogtime());
			java.text.DateFormat timeformat;
			if (time == TIME_FORMAT_SHORT)
				timeformat = new SimpleDateFormat("HH:mm a");
			else
				timeformat = DateFormat.getTimeFormat(mContext);

			return timeformat.format(d);

		}
		else
		{
			return "";
		}
	}


	/**
	 * Handler implementação
	 */

	@Override
	public boolean handleMessage(Message msg)
	{
		// We have been called
		if (msg.what == MESSAGE_NEWLOG)
		{
			LogItem logMessage = msg.getData().getParcelable("logmessage");
			if (addLogMessage(logMessage))
			{

				for (AdapterDataObserver observer : observers)
				{
					observer.onChanged();
				}

				if (!mLockAutoScroll)
					scrollToLastPosition();
			}
		}
		else if (msg.what == MESSAGE_CLEARLOG)
		{
			for (AdapterDataObserver observer : observers)
			{
				observer.onChanged();
			}
			initLogBuffer();
		}
		else if (msg.what == MESSAGE_NEWTS)
		{
			for (AdapterDataObserver observer : observers)
			{
				observer.onChanged();
			}
		}
		else if (msg.what == MESSAGE_NEWLOGLEVEL)
		{
			initCurrentMessages();

			for (AdapterDataObserver observer : observers)
			{
				observer.onChanged();
			}

		}

		return true;
	}


	/**
	 * @param logmessage
	 * @return True if the current entries have changed
	 */
	private boolean addLogMessage(LogItem logmessage)
	{
		allEntries.add(logmessage);

		if (allEntries.size() > MAX_STORED_LOG_ENTRIES)
		{
			Vector<LogItem> oldAllEntries = allEntries;
			allEntries = new Vector<LogItem>(allEntries.size());
			for (int i = 50; i < oldAllEntries.size(); i++)
			{
				allEntries.add(oldAllEntries.elementAt(i));
			}
			initCurrentMessages();
			return true;
		}
		else
		{
			if (logmessage.getLogLevel().getInt() <= mLogLevel)
			{
				currentLevelEntries.add(logmessage);
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	public LogItem getItem(int position)
	{
		return currentLevelEntries.get(position);
	}

	public void clearLog()
	{
		// Actually is probably called from GUI Thread as result of the user
		// pressing a button. But better safe than sorry
		VectrasStatus.clearLog();
	}

	public void scrollToLastPosition()
	{
		// scroll para ultima mensagem
		mLinearLayoutManager.scrollToPosition(
			mLinearLayoutManager.getItemCount() - 1);
	}
	
	public void setLogLevel(int level) {
		mLogLevel = level;
	}


	/**
	 * LogListener
	 */

	@Override
	public void newLog(LogItem logMessage)
	{
		Message msg = Message.obtain();

		assert (msg != null);
		msg.what = MESSAGE_NEWLOG;

		Bundle bundle = new Bundle();
		bundle.putParcelable("logmessage", logMessage);

		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	@Override
	public void onClear()
	{
		mHandler.sendEmptyMessage(MESSAGE_CLEARLOG);
	}

}
