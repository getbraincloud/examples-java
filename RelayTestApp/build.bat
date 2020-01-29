javac ^
  -target 1.8 ^
  -d ./bin/ ^
  -cp ./src/;../thirdparty/braincloud-java/brainCloud/main/java/;../thirdparty/braincloud-java/TestBCClient-Desktop/brainCloud/src/main/java/;../thirdparty/json-20180130-sources.jar;../thirdparty/Java-WebSocket-1.3.8.jar ^
  ./src/com/bitheads/relaytestapp/*.java
