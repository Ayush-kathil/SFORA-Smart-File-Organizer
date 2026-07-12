@echo off
echo ===================================================
echo   SFORA - Smart File Organizer (Pro Edition)
echo ===================================================
echo.
echo Compiling source code...
if not exist "bin" mkdir bin
javac -d bin src\*.java

if %errorlevel% neq 0 (
    echo Compilation failed. Please check the errors above.
    pause
    exit /b %errorlevel%
)

echo Starting SFORA Graphical Interface...
java -cp bin App --gui
