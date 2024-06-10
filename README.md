[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-24ddc0f5d75046c5622901739e7c5dd533143b0c8e959d652212380cedb1ea36.svg)](https://classroom.github.com/a/Pc5rbg6t)

---

# Clockworks - Time Tracker

## ST10024620  
## POE Part 3 - Final Part

### Application Version: 1.1  
### Developed by: Sauraav Jayrajh

---

## Developer Contact Details:

- **Phone:** 071 924 4175
- **Email:** sauraavjayrajh@gmail.com

---

## Changelog:

### 1.1:
- Users can now continue tracking an activity where they left off
- Users can see their old activities in an expanded view for greater detail
- Users can now edit the details of their old tracked sessions
- Users can now see a visual representation of the amount of time they spent working in a custom timeframe
- Users can now see a visual representation of the categories their sessions fall under and their consistency
- Users can now sign in via Google Authentication instead of registering an account and logging in

### 1.0:
- Release version, no changes yet.

---

## Description:

Clockworks is a tool designed for freelancers and contractors. It allows individuals to record the details and amount of time they spend working on their professional activities.

---

## Getting Started:

### Software Requirements:
- Android 8 Based Operating System
- Internet Connection
- Camera Permissions
- Android Studio (If compiling the application yourself)

### Hardware Requirements:
- 1GB of RAM
- 38MB of Free Storage

---

## Installation Instructions:

### For compiling yourself:
1. Clone this repository to your local machine
2. Open the solution in Android Studio
3. Build the solution and run the application

### For running the compiled version:
1. Locate and install "app-debug.apk".

---

## Usage:

Upon running the application, you will see a hamburger menu on the top left-hand side of your screen. It contains the options:

- **Sign In/Register (If Not Signed In) OR Log Out/Close (If Signed In):** Allows users to log in or register if they do not have an account, or log out and close the application if they are already signed in.
- **Track Time:** Manages and tracks time spent on tasks. Users can add new sessions, view existing sessions, and categorize their activities.
- **My Categories:** Allows users to filter and view their tracked sessions by category.
- **My Goals:** Lets users set and monitor their working time goals.
- **About:** Provides some information about the application.

To select an option, simply click on it. Follow the on-screen instructions to complete the different tasks. Remember to refresh the app after adding your first task to see it reflected in the home feed.

---

## FAQ:

### General

**Q: How do I sign up for the app?**  
**A:** To sign up, launch the app and tap the hamburger menu, then click on the "Sign Up" option. Enter your name, email, and desired password. The app will then register you successfully.

**Q: How do I log into my account?**  
**A:** After signing up, you can log in by entering your email and password on the login screen. Alternatively, you can log in using your Google account by clicking on the "Log in with Google" button.

**Q: What should I do if I forget my password?**  
**A:** Currently, if you have forgotten your password, you will need to register a new account or use the Google login feature.

### Tracking Time

**Q: How do I add a new session to track my activities?**  
**A:** Once logged in, click the plus button at the bottom of the home screen. This will open a window to track a new session. Enter a description, select a category, and optionally add photos. The start time is automatically populated based on the current time.

**Q: How does the auto-calculation feature work?**  
**A:** The app can automatically calculate the end time and date for your session based on the start time and duration you worked. Leave the end fields blank to use this feature.

**Q: How can I view my tracked sessions?**  
**A:** Your tracked sessions will appear on the home feed once you refresh the app after adding the first session. For all subsequent sessions, they will appear immediately.

**Q: Can I edit an existing session?**  
**A:** Yes, click on any tracked session to bring up an interface where you can edit the description, category, and other details. Once done, press the back button to save your changes.

### Categories

**Q: Can I filter my sessions by category?**  
**A:** Yes, go to the "My Categories" screen from the hamburger menu. Here you can view all the tasks you've done grouped by category.

### Goals

**Q: How do I set goals for time worked?**  
**A:** Go to the goals section and set a minimum and maximum number of hours you want to work. The app will notify you once the goal is reached and will display your progress toward your daily goal.

**Q: How can I track my progress toward my goals?**  
**A:** The progress towards your goals will be displayed in the goals section. The app will notify you once the goal is completed.

### Images

**Q: How can I add images to my sessions?**  
**A:** When you are adding a new session, you have the option to either upload an image from your device or take a new photo using the camera.

### Analytics 

**Q: How does the pie chart and graph feature work?**  
**A:** The pie chart shows the total consistency of categories for your projects, and the graph indicates the time spent working. These graphs update in real-time as you add new sessions and activities.

**Q: Why doesn't the task breakdown pie chart seem consistent with the number activities I have?**  
**A:** The pie chart considers the amount of time spent on each activity, not the number of activities. Therefore, the breakdown shows the proportion of time spent on different types of activities.

---

## Plugins and Frameworks Used:

- **com.github.bumptech.glide:glide:4.14.1**
- **com.google.firebase:firebase-auth:22.3.1**
- **com.google.firebase:firebase-database:20.3.1**
- **com.google.firebase:firebase-firestore:24.11.1**
- **com.google.firebase:firebase-auth-ktx:22.1.0**
- **com.google.android.gms:play-services-auth:21.2.0**
- **com.google.android.material:material:1.11.0**
- **com.github.PhilJay:MPAndroidChart:v3.1.0**

---

## Attributions/References:

- **My GitHub Repo:** [https://github.com/VCWVL/opsc7311---open-source-coding---part-2-Saupernova13](https://github.com/VCWVL/opsc7311---open-source-coding---part-2-Saupernova13)
- **Google Auth Implementation:** [https://firebase.google.com/docs/auth/android/google-signin](https://firebase.google.com/docs/auth/android/google-signin)
- **Video Assistance with Google Auth Implementation:** [https://www.youtube.com/watch?v=H_maapn4Q3Q](https://www.youtube.com/watch?v=H_maapn4Q3Q)
- **Retrieving Sha-1 Key:** [https://www.devopsschool.com/blog/how-to-get-sha-1-key-in-android-studio-for-firebase/](https://www.devopsschool.com/blog/how-to-get-sha-1-key-in-android-studio-for-firebase/)
- **Login Persistence:** [https://stackoverflow.com/questions/73095648/i-want-to-keep-user-logged-in-until-he-signsout-in-my-app-androidstudio-firebase](https://stackoverflow.com/questions/73095648/i-want-to-keep-user-logged-in-until-he-signsout-in-my-app-androidstudio-firebase)
- **Implementation of Grid Layouts:** [https://www.youtube.com/watch?v=PFEb9FfopFo](https://www.youtube.com/watch?v=PFEb9FfopFo)
- **Further Grid Layout Explanations:** [https://www.youtube.com/watch?v=aRgSrJO40z8](https://www.youtube.com/watch?v=aRgSrJO40z8)
- **Options Menu Removal:** [https://stackoverflow.com/questions/44234410/how-to-remove-three-dots-from-the-toolbar-of-navigation-drawer](https://stackoverflow.com/questions/44234410/how-to-remove-three-dots-from-the-toolbar-of-navigation-drawer)
- **Prevention of Returning to Previous Pages:** [https://stackoverflow.com/questions/8631095/how-to-prevent-going-back-to-the-previous-activity](https://stackoverflow.com/questions/8631095/how-to-prevent-going-back-to-the-previous-activity)
- **Add Events to Floating Action Button:** [https://stackoverflow.com/questions/30876515/onclick-for-floating-action-button](https://stackoverflow.com/questions/30876515/onclick-for-floating-action-button)
- **Building of APK:** [https://stackoverflow.com/questions/17702053/where-is-android-studio-building-my-apk-file](https://stackoverflow.com/questions/17702053/where-is-android-studio-building-my-apk-file)
- **Attempted Auto Dropdown Menu:** [https://www.youtube.com/watch?v=741l_fPKL3Y](https://www.youtube.com/watch?v=741l_fPKL3Y)

---

## License:

This project is licensed under the MIT License.

---
