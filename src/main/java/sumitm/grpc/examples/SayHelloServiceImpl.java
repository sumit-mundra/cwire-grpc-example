package sumitm.grpc.examples;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

/**
 * A sample {@link GsonBasedServiceDefinition.SayHelloService SayHelloService} service that simulates a delay and responds to grpc stream with simple reply
 */
public class SayHelloServiceImpl extends GsonBasedServiceDefinition.SayHelloService {

    private static final String HI_BACK = "Hi ! back";
    final private int delay;

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Implements a thread sleep delay, builds a response to send via provided grpc stream observer
     * throws {@code RuntimeException} if interrupted during delay
     * @param helloRequest input request
     * @param streamObserver grpc observer
     */
    @Override
    public void sayHello(GsonBasedServiceDefinition.HelloRequest helloRequest,
                         StreamObserver<GsonBasedServiceDefinition.HelloResponse> streamObserver) {
        delay(); // simulating backend latency
        GsonBasedServiceDefinition.HelloResponse res = new GsonBasedServiceDefinition.HelloResponse();
        res.setReply(HI_BACK);
        res.setId(helloRequest.getId());
        streamObserver.onNext(res);
        streamObserver.onCompleted();
    }
}
