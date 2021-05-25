@echo off

rem ||============================== SETUP =================================||
rem || MAX_MEMORY            = Max VM memory allocated                      ||
rem || JRE_HOME              = JVM home, empty for deafault system VM       ||
rem || BOOTLOADER_HTTP_PORT  = Inet sock port of http bootloader commander  ||
rem || IGNITE_NODE_PROP_FILE = Property file name, refer for file to        ||
rem ||                         execution dir, but can specify URL like      ||
rem ||                         'http://domain/file.properties'              ||
rem ||======================================================================||

set "MAX_MEMORY=4g"
set "JRE_HOME=path_of_JRE"
set "BOOTLOADER_HTTP_PORT=34445"
set "IGNITE_NODE_PROP_FILE=ignite_node.properties"


set "_HTTP_BASE=http://localhost:%BOOTLOADER_HTTP_PORT%/"
if ""%1"" == ""start"" goto doStart
if ""%1"" == ""stop"" goto doStop
if ""%1"" == ""stopnode"" goto doStopnode
if ""%1"" == ""restart"" goto doRestart
if ""%1"" == ""status"" goto doStatus
if ""%1"" == ""help"" goto doHelp
if ""%1"" == ""?"" goto doHelp
goto doHelp


:doStart
set "_JAVA_BIN_EXEC=%JRE_HOME%\bin\java"
if "%JRE_HOME%"=="" (set "_JAVA_BIN_EXEC=java")
set "_COMMAND_LINE_ARG=-Xmx%MAX_MEMORY% -DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true -XX:+UseG1GC -Xms512m -XX:+DisableExplicitGC -Dfile.encoding=UTF-8 -DIGNITE_NODE_PROP_FILE=%IGNITE_NODE_PROP_FILE%"
"%_JAVA_BIN_EXEC%" %_COMMAND_LINE_ARG% -jar ${enterPoint}.jar %BOOTLOADER_HTTP_PORT%
timeout /t 1
goto end


:doStop
curl "%_HTTP_BASE%stop"
goto end

:doStopnode
curl "%_HTTP_BASE%stopnode"
goto end


:doRestart
curl "%_HTTP_BASE%restart"
goto end

:doStatus
curl "%_HTTP_BASE%status"
goto end

:doHelp
curl "%_HTTP_BASE%help"
goto end



:end
exit /b