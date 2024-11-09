package sumitm.grpc.examples;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This grpc client makes blocking unary calls to {@link ChronicleWireServiceDefinition.SayHelloService}
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
        ClientCall<ChronicleWireServiceDefinition.HelloRequest, ChronicleWireServiceDefinition.HelloResponse> call =
                chan.newCall(ChronicleWireServiceDefinition.SAY_HELLO_METHOD, CallOptions.DEFAULT);
        ChronicleWireServiceDefinition.HelloRequest req = new ChronicleWireServiceDefinition.HelloRequest();
        req.setMessage(HOWDY_THERE);
        long startTime = System.nanoTime();
        req.setId(startTime);
        ClientCalls.blockingUnaryCall(call, req);
        limiter.release();
        final long response_time = System.nanoTime() - startTime;
        executorService.submit(() -> {
            this.latency.getAndAccumulate(response_time, Long::sum);
            rpcCount.incrementAndGet();
        });
//        ListenableFuture<ChronicleWireServiceDefinition.HelloResponse> res = ClientCalls.futureUnaryCall(call, req);
//        limiter.release();
//        res.addListener(() -> {
//            long latency = System.nanoTime() - startTime;
//            this.latency.getAndAccumulate(latency, Long::sum);
//            rpcCount.incrementAndGet();
//        }, MoreExecutors.directExecutor());
//        return res;
    }


}
