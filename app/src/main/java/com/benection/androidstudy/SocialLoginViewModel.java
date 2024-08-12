package com.benection.androidstudy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class SocialLoginViewModel extends AndroidViewModel {
    private static final String APP_STATE_KEY = "appState";
    private final MutableLiveData<String> appState = new MutableLiveData<>();
    private final SharedPreferences sharedPreferences;
    private final MutableLiveData<List<Post>> posts = new MutableLiveData<>();
    private final NetworkManager networkManager;
    private ApiService apiService;

    public SocialLoginViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        String initialAppState = sharedPreferences.getString(APP_STATE_KEY, "checking");
        appState.setValue(initialAppState);
        networkManager = NetworkManager.getInstance(application);
        apiService = networkManager.createService(ApiService.class);
    }

    public LiveData<String> getAppState() {
        return appState;
    }

    public void setAppState(String newState) {
        appState.setValue(newState);
        sharedPreferences.edit().putString(APP_STATE_KEY, newState).apply();
    }

    public LiveData<List<Post>> getPosts() {
        return posts;
    }

    public void fetchPosts() {
        try {
            Call<Posts> call = apiService.getPosts();
            networkManager.request(call, ((data, response) -> {
                posts.postValue(data.posts());
            }));
        } catch (Exception e) {
            // 여기서 예외 처리
            Log.e("SocialLoginActivity", "Request Error: " + e.getMessage());
            // 필요한 추가 처리 (예: 사용자에게 알림 표시)
        }
    }
}
