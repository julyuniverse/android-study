package com.benection.androidstudy;

import static com.benection.androidstudy.DeviceInfoManager.getDeviceInfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;

import retrofit2.Call;

public class SocialLoginActivity extends AppCompatActivity {
    private SocialLoginViewModel socialLoginViewModel;
    Context context = this;
    private NetworkManager networkManager;
    private ApiService apiService;
    private boolean isLoginWithUuidCalled = false;
    private ProgressBar progressBar;
    private Button googleLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_social_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Context가 준비된 후 NetworkManager 인스턴스를 초기화
        networkManager = NetworkManager.getInstance(context);
        apiService = networkManager.createService(ApiService.class);

        // Initialize UI elements
        progressBar = findViewById(R.id.progress_bar);
        googleLoginButton = findViewById(R.id.google_login_button);

        // Initialize ViewModel
        socialLoginViewModel = new ViewModelProvider(this).get(SocialLoginViewModel.class);

        // Observe appState to update UI
        socialLoginViewModel.getAppState().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String appState) {
                updateUI(appState);
                if (appState.equals("loggedIn")) {
                    navigateToHome(); // loggedIn 상태일 때 HomeActivity로 이동
                }
            }
        });

        // Set up Google login button click listener
        googleLoginButton.setOnClickListener(v -> {
            CredentialManager credentialManager = CredentialManager.create(context);
            String rawNonce = UUID.randomUUID().toString();
            String hashedNonce = getHashedNonce(rawNonce);
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setNonce(hashedNonce)
                    .build();
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();
            credentialManager.getCredentialAsync(
                    context,
                    request,
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            handleSignIn(result);
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            handleFailure(e);
                        }
                    }
            );
        });



        // 처음 실행될 때만 loginWithUuid 호출
        if (!isLoginWithUuidCalled) {
            try {
                loginWithUuid();
                isLoginWithUuidCalled = true; // 호출된 후 플래그를 true로 설정
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // 초기 상태로 설정 (Progress 표시)
        socialLoginViewModel.setAppState("checking");
    }

    private void updateUI(String appState) {
        progressBar.setVisibility(View.GONE);
        googleLoginButton.setVisibility(View.GONE);

        switch (appState) {
            case "checking":
                progressBar.setVisibility(View.VISIBLE);
                break;
            case "loggedOut":
                googleLoginButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loginWithUuid() throws IOException {
        DeviceInfo deviceInfo = getDeviceInfo(context);
        UuidLoginRequest uuidLoginRequest = new UuidLoginRequest(deviceInfo.uuid(), deviceInfo.model(), deviceInfo.systemName(), deviceInfo.systemVersion());
        try {
            Call<UuidLoginResponse> call = apiService.loginWithUuid(uuidLoginRequest);
            networkManager.request(call, (data, response) -> {
                // set local storage
                SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("deviceId", String.valueOf(data.deviceId()));
                editor.apply(); // 비동기로 저장
                if (Objects.equals(data.responseStatus().code(), "C0001")) {
                    socialLoginViewModel.setAppState("loggedIn");
                } else {
                    socialLoginViewModel.setAppState("loggedOut");
                }
            });
        } catch (Exception e) {
            // 여기서 예외 처리
            Log.e("SocialLoginActivity", "Request Error: " + e.getMessage());
            // 필요한 추가 처리 (예: 사용자에게 알림 표시)
        }
    }

    private void handleFailure(GetCredentialException e) {
        runOnUiThread(() -> {
            // e.getMessage()를 사용하여 예외의 일반적인 메시지를 가져온다.
            String errorMessage = e.getMessage() != null ? e.getMessage() : "An error occurred";
            Toast toast = Toast.makeText(context, errorMessage, Toast.LENGTH_LONG);
            toast.show();
        });
    }

    private String getHashedNonce(String nonce) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(nonce.getBytes());
            StringBuilder hashedNonce = new StringBuilder();
            for (byte b : digest) {
                hashedNonce.append(String.format("%02x", b));
            }
            return hashedNonce.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found.");
        }
    }

    public void handleSignIn(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
        String googleIdToken = googleIdTokenCredential.getIdToken();
        SocialLoginRequest socialLoginRequest = new SocialLoginRequest(googleIdToken);
        try {
            Call<SocialLoginResponse> call = apiService.loginWithSocial(socialLoginRequest);
            networkManager.request(call, ((data, response) -> {
                // set local storage
                SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("accessToken", data.token().accessToken());
                editor.putString("refreshToken", data.token().refreshToken());
                editor.apply(); // 비동기로 저장
                socialLoginViewModel.setAppState("loggedIn");
            }));
        } catch (Exception e) {
            // 여기서 예외 처리
            Log.e("SocialLoginActivity", "Request Error: " + e.getMessage());
            // 필요한 추가 처리 (예: 사용자에게 알림 표시)
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(SocialLoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish(); // SocialLoginActivity를 종료하여 뒤로 가기 시 다시 돌아올 수 없게 함
    }
}
