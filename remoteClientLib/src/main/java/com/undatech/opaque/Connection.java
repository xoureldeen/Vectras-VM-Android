/**
 * Copyright (C) 2020- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * <p>
 * You should have received a copy of the GNU General License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */
package com.undatech.opaque;

import android.content.ContentValues;
import android.content.Context;
import android.widget.ImageView.ScaleType;

public interface Connection {
    String getLabel();

    String getId();

    String getRuntimeId();

    void setRuntimeId(String id);

    String getNickname();

    void setNickname(String nickname);

    int getConnectionType();

    void setConnectionType(int connectionType);

    String getConnectionTypeString();

    void setConnectionTypeString(String connectionTypeString);

    String getInputMode();

    void setInputMode(String inputMode);

    int getExtraKeysToggleType();

    void setExtraKeysToggleType(int extraKeysToggleType);

    String getHostname();

    void setHostname(String hostname);

    String getVmname();

    void setVmname(String vmname);

    String getUserName();

    void setUserName(String user);

    String getPassword();

    void setPassword(String password);

    boolean getKeepPassword();

    void setKeepPassword(boolean keepPassword);

    String getOtpCode();

    void setOtpCode(String otpCode);

    String getFilename();

    void setFilename(String filename);

    boolean isRotationEnabled();

    void setRotationEnabled(boolean rotationEnabled);

    boolean isRequestingNewDisplayResolution();

    void setRequestingNewDisplayResolution(boolean requestingNewDisplayResolution);

    boolean isAudioPlaybackEnabled();

    void setAudioPlaybackEnabled(boolean audioPlaybackEnabled);

    boolean isUsingCustomOvirtCa();

    void setUsingCustomOvirtCa(boolean useCustomCa);

    boolean isSslStrict();

    void setSslStrict(boolean sslStrict);

    boolean isUsbEnabled();

    void setUsbEnabled(boolean usbEnabled);

    String getOvirtCaFile();

    void setOvirtCaFile(String ovirtCaFile);

    String getOvirtCaData();

    void setOvirtCaData(String ovirtCaData);

    String getLayoutMap();

    void setLayoutMap(String layoutMap);

    void saveAndWriteRecent(boolean saveEmpty, Context c);

    void save(Context context);

    void load(Context context);

    String getAddress();

    void setAddress(String address);

    int getPort();

    void setPort(int port);

    int getTlsPort();

    void setTlsPort(int port);

    ScaleType getScaleMode();

    void setScaleMode(ScaleType value);

    int getRdpResType();

    void setRdpResType(int rdpResType);

    boolean getFollowMouse();

    void setFollowMouse(boolean followMouse);

    boolean getFollowPan();

    void setFollowPan(boolean followPan);

    long getLastMetaKeyId();

    void setLastMetaKeyId(long lastMetaKeyId);

    boolean getUseDpadAsArrows();

    void setUseDpadAsArrows(boolean useDpadAsArrows);

    String getColorModel();

    void setColorModel(String colorModel);

    boolean getRotateDpad();

    void setRotateDpad(boolean rotateDpad);

    String getIdHash();

    void setIdHash(String idHash);

    int getIdHashAlgorithm();

    void setIdHashAlgorithm(int idHashAlgorithm);

    int getPrefEncoding();

    void setPrefEncoding(int prefEncoding);

    boolean getUseRepeater();

    void setUseRepeater(boolean useRepeater);

    String getRepeaterId();

    void setRepeaterId(String repeaterId);

    String saveCaToFile(Context context, String caCertData);

    void populateFromContentValues(ContentValues values);

    boolean isReadyForConnection();

    void setReadyForConnection(boolean readyToBeSaved);

    boolean isReadyToBeSaved();

    void setReadyToBeSaved(boolean readyToBeSaved);

    void determineIfReadyForConnection(int secType);

    String getCaCert();

    void setCaCert(String caCert);

    String getCaCertPath();

    void setCaCertPath(String caCertPath);

    String getCertSubject();

    void setCertSubject(String certSubject);

    String getRdpDomain();

    void setRdpDomain(String rdpDomain);

    int getRdpWidth();

    void setRdpWidth(int rdpWidth);

    int getRdpHeight();

    void setRdpHeight(int rdpHeight);

    int getRdpColor();

    void setRdpColor(int rdpColor);

    boolean getRemoteFx();

    void setRemoteFx(boolean remoteFx);

    boolean getDesktopBackground();

    void setDesktopBackground(boolean desktopBackground);

    boolean getFontSmoothing();

    void setFontSmoothing(boolean fontSmoothing);

    boolean getDesktopComposition();

    void setDesktopComposition(boolean desktopComposition);

    boolean getWindowContents();

    void setWindowContents(boolean windowContents);

    boolean getMenuAnimation();

    void setMenuAnimation(boolean menuAnimation);

    boolean getVisualStyles();

    void setVisualStyles(boolean visualStyles);

    boolean getRedirectSdCard();

    void setRedirectSdCard(boolean redirectSdCard);

    boolean getConsoleMode();

    void setConsoleMode(boolean consoleMode);

    boolean getEnableSound();

    void setEnableSound(boolean enableSound);

    boolean getEnableRecording();

    void setEnableRecording(boolean enableRecording);

    int getRemoteSoundType();

    void setRemoteSoundType(int remoteSoundType);

    boolean getViewOnly();

    void setViewOnly(boolean viewOnly);

    long getForceFull();

    void setForceFull(long forceFull);

    int getUseLocalCursor();

    void setUseLocalCursor(int useLocalCursor);

    String getSshServer();

    void setSshServer(String sshServer);

    int getSshPort();

    void setSshPort(int sshPort);

    String getSshUser();

    void setSshUser(String sshUser);

    String getSshPassword();

    void setSshPassword(String sshPassword);

    boolean getKeepSshPassword();

    void setKeepSshPassword(boolean keepSshPassword);

    String getSshPubKey();

    void setSshPubKey(String sshPubKey);

    String getSshPrivKey();

    void setSshPrivKey(String sshPrivKey);

    String getSshPassPhrase();

    void setSshPassPhrase(String sshPassPhrase);

    boolean getUseSshPubKey();

    void setUseSshPubKey(boolean useSshPubKey);

    int getSshRemoteCommandOS();

    void setSshRemoteCommandOS(int sshRemoteCommandOS);

    int getSshRemoteCommandType();

    void setSshRemoteCommandType(int sshRemoteCommandType);

    int getAutoXType();

    void setAutoXType(int autoXType);

    String getAutoXCommand();

    void setAutoXCommand(String autoXCommand);

    boolean getAutoXEnabled();

    void setAutoXEnabled(boolean autoXEnabled);

    int getAutoXResType();

    void setAutoXResType(int autoXResType);

    int getAutoXWidth();

    void setAutoXWidth(int autoXWidth);

    int getAutoXHeight();

    void setAutoXHeight(int autoXHeight);

    String getAutoXSessionProg();

    void setAutoXSessionProg(String autoXSessionProg);

    int getAutoXSessionType();

    void setAutoXSessionType(int autoXSessionType);

    boolean getAutoXUnixpw();

    void setAutoXUnixpw(boolean autoXUnixpw);

    boolean getAutoXUnixAuth();

    void setAutoXUnixAuth(boolean autoXUnixAuth);

    String getAutoXRandFileNm();

    void setAutoXRandFileNm(String autoXRandFileNm);

    String getSshRemoteCommand();

    void setSshRemoteCommand(String sshRemoteCommand);

    int getSshRemoteCommandTimeout();

    void setSshRemoteCommandTimeout(int sshRemoteCommandTimeout);

    boolean getUseSshRemoteCommand();

    void setUseSshRemoteCommand(boolean useSshRemoteCommand);

    String getSshHostKey();

    void setSshHostKey(String sshHostKey);

    long getMetaListId();

    void setMetaListId(long metaListId);

    public String getScreenshotFilename();

    public void setScreenshotFilename(String screenShotFilename);

    public String getX509KeySignature();

    public void setX509KeySignature(String x509KeySignature);

    boolean getEnableGfx();

    void setEnableGfx(boolean enableGfx);

    boolean getEnableGfxH264();

    void setEnableGfxH264(boolean enableGfxH264);

    boolean getUseLastPositionToolbar();

    void setUseLastPositionToolbar(boolean useLastPositionToolbar);

    int getUseLastPositionToolbarX();

    void setUseLastPositionToolbarX(int useLastPositionToolbarX);

    int getUseLastPositionToolbarY();

    void setUseLastPositionToolbarY(int useLastPositionToolbarY);

    boolean getUseLastPositionToolbarMoved();

    void setUseLastPositionToolbarMoved(boolean useLastPositionToolbarMoved);

    boolean getPreferSendingUnicode();

    void setPreferSendingUnicode(boolean preferSendingUnicode);

    String getExternalId();

    void setExternalId(String gen_externalId);

    boolean getRequiresVpn();

    void setRequiresVpn(boolean gen_requiresVpn);

    String getVpnUriScheme();

    void setVpnUriScheme(String gen_vpnUriScheme);

    boolean getRdpGatewayEnabled();

    void setRdpGatewayEnabled(boolean gen_rdpGatewayEnabled);

    String getRdpGatewayHostname();

    void setRdpGatewayHostname(String rdpGatewayHostname);

    int getRdpGatewayPort();

    void setRdpGatewayPort(int rdpGatewayPort);

    public String getRdpGatewayUsername();

    public void setRdpGatewayUsername(String rdpGatewayUsername);

    public String getRdpGatewayDomain();

    public void setRdpGatewayDomain(String rdpGatewayDomain);

    public String getRdpGatewayPassword();

    public void setRdpGatewayPassword(String rdpGatewayPassword);

    public boolean getKeepRdpGatewayPassword();

    public void setKeepRdpGatewayPassword(boolean keepRdpGatewayPassword);
}
