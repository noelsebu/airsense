#!/bin/sh
# Gradle start up script for UN*X

DIRNAME="$(dirname "$0")"
APP_HOME="$(cd "$DIRNAME" && pwd)"

# Locate java binary
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

exec "$JAVA_CMD" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
