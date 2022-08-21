const transform = require("../src/main");
const { createRecordBatch } = require("@vectorizedio/wasm-api");
const assert = require("assert");

const input = { "user_id": 123, "card_number": "1111222233334444" }
const expected = { "user_id": 123, "card_number": "************4444" }

const record = createRecordBatch({ records: [{ value: Buffer.from(JSON.stringify(input)) }] });

describe("transform", () => {
  it("should apply function", function () {
    // apply the transform
    return transform.default.apply(record)
      .then((resultApply) => {
        // for each of the output records...
        resultApply.get('masked')['records'].forEach(record => {
          // deserialize the output value
          value = JSON.parse(record.value.toString());
          // compare with the expected output
          assert.deepStrictEqual(value, expected)
        })
      })
  });
});