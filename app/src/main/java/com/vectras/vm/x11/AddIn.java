package com.vectras.vm.x11;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.KeyEvent.KEYCODE_ALT_LEFT;
import static android.view.KeyEvent.KEYCODE_CTRL_LEFT;
import static android.view.KeyEvent.KEYCODE_DEL;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;
import static android.view.KeyEvent.KEYCODE_E;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_ESCAPE;
import static android.view.KeyEvent.KEYCODE_Q;
import static android.view.KeyEvent.KEYCODE_R;
import static android.view.KeyEvent.KEYCODE_SPACE;
import static android.view.KeyEvent.KEYCODE_TAB;
import static android.view.KeyEvent.KEYCODE_X;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import androidx.fragment.app.FragmentTransaction;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.Fragment.ControlersOptionsFragment;
import com.vectras.vm.Fragment.LoggerDialogFragment;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivityX11Binding;
import com.vectras.vm.databinding.ControlsFragmentBinding;
import com.vectras.vm.databinding.DesktopControlsBinding;
import com.vectras.vm.databinding.GameControlsBinding;
import com.vectras.vm.main.core.MainStartVM;
import com.vectras.vm.manager.QmpSender;
import com.vectras.vm.manager.VmAudioManager;
import com.vectras.vm.manager.VmControllerDialog;
import com.vectras.vm.manager.VmFileManager;
import com.vectras.vm.manager.VmListManager;
import com.vectras.vm.manager.VmPicker;
import com.vectras.vm.sound.StreamAudio;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.UIUtils;
import com.vectras.vm.view.DynamicBubble;
import com.vectras.vm.x11.input.InputStub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddIn {
    X11Activity activity;

    ActivityX11Binding binding;
    ControlsFragmentBinding bindingControls;
    DesktopControlsBinding bindingDesktopControls;
    GameControlsBinding bindingGameControls;

    public StreamAudio streamAudio;

    public AddIn (X11Activity activity) {
        this.activity = activity;
    }

    public void initialize(ActivityX11Binding binding) {
        this.binding = binding;
        bindingControls = binding.controlsfragment;
        bindingDesktopControls = binding.controlsfragment.desktopcontrols;
        bindingGameControls = binding.controlsfragment.gamecontrols;

        UIUtils.fullScreen(activity);

        initializeControlMode();
        initializeMenuBar();
        initializeDesktopControl();
        initializeGameControl();
        initializeBubble();
        startVM();
    }

    public void startVM() {
        MainStartVM.startPending(activity, new MainStartVM.MainStartVMCallback() {
                    @Override
                    public void onStarted(int statusCode, String message) {
                        MainStartVM.dismissDialog();
                        setupSound();
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        MainStartVM.dismissDialog();
                        if (errorCode == MainStartVM.PENDDING_EMPTY) setupSound();
                    }
                }
        );
    }

    public void initializeControlMode() {
        if (Objects.equals(MainSettingsManager.getControlMode(activity), "D")) {
            bindingControls.desktop.setVisibility(View.VISIBLE);
            bindingControls.gamepad.setVisibility(View.GONE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "G")) {
            bindingControls.desktop.setVisibility(View.GONE);
            bindingControls.gamepad.setVisibility(View.VISIBLE);
        } else if (Objects.equals(MainSettingsManager.getControlMode(activity), "H")) {
            bindingControls.desktop.setVisibility(View.GONE);
            bindingControls.gamepad.setVisibility(View.GONE);
        }
    }

    public void initializeMenuBar() {
        bindingControls.btnFit.setVisibility(View.GONE);
//        final boolean[] isFullScreen = {false};
//        bindingControls.btnFit.setOnClickListener(view -> {
//            sendKey(KEYCODE_CTRL_LEFT, false);
//            sendKey(KEYCODE_ALT_LEFT, false);
//            sendKey(KEYCODE_F, false);
//            sendKey(KEYCODE_CTRL_LEFT, true);
//            sendKey(KEYCODE_ALT_LEFT, true);
//            sendKey(KEYCODE_F, true);
//
//            if (isFullScreen[0]) {
//                bindingControls.btnFit.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.close_fullscreen_24px));
//                isFullScreen[0] = false;
//            } else {
//                bindingControls.btnFit.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.open_in_full_24px));
//                isFullScreen[0] = true;
//            }
//        });

        bindingControls.btnVterm.setOnClickListener(v -> {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            // Create and show the dialog.
            LoggerDialogFragment newFragment = new LoggerDialogFragment();
            newFragment.forceLightText = true;
            newFragment.show(ft, "Logger");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.getSupportFragmentManager().executePendingTransactions();
                if (newFragment.getDialog() == null) return;
                blurLayout();
                newFragment.getDialog().setOnDismissListener(d -> unBlurLayout());
            }
        });

        bindingControls.shutdownBtn.setOnClickListener(v -> activity.finish());

        bindingControls.kbdBtn.setOnClickListener(v -> new Handler(Looper.getMainLooper()).postDelayed(() -> activity.toggleKeyboardVisibility(activity), 200));
        bindingControls.btnMode.setOnClickListener(v -> {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            // Create and show the dialog.
            ControlersOptionsFragment newFragment = new ControlersOptionsFragment();
            newFragment.binding = binding.controlsfragment;
            newFragment.show(ft, "Controllers");
            newFragment.setOnDismiss = () -> binding.lnBubbleContainer.setVisibility(bindingControls.mainControl.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        });


        bindingControls.btnSettings
                .setOnClickListener((l) -> activity.startActivity(new Intent(activity, LoriePreferences.class) {{ setAction(Intent.ACTION_MAIN); }}));

        bindingControls.btnVmManager.setOnClickListener(v -> vmController());

        bindingControls.btnQmp.setVisibility(View.GONE);
//        bindingControls.btnQmp.setOnClickListener(v -> {
//                if (monitorMode) {
//                    onVNC();
//                    qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_terminal_24));
//                } else {
//                    onMonitor();
//                    qmpBtn.setImageDrawable(getResources().getDrawable(R.drawable.round_computer_24));
//                }
//        });

        //        bindingControls.btnPrograms.setOnClickListener(v -> {
//            Dialog dialog = new Dialog(this);
//            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//            dialog.setContentView(R.layout.dialog_programs);
//            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
//
//            WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
//            layoutParams.alpha = 1f;
//            dialog.getWindow().setAttributes(layoutParams);
//
//            ImageButton termBtn = dialog.findViewById(R.id.btnTerminal);
//            ImageButton vkCubeBtn = dialog.findViewById(R.id.btnVkCube);
//            ImageButton glxGearsBtn = dialog.findViewById(R.id.btnGlxGears);
//
//            termBtn.setOnClickListener(v1 -> {
//                new Terminal(this).executeShellCommand2("xfce4-terminal", false, this);
//                dialog.dismiss();
//            });
//
//            glxGearsBtn.setOnClickListener(v1 -> {
//                new Terminal(this).executeShellCommand2("glxgears", false, this);
//                dialog.dismiss();
//            });
//
//            vkCubeBtn.setOnClickListener(v1 -> {
//                new Terminal(this).executeShellCommand2("vkcube", false, this);
//                dialog.dismiss();
//            });
//
//            try {
//                dialog.show();
//            } catch (WindowManager.BadTokenException e) {
//                Log.e(TAG, "Failed to show dialog", e);
//            }
//        });
    }

    public boolean ctrlClicked = false;
    public boolean altClicked = false;

    public void initializeDesktopControl() {
        bindingDesktopControls.upBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_UP, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_UP, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingDesktopControls.leftBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_LEFT, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_LEFT, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingDesktopControls.downBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_DOWN, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_DOWN, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingDesktopControls.rightBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_RIGHT, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_RIGHT, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });

        bindingDesktopControls.escBtn.setOnClickListener(v -> keyDownUp(KEYCODE_ESCAPE));

        bindingDesktopControls.enterBtn.setOnClickListener(v -> keyDownUp(KEYCODE_ENTER));

        bindingDesktopControls.ctrlBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public void onClick(View v) {
                if (!ctrlClicked) {
                    sendKey(KEYCODE_CTRL_LEFT, false);
                    bindingDesktopControls.ctrlBtn.setBackground(activity.getResources().getDrawable(R.drawable.controls_button2));
                    ctrlClicked = true;
                } else {
                    sendKey(KEYCODE_CTRL_LEFT, true);
                    bindingDesktopControls.ctrlBtn.setBackground(activity.getResources().getDrawable(R.drawable.controls_button1));
                    ctrlClicked = false;
                }
            }
        });
        bindingDesktopControls.altBtn.setOnClickListener(v -> {
            if (!altClicked) {
                sendKey(KEYCODE_ALT_LEFT, false);
                bindingDesktopControls.altBtn.setBackground(activity.getResources().getDrawable(R.drawable.controls_button2));
                altClicked = true;
            } else {
                sendKey(KEYCODE_ALT_LEFT, true);
                bindingDesktopControls.altBtn.setBackground(activity.getResources().getDrawable(R.drawable.controls_button1));
                altClicked = false;
            }
        });

        bindingDesktopControls.delBtn.setOnClickListener(v -> keyDownUp(KEYCODE_DEL));

        if (SDK_INT >= Build.VERSION_CODES.N) {
            Map.of(
                            bindingDesktopControls.leftClickBtn,
                            InputStub.BUTTON_LEFT,
                            bindingDesktopControls.middleBtn,
                            InputStub.BUTTON_MIDDLE,
                            bindingDesktopControls.rightClickBtn,
                            InputStub.BUTTON_RIGHT)
                    .forEach(
                            (v, b) ->
                                    v.setOnTouchListener(
                                            (__, e) -> {
                                                switch (e.getAction()) {
                                                    case MotionEvent.ACTION_DOWN:
                                                    case MotionEvent.ACTION_POINTER_DOWN:
                                                        activity.getLorieView()
                                                                .sendMouseEvent(0, 0, b, true, true);
                                                        v.setPressed(true);
                                                        break;
                                                    case MotionEvent.ACTION_UP:
                                                    case MotionEvent.ACTION_POINTER_UP:
                                                        activity.getLorieView()
                                                                .sendMouseEvent(0, 0, b, false, true);
                                                        v.setPressed(false);
                                                        break;
                                                }
                                                return true;
                                            }));
        } else {
            bindingDesktopControls.leftClickBtn.setVisibility(View.GONE);
            bindingDesktopControls.middleBtn.setVisibility(View.GONE);
            bindingDesktopControls.rightClickBtn.setVisibility(View.GONE);
        }

        bindingDesktopControls.winBtn.setOnClickListener(v -> {
            sendKey(KEYCODE_CTRL_LEFT, false);
            sendKey(KEYCODE_ESCAPE, false);
            sendKey(KEYCODE_CTRL_LEFT, false);
            sendKey(KEYCODE_ESCAPE, false);
        });

        bindingDesktopControls.ctrlaltdelBtn.setVisibility(View.GONE);
    }

    public void initializeGameControl() {
        bindingGameControls.upGameBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_UP, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_UP, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingGameControls.leftGameBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_LEFT, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_LEFT, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingGameControls.downGameBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_DOWN, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_DOWN, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });
        bindingGameControls.rightGameBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendKey(KEYCODE_DPAD_RIGHT, false);
                v.animate().scaleXBy(-0.2f).setDuration(200).start();
                v.animate().scaleYBy(-0.2f).setDuration(200).start();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendKey(KEYCODE_DPAD_RIGHT, true);
                v.animate().cancel();
                v.animate().scaleX(1f).setDuration(200).start();
                v.animate().scaleY(1f).setDuration(200).start();
                return true;
            }
            return false;
        });

        bindingGameControls.joyStick.setVisibility(View.GONE);
        bindingDesktopControls.tabBtn.setOnClickListener(v -> keyDownUp(KEYCODE_TAB));
        bindingGameControls.tabGameBtn.setOnClickListener(v -> keyDownUp(KEYCODE_TAB));
        bindingGameControls.enterGameBtn.setOnClickListener(v -> keyDownUp(KEYCODE_ENTER));
        bindingGameControls.eBtn.setOnClickListener(v -> keyDownUp(KEYCODE_E));
        bindingGameControls.rBtn.setOnClickListener(v -> keyDownUp(KEYCODE_R));
        bindingGameControls.qBtn.setOnClickListener(v -> keyDownUp(KEYCODE_Q));
        bindingGameControls.xBtn.setOnClickListener(v -> keyDownUp(KEYCODE_X));
        bindingGameControls.ctrlGameBtn.setOnClickListener(v -> keyDownUp(KEYCODE_CTRL_LEFT));
        bindingGameControls.spaceBtn.setOnClickListener(v -> keyDownUp(KEYCODE_SPACE));
    }

    public void initializeBubble() {
        DynamicBubble dynamicBubble = new DynamicBubble(binding.lnBubbleContainer, binding.btnBubble);
        dynamicBubble.onClicked(() -> {
            bindingControls.mainControl.setVisibility(View.VISIBLE);
            binding.lnBubbleContainer.setVisibility(View.GONE);
        });
    }

    public void sendKey(int keyEventCode, boolean up) {
        if (up)
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
        else activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
    }

    public void keyDownUp(int keyEventCode) {
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void vmController() {
        ArrayList<HashMap<String, Object>> list = VmListManager.getAllVmForPickRunningNoVncSocketOnly(activity);

        if (list.isEmpty()) {
            DialogUtils.oopsDialog(activity, activity.getString(R.string.no_vms_are_available));
        } else if (list.size() == 1) {
            Config.vmID = Objects.requireNonNull(list.get(0).get("value")).toString();

            VmControllerDialog vmControllerDialog = new VmControllerDialog();
            vmControllerDialog.streamAudio = streamAudio;
            vmControllerDialog.show(activity.getSupportFragmentManager(), "VmControllerDialog");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.getSupportFragmentManager().executePendingTransactions();
                blurLayout();
                vmControllerDialog.setOnDismissCallback(this::unBlurLayout);
            }
        } else {
            VmPicker vmPicker = new VmPicker(activity);
            vmPicker.currentVmId = "";
            vmPicker.listVm = list;
            vmPicker.pick((position, name, value) -> {
                if (position < 0) {
                    DialogUtils.oopsDialog(activity, activity.getString(R.string.no_vms_are_available));
                    return;
                }

                Config.vmID = value;

                VmControllerDialog vmControllerDialog = new VmControllerDialog();
                vmControllerDialog.streamAudio = streamAudio;
                vmControllerDialog.show(activity.getSupportFragmentManager(), "VmControllerDialog");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.getSupportFragmentManager().executePendingTransactions();
                    blurLayout();
                    vmControllerDialog.setOnDismissCallback(this::unBlurLayout);
                }
            });
        }
    }

    private void setupSound() {
        if (streamAudio == null) {
            streamAudio = new StreamAudio(activity);
            streamAudio.setFile(VmFileManager.findAudioRaw(activity, Config.vmID));

            if (VmAudioManager.currentVmId.equals(Config.vmID) && VmAudioManager.streamAudio.isPlaying())
                streamAudio.setCross(VmAudioManager.streamAudio);

            playSound();
        }
    }

    int playSoundRequests;

    private void playSound() {
        if (streamAudio == null || streamAudio.isPlaying() || playSoundRequests > 0) return;
        playSoundRequests++;

        streamAudio.stop();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!streamAudio.isPlaying()) streamAudio.play();
            playSoundRequests--;
        }, 100);
    }

    public void shutdownthisvm() {
        QmpSender.quickShutdown();
        Config.setDefault();
        activity.finish();
    }

    boolean isBlurring;

    private void blurLayout() {
        if (isBlurring || !MainSettingsManager.getBlurEffect(activity)) return;
        isBlurring = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect blurEffect = RenderEffect.createBlurEffect(
                    25f, 25f,
                    Shader.TileMode.CLAMP
            );
            binding.main.setRenderEffect(blurEffect);
            binding.lorieView.animate().alpha(0.1f).setDuration(500);
        }
    }

    private void unBlurLayout() {
        if (!isBlurring) return;
        isBlurring = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.main.setRenderEffect(null);
            binding.lorieView.animate().alpha(1f).setDuration(500);
        }
    }

    public void handleOnDestroy() {
        if (streamAudio != null) streamAudio.release();
    }

    public void handleOnBack() {
        if (bindingControls.mainControl.getVisibility() == View.GONE) {
            bindingControls.mainControl.setVisibility(View.VISIBLE);

        } else if (streamAudio != null) {
            if (!VmAudioManager.currentVmId.equals(Config.vmID))
                streamAudio.setCross(null);
            if (streamAudio.isPlaying()) streamAudio.stop();
        } else {
            activity.finish();
        }
    }
}
