@echo off
title VM Allocator Simulation Starter

echo ================================================
echo        Building and Running Project
echo ================================================

set CP=.;lib\*;out

echo Creating output directory...
if exist out (
    echo Cleaning old class files...
    rmdir /s /q out
)
mkdir out

echo.
echo ================================================
echo            Compiling Java Files
echo ================================================
echo.

javac -cp "%CP%" -d out src\agents\*.java src\cloud\*.java

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo *** COMPILATION FAILED ***
    pause
    exit /b
)

echo.
echo ================================================
echo           Starting JADE Main Container
echo ================================================
echo.

java -cp "%CP%" jade.Boot -gui ^
   -agents "ManagerAgent:agents.ManagerAgent;ResourceAgent1:agents.ResourceAgent;ResourceAgent2:agents.ResourceAgent"

echo.
echo ================================================
echo        JADE HAS STOPPED
echo ================================================
pause
