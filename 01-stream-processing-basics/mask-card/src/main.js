const {
  SimpleTransform,
  PolicyError,
  PolicyInjection
 } = require("@vectorizedio/wasm-api");
 
 // instantiate a class for registering the transform
 const transform = new SimpleTransform();
 
 // subscribe to the source topic, and read from the Earliest offset
 transform.subscribe([["purchases", PolicyInjection.Earliest]]);
 
 // Set the error handling strategy. In this case, we will skip an event
 // if the transform fails
 transform.errorHandler(PolicyError.SkipOnFailure);
 
 // Create a stateless transform function that masks credit card numbers
 const maskCreditCardNumbers = (record) => {
 
  // The record value comes in as a byte string, so deserialize
  // this to a JSON object
  value = JSON.parse(record.value.toString());
 
  // If the record has a card number, mask the card number
  // i.e. 1111222233334444 will become ************4444
  if (value.hasOwnProperty('card_number')) {
    value['card_number'] = value['card_number'].replace(/\d(?=\d{4})/g, "*")
  }
 
  // In order to write back to Redpanda, we need to serialize the new
  // record back into a byte array
  buf = Buffer.from(JSON.stringify(value))
 
  const newRecord = {
    ...record,
    value: buf,
  };
  return newRecord;
 }
 
 // Register the processRecord handler, which will invoke our card masking
 // function on a batch of records
 transform.processRecord((recordBatch) => {
  const result = new Map();
  const transformedRecord = recordBatch.map(({ header, records }) => {
    return {
      header,
      records: records.map(maskCreditCardNumbers),
    };
  });
 
  // Write the result to a new output topic
  result.set("masked", transformedRecord);
 
  // processRecord function returns a Promise
  return Promise.resolve(result);
 });
 
 exports["default"] = transform;
 