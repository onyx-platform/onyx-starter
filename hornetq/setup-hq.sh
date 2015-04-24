#!/bin/sh
wget http://downloads.jboss.org/hornetq/hornetq-2.4.0.Final-bin.tar.gz 
tar zxvf hornetq-2.4.0.Final-bin.tar.gz 
cp hornetq-configuration-standalone.xml hornetq-2.4.0.Final/config/stand-alone/non-clustered/hornetq-configuration.xml
