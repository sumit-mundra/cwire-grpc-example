package sumitm.grpc.examples;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class SayHelloServiceImpl extends ChronicleWireServiceDefinition.SayHelloService {

    private static final String HI_BACK = "Hi ! back";
    private int delay;

    public SayHelloServiceImpl(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return delay;
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

    @Override
    public void sayHello(ChronicleWireServiceDefinition.HelloRequest helloRequest,
                         StreamObserver<ChronicleWireServiceDefinition.HelloResponse> streamObserver) {
        delay(); // simulating backend latency
        ChronicleWireServiceDefinition.HelloResponse res = new ChronicleWireServiceDefinition.HelloResponse();
        res.setReply(HI_BACK);
        res.setId(helloRequest.getId());
        streamObserver.onNext(res);
        streamObserver.onCompleted();
    }
}
