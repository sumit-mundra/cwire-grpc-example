package sumitm.grpc.examples;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A sample {@link sumitm.grpc.examples.ChronicleWireServiceDefinition.SayHelloService SayHelloService} service that simulates a delay and responds to grpc stream with simple reply
 */
public class SayHelloServiceImpl extends ChronicleWireServiceDefinition.SayHelloService {

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
    public void sayHello(ChronicleWireServiceDefinition.HelloRequest helloRequest,
                         StreamObserver<ChronicleWireServiceDefinition.HelloResponse> streamObserver) {
        delay(); // simulating backend latency
        ChronicleWireServiceDefinition.HelloResponse res = new ChronicleWireServiceDefinition.HelloResponse();
        res.setReply(HI_BACK);
        res.setId(helloRequest.getId());
        res.setDummyPayload(buildPayload());
        streamObserver.onNext(res);
        streamObserver.onCompleted();
    }

    private List<Long> buildPayload() {
        List<Long> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            list.add(this.random.nextLong());
        }
        return list;
    }
}
