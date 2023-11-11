package com.epicstudios.vectras;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.epicstudios.vectras.Config;
import com.epicstudios.vectras.MainRoms.AdapterMainRoms;
import com.epicstudios.vectras.MainRoms.DataMainRoms;
import com.epicstudios.vectras.Roms.AdapterRoms;
import com.epicstudios.vectras.Roms.DataRoms;
import com.epicstudios.vectras.logger.VectrasStatus;
import com.epicstudios.vectras.utils.FileInstaller;
import com.epicstudios.vectras.utils.FileUtils;
import java.io.BufferedInputStream;

import com.epicstudios.vectras.utils.UIUtils;
import com.google.android.material.button.MaterialButton;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FirstActivity extends AppCompatActivity {
	public static FirstActivity activity;

	public static MaterialButton goBtn;

	public static CheckBox acceptLiceneseChkBox;
	public static AlertDialog ad;

	public static String license;
	private RecyclerView mRVRoms;
	private AdapterRoms mAdapter;
	public String Data;
	public static Boolean selected = false;
	public static String selectedPath = null;
	public static String selectedExtra = null;
	public static String selectedLink = null;
	public static String selectedName = null;
	public static String selectedIcon = null;
	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		activity = this;
		this.setContentView(R.layout.first_activity);
		mRVRoms = findViewById(R.id.romsRv);

		new FirstActivity.AsyncLogin().execute();
		new Thread(new Runnable() {

			public void run() {

				BufferedReader reader = null;
				final StringBuilder builder = new StringBuilder();

				try {
					// Create a URL for the desired page
					URL url = new URL(Config.vectrasTerms); //My text file location
					//First open the connection
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(60000); // timing out in a minute

					BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

					//t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
					String str;
					while ((str = in.readLine()) != null) {
						builder.append(str);
					}
					in.close();
				} catch (Exception e) {
					acceptLiceneseChkBox.setEnabled(false);
					Toast.makeText(activity, "no internet connection", Toast.LENGTH_LONG).show();
					Log.d("VECTRAS", e.toString());
				}

				//since we are in background thread, to post results we have to go back to ui thread. do the following for that

				activity.runOnUiThread(new Runnable() {
					public void run() {
						license = builder.toString(); // My TextFile has 3 lines
						acceptLiceneseChkBox.setEnabled(true);
					}
				});

			}
		}).start();
		
		acceptLiceneseChkBox = findViewById(R.id.acceptLiceneseChkBox);

		acceptLiceneseChkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					UIAlertLicense("Terms&Conditions", license, activity);
				} else {
					goBtn.setEnabled(false);
				}
			}
		});
		goBtn = (MaterialButton) findViewById(R.id.goBtn);

		goBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				onFirstStartup();
			}
		});

	}
	
	public static void UIAlertLicense(String title, String html, final Activity activity) {
		AlertDialog alertDialog;
		alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
		alertDialog.setTitle(title);
		alertDialog.setCancelable(true);

		alertDialog.setMessage(Html.fromHtml(html));

		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "I Acknowledge", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				acceptLiceneseChkBox.setChecked(true);
				goBtn.setEnabled(true);
				return;
			}
		});
		alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				acceptLiceneseChkBox.setChecked(false);
				goBtn.setEnabled(false);
			}
		});
		alertDialog.show();
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
				URL u = new URL(Config.romsJson);
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
			List<DataRoms> data = new ArrayList<>();

			try {

				JSONArray jArray = new JSONArray(Data);

				// Extract data from json and store into ArrayList as class objects
				for (int i = 0; i < jArray.length(); i++) {
					JSONObject json_data = jArray.getJSONObject(i);
					DataRoms romsData = new DataRoms();
					romsData.itemName = json_data.getString("rom_name");
					romsData.itemIcon = json_data.getString("rom_icon");
					romsData.itemUrl = json_data.getString("rom_url");
					romsData.itemPath = json_data.getString("rom_path");
					romsData.itemAvail = json_data.getBoolean("rom_avail");
					romsData.itemSize = json_data.getString("rom_size");
					romsData.itemArch = json_data.getString("rom_arch");
					romsData.itemExtra = json_data.getString("rom_extra");
					data.add(romsData);
				}

				// Setup and Handover data to recyclerview

			} catch (JSONException e) {
				UIUtils.toastLong(activity,e.toString());
			}
			mRVRoms = (RecyclerView) findViewById(R.id.romsRv);
			mAdapter = new AdapterRoms(activity, data);
			mRVRoms.setAdapter(mAdapter);
			mRVRoms.setLayoutManager(new LinearLayoutManager(activity));

		}

	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	public class RomsJso extends JSONObject {

		public JSONObject makeJSONObject (String imgName, String imgIcon, String imgPath, String imgExtra) {

			JSONObject obj = new JSONObject() ;

			try {
				obj.put("imgName", imgName);
				obj.put("imgIcon", imgIcon);
				obj.put("imgPath", imgPath);
				obj.put("imgExtra", imgExtra);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return obj;
		}
	}
	public static final String CREDENTIAL_SHARED_PREF = "settings_prefs";

	private void startIconDownload() {
		new DownloadsImage().execute(selectedIcon);
	}

	public void onFirstStartup()  {
		if (selected) {
			if (FileUtils.fileValid(activity, Config.maindirpath+selectedPath)) {
				SharedPreferences credentials = activity.getSharedPreferences(CREDENTIAL_SHARED_PREF, Context.MODE_PRIVATE);
				ProgressDialog mProgressDialog = new ProgressDialog(this, R.style.MainDialogTheme);
				mProgressDialog.setMessage("Data Setup");
				mProgressDialog.setMessage("Please Wait...");
				mProgressDialog.setCancelable(false);
				mProgressDialog.show();
				FileInstaller.installFiles(activity);
				SharedPreferences.Editor editor = credentials.edit();
				editor.putBoolean("isFirstLaunch", Boolean.TRUE);
				editor.commit();
				RomsJso obj = new RomsJso();
				startIconDownload();
				final File jsonFile = new File(Config.maindirpath + "roms-data" + ".json");

				if (jsonFile.exists()) {
					try {
						List<DataMainRoms> data=new ArrayList<>();
						JSONArray jArray = new JSONArray(FileUtils.readFromFile(MainActivity.activity, jsonFile));

						try {
							// Extract data from json and store into ArrayList as class objects
							for(int i=0;i<jArray.length();i++){
								JSONObject json_data = jArray.getJSONObject(i);
								DataMainRoms romsMainData = new DataMainRoms();
								romsMainData.itemName= json_data.getString("imgName");
								romsMainData.itemIcon= json_data.getString("imgIcon");
								romsMainData.itemPath= json_data.getString("imgPath");
								romsMainData.itemExtra= json_data.getString("imgExtra");
								data.add(romsMainData);
							}

						} catch (JSONException e) {
							Toast.makeText(MainActivity.activity, e.toString(), Toast.LENGTH_LONG).show();
						}

						JSONObject jsonObject = obj.makeJSONObject(selectedName, Config.maindirpath+"icons/"+selectedPath.replace(".IMG", ".jpg"), Config.maindirpath + selectedPath, selectedExtra);
						jArray.put(jsonObject);
						try {
							Writer output = null;
							output = new BufferedWriter(new FileWriter(jsonFile));
							output.write(jArray.toString().replace("\\","").replace("//","/"));
							output.close();
						} catch (Exception e) {
							UIUtils.toastLong(activity, e.toString());
						}
					} catch (JSONException e) {
						UIUtils.toastLong(activity, e.toString());
					}
				} else {
					JSONObject jsonObject = obj.makeJSONObject(selectedName, Config.maindirpath+"icons/"+selectedPath.replace(".IMG", ".jpg"), Config.maindirpath + selectedPath, selectedExtra);
					JSONArray jsonArray = new JSONArray();
					jsonArray.put(jsonObject);
					try {
						Writer output = null;
						output = new BufferedWriter(new FileWriter(jsonFile));
						output.write(jsonArray.toString().replace("\\","").replace("//","/"));
						output.close();
					} catch (Exception e) {
						UIUtils.toastLong(activity, e.toString());
					}
					VectrasStatus.logInfo(String.format("Welcome to Vectras ♡"));
				}
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						mProgressDialog.dismiss();
						activity.startActivity(new Intent(activity, MainActivity.class));
					}
				}, 3000);
			} else {
				AlertDialog ad;
				ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
				ad.setTitle(selectedPath.replace(".IMG", ".vbi")+" Needs to import");
				ad.setMessage("press import button and select "+selectedPath.replace(".IMG", ".vbi")+" file.");
				ad.setButton(Dialog.BUTTON_POSITIVE, "IMPORT", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent chooseFile = new Intent(ACTION_OPEN_DOCUMENT);
						// Ask specifically for something that can be opened:
						chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
						chooseFile.setType("*/*");
						startActivityForResult(
								Intent.createChooser(chooseFile, "Choose a file"),
								0
						);
					}
				});
				ad.setButton(Dialog.BUTTON_NEGATIVE, "DOWNLAOD "+selectedPath.replace(".IMG", ".vbi"), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String gt = selectedLink;
						Intent g = new Intent(Intent.ACTION_VIEW);
						g.setData(Uri.parse(gt));
						FirstActivity.activity.startActivity(g);
						FirstActivity.activity.finish();
					}
				});
				ad.show();
			}
		} else {
			AlertDialog ad;
			ad = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
			ad.setTitle("Please Select");
			ad.setMessage("Select the os (operating system) you need.");
			ad.setButton(Dialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			ad.show();
		}
	}
	public String getPath(Uri uri) {
		return FileUtils.getPath(activity, uri);
	}
    public ProgressDialog progressDialog =null;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0 && resultCode == RESULT_OK){
			Uri content_describer = data.getData();

			File selectedFilePath = new File(getPath(content_describer));
			if (selectedFilePath.getName().equals(selectedPath.replace(".IMG", ".vbi"))) {
			
                try {
                    unzip(selectedFilePath.toString(), Config.maindirpath);
                } catch (IOException e) {
                    progressDialog.dismiss(); // Close Progress Dialog
                	UIUtils.toastLong(activity, e.toString());
                	throw new RuntimeException(e);
                }
            			
			} else {
				MainActivity.UIAlert("File not supported", "please select "+selectedPath.replace(".IMG", ".vbi")+" file to continue.", activity);
			}

		}
	}
	
    public void unzip(String _zipFile, String _location) throws IOException {
        progressDialog = new ProgressDialog(activity,
                R.style.MainDialogTheme);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show(); // Showing Progress Dialog
        Thread t = new Thread() {
            public void run() {
                File zipFile = new File(_zipFile);
                File targetDirectory = new File(_location);
				ZipInputStream zis = null;
				try {
					zis = new ZipInputStream(
							new BufferedInputStream(new FileInputStream(zipFile)));
				} catch (FileNotFoundException e) {
					UIUtils.toastLong(activity, e.toString());
					throw new RuntimeException(e);
				}
				try {
                    ZipEntry ze;
                    int count;
                    byte[] buffer = new byte[8192];
                    while ((ze = zis.getNextEntry()) != null) {
                        File file = new File(targetDirectory, ze.getName());
                        File dir = ze.isDirectory() ? file : file.getParentFile();
                        if (!dir.isDirectory() && !dir.mkdirs())
                            throw new FileNotFoundException("Failed to ensure directory: " +
                                    dir.getAbsolutePath());
                        if (ze.isDirectory())
                            continue;
                        FileOutputStream fout = new FileOutputStream(file);
                        try {
                            while ((count = zis.read(buffer)) != -1)
                                fout.write(buffer, 0, count);
                        } finally {
                            fout.close();
                        }
                        /* if time should be restored as well
                        long time = ze.getTime();
                        if (time > 0)
                            file.setLastModified(time);
                        */
                    }
                } catch (FileNotFoundException e) {
					UIUtils.toastLong(activity, e.toString());
					throw new RuntimeException(e);
				} catch (IOException e) {
					UIUtils.toastLong(activity, e.toString());
					throw new RuntimeException(e);
				} finally {
					progressDialog.cancel(); // cancelling Dialog.

					try {
						zis.close();
					} catch (IOException e) {
						UIUtils.toastLong(activity, e.toString());
						throw new RuntimeException(e);
					}
				}
            }
        };
        t.start();
    }

	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
	class DownloadsImage extends AsyncTask<String, Void,Void>{

		@Override
		protected Void doInBackground(String... strings) {
			URL url = null;
			try {
				url = new URL(strings[0]);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			Bitmap bm = null;
			try {
				bm =    BitmapFactory.decodeStream(url.openConnection().getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}

			//Create Path to save Image
			File path = new File(Config.maindirpath + "icons"); //Creates app specific folder

			if(!path.exists()) {
				path.mkdirs();
			}

			File imageFile = new File(path, selectedPath.replace(".IMG", ".jpg")); // Imagename.png
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(imageFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			try{
				bm.compress(Bitmap.CompressFormat.PNG, 100, out); // Compress Image
				out.flush();
				out.close();
				// Tell the media scanner about the new file so that it is
				// immediately available to the user.
				MediaScannerConnection.scanFile(activity,new String[] { imageFile.getAbsolutePath() }, null,new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
						// Log.i("ExternalStorage", "Scanned " + path + ":");
						//    Log.i("ExternalStorage", "-> uri=" + uri);
					}
				});
			} catch(Exception e) {
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

}
