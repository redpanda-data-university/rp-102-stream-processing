package com.example;

import java.util.Properties;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
  private static final Logger log = LoggerFactory.getLogger(App.class);

  public static void main(String[] args) {
    Topology topology;

    if (args.length == 0) {
      log.info("Running high purchase count topology");
      topology = HighPurchaseCountTopology.build();
    } else {
      log.info("Running high travel distance topology");
      topology = HighTravelDistanceTopology.build();
    }

    // load the Kafka Streams configuration
    Properties config = com.example.AppConfig.getKafkaStreamsConfig();

    // build the topology and start streaming!
    KafkaStreams streams = new KafkaStreams(topology, config);

    // close the Kafka Streams threads on shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    log.info("Starting Kafka Streams application");
    streams.start();
  }
}
