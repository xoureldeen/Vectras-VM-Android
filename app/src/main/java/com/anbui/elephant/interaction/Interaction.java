package com.anbui.elephant.interaction;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.anbui.elephant.log.LogPrinter;
import com.anbui.elephant.retrofit2utils.Retrofit2Utils;
import com.google.gson.Gson;
import com.vectras.vm.utils.JSONUtils;

import java.util.HashSet;
import java.util.Set;

public class Interaction {
    private final String TAG = "com.anbui.elephant.interaction.Interaction";
    private final String EGG_URL = "https://anbui.ovh/egg/";
    private String GET_URL = EGG_URL + "contentinfo?id=%s&app=vectrasvm";
    private final String VIEW_URL = EGG_URL + "updateview?app=vectrasvm";
    private final String LIKE_URL = EGG_URL + "updatelike?app=vectrasvm";

    private final String contentId;
    private DataInteraction dataInteraction;
    private final SharedPreferences sharedPreferences;

    public interface InteractionCallback {
        void onResult(boolean isSuccess, int views, int likes);
    }

    public Interaction(Context context, String contentId) {
        this.contentId = contentId;
        GET_URL = String.format(GET_URL, contentId);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        convertOldLocalDataToNewFormat("views");
        convertOldLocalDataToNewFormat("likes");
    }

    public void initialize(InteractionCallback callback) {
        if (!isReady()) {
            callback.onResult(false, 1, 0);
            LogPrinter.print(TAG, "Not ready in initialize.");
            return;
        }

        get(((isSuccess, views, likes) -> view(callback)));

        LogPrinter.print(TAG, "Initialized.");
    }

    public void get(InteractionCallback callback) {
        if (!isReady()) {
            callback.onResult(false, 1, 0);
            LogPrinter.print(TAG, "Not ready in get.");
            return;
        }

        Retrofit2Utils.get(GET_URL, ((isSuccess, body, status, error) -> {
            if (isSuccess && JSONUtils.isValidFromString(body)) {
                dataInteraction = new Gson().fromJson(body, DataInteraction.class);
                callback.onResult(true, dataInteraction.views, dataInteraction.likes);
                LogPrinter.print(TAG, "Get succeed.");
            } else {
                callback.onResult(false, 1, 0);
                LogPrinter.print(TAG, "Get unsucceed.");
            }
        }));
    }

    private boolean isTryingView;

    public void view(InteractionCallback callback) {
        if (isViewed()) {
            callback.onResult(true, dataInteraction != null ? dataInteraction.views : 1, dataInteraction != null ? dataInteraction.likes : 0);
            LogPrinter.print(TAG, "Viewed.");
            return;
        }

        if (!isReadyToPost() && !isTryingView) {
            isTryingView = true;
            get((success, views, likes) -> view(callback));
            LogPrinter.print(TAG, "Not ready to post in view.");
            return;
        } else {
            isTryingView = false;
            if (!isReadyToPost()) {
                callback.onResult(false, 1, 0);
                return;
            }
        }

        String jsonRaw = "{"
                + "\"id\":\"" + contentId + "\","
                + "\"token\":" + "\"" + dataInteraction.token + "\""
                + "}";

        Retrofit2Utils.post(VIEW_URL, jsonRaw, ((isSuccess, body, status, error) -> {
            if (isNeedRetry(status) && !isTryingView) {
                isTryingView = true;
                get((success, views, likes) -> view(callback));
                LogPrinter.print(TAG, "Trying again in view.");
                return;
            } else {
                isTryingView = false;
            }

            if (isSuccess && JSONUtils.isValidFromString(body)) {
                DataInteraction data = new Gson().fromJson(body, DataInteraction.class);
                dataInteraction.views = data.count;
                callback.onResult(true, data.count, getLikeCount());
                LogPrinter.print(TAG, "View succeed.");
                setViews();
            } else {
                callback.onResult(false, 1, 0);
                LogPrinter.print(TAG, "View unsucceed.");
            }
        }));
    }

    private boolean isTryingLike;

    public void like(InteractionCallback callback) {
        if (!isReadyToPost() && !isTryingLike) {
            isTryingLike = true;
            get((success, views, likes) -> like(callback));
            LogPrinter.print(TAG, "Not ready to post in like.");
            return;
        } else {
            isTryingLike = false;
            if (!isReadyToPost()) {
                callback.onResult(false, 1, 0);
                return;
            }
        }

        String jsonRaw = "{"
                + "\"id\":\"" + contentId + "\","
                + "\"addcount\":" + (isLiked() ? "-1" : "1") + ","
                + "\"token\":" + "\"" + dataInteraction.token + "\""
                + "}";

        Retrofit2Utils.post(LIKE_URL, jsonRaw, ((isSuccess, body, status, error) -> {
            if (isNeedRetry(status) && !isTryingLike) {
                isTryingLike = true;
                get((success, views, likes) -> like(callback));
                LogPrinter.print(TAG, "Trying again in like.");
                return;
            } else {
                isTryingLike = false;
            }

            if (isSuccess && JSONUtils.isValidFromString(body)) {
                DataInteraction data = new Gson().fromJson(body, DataInteraction.class);
                dataInteraction.likes = data.count;
                callback.onResult(true, getViewCount(), data.count);
                setLikes();
                LogPrinter.print(TAG, "Like succeed.");
            } else {
                callback.onResult(false, 1, 0);
                LogPrinter.print(TAG, "Like unsucceed.");
            }
        }));
    }

    public String getFomatedViewCount() {
        return InteractionUtils.formatCount(getViewCount());
    }

    public String getFormatedLikeCount() {
        return InteractionUtils.formatCount(getLikeCount());
    }

    public int getViewCount() {
        return dataInteraction.views;
    }

    public int getLikeCount() {
        return dataInteraction.likes;
    }

    private boolean isReady() {
        return contentId != null && !contentId.isEmpty();
    }

    private boolean isReadyToPost() {
        return dataInteraction != null
                && dataInteraction.token != null && !dataInteraction.token.isEmpty()
                && contentId != null && !contentId.isEmpty();
    }

    private boolean isNeedRetry(int statusCode) {
        return statusCode == 403;
    }

    public boolean isLiked() {
        return getLikes().contains(contentId);
    }

    private boolean isViewed() {
        return getViews().contains(contentId);
    }

    public void setLikes() {
        Set<String> set = getLikes();
        if (isLiked()) {
            set.remove(contentId);
        } else {
            set.add(contentId);
        }
        sharedPreferences.edit().putStringSet("likes", set).apply();
    }

    public Set<String> getLikes() {
        return  new HashSet<>(sharedPreferences.getStringSet("likes", new HashSet<>()));
    }

    public void setViews() {
        if (isViewed()) return;
        Set<String> set = getViews();
        set.add(contentId);
        sharedPreferences.edit().putStringSet("views", set).apply();
    }

    public Set<String> getViews() {
        return new HashSet<>(sharedPreferences.getStringSet("views", new HashSet<>()));
    }

    private void convertOldLocalDataToNewFormat(String key) {
        Object raw = sharedPreferences.getAll().get(key);

        if (raw instanceof Set) return;

        if (raw instanceof String old) {

            Set<String> newSet = new HashSet<>();

            if (old != null && !old.isEmpty()) {
                String[] parts = old.split(",");

                for (String part : parts) {
                    if (!part.trim().isEmpty()) {
                        newSet.add(part.trim());
                    }
                }
            }

            sharedPreferences.edit()
                    .putStringSet(key, newSet)
                    .apply();
        }
    }
}
