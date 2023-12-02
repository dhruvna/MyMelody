package edu.ucsb.ece251.DATQ.mymelody;

import android.util.Log;
public class User {
    private String id;
    private String username;
    private String email;
    private String profileLink;
    private String pfpLink;
    private String accessToken;

    // Constructor
    public User(String id, String username, String email, String profileLink, String pfpLink, String accessToken) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profileLink = profileLink;
        this.pfpLink = pfpLink;
        this.accessToken = accessToken;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public String getProfileLink() {
        return profileLink;
    }
    public String getPFPLink() {
        return pfpLink;
    }
    public String getAccessToken() { return accessToken; }

    @Override
    public String toString() {
        String userString =  "id: " + id + "\n" +
                "Username: " + username + "\n" +
                "Email: " + email + "\n" +
                //Html.fromHtml("<a href=" + profileLink + "> Profile </a>")
                "Profile Link: " + profileLink + "\n" +
                "PFP Link: " + pfpLink + "\n" +
                "Access Token: " + accessToken + "\n"
        ;
        Log.println(Log.VERBOSE, "USER INFO", userString);
        return userString;
    }
}
