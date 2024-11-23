package sumitm.grpc.examples;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Channel;
import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This grpc client makes blocking unary calls to {@link SayHelloServiceGrpc.SayHelloServiceImplBase}
 * The limiter blocks the client if max concurrent executions limit (@link permits) is reached
 */
public class HelloServiceClient {
    private static final String HOWDY_THERE = "Howdy there";
    private final Semaphore limiter;
    private final ManagedChannel channel;
    private final AtomicLong rpcCount = new AtomicLong();
    private final AtomicLong latency = new AtomicLong(0);
    private final ExecutorService executorService;
    private final Random rand = new Random(100);

    public HelloServiceClient(ManagedChannel channel, int permits) {
        this.channel = channel;
        this.limiter = new Semaphore(permits);
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * @return sum of observed server latency in nanoseconds until now
     */
    public AtomicLong getLatency() {
        return latency;
    }

    /**
     * @return observed sum of rpc count until now
     */
    public AtomicLong getRpcCount() {
        return this.rpcCount;
    }

    /**
     * Client will invoke blocking unary calls in a loop until duration is over.
     *
     * @param durationS duration in seconds
     * @throws InterruptedException if the client is interrupted
     */
    public void doTimedClientWork(long durationS) throws InterruptedException, ExecutionException {
        long start = System.nanoTime();
        long durationUs = durationS * 1000000;
        List<ListenableFuture<HelloResponse>> list = new ArrayList<>();
        while (((System.nanoTime() - start) / 1000) < durationUs) {
            list.add(sayHello(channel));
        }
        for (ListenableFuture<HelloResponse> helloResponseListenableFuture : list) {
            helloResponseListenableFuture.get();
        }
        close();
    }

    private void close() {
        if (this.executorService != null && !this.executorService.isShutdown())
            this.executorService.shutdown();
    }

    private ListenableFuture<HelloResponse> sayHello(Channel chan)
            throws InterruptedException, ExecutionException {
        limiter.acquire();
        long startTime = System.nanoTime();
        HelloRequest.Builder requestBuilder = HelloRequest.newBuilder()
                                                      .setId(startTime)
                                                      .setMessage(HOWDY_THERE);
        buildData(requestBuilder);
//        SayHelloServiceGrpc.newBlockingStub(chan).sayHello(requestBuilder.build());
        ListenableFuture<HelloResponse> future = SayHelloServiceGrpc.newFutureStub(chan).sayHello(requestBuilder.build());
        future.addListener(
                () -> {
                    final long response_time = System.nanoTime() - startTime;
                    this.latency.getAndAccumulate(response_time, Long::sum);
                    rpcCount.incrementAndGet();
                    limiter.release();
                }, executorService
        );
        return future;
    }

    private void buildData(HelloRequest.Builder builder) {
        for (int i = 0; i < 1000; i++) {
            builder.addDummydata(rand.nextLong());
        }
    }


}
