**Overview of StepFit Application**
The FitPulse App is a lightweight Android fitness tracking application that provides step counting, daily goal tracking, live sensor monitoring, historical stats, and user profile management. Built in Java, FitPulse keeps credentials and settings locally with SharedPreferences and stores step history using Room (SQLite). It integrates sensor technology via the hardware-backed Step Counter for accurate, power-efficient step detection, and also exposes Accelerometer, Gyroscope readouts in the Monitor screen. The app is organized into focused activities—Login, Register, Home, SensorMonitorActivity, StatsActivity, SettingsActivity (set the daily step goal, view profile, logout), and UserProfileActivity (name/email plus goal progress). Live progress on the Profile screen updates in real time through a local broadcast emitted by the StepCounterManager.

**Key Activities and Features**

**LoginActivity:**
This screen lets users sign in with their email and password. It checks the saved credentials in the preferences. If the details match, sends the user to MainActivity. If the user has logged in before, it skips this screen automatically.

**RegisterActivity:**
This screen allows new users to create an account by entering their name, email, and password. The data is stored locally. After successful registration, the user is redirected back to LoginActivity to sign in.

**MainActivity(Home):**
This is the first page after login. It shows a circular progress ring for today’s steps vs your daily goal, plus Calories (today) and Time Walking (today). These values update in real time as steps come in. The bottom navigation is also here so you can jump to Stats, Monitor, or Settings.

**SensorMonitorActivity:**
This live screen shows your current steps from the phone’s Step Counter and also displays Accelerometer and Gyroscope readings. It includes a BMI calculator—you enter your height and weight, and it shows your BMI value along with the category (Underweight/Normal/Overweight/Obese). The step numbers update in real time while you’re on this screen.

**StatsActivity:**
This page displays your past days step totals as charts. The numbers come from the local database (Room/SQLite), where each day’s steps are saved for history.

**SettingsActivity:**
Here you set your daily step goal. The goal is saved in FitPulsePrefs under the key step_goal. This screen also has buttons to view your profile and log out.

**UserProfileActivity:**
This page shows your name and email, plus a Daily Goals card.

**Conclusion**
The FitPulse app is a lightweight Android tracker that covers accurate step counting, daily goal progress (ring, calories, time walked), and a built-in BMI tool. Built in Java and offline-first, it uses SharedPreferences for credentials/settings, Room (SQLite) for step history, and real-time updates via StepCounterManager broadcasts—leveraging the hardware Step Counter plus Accelerometer/Gyroscope for stability data. A clean UI across Home, Monitor, Stats, Settings, Profile, and Login/Register keeps everyday tracking simple, reliable, and power-efficient.


**Screenshots:**

<img width="587" height="513" alt="Screenshot 2025-09-04 at 17 57 45" src="https://github.com/user-attachments/assets/e1ab5fa6-7ea2-4a0f-ac05-94ed9b72dd88" />
<img width="579" height="460" alt="Screenshot 2025-09-04 at 17 57 55" src="https://github.com/user-attachments/assets/3cacdd38-1cc3-44c9-9b22-dfc46366c5e2" />
<img width="577" height="446" alt="Screenshot 2025-09-04 at 17 58 03" src="https://github.com/user-attachments/assets/5141d7d1-59b6-4f26-aacf-0c5545c137b3" />
<img width="573" height="448" alt="Screenshot 2025-09-04 at 17 58 10" src="https://github.com/user-attachments/assets/68adfb5d-a87f-4bc1-a32e-f28d5c1c431b" />
