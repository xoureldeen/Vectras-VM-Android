package com.epicstudios.vectras.Fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.telephony.NetworkScanRequest;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.Blog.AdapterBlog;
import com.epicstudios.vectras.Blog.DataBlog;
import com.epicstudios.vectras.Config;
import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.utils.UIUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {

	View view;

	private RecyclerView mRVBlog;
	private AdapterBlog mAdapter;
	public MainActivity activity;
	public String Data;
	public static LinearLayout noConnectionLayout;
	public SwipeRefreshLayout pullToRefresh;

	/*private ImageButton mStop;
	private ImageButton mRestart;*/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		activity = MainActivity.activity;

		view = inflater.inflate(R.layout.home_fragment, container, false);

		noConnectionLayout = view.findViewById(R.id.noConnectionLayout);
		mRVBlog = view.findViewById(R.id.blogRv);

		if (checkConnection(activity)) {
			new AsyncLogin().execute();
			noConnectionLayout.setVisibility(View.GONE);
			//mRVBlog.setVisibility(View.VISIBLE);
		} else {
			noConnectionLayout.setVisibility(View.VISIBLE);
			//mRVBlog.setVisibility(View.GONE);
		}
		
		pullToRefresh = view.findViewById(R.id.refreshLayout);
		pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (checkConnection(activity)) {
					new AsyncLogin().execute();
				} else {
					noConnectionLayout.setVisibility(View.VISIBLE);
					pullToRefresh.setRefreshing(false);
				}
			}
		});

		/*mStop = (ImageButton) view.findViewById(R.id.stopvm);
		mStop.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		            MainActivity.onStopButton(false);
		
		        }
		    });
		
		mRestart = (ImageButton) view.findViewById(R.id.restartvm);
		mRestart.setOnClickListener(new View.OnClickListener() {
		        public void onClick(View view) {
		
		            MainActivity.onRestartButton();
		
		        }
		    });*/

		return view;
	}

	/**
	* CHECK WHETHER INTERNET CONNECTION IS AVAILABLE OR NOT
	*/
	public boolean checkConnection(Context context) {
		final ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connMgr != null) {
			NetworkInfo activeNetworkInfo = connMgr.getActiveNetworkInfo();

			if (activeNetworkInfo != null) { // connected to the internet
				// connected to the mobile provider's data plan
				if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
					// connected to wifi
					return true;
				} else
					return activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE;
			}
		}
		return false;
	}

	private class AsyncLogin extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			//this method will be running on UI thread

		}

		@Override
		protected String doInBackground(String... params) {
			HttpsURLConnection con = null;
			try {
				URL u = new URL(Config.blogJson);
				con = (HttpsURLConnection) u.openConnection();

				con.connect();

				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();
				Data = sb.toString();

				return (Data);

			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				if (con != null) {
					try {
						con.disconnect();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				return ("unsuccessful!");
			}

		}

		@Override
		protected void onPostExecute(String result) {

			//this method will be running on UI thread
			pullToRefresh.setRefreshing(false);
			
			noConnectionLayout.setVisibility(View.GONE);

			List<DataBlog> data = new ArrayList<>();

			try {

				JSONArray jArray = new JSONArray(Data);

				// Extract data from json and store into ArrayList as class objects
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json_data = jArray.getJSONObject(i);
					DataBlog postData = new DataBlog();
					postData.postThumb = json_data.getString("post_thumb");
					postData.postTitle = json_data.getString("post_title");
					postData.postContent = json_data.getString("post_content");
					postData.postDate = json_data.getString("post_date");
					data.add(postData);
				}

				// Setup and Handover data to recyclerview

			} catch (JSONException e) {
				Toast.makeText(activity, e.toString(), Toast.LENGTH_LONG).show();
			}
			mRVBlog = (RecyclerView) view.findViewById(R.id.blogRv);
			mAdapter = new AdapterBlog(activity, data);
			mRVBlog.setAdapter(mAdapter);
			mRVBlog.setLayoutManager(new LinearLayoutManager(activity));

		}

	}
}
