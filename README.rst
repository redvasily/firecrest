=========
firecrest
=========

Kafka instructions
==================

Start a zookeeper::

    bin/zookeeper-server-start.sh config/zookeeper.properties

Start the kafka server::

    bin/kafka-server-start.sh config/server.properties

Create a topic (once)::

    bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic firecrest-messages

Sending messages::

    bin/kafka-console-producer.sh --broker-list localhost:9092 --topic firecrest-messages

Consuming messages::

    bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic firecrest-messages --from-beginning


Logstash-encoded-messages
=========================

::

    {
        "@timestamp": "2016-03-27T19:39:55.276+02:00",
        "@version": 1,
        "HOSTNAME": "lumpy",
        "level": "INFO",
        "level_value": 20000,
        "logger_name": "io.bluejay.App",
        "message": "Finished: wtf",
        "thread_name": "main"
    }

    {
        "@timestamp": "2016-03-27T19:39:54.251+02:00",
        "@version": 1,
        "HOSTNAME": "lumpy",
        "level": "INFO",
        "level_value": 20000,
        "logger_name": "io.bluejay.App",
        "message": "Yo",
        "thread_name": "main"
    }
