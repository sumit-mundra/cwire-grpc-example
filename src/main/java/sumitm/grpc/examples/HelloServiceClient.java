package sumitm.grpc.examples;

import io.grpc.Channel;
import io.grpc.ManagedChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public HelloServiceClient(ManagedChannel channel, int permits) {
        this.channel = channel;
        this.limiter = new Semaphore(permits);
        this.executorService = Executors.newFixedThreadPool(100);
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
     * @param done A boolean switch which toggles the infinite client execution
     * @throws InterruptedException if interrupted in between
     */
    public void doClientWork(AtomicBoolean done) throws InterruptedException {
        while (!done.get()) {
            sayHello(channel);
        }
        close();
    }

    private void close() {
        if (this.executorService != null && !this.executorService.isShutdown())
            this.executorService.shutdown();
    }

    private void sayHello(Channel chan)
            throws InterruptedException {
        limiter.acquire();
        long startTime = System.nanoTime();
        HelloRequest request = HelloRequest.newBuilder()
                .setId(startTime)
                .setMessage(HOWDY_THERE)
                .build();
        SayHelloServiceGrpc.newBlockingStub(chan).sayHello(request);
        limiter.release();
        final long response_time = System.nanoTime() - startTime;
        executorService.submit(() -> {
            this.latency.getAndAccumulate(response_time, Long::sum);
            rpcCount.incrementAndGet();
        });
    }


}
