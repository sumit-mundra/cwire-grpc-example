package sumitm.grpc.examples;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HelloServiceClient {
    private static final String HOWDY_THERE = "Howdy there";
    private final Semaphore limiter;
    private final ManagedChannel channel;
    private final AtomicLong rpcCount = new AtomicLong();

    public HelloServiceClient(ManagedChannel channel, int permits) {
        this.channel = channel;
        this.limiter = new Semaphore(permits);
    }

    public void doClientWork(AtomicBoolean done) throws InterruptedException {
        AtomicReference<Throwable> errors = new AtomicReference<>();

        while (!done.get() && errors.get() == null) {
            sayHello(channel);
        }
        if (errors.get() != null) {
            throw new RuntimeException(errors.get());
        }

    }


    private void sayHello(Channel chan)
            throws InterruptedException {
        limiter.acquire();
        ClientCall<ChronicleWireServiceDefinition.HelloRequest, ChronicleWireServiceDefinition.HelloResponse> call =
                chan.newCall(ChronicleWireServiceDefinition.SAY_HELLO_METHOD, CallOptions.DEFAULT);
        ChronicleWireServiceDefinition.HelloRequest req = new ChronicleWireServiceDefinition.HelloRequest();
        req.setId(System.currentTimeMillis());
        req.setMessage(HOWDY_THERE);
        ListenableFuture<ChronicleWireServiceDefinition.HelloResponse> res = ClientCalls.futureUnaryCall(call, req);
        res.addListener(() -> {
            rpcCount.incrementAndGet();
            limiter.release();
        }, MoreExecutors.directExecutor());
    }

    public AtomicLong getRpcCount() {
        return this.rpcCount;
    }
}
