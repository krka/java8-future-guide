import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import com.spotify.futures.FuturesExtra;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
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

    final CompletableFuture<String> hello2 = immediateFailed(new IllegalArgumentException("hello"));

    try {
      hello2.get();
      fail();
    } catch (ExecutionException e) {
      assertEquals("hello", e.getCause().getMessage());
    }
  }

  private CompletableFuture<String> immediateFailed(IllegalArgumentException ex) {
    final CompletableFuture<String> hello2 = new CompletableFuture<>();
    hello2.completeExceptionally(ex);
    return hello2;
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

    final CompletableFuture<String> result2 = immediateFailed(new IllegalArgumentException("hello")).exceptionally(t -> "recover");
    assertEquals("recover", result2.get());
  }

  @Test
  public void testDeferredFallback() throws Exception {

    final ListenableFuture<String> result1 = Futures.withFallback(Futures.immediateFailedFuture(new IllegalArgumentException("hello")),
                                                                  t -> SettableFuture.create());
    assertFalse(result1.isDone());

    // Not sure how to do this in java8
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
}
