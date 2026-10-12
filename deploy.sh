#!/bin/bash
set -e

JAVA17_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

rm -rf out framework.jar
mkdir -p out

find mg -name "*.java" > sources.txt
"$JAVA17_HOME/bin/javac" -parameters -cp "lib/*" -d out @sources.txt
rm sources.txt

"$JAVA17_HOME/bin/jar" cf framework.jar -C out/ .
rm -rf out

echo "✅ framework.jar reconstruit (Java 17)"