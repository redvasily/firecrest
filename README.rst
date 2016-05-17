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

