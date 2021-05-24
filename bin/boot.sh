#!/bin/sh


# ||============================= SETUP =================================||
# || MAX_MEMORY           = Max VM memory allocated                      ||
# || JRE_HOME             = JVM home, empty for deafault system VM       ||
# || BOOTLOADER_HTTP_PORT = Inet sock port of http bootloader commander  ||
# ||=====================================================================||

export "MAX_MEMORY=4g"
export "JRE_HOME=../jre1.8.0_291"
export "BOOTLOADER_HTTP_PORT=34445"


export "_HTTP_BASE=http://localhost:$BOOTLOADER_HTTP_PORT/"

case $1 in
	start)
		export "_JAVA_BIN_EXEC=$JRE_HOME/bin/java"
		if [ -z "$JRE_HOME" ]
		then
			export "_JAVA_BIN_EXEC=java"
		fi
		export "_COMMAND_LINE_ARG=-Xmx$MAX_MEMORY -DIGNITE_PERFORMANCE_SUGGESTIONS_DISABLED=true -XX:+UseG1GC -Xms512m -XX:+DisableExplicitGC"
		$_JAVA_BIN_EXEC "$_COMMAND_LINE_ARG" -jar ${enterPoint}.jar "$BOOTLOADER_HTTP_PORT"
		;;
	stop)
		curl "$_HTTP_BASE""stop"
		;;
	stopnode)
		curl "$_HTTP_BASE""stopnode"
		;;
	restart)
		curl "$_HTTP_BASE""restart"
		;;
	status)
		curl "$_HTTP_BASE""status"
		;;
	help|?|*)
		curl "$_HTTP_BASE""help"
		;;
esac
