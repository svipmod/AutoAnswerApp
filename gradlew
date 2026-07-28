#!/bin/sh
APP_HOME=$(cd $(dirname $0) && pwd)
cd "$APP_HOME"

if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  mkdir -p gradle/wrapper
  curl -sL "https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar" -o gradle/wrapper/gradle-wrapper.jar 2>/dev/null
  cat > gradle/wrapper/gradle-wrapper.properties << 'PROPS'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
PROPS
fi

JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/temurin-17-jdk}
exec "$JAVA_HOME/bin/java" \
  -Dorg.gradle.appname=gradlew \
  -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
