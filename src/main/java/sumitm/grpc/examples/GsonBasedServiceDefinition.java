package sumitm.grpc.examples;

import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;


/**
 * A simple demonstration for plugging gson serializer in a grpc based service
 */
public final class GsonBasedServiceDefinition {

    public static final MethodDescriptor<HelloRequest, HelloResponse> SAY_HELLO_METHOD =
            MethodDescriptor.newBuilder(
                            new GsonMarshaller<>(HelloRequest.class),
                            new GsonMarshaller<>(HelloResponse.class))
                    .setFullMethodName("sayHelloService/sayHello")
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .build();

    public static final class HelloRequest {

        private long id;
        private String message;

        public HelloRequest() {
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static final class HelloResponse {
        private long id;
        private String reply;

        public HelloResponse() {
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }
    }


    public static abstract class SayHelloService implements BindableService {

        private static final String SAY_HELLO_SERVICE = "sayHelloService";

        @Override
        public ServerServiceDefinition bindService() {
            ServerMethodDefinition<HelloRequest, HelloResponse> sayHelloMethod =
                    ServerMethodDefinition.create(SAY_HELLO_METHOD, ServerCalls
                            .asyncUnaryCall(this::sayHello));
            return ServerServiceDefinition
                    .builder(SAY_HELLO_SERVICE)
                    .addMethod(sayHelloMethod)
                    .build();
        }

        public abstract void sayHello(HelloRequest helloRequest, StreamObserver<HelloResponse> streamObserver);
    }

}