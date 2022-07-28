package com.example.serialization.avro;

import com.example.model.Alert;
import com.example.model.Purchase;
import com.example.model.TravelFlags;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;

public class AvroSerdes {

  public static Serde<Alert> Alert(String url) {
    Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", url);
    Serde<Alert> serde = new SpecificAvroSerde<>();
    serde.configure(serdeConfig, false);
    return serde;
  }

  public static Serde<Purchase> Purchase(String url) {
    Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", url);
    Serde<Purchase> serde = new SpecificAvroSerde<>();
    serde.configure(serdeConfig, false);
    return serde;
  }

  public static Serde<TravelFlags> TravelFlags(String url) {
    Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url", url);
    Serde<TravelFlags> serde = new SpecificAvroSerde<>();
    serde.configure(serdeConfig, false);
    return serde;
  }
}
