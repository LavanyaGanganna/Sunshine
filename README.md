# Sunshine
Displays weekly weather forecast on mobile phone and current weather updates on android wear.

## Getting Started
Follow these instructions to build and run the project 

1. Clone this repository.
2. Download the appropriate JDK for your system.I am currently on JDK 8.
3. Install Android Studio.
4. Import the project. Open Android Studio, click `Open an existing Android Studio project` and select the project. Gradle       will build the project.
5. After the project builds launch the android wear emulator
6. Download Android wear app in the mobile phone.and connect with the android wear emulator
7. Activate "Debugging over Bluetooth" in the wearable.Enable bluetooth in the mobile also
8. Open the terminal and type this command `adb -s handheldDeviceName forward tcp:5601 tcp:5601`,where hanheldDevicename is 
    mobile phone's serial number obtained by running `adb devices` command.
9. Run the app. Click `Run > Run 'app'`. After that Click `Run > Run 'wear'`.
10. Select the custom watch face from the android wear.
## Features
* Displays a list of weather forecast from Today's date.
* Clicking on each item shows detail view of temperature,pressure,etc.
* Uses Openweathermap Api to pull weather JSON data
* Saves weather data in the database to show the weather forecast to the user incaes network signal is weak.
* Uses Sync Adapter to sync weather data between the device and the Opemweathermap server after every 3 hours.
* Displays weather in the tablet too.

## Licence


