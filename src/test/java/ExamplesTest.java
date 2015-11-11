import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import com.spotify.futures.CompletableFuturesExtra;
import com.spotify.futures.FuturesExtra;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ExamplesTest {

  @Test
  public void testImmediateFuture() throws Exception {
    final ListenableFuture<String> hello1 = Futures.immediateFuture("hello");
    assertEquals("hello", hello1.get());

    final CompletableFuture<String> hello2 = CompletableFuture.completedFuture("hello");
    assertEquals("hello", hello2.get());

  }

  @Test
  public void testImmediateFailedFuture() throws Exception {
    final ListenableFuture<String> hello1 = Futures.immediateFailedFuture(new IllegalArgumentException("hello"));
    try {
      hello1.get();
      fail();
    } catch (ExecutionException e) {
      assertEquals("hello", e.getCause().getMessage());
    }

    final CompletableFuture<String> hello2 = CompletableFuturesExtra.exceptionallyCompletedFuture(new IllegalArgumentException("hello"));

    try {
      hello2.get();
      fail();
    } catch (ExecutionException e) {
      assertEquals("hello", e.getCause().getMessage());
    }
  }

  @Test
  public void testTransform() throws Exception {
    final ListenableFuture<String> result1 = FuturesExtra.syncTransform(Futures.immediateFuture("hello"), s -> s + s);
    assertEquals("hellohello", result1.get());

    final CompletableFuture<String> result2 = CompletableFuture.completedFuture("hello").thenApply(s -> s + s);
    assertEquals("hellohello", result2.get());
  }

  @Test
  public void testFallback() throws Exception {
    final ListenableFuture<String> result1 = Futures.withFallback(Futures.immediateFailedFuture(new IllegalArgumentException("hello")),
                                                                  t -> Futures.immediateFuture("recover"));
    assertEquals("recover", result1.get());

    final CompletableFuture<String> result2 = CompletableFuturesExtra.<String>exceptionallyCompletedFuture(new IllegalArgumentException("hello")).exceptionally(t -> "recover");
    assertEquals("recover", result2.get());
  }

  @Test
  public void testDeferredFallback() throws Exception {

    final ListenableFuture<String> failedFuture1 = Futures.immediateFailedFuture(new IllegalArgumentException("hello"));
    final SettableFuture<String> deferred1 = SettableFuture.create();
    final ListenableFuture<String> result1 = Futures.withFallback(failedFuture1, t -> deferred1);
    assertFalse(result1.isDone());
    deferred1.set("world");
    assertEquals("world", FuturesExtra.getCompleted(result1));

    final CompletableFuture<String> failedFuture2 = CompletableFuturesExtra.exceptionallyCompletedFuture(new IllegalArgumentException("hello"));
    final CompletableFuture<String> deferred2 = new CompletableFuture<>();
    final CompletionStage<String> result2 = CompletableFuturesExtra.exceptionallyCompose(failedFuture2, throwable -> deferred2);

    assertFalse(result2.toCompletableFuture().isDone());
    deferred2.complete("world");
    assertEquals("world", CompletableFuturesExtra.getCompleted(result2));
  }

  @Test
  public void testCombine() throws Exception {
    final ListenableFuture<String> f1 = Futures.immediateFuture("a");
    final ListenableFuture<String> f2 = Futures.immediateFuture("b");
    final ListenableFuture<String> f3 = Futures.immediateFuture("c");
    assertEquals("abc", FuturesExtra.syncTransform3(f1, f2, f3, (s1, s2, s3) -> s1 + s2 + s3).get());

    final CompletableFuture<String> g1 = CompletableFuture.completedFuture("a");
    final CompletableFuture<String> g2 = CompletableFuture.completedFuture("b");
    final CompletableFuture<String> g3 = CompletableFuture.completedFuture("c");
    assertEquals("abc", g1.thenCombine(g2, (s1, s2) -> s1 + s2).thenCombine(g3, (s12, s3) -> s12 + s3).get());
  }

  @Test
  public void testDereference() throws Exception {
    final ListenableFuture<ListenableFuture<String>> f = Futures.immediateFuture(Futures.immediateFuture("hello"));
    final ListenableFuture<String> result1 = Futures.dereference(f);
    assertEquals("hello", FuturesExtra.getCompleted(result1));

    final CompletableFuture<CompletableFuture<String>> g = CompletableFuture.completedFuture(CompletableFuture.completedFuture("hello"));

    final CompletionStage<String> result2 = CompletableFuturesExtra.dereference(g);
    assertEquals("hello", CompletableFuturesExtra.getCompleted(result2));

  }
}
