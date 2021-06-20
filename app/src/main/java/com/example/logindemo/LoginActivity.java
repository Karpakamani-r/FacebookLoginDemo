package com.example.logindemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.logindemo.databinding.ActivityLoginBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getName();
    private CallbackManager callbackManager;
    private ActivityLoginBinding activityLoginBinding;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityLoginBinding = ActivityLoginBinding.inflate(LayoutInflater.from(this));
        setContentView(activityLoginBinding.getRoot());
        initFB();
        activityLoginBinding.btnFBLogin.setOnClickListener(v -> {
            if (activityLoginBinding.group.getVisibility() == View.VISIBLE) {
                fbLogOut();
            } else {
                proceedLogin();
            }
        });
    }

    private void proceedLogin() {
        if (isUserLoggedIn()) {
            callGraphAPIToGetUserDetails();
        } else {
            loginWithFB();
        }
    }

    private void initFB() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, loginResult.getAccessToken().getUserId());
                        callGraphAPIToGetUserDetails();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Cancelled by user");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d(TAG, "Error " + exception.getMessage());
                    }
                });
    }

    private void loginWithFB() {
        LoginManager.getInstance().logInWithReadPermissions(this,
                Collections.singletonList("email,public_profile,user_birthday,user_gender"));
    }

    private boolean isUserLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null && !accessToken.isExpired();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void callGraphAPIToGetUserDetails() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), (object, response) -> {
            Log.d(TAG, object.toString());
            loginUIChanges(object);
        });

        request.setParameters(getUserInfoParams());
        request.executeAsync();
    }

    private Bundle getUserInfoParams() {
        Bundle bundle = new Bundle();
        bundle.putString(
                "fields",
                "id, name, email, gender, birthday");
        return bundle;
    }

    private void fbLogOut() {
        if (AccessToken.getCurrentAccessToken() == null) {
            return; // already logged out
        }
        new GraphRequest(
                AccessToken.getCurrentAccessToken(), "/me/permissions/",
                null, HttpMethod.DELETE,
                graphResponse -> {
                    LoginManager.getInstance().logOut();
                    logoutUIChanges();
                })
                .executeAsync();
    }

    @SuppressLint("SetTextI18n")
    private void logoutUIChanges() {
        activityLoginBinding.tvEmail.setText("");
        activityLoginBinding.tvUserName.setText("");
        activityLoginBinding.ivProfilePic.setImageResource(0);
        activityLoginBinding.btnFBLogin.setText("Facebook Login");
        activityLoginBinding.group.setVisibility(View.GONE);
    }

    @SuppressLint("SetTextI18n")
    private void loginUIChanges(JSONObject response) {
        activityLoginBinding.btnFBLogin.setText("Logout");
        activityLoginBinding.group.setVisibility(View.VISIBLE);
        try {
            activityLoginBinding.tvUserName.setText(response.getString("name"));
            activityLoginBinding.tvEmail.setText(response.getString("email"));
            activityLoginBinding.tvGender.setText(response.getString("gender"));
            activityLoginBinding.tvDOB.setText(response.getString("birthday"));
            loadProfileImage(response.getString("id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //For Image: https://graph.facebook.com/{id}/picture?width=9999
    private void loadProfileImage(String id) {
        String profileImageURL = "https://graph.facebook.com/" + id + "/picture?width=9999";
        Glide.with(this).load(profileImageURL)
                .into(activityLoginBinding.ivProfilePic);
    }
}