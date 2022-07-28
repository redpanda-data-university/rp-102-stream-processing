package com.example;

import com.example.model.Alert;
import com.example.model.Purchase;
import com.example.serialization.avro.AvroSerdes;
import java.time.Duration;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fraud detection tutorial
 *
 * <p>Detects high purchase counts with Redpanda + Kafka Streams
 *
 * <p>Run with: mvn clean compile exec:java -Dexec.mainClass="com.example.App"
 */
class HighPurchaseCountTopology {
  private static final Logger log = LoggerFactory.getLogger(HighPurchaseCountTopology.class);
  private static final String REDPANDA_SCHEMA_REGISTRY_URL = AppConfig.getSchemaRegistryUrl();
  private static final Long HIGH_PURCHASE_THRESHOLD = 3L;

  public static Topology build() {
    // Instantiate a streams builder
    StreamsBuilder builder = new StreamsBuilder();

    // Set the consumer options on the source topic. Specifically, we'll:
    // 1. deserialize the keys as strings
    // 2. deserialize the values as Purchase objects
    // Note: The Purchase class is auto-generated from the purchase schema
    // that is located in the Redpanda Schema Registry.
    Consumed<String, Purchase> purchaseConsumerOptions =
        Consumed.with(Serdes.String(), AvroSerdes.Purchase(REDPANDA_SCHEMA_REGISTRY_URL))
            .withTimestampExtractor(new PurchaseTimestampExtractor());

    // Stream data from the purchases topic
    KStream<String, Purchase> purchases = builder.stream("purchases", purchaseConsumerOptions);

    // We want to detect a high number of purchases within a 60 second time
    // window, so we'll create the window here.
    TimeWindows tumblingWindow = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(60));

    // Perform a windowed aggregation
    KStream<Windowed<String>, Long> windowedCounts =
        purchases
            // Tell Kafka Streams how to group related records. Since we configured the
            // the credit_card value as the key in Kafka Connect, we can simply group by key
            .groupByKey()
            // Create a windowed aggregation using the windowedBy + count operator
            .windowedBy(tumblingWindow)
            // Count the number of purchases within the window
            .count()
            // Aggregations return a table object, so convert the table back to a
            // stream since we're going to write this out to an alerts topic
            .toStream();
          

    // Detect high purchase counts and generate alerts
    KStream<String, Alert> alerts  =
        // Use the peek operator to inspect each result and log some useful
        // information. This is optional but it's useful for development purposes
        windowedCounts.peek(
            (key, value) -> {
              String id = new String(key.key());
              Long start = key.window().start();
              Long end = key.window().end();
              log.info(
                  "Credit card {} had a purchase count of {} between {}" + " and {}",
                  id,
                  value,
                  start,
                  end);
            })
        // Look for high purchase counts within the time window
        .filter((key, value) -> value == HIGH_PURCHASE_THRESHOLD)
        // We now have a stream of high purchase counts. Create alerts so that
        // we can notify downstream systems
        .map(
            (windowedKey, value) -> {
              // Extract the credit card from the record key
              String creditCard = new String(windowedKey.key());
              // Create an Alert object
              Alert alert =
                  Alert.newBuilder()
                      .setTimestamp(windowedKey.window().start())
                      .setCreditCard(creditCard)
                      .setType(PurchaseAlerts.ALERT_HIGH_PURCHASE_COUNT)
                      .build();
              return KeyValue.pair(creditCard, alert);
            });

    // Save the alerts. Will have two destination topics:
    // 1. alerts-avro (contains the alerts stored in Avro format)
    // 2. alerts-json (contains the alerts stored in JSON format)

    // Produce the alerts to the Avro topic
    // Note: the Alert schema will automatically be saved to Redpanda Schema Registry
    alerts.to(
        "alerts-avro",
        Produced.with(Serdes.String(), AvroSerdes.Alert(REDPANDA_SCHEMA_REGISTRY_URL)));

    // Produce the alerts to the JSON topic
    // This was primarily created to help students view the output of this
    // application in a human readable format :)
    alerts
        // coverting the alerts to a string will generate a JSON representation of the object
        .mapValues((alert) -> alert.toString())
        // write to the output topic using String serializers
        .to(
          "alerts-json",
          Produced.with(Serdes.String(), Serdes.String()));

    // Build and return the topology instance
    return builder.build();
  }
}
