# Introduction

Java 8 introduced big improvements to its handling of futures.
There is now [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)
and [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) which are much better
adapted for asyncronous code compared to
the old [Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) interface.

This guide is mostly targetted at developers who want to start using the futures
in Java 8, but come from a [Google Guava](https://github.com/google/guava)
([ListenableFuture](https://github.com/google/guava/wiki/ListenableFutureExplained)) background.

# Subtle differences

Guava uses the terms `Function` and `AsyncFunction` where Async means that the
function returns a new future. (This means that all methods that use a regular
`Function` can be implemented with the method that takes an `AsyncFunction` and
wraps the value in a `Futures.immediateFuture(x)`.)

The equivalent of methods that take an `AsyncFunction` in Java 8 is
`thenCompose` (but that is only implemented for successful futures, not exceptions).

If you want to transform an exception by returning a different future you
have to use a workaround (see below).

There are Async variants of the methods for futures in Java 8 too, but
that means something completely different: the function or callback you pass in
will just be executed on a different thread.

# Guava -> Java 8 cheat sheet

This is a one way mapping. If you used to use Guava futures, you probably want to
do the java 8 equivalent. This does not mean that the reverse mapping works.

| Guava style | Java 8 style |
|---|---|
| [`Listenablefuture.addListener(callback)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/ListenableFuture.html#addListener%28java.lang.Runnable,%20java.util.concurrent.Executor%29) | [`future.whenComplete(callback)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#whenComplete-java.util.function.BiConsumer-) |
| [`Futures.addCallback(callback)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#addCallback%28com.google.common.util.concurrent.ListenableFuture,%20com.google.common.util.concurrent.FutureCallback%29) | [`future.whenComplete(callback)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#whenComplete-java.util.function.BiConsumer-) |
| [`Futures.transform(future, function)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#transform%28com.google.common.util.concurrent.ListenableFuture,%20com.google.common.base.Function%29) | [`future.thenApply(function)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#thenApply-java.util.function.Function-)  |
| [`Futures.transform(future, asyncFunction)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#transform%28com.google.common.util.concurrent.ListenableFuture,%20com.google.common.util.concurrent.AsyncFunction%29) | [`future.thenCompose(function)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#thenCompose-java.util.function.Function-)  |
| [`Futures.dereference(future)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#dereference%28com.google.common.util.concurrent.ListenableFuture%29) | `future.thenCompose(stage -> stage)`  |
| [`Futures.immediateFuture(value)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#immediateFuture%28V%29) | [`CompletableFuture.completedFuture(value)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#completedFuture-U-)  |
| [`Futures.immediateFailedFuture(throwable)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#immediateFailedFuture%28java.lang.Throwable%29) | `new CompletableFuture().completeExceptionally(throwable)`  |
| [`Futures.withFallback(future, function)`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/Futures.html#withFallback%28com.google.common.util.concurrent.ListenableFuture,%20com.google.common.util.concurrent.FutureFallback%29) | [`future.exceptionally(function)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html#exceptionally-java.util.function.Function-)  |
| [`SettableFuture.create()`](http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/util/concurrent/SettableFuture.html#create%28%29) | [`new CompletableFuture()`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html#CompletableFuture--)  |

`Futures.withFallback(future, asyncFunction)` can be mapped to
```java
stage
  .thenApply(v -> CompletableFuture.completedFuture(v))
  .exceptionally(asyncFunction)
  .thenCompose(stage -> stage)
```

# Futures-extra

There is a library called [futures-extra](https://github.com/spotify/futures-extra) which provides
more helpers for dealing with both Guava and Java 8 futures.

Particularly for Java 8, there are  some useful helper methods.
CFE here is short hand for CompletableFuturesExtra.
```java

// convert a java 8 future to a guava future
ListenableFuture<T> output = CFE.toListenableFuture(CompletionStage<T> input);

// convert a guava future to a java 8 future
CompletableFuture<T> output = CFE.toCompletableFuture(ListenableFuture<T> input); 

// equivalent to Futures.immediateFailedFuture(throwable) in guava
CompletableFuture<T> output = CFE.exceptionallyCompletedFuture(throwable); 

// equivalent to Futures.withFallback(Futures.transform(future, successAsyncFunction), failureAsyncFunction)
// implemented as CFE.dereference(stage.thenCompose(function))
CompletionStage<T> output = CFE.handleCompose(stage, function);

// equivalent to Futures.withFallback(failureAsyncFunction)
// implemented as CFE.dereference(CFE.wrap(stage.thenCompose(function))
CompletionStage<T> output = CFE.exceptionallyCompose(stage, function);

// equivalent to FuturesExtra.checkCompleted(future);
// throws IllegalStateException if stage is not completed.
CFE.checkCompleted(stage);

// equivalent to FuturesExtra.getCompleted(future);
// returns the output if the stage is completed, otherwise throws exception.
// unlike variants of future.get(), this never blocks.
T output = CFE.getCompleted(stage);

// equivalent to Futures.dereference(future)
// implemented as stage.thenCompose(stage -> stage)
CompletionStage<T> output = CFE.dereference(CompletionStage<CompletionStage<T>> stage);
```

### TODO: document allAsList / successfulAsList
### TODO: document some of the things in futures-extra

