package com.example;

import com.example.model.Alert;
import com.example.model.Purchase;
import com.example.model.TravelFlags;
import com.example.serialization.avro.AvroSerdes;
import java.util.Collections;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fraud detection tutorial
 *
 * <p>Detects high travel distances with Redpanda + Kafka Streams
 *
 * <p>Run with: mvn clean compile exec:java -Dexec.mainClass="com.example.App"
 * -Dexec.args="high_travel_distance"
 */
class HighTravelDistanceTopology {
  private static final Logger log = LoggerFactory.getLogger(HighTravelDistanceTopology.class);
  private static final String REDPANDA_SCHEMA_REGISTRY_URL = AppConfig.getSchemaRegistryUrl();
  private static final Long HIGH_TRAVEL_DISTANCE_THRESHOLD = 2000L;

  // we'll represent joined purchase and travel flag data using this class
  static class JoinValue {
    public Purchase purchase;
    public boolean travelFlagOn;

    JoinValue(Purchase purchase, boolean travelFlagOn) {
      this.purchase = purchase;
      this.travelFlagOn = travelFlagOn;
    }
  }

  public static Topology build() {
    // Instantiate a streams builder
    StreamsBuilder builder = new StreamsBuilder();

    // Consumer options for the purchases topic
    Consumed<String, Purchase> purchaseConsumerOptions =
        Consumed.with(Serdes.String(), AvroSerdes.Purchase(REDPANDA_SCHEMA_REGISTRY_URL));

    // Consumer options for the travel_flags topic
    Consumed<String, TravelFlags> travelFlagsConsumerOptions =
        Consumed.with(Serdes.String(), AvroSerdes.TravelFlags(REDPANDA_SCHEMA_REGISTRY_URL));

    // Stream data from the purchases topic
    KStream<String, Purchase> purchases = builder.stream("purchases", purchaseConsumerOptions);

    // Stream data from the travel flags topic into a table
    KTable<String, TravelFlags> travelFlags =
        builder.table("travel_flags", travelFlagsConsumerOptions);

    Joined joinParams =
        Joined.with(
            Serdes.String(), /* key */
            AvroSerdes.Purchase(REDPANDA_SCHEMA_REGISTRY_URL), /* left value */
            AvroSerdes.TravelFlags(REDPANDA_SCHEMA_REGISTRY_URL) /* right value */);

    KStream<String, JoinValue> joined =
        purchases
            .filter(
                (key, value) -> value.getDistanceFromBillingZip() >= HIGH_TRAVEL_DISTANCE_THRESHOLD)
            .leftJoin(travelFlags, (left, right) -> new JoinValue(left, right != null), joinParams);

    KStream<String, Alert> alerts =
        joined.flatMapValues(
            (value) -> {
              Purchase purchase = value.purchase;
              String creditCard = purchase.getCreditCard().toString();
              Double distance = purchase.getDistanceFromBillingZip();

              log.info(
                  "Credit card {} has purchase activity {} miles from billing zip code",
                  creditCard,
                  distance);

              if (!value.travelFlagOn) {
                // the join failed, which means there is no travel flag
                log.info("Generating alert because travel flag is OFF for {}", creditCard);

                Alert alert =
                    Alert.newBuilder()
                        .setTimestamp(purchase.getTimestamp().toEpochMilli())
                        .setCreditCard(purchase.getCreditCard().toString())
                        .setType(PurchaseAlerts.ALERT_TRAVEL_DISTANCE)
                        .build();
                return Collections.singleton(alert);
              }
              log.info("NOT generating alert because travel flag is ON for {}", creditCard);
              return Collections.emptyList();
            });

    // Produce the alerts to the Avro topic
    // Note: the Alert schema will automatically be saved to Redpanda Schema Registry
    alerts.to(
        "alerts-avro",
        Produced.with(Serdes.String(), AvroSerdes.Alert(REDPANDA_SCHEMA_REGISTRY_URL)));

    // Produce the alerts to the JSON topic
    // This was primarily created to help students view the output of this
    // application in a human readable format :)
    alerts
        .mapValues((alert) -> alert.toString())
        .to("alerts-json", Produced.with(Serdes.String(), Serdes.String()));

    // Build and return the topology instance
    return builder.build();
  }
}
