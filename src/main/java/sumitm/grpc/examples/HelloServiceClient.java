package sumitm.grpc.examples;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class HelloServiceClient {
    private static final String HOWDY_THERE = "Howdy there";
    private final Semaphore limiter;
    private final ManagedChannel channel;
    private final AtomicLong rpcCount = new AtomicLong();
    private final AtomicLong latency = new AtomicLong(0);

    public HelloServiceClient(ManagedChannel channel, int permits) {
        this.channel = channel;
        this.limiter = new Semaphore(permits);
    }

    public AtomicLong getLatency() {
        return latency;
    }

    public void doClientWork(AtomicBoolean done) throws InterruptedException {
        while (!done.get()) {
            sayHello(channel);
        }
    }

    private void sayHello(Channel chan)
            throws InterruptedException {
        limiter.acquire();
        ClientCall<ChronicleWireServiceDefinition.HelloRequest, ChronicleWireServiceDefinition.HelloResponse> call =
                chan.newCall(ChronicleWireServiceDefinition.SAY_HELLO_METHOD, CallOptions.DEFAULT);
        ChronicleWireServiceDefinition.HelloRequest req = new ChronicleWireServiceDefinition.HelloRequest();
        long startTime = System.nanoTime();
        req.setId(startTime);
        req.setMessage(HOWDY_THERE);
        ChronicleWireServiceDefinition.HelloResponse response = ClientCalls.blockingUnaryCall(call, req);
        latency.getAndAccumulate(System.nanoTime() - startTime, (a, b) -> a + b);
//    latency.set(latency.getAndAdd(System.currentTimeMillis() - req.getId()));
        rpcCount.incrementAndGet();
        limiter.release();
//        ListenableFuture<ChronicleWireServiceDefinition.HelloResponse> res = ClientCalls.futureUnaryCall(call, req);
//        res.addListener(() -> {
//            rpcCount.incrementAndGet();
//            limiter.release();
//        }, MoreExecutors.directExecutor());
    }

    public AtomicLong getRpcCount() {
        return this.rpcCount;
    }
}
