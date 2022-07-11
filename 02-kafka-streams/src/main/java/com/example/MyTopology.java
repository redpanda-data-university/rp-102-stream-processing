package com.example;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

class MyTopology {

  public static Topology build() {
    StreamsBuilder builder = new StreamsBuilder();

    KStream<byte[], byte[]> stream = builder.stream("purchases");
    stream.print(Printed.<byte[], byte[]>toSysOut().withLabel("purchases"));

    return builder.build();
  }
}
