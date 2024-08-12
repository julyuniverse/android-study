package com.benection.androidstudy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkManager {
    private static NetworkManager instance = null;
    private final Retrofit retrofit;
    private final Context context;
    private final SharedPreferences sharedPreferences;

    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);
    private boolean isTokenReissuing = false; // 토큰 재발행 중인지 여부
    private final List<Call<?>> waitingRequests = new ArrayList<>(); // 재발행 대기 중인 요청 리스트

    private NetworkManager(final Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
        Interceptor headerInterceptor = new Interceptor() {
            @NonNull
            @Override
            public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
                Request originalRequest = chain.request();

                // SharedPreferences에서 값 가져오기
                String deviceId = sharedPreferences.getString("deviceId", "");
                String accessToken = sharedPreferences.getString("accessToken", "");
                if (!accessToken.isEmpty()) {
                    accessToken = "Bearer " + accessToken;
                }

                // 헤더를 추가한 새로운 요청 만들기
                Request newRequest = originalRequest.newBuilder()
                        .header("Content-Type", "application/json")
                        .header("Platform", "Android")
                        .header("Device-Id", deviceId)
                        .header("Authorization", accessToken)
                        .build();

                return chain.proceed(newRequest);
            }
        };

        // OkHttpClient에 Interceptor 추가
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(headerInterceptor)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080") // 서버의 base URL
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static NetworkManager getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkManager.class) {
                if (instance == null) {
                    instance = new NetworkManager(context);
                }
            }
        }

        return instance;
    }

    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    // 공통된 enqueue 처리 메소드
    public <T> void request(Call<T> call, final NetworkCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body(), response);
                } else {
                    if (response.code() == 401) { // Unauthorized: 토큰 만료 등
                        handleTokenReissue(call);
                    } else {
                        handleError(new Throwable(response.errorBody() != null ? response.errorBody().toString() : "Unknown error"));
                    }
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                handleError(t);
            }
        });
    }

    // 토큰 재발행 처리
    private <T> void handleTokenReissue(Call<T> originalCall) {
        if (!isTokenReissuing) {
            isTokenReissuing = true;
            reissueToken(new TokenReissueCallback() {
                @Override
                public void onTokenReissued() {
                    isTokenReissuing = false;
                    retryWaitingRequests(); // 대기 중인 요청 재시도
                }

                @Override
                public void onTokenReissueFailed(Throwable error) {
                    isTokenReissuing = false;
                    cancelWaitingRequests(error); // 대기 중인 요청 취소
                    handleError(error);
                }
            });
        }
        waitingRequests.add(originalCall);
    }

    // 토큰 재발행 API 호출
    private void reissueToken(TokenReissueCallback callback) {
        ApiService apiService = createService(ApiService.class);
        String refreshToken = sharedPreferences.getString("refreshToken", "");
        ReissueTokenRequest reissueTokenRequest = new ReissueTokenRequest(refreshToken);
        Call<Token> call = apiService.reissueToken(reissueTokenRequest);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Token newToken = response.body();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("accessToken", newToken.accessToken());
                    editor.putString("refreshToken", newToken.refreshToken());
                    editor.apply();
                    callback.onTokenReissued();
                } else {
                    callback.onTokenReissueFailed(new Throwable("Failed to reissue token."));
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                callback.onTokenReissueFailed(t);
            }
        });
    }

    // 대기 중인 요청 재시도
    private <T> void retryWaitingRequests() {
        for (Call<T> call : (List<Call<T>>) (List<?>) waitingRequests) { // 안전한 타입 캐스팅
            request(call.clone(), new NetworkCallback<T>() {
                @Override
                public void onSuccess(T data, Response<T> response) {
                    // 재시도 성공 처리
                    System.out.println("Request retried successfully.");
                }
            });
        }
        waitingRequests.clear();
    }

    // 대기 중인 요청 취소
    private void cancelWaitingRequests(Throwable error) {
        for (Call<?> call : waitingRequests) {
            // 취소된 요청의 콜백에 에러 전달
            handleError(error);
        }
        waitingRequests.clear();
    }

    // 에러 처리 메소드
    private void handleError(Throwable t) {
        // 여기에서 에러를 로그로 출력하거나, UI에 에러를 표시하는 등의 처리를 합니다.
        Log.e("NetworkManager", "Error: " + t.getMessage());
        // 필요시 사용자에게 알림을 띄우거나, 다른 에러 처리 로직을 추가할 수 있습니다.
    }

    // 콜백 인터페이스 정의
    public interface NetworkCallback<T> {
        void onSuccess(T data, Response<T> response);
    }

    // 토큰 재발행 콜백 인터페이스 정의
    private interface TokenReissueCallback {
        void onTokenReissued();

        void onTokenReissueFailed(Throwable error);
    }
}
