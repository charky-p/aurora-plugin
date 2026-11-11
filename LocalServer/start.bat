@echo off
:start
echo Starting Aurora Development Server...
"C:\Users\Chill\.jdks\openjdk-25.0.1\bin\java.exe" -Xmx2G -Xms2G -jar versions\26.3\paper-26.3-248.jar nogui
echo.
goto start