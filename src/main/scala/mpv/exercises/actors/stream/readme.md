Exercise 2.3
===

### Task e)

1. accomplished by several `println` statements that have been overloaded to also print out the thread id
2. simple, if the amount of measurements drastically exceeds the amount which can be persisted you will end up with
 either a lot of measurements lost at system termination (like in the test suite due to the hard time limit on system up-time)
 or the actor system getting flooded with so many messages that it crashes or fails to send more to the actors full mailbox.
3. Ways to mitigate the problem:
   31. realize some way to propagate backpressure to the weather station, so it won't flood the StorageActors mail box 
   (requires the source to react on backpressure as well)
   32. adjust the parameters for the storage actor (would have more of an effect for the `BufferedStorageActors`)