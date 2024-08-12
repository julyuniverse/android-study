package com.benection.androidstudy;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import retrofit2.Call;

public class HomeActivity extends AppCompatActivity {
    private SocialLoginViewModel socialLoginViewModel;
    Context context = this;
    private NetworkManager networkManager;
    private ApiService apiService;
    private Button getPostsButton;
    private TextView postsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Context가 준비된 후 NetworkManager 인스턴스를 초기화
        networkManager = NetworkManager.getInstance(context);
        apiService = networkManager.createService(ApiService.class);

        // Initialize UI elements
        getPostsButton = findViewById(R.id.get_posts_button);

        // Initialize ViewModel
        socialLoginViewModel = new ViewModelProvider(this).get(SocialLoginViewModel.class);

        socialLoginViewModel.getPosts().observe(this, new Observer<List<Post>>() {
            @Override
            public void onChanged(List<Post> posts) {
                StringBuilder postsContent = new StringBuilder();
                for (Post post : posts) {
                    postsContent.append("Title: ").append(post.title()).append("\n");
                    postsContent.append("Content: ").append(post.content()).append("\n\n");
                }
                postsTextView.setText(postsContent.toString());
            }
        });

        // get posts
        getPostsButton.setOnClickListener(v -> {
            socialLoginViewModel.fetchPosts();
        });
    }
}