Exercise 2.4
===

### Task d)

1. accomplished by several `println` statements that have been overloaded to also print out the thread id\
  **note:** the sink could not be extended with println as it get generated and managed by File IO
2. Analysis of what happens if Generation Rate exceeds Persistence rate drastically (witnessed via log/print output)
   21. The implementation using only `map` and not `mapAsync` actually gets backed up to the source with messages due to 
   backpressure, which means that the source is not pumping out more messages, even though it would be allowed to.
   22. The implementation using `mapAsync` with `batch` allows the stream to be more flexible and wont back up so much 
   as the first implementation, it makes good use of backpressure by collecting batches of elements and using up to `coreCnt`
   threads to map the collections to `ByteStrings` (for use with FileIO).
   Due to this behaviour the Source doesnt get too clogged up by backpressure and is capable of sending messages a lot 
   quicker and more to schedule.
3. The stream based solution implicitly makes use of backpressure and wont get flooded with messages till it crashes or 
  can't handle any more messages. This means it would not loose messages unless the stream fails 
  (or is forced to stop like in the test via limit).\
  The actor based solution doesnt use backpressure in any form and therefor could crash or loose messages due to a full
  actor inbox.