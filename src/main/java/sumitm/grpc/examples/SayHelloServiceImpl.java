package sumitm.grpc.examples;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;

public class SayHelloServiceImpl extends ChronicleWireServiceDefinition.SayHelloService {

    private static final int MILLIS = 10;
    private static final String HI_BACK = "Hi ! back";

    private static void delay() {
        try {
            TimeUnit.MILLISECONDS.sleep(SayHelloServiceImpl.MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sayHello(ChronicleWireServiceDefinition.HelloRequest helloRequest,
                         StreamObserver<ChronicleWireServiceDefinition.HelloResponse> streamObserver) {
        delay(); // simulating backend latency of 10ms
        ChronicleWireServiceDefinition.HelloResponse res = new ChronicleWireServiceDefinition.HelloResponse();
        res.setReply(HI_BACK);
        res.setId(helloRequest.getId());
        streamObserver.onNext(res);
        streamObserver.onCompleted();
    }
}
