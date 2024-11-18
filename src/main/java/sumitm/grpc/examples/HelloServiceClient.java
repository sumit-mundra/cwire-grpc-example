package sumitm.grpc.examples;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This grpc client makes blocking unary calls to {@link GsonBasedServiceDefinition.SayHelloService}
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
     * Client will invoke blocking unary calls in a loop until duration is over.
     *
     * @param durationS duration in seconds
     * @throws InterruptedException if the client is interrupted
     */
    public void doTimedClientWork(long durationS) throws InterruptedException {
        long start = System.nanoTime();
        long durationUs = durationS * 1000000;
        while (((System.nanoTime() - start) / 1000) < durationUs) {
            sayHello(channel);
        }
        close();
    }

    private void close() {
        if (this.executorService != null && !this.executorService.isShutdown())
            this.executorService.shutdown();
    }

    private void sayHello(Channel chan) throws InterruptedException {
        limiter.acquire();
        ClientCall<GsonBasedServiceDefinition.HelloRequest, GsonBasedServiceDefinition.HelloResponse> call =
                chan.newCall(GsonBasedServiceDefinition.SAY_HELLO_METHOD, CallOptions.DEFAULT);
        GsonBasedServiceDefinition.HelloRequest req = new GsonBasedServiceDefinition.HelloRequest();
        req.setMessage(HOWDY_THERE);
        req.setDummyData(buildData());
        long startTime = System.nanoTime();
        req.setId(startTime);
        ClientCalls.blockingUnaryCall(call, req);
        limiter.release();
        final long response_time = System.nanoTime() - startTime;
        executorService.submit(() -> {
            this.latency.getAndAccumulate(response_time, Long::sum);
            rpcCount.incrementAndGet();
        });
    }

    private List<Long> buildData() {
        List<Long> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            list.add(rand.nextLong());
        }
        return list;
    }


}
