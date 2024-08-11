package com.benection.androidstudy;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
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

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    Button btnGoogleLogin;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnGoogleLogin = findViewById(R.id.btn_google_login);
        btnGoogleLogin.setOnClickListener(v -> {
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
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

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
        System.out.println(googleIdToken);
    }
}
