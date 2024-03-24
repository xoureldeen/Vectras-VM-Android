/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2012 Iordan Iordanov
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iiordanov.pubkeygenerator;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.morpheusly.common.Utilities;

import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

public class GeneratePubkeyActivity extends Activity implements OnEntropyGatheredListener {
    public final static String TAG = "GeneratePubkeyActivity";

    final static int MIN_BITS_RSA = 768;
    final static int DEFAULT_BITS_RSA = 2048;
    final static int MAX_BITS_RSA = 4096;
    final static int MIN_BITS_DSA = 512;
    final static int DEFAULT_BITS_DSA = 1024;
    final static int MAX_BITS_DSA = 1024;
    private static final int SAVE_KEY_REQUEST = 1;
    private static final int IMPORT_KEY_REQUEST = 2;
    ClipboardManager cm;
    private LayoutInflater inflater = null;
    private RadioGroup keyTypeGroup;
    private SeekBar bitsSlider;
    private EditText bitsText;
    private Button generate;
    private Button importKey;
    private Button share;
    private Button decrypt;
    private Button copy;
    private Button save;
    private Dialog entropyDialog;
    private ProgressDialog progress;
    private EditText password1;
    private String keyType = PubkeyDatabase.KEY_TYPE_RSA;
    private int minBits = MIN_BITS_RSA;
    private int bits = DEFAULT_BITS_RSA;
    private byte[] entropy;
    // Variables we use to receive (from calling activity)
    // and recover all key-pair related information.
    private String passphrase;
    private String sshPrivKey;
    private String sshPubKey;
    private boolean recovered = false;
    final private TextWatcher textChecker = new TextWatcher() {
        public void afterTextChanged(Editable s) {
        }

        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
            checkEntries();
        }
    };
    private KeyPair kp = null;
    private String publicKeySSHFormat;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            progress.setMessage(getResources().getText(R.string.generated));
            progress.dismiss();
            GeneratePubkeyActivity.this.finish();
        }
    };
    final private Runnable mKeyGen = new Runnable() {
        public void run() {
            try {
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                random.setSeed(entropy);

                KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(keyType);
                keyPairGen.initialize(bits, random);

                KeyPair pair = keyPairGen.generateKeyPair();
                convertToBase64AndSendIntent(pair);

            } catch (Exception e) {
                Log.e(TAG, "Could not generate key pair");
                e.printStackTrace();
            }

            handler.sendEmptyMessage(0);
        }

    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.act_generatepubkey);

        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        keyTypeGroup = (RadioGroup) findViewById(R.id.key_type);

        bitsText = (EditText) findViewById(R.id.bits);
        bitsSlider = (SeekBar) findViewById(R.id.bits_slider);

        password1 = (EditText) findViewById(R.id.password);

        generate = (Button) findViewById(R.id.generate);
        share = (Button) findViewById(R.id.share);
        decrypt = (Button) findViewById(R.id.decrypt);
        copy = (Button) findViewById(R.id.copy);
        save = (Button) findViewById(R.id.save);
        importKey = (Button) findViewById(R.id.importKey);

        inflater = LayoutInflater.from(this);

        password1.addTextChangedListener(textChecker);

        // Get the private key and passphrase from calling activity if added.
        sshPrivKey = getIntent().getStringExtra("PrivateKey");
        passphrase = password1.getText().toString();
        if (sshPrivKey != null && sshPrivKey.length() != 0) {
            decryptAndRecoverKey();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.key_not_generated_yet), Toast.LENGTH_LONG).show();
        }

        keyTypeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rsa) {
                    minBits = MIN_BITS_RSA;

                    bitsSlider.setEnabled(true);
                    bitsSlider.setProgress(DEFAULT_BITS_RSA);
                    bitsSlider.setMax(MAX_BITS_RSA - minBits);

                    bitsText.setText(String.valueOf(DEFAULT_BITS_RSA));
                    bitsText.setEnabled(true);

                    keyType = PubkeyDatabase.KEY_TYPE_RSA;
                } else if (checkedId == R.id.dsa) {
                    minBits = MIN_BITS_DSA;

                    bitsSlider.setEnabled(true);
                    bitsSlider.setProgress(DEFAULT_BITS_DSA);
                    bitsSlider.setMax(MAX_BITS_DSA - minBits);

                    bitsText.setText(String.valueOf(DEFAULT_BITS_DSA));
                    bitsText.setEnabled(true);

                    keyType = PubkeyDatabase.KEY_TYPE_DSA;
                }
            }
        });

        bitsSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromTouch) {
                // Stay evenly divisible by 8 because it looks nicer to have
                // 2048 than 2043 bits.

                int leftover = progress % 8;
                int ourProgress = progress;

                if (leftover > 0)
                    ourProgress += 8 - leftover;

                bits = minBits + ourProgress;
                bitsText.setText(String.valueOf(bits));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // We don't care about the start.
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                // We don't care about the stop.
            }
        });

        bitsText.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    try {
                        bits = Integer.parseInt(bitsText.getText().toString());
                        if (bits < minBits) {
                            bits = minBits;
                            bitsText.setText(String.valueOf(bits));
                        }
                    } catch (NumberFormatException nfe) {
                        bits = DEFAULT_BITS_RSA;
                        bitsText.setText(String.valueOf(bits));
                    }

                    bitsSlider.setProgress(bits - minBits);
                }
            }
        });

        generate.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);
                GeneratePubkeyActivity.this.startEntropyGather();
            }
        });

        decrypt.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);
                decryptAndRecoverKey();
            }
        });

        share.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);
                // This is an UGLY HACK for Blackberry devices which do not transmit the "+" character.
                // Remove as soon as the bug is fixed.
                String s = android.os.Build.MODEL;
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_TEXT, publicKeySSHFormat);
                startActivity(Intent.createChooser(share, "Share Pubkey"));
            }
        });

        copy.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);
                cm.setText(publicKeySSHFormat);
                Toast.makeText(getBaseContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            }
        });

        save.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "*/*"
                });
                startActivityForResult(intent, SAVE_KEY_REQUEST);
            }
        });

        importKey.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                hideSoftKeyboard(view);

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "*/*"
                });
                startActivityForResult(intent, IMPORT_KEY_REQUEST);
            }
        });
    }

    /**
     * Hides the soft keyboard.
     */
    public void hideSoftKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Decrypts and recovers the key pair, as well as generating public key in OpenSSH format.
     */
    public boolean decryptAndRecoverKey() {
        boolean success = true;
        passphrase = password1.getText().toString();
        if (!recovered) {
            kp = PubkeyUtils.decryptAndRecoverKeyPair(sshPrivKey, passphrase);
            if (kp == null) {
                success = false;
            } else {
                try {
                    publicKeySSHFormat = PubkeyUtils.convertToOpenSSHFormat(kp.getPublic(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                    success = false;
                }
            }
            if (success)
                recovered = true;
        }

        if (recovered) {
            Toast.makeText(getBaseContext(), getString(R.string.success_decrypting), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.error_decrypting_key), Toast.LENGTH_LONG).show();
        }
        checkEntries();
        return success;
    }

    /**
     * Turns buttons on and off depending on state of pubkey.
     */
    private void checkEntries() {
        if (recovered) {
            share.setEnabled(true);
            copy.setEnabled(true);
            save.setEnabled(true);
            decrypt.setEnabled(false);
        } else {
            share.setEnabled(false);
            copy.setEnabled(false);
            save.setEnabled(false);
            if (sshPrivKey.length() != 0)
                decrypt.setEnabled(true);
        }
    }

    private void startEntropyGather() {
        final View entropyView = inflater.inflate(R.layout.dia_gatherentropy, null, false);
        ((EntropyView) entropyView.findViewById(R.id.entropy)).addOnEntropyGatheredListener(GeneratePubkeyActivity.this);
        entropyDialog = new EntropyDialog(GeneratePubkeyActivity.this, entropyView);
        entropyDialog.show();
    }

    public void onEntropyGathered(byte[] entropy) {
        // For some reason the entropy dialog was aborted, exit activity
        if (entropy == null) {
            finish();
            return;
        }

        this.entropy = entropy.clone();

        int numSetBits = 0;
        for (int i = 0; i < 20; i++)
            numSetBits += measureNumberOfSetBits(this.entropy[i]);

        Log.d(TAG, "Entropy distribution=" + (int) (100.0 * numSetBits / 160.0) + "%");

        Log.d(TAG, "entropy gathered; attemping to generate key...");
        startKeyGen();
    }

    private void startKeyGen() {
        progress = new ProgressDialog(GeneratePubkeyActivity.this);
        progress.setMessage(getResources().getText(R.string.generating));
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        progress.show();

        Thread keyGenThread = new Thread(mKeyGen);
        keyGenThread.setName("KeyGen");
        keyGenThread.start();
    }

    private int measureNumberOfSetBits(byte b) {
        int numSetBits = 0;

        for (int i = 0; i < 8; i++) {
            if ((b & 1) == 1)
                numSetBits++;
            b >>= 1;
        }

        return numSetBits;
    }

    /**
     * Sets the sshPrivKey and sshPubKey private variables from provided KeyPair.
     *
     * @param pair
     * @throws Exception
     */
    private void convertToBase64AndSendIntent(KeyPair pair) throws Exception {
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();

        String secret = password1.getText().toString();
        Log.d(TAG, "private: " + PubkeyUtils.formatKey(priv));
        Log.d(TAG, "public: " + PubkeyUtils.formatKey(pub));
        sshPrivKey = Base64.encodeToString(PubkeyUtils.getEncodedPrivate(priv, secret), Base64.DEFAULT);
        sshPubKey = Base64.encodeToString(PubkeyUtils.getEncodedPublic(pub), Base64.DEFAULT);

        // Send the generated data back to the calling activity.
        Intent databackIntent = new Intent();
        databackIntent.putExtra("PrivateKey", sshPrivKey);
        databackIntent.putExtra("PublicKey", sshPubKey);

        setResult(Activity.RESULT_OK, databackIntent);
    }

    /**
     * This function is used to retrieve data returned by activities started with startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        android.util.Log.i(TAG, "onActivityResult");

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case IMPORT_KEY_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        String keyData = Utilities.Companion.getStringDataFromIntent(data, this);
                        try {
                            passphrase = password1.getText().toString();
                            KeyPair pair = PubkeyUtils.tryImportingPemAndPkcs8(GeneratePubkeyActivity.this, keyData, passphrase);
                            convertToBase64AndSendIntent(pair);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e(TAG, "Failed to decode key.");
                            Toast.makeText(getBaseContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(getBaseContext(), getString(R.string.success_importing), Toast.LENGTH_LONG).show();
                    } else {
                        android.util.Log.e(TAG, "File uri not found, not importing key");
                    }
                } else {
                    android.util.Log.e(TAG, "Error while selecting file to import key from");
                }
                break;
            case SAVE_KEY_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getData() != null) {
                        ContentResolver resolver = getContentResolver();

                        OutputStream out = Utilities.Companion.getOutputStreamFromUri(resolver, data.getData());
                        Utilities.Companion.outputToStream(publicKeySSHFormat, out);
                    } else {
                        android.util.Log.e(TAG, "File uri not found, not exporting pubkey");
                    }
                } else {
                    android.util.Log.e(TAG, "Error while selecting file to export pubkey to");
                }
                break;
        }
    }
}
