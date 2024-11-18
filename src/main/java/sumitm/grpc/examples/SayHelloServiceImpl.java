package sumitm.grpc.examples;

import io.grpc.stub.StreamObserver;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A sample {@link SayHelloServiceGrpc.SayHelloServiceImplBase SayHelloServiceImpl} service that simulates a delay and responds to grpc stream with simple reply
 */
public class SayHelloServiceImpl extends SayHelloServiceGrpc.SayHelloServiceImplBase {

    private static final String HI_BACK = "Hi ! back";
    final private int delay;
    final private Random random = new Random(10);

    /**
     * @param delay duration in microseconds
     */
    public SayHelloServiceImpl(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return this.delay;
    }

    private void delay() {
        if (getDelay() == 0) {
            return;
        }
        try {
            TimeUnit.MICROSECONDS.sleep(getDelay());
        } catch (
                InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Implements a thread sleep delay, builds a response to send via provided grpc stream observer
     * throws {@code RuntimeException} if interrupted during delay
     *
     * @param request          input request
     * @param responseObserver grpc observer
     */
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        delay(); // simulating backend latency
        HelloResponse.Builder resBuilder = HelloResponse.newBuilder();
        resBuilder.setReply(HI_BACK).setId(request.getId());
        buildPayload(resBuilder);
        responseObserver.onNext(resBuilder.build());
        responseObserver.onCompleted();
    }

    private void buildPayload(HelloResponse.Builder builder) {
        for (int i = 0; i < 1000; i++) {
            builder.addDummyPayload(random.nextLong());
        }
    }
}
