package sumitm.grpc.examples;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Runner {
    private static final Logger logger = Logger.getLogger(Runner.class.getName());

    private static final long DURATION_SECONDS = 5L;

    private Server server;
    private ManagedChannel channel;

    public static void main(String[] args) throws Exception {
        int[] permits = new int[]{1, 10, 20, 50, 100, 150, 250, 500, 1000};
        for (int permit : permits) {
            Runner example = new Runner();
            example.startServer();
            try {
                example.runClient(permit);
            } finally {
                example.stopServer();
            }
        }

    }

    private void startServer() throws IOException {
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        server = ServerBuilder.forPort(0).addService(new SayHelloServiceImpl()).build();
        server.start();
    }

    private void stopServer() throws InterruptedException {
        Server s = server;
        if (s == null) {
            throw new IllegalStateException("Already stopped");
        }
        server = null;
        s.shutdown();
        if (s.awaitTermination(1, TimeUnit.SECONDS)) {
            return;
        }
        s.shutdownNow();
        if (s.awaitTermination(1, TimeUnit.SECONDS)) {
            return;
        }
        throw new RuntimeException("Unable to shutdown server");
    }

    private void runClient(int permit) throws InterruptedException {
        if (channel != null) {
            throw new IllegalStateException("Already started");
        }
        channel = ManagedChannelBuilder.forTarget("dns:///localhost:" + server.getPort()).usePlaintext().build();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            AtomicBoolean done = new AtomicBoolean();
            HelloServiceClient client = new HelloServiceClient(channel, permit);
            logger.log(Level.INFO, "Starting for permit: {0}", new Object[]{permit});
            scheduler.schedule(() -> done.set(true), DURATION_SECONDS, TimeUnit.SECONDS);
            client.doClientWork(done);
            double qps = client.getRpcCount().doubleValue() / DURATION_SECONDS;
            logger.log(Level.INFO, "Did {0} RPCs/s", new Object[]{qps});
        } finally {
            scheduler.shutdownNow();
            channel.shutdownNow();
        }
    }
}
