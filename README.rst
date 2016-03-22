=======
firecrest
=======

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


