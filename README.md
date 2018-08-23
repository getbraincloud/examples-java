# Match-Game Java Example

This is a simple matching game for Android using the brainCloud client in Java. 

## To Play 

Do one of the following:

1. Download the latest version of Android Studio here : https://developer.android.com/studio/ 
Then connect your Android device to the computer, press run in Android Studio, and select your device.
This will build the project then run it off your device.  

2. Take the APK file in BasicJava/app/build/outputs/apk/debug and drag it into your installs on your
device OR e-mail the APK file to yourself as an attachment, open it on your device and the install will 
start automatically. 

Note : brainCloud push notifications do not work with the Android Studio emulators. 

## Demonstrates

- Setting up your Java app using the brainCloudWrapper
- Authentication
- Push Notifications
- Reading and incrementing stats
- Awarding achievements 

# Setting Up your Own Project

## Integrate with brainCloud

To integrate your Android Studio project to use the brainCloud client,
follow the steps here : http://18.220.113.13/apidocs/tutorials/android-java-tutorials/getting-started-with-android-java/

You can find the Java client Libs that you will need to add to your project
here : https://github.com/getbraincloud/braincloud-java

## Setup push notifications through Firebase
To set up your app so that it can be sent push notifications through Firebase, and our brainCloud client
follow the steps here : https://getbraincloud.com/apidocs/portal-usage/push-notification-setup-firebase/
