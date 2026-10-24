@echo off
title PhoneTrace - Web
cd /d "%~dp0src"
if not exist "..\lib\*.jar" (
  echo The MySQL driver is missing. Put the mysql-connector-j .jar file into the lib folder.
  pause & exit /b
)
echo Compiling PhoneTrace...
javac -cp ".;..\lib\*" *.java
if errorlevel 1 (
  echo.
  echo Compiling failed. Check that the Java JDK is installed: open cmd and type  javac -version
  pause & exit /b
)
echo Starting the website. Keep this window open while you use it.
java -cp ".;..\lib\*" WebServer
pause
