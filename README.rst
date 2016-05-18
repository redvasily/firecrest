=========
firecrest
=========

Overview
========

Firecrest is a reliable logs / metrics management system built on top of Kafka / Elasticsearch.


Motivation
==========

It's essential to have access to your system's logs/metrics with reach search capability.
Logstash provides a nice set of features, however it has some problems with stability and
exhibits some weird performance problems from time to time.

After having been let down by Logstash several times, I've decided to write a reliable Logstash
"replacement". Firecrest is not aiming to provide all of the features of Logstash, but to
provide a subset of features that is useful in a modern mircoservice-oriented tech stack,
while being as hassle-free as possible.


Architecture
============

::

    logs      +-----------+                     +-----------+
    --------> |           |      +-------+      |           |       +--------------+
              | Firecrest |      |       |      | Firecrest |       |              |
              |  bridge   | ---> | Kafka | ---> |  indexer  | ----> | Elasicsearch |
    metrics   |           |      |       |      |           |       |              |
    --------> |           |      +-------+      +-----------+       +--------------+
              +-----------+


Firecrest is written in Scala/Akka, using supervisors through the whole system. Such
architecture common for Erlang applications has proven itself to be extremely reliable,
so I hope it will server here as well.

Bridge provides accepts different log and metrics data over simple TCP/UDP, converts
formats, repacks and put them into Kafka.

Kafka is necessary to provide reliability in case of a temporary log storage
subsystem (Elasticsearch) problems / downtime (out of memory, upgrade etc).

Bridge saves clients from having to implement kafka protocol details or from having to depend on
kafka client libraries. At the moment bridge accepts graphite compatible metrics data over TCP and UDP
and Logstash compatible json log data over TCP. Such json log data can be produced for instance by
Logstash-logback-encoder.

Indexer simply takes already formatted and prepared data from kafka and stores it into
Elasticsearch.

With such architecture, reliability of the whole system is the same as reliability of Kafka,
which means that the whole system is very reliable. A temporary downtime of Elasticsearch
will not lead to loss of data.

Building firecrest
==================

::

    mvn clean package

Usage
=====

To use firecrest you need Kafka and Elasticsearch. The easiest way to get started is by
setting up a VM with all dependencies via vagrant, you can do this by running::

    $ vagrant up

It will configure a debian jessie machine running on a private network with an IP address
192.168.13.37 with kafka and elasticsearch listening on standard ports:
9092 for kakfa, and 9300 for elasticsearch.

Now you can start a firecrest bridge::

    $ java -jar firecrest-kafka-bridge-0.1-SNAPSHOT.jar server config-bridge.yml

and an indexer::

    $ java -jar firecrest-indexer-0.1-SNAPSHOT.jar server config-indexer.yml

You can change different parameters in config-bridge.yml and config-indexer.yml.

By default firecrest-bridge listens for graphite-compatible metrics data on TCP port 9126 and
for logstash compatible json-formatted log data on TCP port 9125.

Sending logs and metrics from Dropwizard apps
---------------------------------------------

If you are using Dropwizard you can send metrics data to firecrest-bridge via
dropwizard-metrics-graphite plugin, by putting this into your config file::

    config goes here

You can send Dropwizard logs to firecrest-bridge by using ``package name here`` package and putting this into your
configuration file::

    config goes here

Accessing logs and metrics
--------------------------

Firecrest-indexer will make will put data into elasticsearch and will make sure that a correct mapping is used.
To access indexed data you can use standard tools such as Kibana and Grafana.





