#! /bin/bash

alias rpk="docker exec -ti redpanda-1 rpk"

# create the source topics
rpk topic create purchases --partitions 4 --replicas 1
rpk topic create travel_flags --partitions 4 --replicas 1 --topic-config cleanup.policy=compact

# create the sink topics
rpk topic create alerts-avro --partitions 4 --replicas 1
rpk topic create alerts-json --partitions 4 --replicas 1

# create the purchases connector
docker exec -ti kafka-connect \
    curl -XPUT localhost:8083/connectors/purchases-source-connector/config \
    -H "Content-Type: application/json" \
    -d '
    {
     "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
     "connection.url": "jdbc:postgresql://postgres:5432/root?user=root&password=secret",
     "mode": "incrementing",
     "incrementing.column.name": "id",
     "topic.prefix": "",
     "table.whitelist": "purchases",
     "value.converter": "io.confluent.connect.avro.AvroConverter",
     "value.converter.schema.registry.url": "http://redpanda:8081",
     "transforms": "SetValueSchema,ValueToKey,ExtractKey",
     "transforms.SetValueSchema.type": "org.apache.kafka.connect.transforms.SetSchemaMetadata$Value",
     "transforms.SetValueSchema.schema.name": "com.example.model.Purchase",
     "transforms.ValueToKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
     "transforms.ValueToKey.fields": "credit_card",
     "transforms.ExtractKey.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
     "transforms.ExtractKey.field": "credit_card"
    }'

# create the travel flags connector
docker exec -ti kafka-connect \
    curl -XPUT localhost:8083/connectors/travel-flags-source-connector/config \
    -H "Content-Type: application/json" \
    -d '
    {
     "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
     "connection.url": "jdbc:postgresql://postgres:5432/root?user=root&password=secret",
     "mode": "incrementing",
     "incrementing.column.name": "id",
     "topic.prefix": "",
     "table.whitelist": "travel_flags",
     "value.converter": "io.confluent.connect.avro.AvroConverter",
     "value.converter.schema.registry.url": "http://redpanda:8081",
     "transforms": "SetValueSchema,ValueToKey,ExtractKey",
     "transforms.SetValueSchema.type": "org.apache.kafka.connect.transforms.SetSchemaMetadata$Value",
     "transforms.SetValueSchema.schema.name": "com.example.model.TravelFlags",
     "transforms.ValueToKey.type": "org.apache.kafka.connect.transforms.ValueToKey",
     "transforms.ValueToKey.fields": "credit_card",
     "transforms.ExtractKey.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
     "transforms.ExtractKey.field": "credit_card"
    }'


# get the Avro schemas that were auto-generated by the connectors
curl -X GET http://localhost:8081/subjects/purchases-value/versions/1 | jq -r '.schema' | jq '.' > src/main/avro/purchase.avsc
curl -X GET http://localhost:8081/subjects/travel_flags-value/versions/1 | jq -r '.schema' | jq '.' > src/main/avro/travel_flags.avsc

# compile
mvn clean compile
