#!/usr/bin/env bash
export SOA_JAVA_COMMON_OPTS='-Xss512k -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -Djava.util.concurrent.ForkJoinPool.common.parallelism=1'
export WF1_JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:CompressedClassSpaceSize=64m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m $SOA_JAVA_COMMON_OPTS"
export WF2_JAVA_OPTS="-Xms32m -Xmx192m -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=24m $SOA_JAVA_COMMON_OPTS"
export TOMCAT_JAVA_OPTS="-Xms32m -Xmx128m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=48m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=24m $SOA_JAVA_COMMON_OPTS"
export MULE_JAVA_OPTS="-Xms32m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:CompressedClassSpaceSize=64m -XX:ReservedCodeCacheSize=48m -XX:MaxDirectMemorySize=32m $SOA_JAVA_COMMON_OPTS"
export MULE_JAVA_OPTS="${MULE_JAVA_OPTS/-XX:ActiveProcessorCount=1/-XX:ActiveProcessorCount=2}"
export CLI_JAVA_OPTS="-Xms16m -Xmx96m -XX:MaxMetaspaceSize=96m -XX:CompressedClassSpaceSize=32m -XX:ReservedCodeCacheSize=24m -XX:MaxDirectMemorySize=16m $SOA_JAVA_COMMON_OPTS"
