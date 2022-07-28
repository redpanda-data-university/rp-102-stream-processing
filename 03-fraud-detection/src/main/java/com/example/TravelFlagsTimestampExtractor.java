package com.example;

import com.example.model.TravelFlags;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/** This class allows us to use event-time semantics for purchase streams */
public class TravelFlagsTimestampExtractor implements TimestampExtractor {

  @Override
  public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
    TravelFlags travelFlags = (TravelFlags) record.value();
    if (travelFlags != null && travelFlags.getCreatedAt() != null) {
      return travelFlags.getCreatedAt().toEpochMilli();
    }
    // fallback to stream time
    return partitionTime;
  }
}
