# Relay Service Test Application
This application is a multiplayer game where you see other players' mouse movement. Players can create shockwaves by clicking.
![](readme_images/screencap.jpg)

## How to setup the app
1. Create a new app in the portal: https://portal.braincloudservers.com/
   Take note of the **app id** and **app secret**.

2. Create a new server with the following settings:
   ![](readme_images/relayserver.jpg)
   Make sure to choose **Relay Server (hosted)** as shown in the image. Pick a region close to you in the Regions tab. For example, we chose `ca-central-1` for our tests.
   Name this new server "CursorParty".

3. Create a lobby type with the following settings:
   ![](readme_images/lobby1.jpg)
   Choose your lobby type "CursorParty" and make sure the Server setting is set to "CursorParty". Leave the Teams tab as default. In the Rules tab, then set the following settings:
   ![](readme_images/lobby2.jpg)
   Those settings will start the game as soon as at least 1 player is ready. The application is built in a such way that only the room owner can ready up.

4. In the main application file `App.java`, replace the following line:
   ```
   //_bcWrapper.initialize("appId", "secretKey", "appVersion", "serverUrl");
   ```
   With your own:
   ```
   _bcWrapper.initialize(your_appId, your_appSecret, Version.version);
   ```

5. To run the project via CLI:
      - Clean the package and create and executable JAR: `mvn clean package`
      - Execute the created JAR: `java -jar target/relay-test-app-1.jar`
