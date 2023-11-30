package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class LoginActivity extends AppCompatActivity {
    private TextView LoginPrompt;
    private TextView UserInfo;
    private ImageView PFP;
    private SpotifyService spotifyService;
    private Button loginButton;
    private Button logoutButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.Toolbar);
//        setSupportActionBar(toolbar);

        LoginPrompt = findViewById(R.id.LoginPrompt);
        UserInfo = findViewById(R.id.UserInfo);
        PFP = findViewById(R.id.pfp);
        loginButton = findViewById(R.id.LoginButton);
        logoutButton = findViewById(R.id.LogoutButton);

        spotifyService = new SpotifyService(this);
        loginButton.setOnClickListener(view -> spotifyService.authenticateSpotify(this));
        logoutButton.setOnClickListener(view-> {
            boolean logOutSuccess = spotifyService.logOut();
            if(logOutSuccess) logout();
        });

    }

    private void fetchUserInfo(String accessToken) {
        spotifyService.fetchUserInfo(accessToken, new SpotifyService.FetchUserInfoCallback() {
            @Override
            public void onUserInfoFetched(User user) {
                UserInfo.setText(user.toString());
                UserInfo.setVisibility(View.VISIBLE);
                String pfpURL = user.getPFPLink();
                Picasso.get().load(pfpURL).into(PFP);
                PFP.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError() {
                showToast("Failed to fetch user information.");
            }
        });
    }
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String accessToken = spotifyService.handleAuthResponse(intent);

        if(accessToken != "null") {
            LoginPrompt.setText(R.string.success_msg);
            showToast("Login successful.");
            loginButton.setVisibility(View.INVISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            fetchUserInfo(accessToken);
        } else {
            LoginPrompt.setText(R.string.fail_msg);
            showToast("Log in failure.");
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        LoginPrompt.setText(R.string.login_msg);
        showToast("Logged out.");
        loginButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.INVISIBLE);
        UserInfo.setVisibility(View.INVISIBLE);
        PFP.setVisibility(View.INVISIBLE);
    }

}