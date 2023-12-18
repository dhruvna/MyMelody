# Spotify Listening Habits Analyzer

## Overview

This Android application integrates with the Spotify API and Firebase to track and analyze a user's listening habits, including favorite albums, artists, songs, and genres. Users can input their current favorite artist or song, and the app uses Google Charts to visually represent how preferences change over time.

## Features

- **Spotify Integration**: Connects with Spotify to fetch user data related to listening habits.
- **Firebase Integration**: Utilizes Firebase for storing and retrieving user data and preferences.
- **User Preferences Input**: Allows users to manually input their current favorite artist or song.
- **Google Charts Visualization**: Dynamic visualizations of listening data using Google Charts API.
- **Responsive Design**: Optimized for various Android devices for a smooth user experience.

## Setup

1. **Clone the Repository**:  
   `git clone [repository URL]`

2. **Import into Android Studio**:  
   Open Android Studio and import the project.

3. **Spotify API Key**:  
   Register your application with Spotify Developer Dashboard to obtain the API key. Place the API key in the designated file.

4. **Firebase Configuration**:  
   Set up a Firebase project and download the `google-services.json` file. Add it to your project's app-level directory.

5. **Google Charts API**:  
   Ensure the Google Charts API is properly referenced in the HTML file used for chart rendering.

6. **Build the Project**:  
   Build and run the application using Android Studio.

## Usage

- **Login**: Authenticate with your Spotify account to allow the app to access your listening data.
- **Firebase Data Storage**: Your listening data and preferences are securely stored in Firebase.
- **View Listening Data**: Navigate through the app to see your most listened to songs, artists, and genres.
- **Update Preferences**: Regularly update your favorite artist or song. This data is stored and retrieved from Firebase.
- **Visualization**: Access visual representations of your music preferences over time.

## Dependencies

- Android Studio
- Spotify Android AppAuth (version 1.2.6, will be updated to 2.1.0 soon)
- Firebase SDK
- Google Charts API

## Contributing

- Fork the repository.
- Create a new branch for each feature or improvement.
- Submit a pull request with a clear description of your changes.
