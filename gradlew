#!/bin/sh
# Gradle start up script for UN*X

# Attempt to set APP_HOME
DIRNAME="$(dirname "$0")"
APP_HOME="$(cd "$DIRNAME" && pwd)"

exec "$JAVA_HOME/bin/java" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
