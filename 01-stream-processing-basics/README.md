# Redpanda Data Transforms tutorial
In Chapter 1 of the [Hands-on Redpanda: Stream Processing course][course-link], we demonstrate how to build a stateless stream processing application with Redpanda Data Transforms. The transform does the following:

- Reads from a topic called `purchases`
- Masks sensitive credit card information in the input records
- Writes the masked records to a new output topic: `purchases._masked_`

The full tutorial is contained in the [course][course-link], and the transform code can be viewed in the [01-stream-processing-basics
/transforms/mask-card-complete/][mask-card] directory in this repo.


[course-link]: https://university.redpanda.com/courses/hands-on-redpanda-stream-processing
[mask-card]: /01-stream-processing-basics/transforms/mask-card-complete/
