#!/bin/bash
cp=target/neji-1.0-SNAPSHOT-jar-with-dependencies.jar:$CLASSPATH
MEMORY=4G
JAVA_COMMAND="java -Xmx$MEMORY -classpath $cp"
CLASS=pt.ua.tm.neji.Main

$JAVA_COMMAND $CLASS $*
