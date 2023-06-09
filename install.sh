#!/bin/bash

SETTINGS="/Users/weasley/Development/program/apache-maven/conf/settings-sonatype.xml"
MODULE="multiple-email-spring-boot-starter"

clear &&
  mvn clean install -pl :$MODULE -am --settings $SETTINGS &&
  mvn clean
