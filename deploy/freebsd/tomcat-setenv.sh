#!/bin/sh
if [ "${CATALINA_HOME:-}" != "${CATALINA_BASE:-}" ] &&
   [ -r "${CATALINA_HOME:-}/bin/setenv.sh" ]; then
    . "$CATALINA_HOME/bin/setenv.sh"
fi
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
