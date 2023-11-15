package com.epicstudios.vectras.Fragment;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.BaseInputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.epicstudios.vectras.MainActivity;
import com.epicstudios.vectras.R;
import com.epicstudios.vectras.Config;
import com.epicstudios.vectras.VectrasSDLActivity;
import com.epicstudios.vectras.widgets.JoystickView;
import com.epicstudios.vectras.utils.KeyboardUtils;
import com.epicstudios.vectras.utils.UIUtils;
import android.view.View.OnClickListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SDLSurface;

public class ControlsFragment extends Fragment {

	View view;

	private Timer _timer = new Timer();
	private TimerTask t;

	public static final int MAX_LINES = 1;
	public static final String TWO_SPACES = " ";
	public static LinearLayout escBtn, enterBtn, shiftBtn, delBtn, gamepadLayout, desktopLayout, BtnUp, BtnDown,
			BtnRight, BtnLeft, BtnF, BtnShift, Btn0, BtnSpace, BtnSettings, kbdBtn, BtnMode, BtnHide;

	public static TextView txtCpu, txtMem, txtDev, TxtHide;

	public static LinearLayout F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12;

	public static LinearLayout rightClick, leftClick;

	ProcessBuilder processBuilder;
	String Holder = "";
	String[] DATA = {"/system/bin/cat", "/proc/cpuinfo"};
	InputStream inputStream;
	Process process ;
	byte[] byteArry ;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		view = inflater.inflate(R.layout.controls_fragment, container, false);
		gamepadLayout = view.findViewById(R.id.gamepadLayout);
		desktopLayout = view.findViewById(R.id.desktopLayout);
		BtnUp = view.findViewById(R.id.upBtn);
		BtnDown = view.findViewById(R.id.downBtn);
		BtnRight = view.findViewById(R.id.rightBtn);
		BtnLeft = view.findViewById(R.id.leftBtn);
		BtnF = view.findViewById(R.id.BtnF);
		BtnShift = view.findViewById(R.id.BtnShift);
		BtnSpace = view.findViewById(R.id.BtnSpace);
		Btn0 = view.findViewById(R.id.Btn0);
		escBtn = view.findViewById(R.id.escBtn);
		enterBtn = view.findViewById(R.id.enterBtn);
		shiftBtn = view.findViewById(R.id.shiftBtn);
		delBtn = view.findViewById(R.id.delBtn);
		BtnSettings = view.findViewById(R.id.settingsBtn);
		BtnMode = view.findViewById(R.id.modeBtn);
		BtnHide = view.findViewById(R.id.hideBtn);
		TxtHide = view.findViewById(R.id.hideTxt);
		kbdBtn = view.findViewById(R.id.kbdBtn);
		txtCpu = view.findViewById(R.id.cpuTxt);
		txtMem = view.findViewById(R.id.ramTxt);
		txtDev = view.findViewById(R.id.dvcTxt);

		F1 = view.findViewById(R.id.F1);
		F2 = view.findViewById(R.id.F2);
		F3 = view.findViewById(R.id.F3);
		F4 = view.findViewById(R.id.F4);
		F5 = view.findViewById(R.id.F5);
		F6 = view.findViewById(R.id.F6);
		F7 = view.findViewById(R.id.F7);
		F8 = view.findViewById(R.id.F8);
		F9 = view.findViewById(R.id.F9);
		F10 = view.findViewById(R.id.F10);
		F11 = view.findViewById(R.id.F11);
		F12 = view.findViewById(R.id.F12);

		rightClick = view.findViewById(R.id.rightClick);
		leftClick = view.findViewById(R.id.leftClick);

		txtDev.setText("Device Model: " + android.os.Build.MODEL);

		t = new TimerTask() {
			@Override
			public void run() {
				VectrasSDLActivity.activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//update
						ActivityManager.MemoryInfo miI = new ActivityManager.MemoryInfo();
						ActivityManager activityManagerr = (ActivityManager) VectrasSDLActivity.activity.getSystemService(VectrasSDLActivity.activity.ACTIVITY_SERVICE);
						activityManagerr.getMemoryInfo(miI);
						//update textview here
						long freeMemory = miI.availMem / 1048576L;
						long totalMemory = miI.totalMem / 1048576L;
						long usedMemory = totalMemory - freeMemory;

						txtMem.setText("Free Memory: " + freeMemory + " MB");
					}
				});
			}
		};
		_timer.scheduleAtFixedRate(t, (int) (0), (int) (1000));

		//txtMem.setVisibility(View.GONE);
		//txtCpu.setVisibility(View.GONE);
		byteArry = new byte[1024];

		try{
			processBuilder = new ProcessBuilder(DATA);

			process = processBuilder.start();

			inputStream = process.getInputStream();

			while(inputStream.read(byteArry) != -1){

				Holder = Holder + new String(byteArry);
			}

			inputStream.close();

		} catch(IOException ex){

			ex.printStackTrace();
		}

		String cpuTxt = "Cpu Info:\n" + Holder;

		txtCpu.setText(cpuTxt);
		txtCpu.post(() -> {
			// Past the maximum number of lines we want to display.
			if (txtCpu.getLineCount() > MAX_LINES) {
				int lastCharShown = txtCpu.getLayout().getLineVisibleEnd(MAX_LINES - 1);

				txtCpu.setMaxLines(MAX_LINES);

				String moreString = "Show More";
				String suffix = TWO_SPACES + moreString;

				// 3 is a "magic number" but it's just basically the length of the ellipsis we're going to insert
				String actionDisplayText = cpuTxt.substring(0, lastCharShown - suffix.length() - 3) + "..." + suffix;

				SpannableString truncatedSpannableString = new SpannableString(actionDisplayText);
				int startIndex = actionDisplayText.indexOf(moreString);
				truncatedSpannableString.setSpan(new ForegroundColorSpan(Color.GRAY), startIndex, startIndex + moreString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				txtCpu.setText(truncatedSpannableString);
			}
		});

		txtCpu.setVisibility(View.GONE);

		if (gamepadLayout.getVisibility() != View.VISIBLE) {
			gamepadLayout.setVisibility(View.VISIBLE);
			desktopLayout.setVisibility(View.GONE);
		}

		BtnUp.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {

					SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP);

			}
		});

		BtnDown.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN);
			}
		});

		BtnLeft.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_LEFT);
			}
		});

		BtnRight.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
			}
		});

		BtnF.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F);
			}
		});

		BtnShift.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_RIGHT);
			}
		});

		Btn0.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_NUMPAD_0);
			}
		});

		BtnSpace.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SPACE);
			}
		});
		int loop =25;
		JoystickView joystick = (JoystickView) view.findViewById(R.id.joyStick);
		joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
			@Override
			public void onMove(int angle, int strength) {
				// do whatever you want
				if (angle > 0) {
					if (angle < 30) {
						SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
					} else if (angle > 30) {
							if (angle < 60) {
								SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP_RIGHT);
							} else if (angle > 60) {
								if (angle < 120) {
									SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP);
								} else if (angle > 120) {
									if (angle < 150) {
										SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_UP_LEFT);
									} else if (angle > 150) {
										if (angle < 210) {
											SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_LEFT);
										} else if (angle > 210) {
											if (angle < 240) {
												SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN_LEFT);
											} else if (angle > 240) {
												if (angle < 300) {
													SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN);
												} else if (angle > 300) {
													if (angle < 330) {
														SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT);
													} else if (angle > 330) {
														SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT);
													}
												}
											}
										}
									}
								}
							}
						}
					}
			}
		}, loop);
		BtnMode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				if (gamepadLayout.getVisibility() != View.VISIBLE) {
					Animation animation2;
					animation2 = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_to_left);
					animation2.setDuration(300);
					desktopLayout.startAnimation(animation2);
					desktopLayout.setVisibility(View.GONE);
					Animation animation;
					animation = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_from_left);
					animation.setDuration(300);
					gamepadLayout.startAnimation(animation);
					gamepadLayout.setVisibility(View.VISIBLE);
				} else {
					Animation animation;
					animation = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_to_left);
					animation.setDuration(300);
					gamepadLayout.startAnimation(animation);
					gamepadLayout.setVisibility(View.GONE);
					Animation animation2;
					animation2 = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_from_left);
					animation2.setDuration(300);
					desktopLayout.startAnimation(animation2);
					desktopLayout.setVisibility(View.VISIBLE);
				}
			}
		});
		BtnHide.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				if (gamepadLayout.getVisibility() == View.GONE && desktopLayout.getVisibility() == View.GONE) {
					Animation animation;
					animation = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_from_left);
					animation.setDuration(300);
					desktopLayout.startAnimation(animation);
					gamepadLayout.startAnimation(animation);
					gamepadLayout.setVisibility(View.VISIBLE);
					desktopLayout.setVisibility(View.GONE);
					TxtHide.setText("Hide");
				} else {
					Animation animation;
					animation = AnimationUtils.loadAnimation(VectrasSDLActivity.activity, R.anim.slide_to_left);
					animation.setDuration(300);
					desktopLayout.startAnimation(animation);
					gamepadLayout.startAnimation(animation);
					gamepadLayout.setVisibility(View.GONE);
					desktopLayout.setVisibility(View.GONE);
					TxtHide.setText("Show");
				}
			}
		});
		BtnSettings.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {

			}
		});

		kbdBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				InputMethodManager imm = (InputMethodManager)   VectrasSDLActivity.activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
				//VectrasSDLActivity.onKeyboard(SDLActivity.activity);
			}
		});

		//desktop layout
		escBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ESCAPE);
			}
		});

		enterBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_ENTER);
			}
		});

		shiftBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_SHIFT_RIGHT);
			}
		});

		delBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_DEL);
			}
		});

		F1.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F1);
			}
		});

		F2.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F2);
			}
		});

		F3.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F3);
			}
		});

		F4.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F4);
			}
		});

		F5.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F5);
			}
		});

		F6.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F6);
			}
		});

		F7.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F7);
			}
		});

		F8.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F8);
			}
		});

		F9.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F9);
			}
		});

		F10.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F10);
			}
		});

		F11.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F11);
			}
		});

		F12.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				SDLActivity.onNativeKeyDown(KeyEvent.KEYCODE_F12);
			}
		});

		rightClick.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
						InputDevice.SOURCE_TOUCHSCREEN, 0);
				SDLSurface.rightClick(e);
			}
		});

		leftClick.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				MotionEvent e = MotionEvent.obtain(1000, 1000, MotionEvent.ACTION_DOWN, 0, 0, 0, 0, 0, 0, 0,
						InputDevice.SOURCE_TOUCHSCREEN, 0);
				SDLSurface.leftClick(e);
			}
		});
		return view;
	}
}
