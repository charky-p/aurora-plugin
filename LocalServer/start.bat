@echo off
:start
echo Starting Aurora Development Server...
"C:\Users\Chill\.jdks\ms-21.0.9\bin\java.exe" -Xmx2G -Xms2G -jar server.jar nogui
echo.
goto start