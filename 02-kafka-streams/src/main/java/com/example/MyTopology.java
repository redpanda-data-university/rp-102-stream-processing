package com.example;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

class MyTopology {

  public static Topology build() {
    StreamsBuilder builder = new StreamsBuilder();

    builder.stream("purchases", Consumed.with(Serdes.String(), Serdes.String()))
        // group by the key
        .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
        // count per key
        .count()
        // convert the table to a stream
        .toStream()
        // print the output
        .print(Printed.<String, Long>toSysOut().withLabel("purchases"));

    return builder.build();
  }
}
