@echo off
setlocal

set DIR=%~dp0
if "%DIR:~-1%"=="\\" set DIR=%DIR:~0,-1%

set CLASSPATH=%DIR%\\gradle\\wrapper\\gradle-wrapper.jar

if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\\bin\\java.exe
) else (
  set JAVA_EXE=java.exe
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal