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
curl "%_HTTP_BASE%%1"
goto end

rem if ""%1"" == ""stop"" goto doStop
rem if ""%1"" == ""stopnode"" goto doStopnode
rem if ""%1"" == ""restart"" goto doRestart
rem if ""%1"" == ""status"" goto doStatus
rem if ""%1"" == ""help"" goto doHelp
rem if ""%1"" == ""switchclusterstate"" goto doSwitchclusterstate
rem if ""%1"" == ""switchfrombaseline"" goto doSwitchfrombaseline
rem if ""%1"" == ""?"" goto doHelp
rem goto doHelp


:doStart
set "_JAVA_BIN_EXEC=%JRE_HOME%\bin\java"
if "%JRE_HOME%"=="" (set "_JAVA_BIN_EXEC=java")
set "_COMMAND_LINE_ARG=-Xmx%MAX_MEMORY% -DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true -XX:+UseG1GC -Xms512m -XX:+DisableExplicitGC -Dfile.encoding=UTF-8 -DIGNITE_NODE_PROP_FILE=%IGNITE_NODE_PROP_FILE%"
"%_JAVA_BIN_EXEC%" %_COMMAND_LINE_ARG% -jar ${enterPoint}.jar %BOOTLOADER_HTTP_PORT%
timeout /t 1
goto end


rem :doStop
rem curl "%_HTTP_BASE%stop"
rem goto end

rem :doStopnode
rem curl "%_HTTP_BASE%stopnode"
rem goto end

rem :doRestart
rem curl "%_HTTP_BASE%restart"
rem goto end

rem :doStatus
rem curl "%_HTTP_BASE%status"
rem goto end

rem :doHelp
rem curl "%_HTTP_BASE%help"
rem goto end

rem :doSwitchclusterstate
rem curl "%_HTTP_BASE%switchclusterstate"
rem goto end

rem :doSwitchfrombaseline
rem curl "%_HTTP_BASE%switchfrombaseline"
rem goto end


:end
exit /b