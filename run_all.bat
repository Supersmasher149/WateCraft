@echo off
title KissCraft Launcher

echo Starting server...
start "KissCraft Server" cmd /c "gradlew.bat runServer & pause"

echo Waiting 15 seconds for server to start...
timeout /t 15 /nobreak >nul

echo Starting Player One...
start "KissCraft PlayerOne" cmd /c "gradlew.bat runClientPlayerOne & pause"

echo Starting Player Two...
start "KissCraft PlayerTwo" cmd /c "gradlew.bat runClientPlayerTwo & pause"

echo All windows launched. Close each window to stop.
