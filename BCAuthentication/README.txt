BCAuthentication Demo App
This is an Android application that demonstrates how some of the common brainCloud functions work.
*	*	*

Getting Setup
This application uses the Java Client Library for brainCloud. It was created with Android Studio 2021.3.1 and uses Java 1.8.

Downloads
The Java Client Library: https://github.com/getbraincloud/braincloud-java
Android Studio: https://developer.android.com/studio
JDK: https://www.oracle.com/java/technologies/downloads/#java8

Some of the functions will require additional setup for the app in the brainCloud portal.
- XP: Currently, only XP points will be incremented. 
	- To increase XP Level, levels must first be created on the XP Levels page via Design > Gamification > XP Levels
- Currency: The Virtual Currency used in this example is "gems". It modifies a user's balance via Cloud Code Scripts.
	- Two scripts are necessary for this example: AwardCurrency, and ConsumeCurrency.
		- Create scripts on the Scripts page via Design > Cloud Code > Scripts.
	- Create virtual currencies from the Virtual Currencies page via Design > Marketplace > Virtual Currencies
		- NOTE: if you wish to create a currency with a different name then "gems" you will need to modify the currency functions within the project.
			- getCurrency() at BCClient.java L201
			- parseCurrencyJSON() at ExploreCurrency.java L111
			- awardCurrency() at ExploreCurrency.java L152
			- consumeCurrency() at ExploreCurrency.java L175
			- You will also need to change the parameters of your scripts to reflect the changed currency name
- Stats: Both User and Global Statistics must be created in order to view/increment them from the app.
	- Create User Statistics on the Statistics page via Users > Data > Statistics
	- Create Global Statistics on the Global Statistics page via Global > Global Data > Global Statistics
