#!/bin/sh
# Install as $CATALINA_BASE/bin/setenv.sh for this lab's Tomcat instance.
# Keep common Tomcat settings, then allow the explicit JVM profile in
# CATALINA_OPTS to take effect without JVM environment variable overrides.
if [ "${CATALINA_HOME:-}" != "${CATALINA_BASE:-}" ] &&
   [ -r "${CATALINA_HOME:-}/bin/setenv.sh" ]; then
    . "$CATALINA_HOME/bin/setenv.sh"
fi
unset _JAVA_OPTIONS JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS
