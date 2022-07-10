# Redpanda + Kafka Connect tutorial
In Chapter 2 of the [Hands-on Redpanda: Stream Processing course][course-link], we demonstrate how to use Kafka Connect with Redpanda. Specifically, we do the following:

- Setup a source connector to read data from Postgres, and write it to a _purchases_ topic in Redpanda
- Create a Single Message Transform (SMT) to read data from the _purchases_ topic, and mask the credit card field
- Setup a sink connector to write the masked _purchases_ data from Redpanda to Elasticsearch

The full tutorial is contained in the [course][course-link].

[course-link]: https://university.redpanda.com/courses/hands-on-redpanda-stream-processing

A condensed form of the setup / deployment is shown below.

# Setup

1. Download and unzip the source and sink Connector JARs, and place them in the `data/connect-jars/` directory.
    - [JDBC Connector (Source and Sink)][jdbc-source-dl]
    - [ElasticSearch Sink Connector][es-sink-dl]
    ```sh
      .
    ├── docker-compose.yml
    ├── data
    │   └── connect-jars
    │       ├── confluentinc-kafka-connect-elasticsearch-13.1.0
    │       └── confluentinc-kafka-connect-jdbc-10.5.1

    ```
2. Start the Redpanda, Kafka Connect, Postgres, and Elasticsearch containers
   ```sh
   docker-compose up -d
   ```
3. Setup the `rpk` alias
   ```sh
   alias rpk="docker exec -ti redpanda-1 rpk"
   ```
4. Install the JDBC source connector:
   ```sh
    docker exec -ti kafka-connect \
      curl localhost:8083 -XPUT localhost:8083/connectors/postgres-source-connector/config \
      -H "Content-Type: application/json" \
      -d '
      {
       "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
       "connection.url": "jdbc:postgresql://postgres:5432/root?user=root&password=secret",
       "mode": "incrementing",
       "incrementing.column.name": "id",
       "topic.prefix": "",
       "table.whitelist": "purchases",
       "value.converter": "org.apache.kafka.connect.json.JsonConverter",
       "key": "id"
      }'
    ```

5. Ensure the data was written from Postgres to Redpanda
    ```sh
    rpk topic consume purchases
    ```
    
6. Install the Elasticsearch sink connector
  ```sh
  docker exec -ti kafka-connect \
    curl -XPUT localhost:8083/connectors/elasticsearch-source-connector/config \
    -H "Content-Type: application/json" \
    -d '
    {
     "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
     "connection.url": "http://elasticsearch:9200",
     "connection.username": "",
     "connection.password": "",
     "batch.size": "1",
     "key.ignore": "true",
     "value.converter": "org.apache.kafka.connect.json.JsonConverter",
     "write.method": "insert",
     "topics": "purchases",
     "key": "id"
    }'
  ```
  
 7. Ensure the data was written to Elasticsearch
  ```sh
  docker exec -ti elasticsearch \
      curl -XGET 'localhost:9200/purchases/_search?format=json&pretty'
  ```

8. Add a Single Message Transform (SMT) to mask / redact the credit card field:
  ```sh
  docker exec -ti kafka-connect \
    curl -XPUT localhost:8083/connectors/elasticsearch-source-connector/config \
    -H "Content-Type: application/json" \
    -d '
    {
     "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
     "connection.url": "http://elasticsearch:9200",
     "connection.username": "",
     "connection.password": "",
     "batch.size": "1",
     "key.ignore": "true",
     "value.converter": "org.apache.kafka.connect.json.JsonConverter",
     "write.method": "insert",
     "topics": "purchases",
     "key": "id",
     "transforms": "mask",
     "transforms.mask.type": "org.apache.kafka.connect.transforms.MaskField$Value",
     "transforms.mask.fields": "credit_card",
     "transforms.mask.replacement": "REDACTED"
    }'
  ```
  
9. Insert a new record into Postgres:

  ```sh
  docker exec -ti postgres \
    psql -c "INSERT INTO purchases (category, amount, credit_card) values ('Entertainment', 400.00, '1111-2222-3333-4444')"
```

10. Finally, ensure the credit card field was redacted from the new record in Elasticsearch:

  ```sh
  docker exec -ti elasticsearch \
  curl -XGET 'localhost:9200/purchases/_search?format=json&pretty'
  ```
  
  The old records that were added before the SMT will still show the card information. The new record should show `REDACTED`:
  ```json
  {
    "_index": "purchases",
    "_type": "_doc",
    "_id": "purchases+0+3",
    "_score": 1,
    "_source": {
      "id": 4,
      "category": "Entertainment",
      "amount": 400,
      "credit_card": "REDACTED",
      "timestamp": 1657402394064
    }
  }
  ```


[jdbc-source-dl]: https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc?_ga=2.43948069.163077205.1657245042-645359300.1645061420
[es-sink-dl]: https://www.confluent.io/hub/confluentinc/kafka-connect-elasticsearch?_ga=2.216546198.163077205.1657245042-645359300.1645061420
