package edu.ucsb.ece251.DATQ.mymelody;

import android.text.Html;

public class User {
    private String id;
    private String username;
    private String email;
    private String profileLink;
    private String pfpLink;

    public User() {
        //this.id = "";
        this.username= "";
        this.email= "";
        this.profileLink= "";
        this.pfpLink= "";
    }
    // Constructor
    public User(String id, String username, String email, String profileLink, String pfpLink) {
        //this.id = id;
        this.username = username;
        this.email = email;
        this.profileLink = profileLink;
        this.pfpLink = pfpLink;
    }

    // Getters and Setters
//    public String getId() {
//        return id;
//    }
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

    public void setId(String id) {
        this.id = id;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setProfileLink(String profileLink) {
        this.profileLink = profileLink;
    }
    public void setPFPLink(String pfpLink) {
        this.pfpLink = pfpLink;
    }
    // Optional: Override toString() for easy printing of User object details
    @Override
    public String toString() {
        return "Username: " + username + "\n" +
                //"id: " + id + "\n" +
                "Email: " + email + "\n" +
                Html.fromHtml("<a href=" + profileLink + "> Profile </a>")
                ;
    }
}
