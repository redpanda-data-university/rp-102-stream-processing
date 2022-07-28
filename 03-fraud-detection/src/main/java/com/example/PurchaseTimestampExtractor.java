package com.example;

import com.example.model.Purchase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/** This class allows us to use event-time semantics for purchase streams */
public class PurchaseTimestampExtractor implements TimestampExtractor {

  @Override
  public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
    Purchase purchase = (Purchase) record.value();
    if (purchase != null && purchase.getTimestamp() != null) {
      return purchase.getTimestamp().toEpochMilli();
    }
    // fallback to stream time
    return partitionTime;
  }
}
