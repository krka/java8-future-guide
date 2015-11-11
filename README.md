# Introduction

Java 8 introduced big improvements to its handling of futures.
There is now CompletionStage and CompletableFuture which are much better
adapted for asyncronous code compared to the old Future class.

This guide is mostly targetted to developers who want to start using the futures
in Java 8, but are mostly used to working with
futures in Google Guava (ListenableFuture).

# Subtle differences

Guava uses the terms `Function` and `AsyncFunction` where Async means that the
function returns a new future. This means that all methods that use a regular
`Function` can be implemented with the method that takes an `AsyncFunction` and
wraps the value in a `Futures.immediateFuture(x)`.

The equivalent of methods that take an `AsyncFunction` in Java 8 is
`thenCompose` (but that is only implemented for successful futures).

If you want to transform an exception by returning a different future you
have to use a workaround (see below).

There are Async variants of the methods for futures in Java 8 too, but
that means something completely different: the function or callback you pass in
will just be executed on a different thread.

# Guava -> Java 8 cheat sheet

This is one way mapping. If you used to use Guava futures, you probably want to
do the java 8 equivalent. This does not mean that the reverse mapping works.

| Guava style | Java 8 style |
|---|---|
| `Listenablefuture.addListener` | `future.whenComplete` |
| `Futures.addCallback` | `future.whenComplete` |
| `Futures.transform(future, function)` | `future.thenApply(function)`  |
| `Futures.transform(future, asyncFunction)` | `future.thenCompose(function)`  |
| `Futures.dereference(future)` | `future.thenCompose(t -> t)`  |
| `Futures.immediateFuture(value)` | `CompletableFuture.completedFuture(value)`  |
| `Futures.immediateFailedFuture(throwable)` | `new CompletableFuture().completeExceptionally(throwable)`  |
| `Futures.withFallback(future, function)` | `future.exceptionally(function)`  |
| `SettableFuture.create()` | `new CompletableFuture()`  |

`Futures.withFallback(future, asyncFunction)` can be mapped to
```java
future
  .thenApply(v -> CompletableFuture.completedFuture(v))
  .exceptionally(asyncFunction)
  .thenCompose(t -> t)
```

# TODO: document allAsList / successfulAsList
# TODO: document some of the things in futures-extra
