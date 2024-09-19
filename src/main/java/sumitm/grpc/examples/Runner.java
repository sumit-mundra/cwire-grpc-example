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

    private static final long DURATION_SECONDS = 10L;

    private Server server;
    private ManagedChannel channel;

    public static void main(String[] args) throws Exception {

        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$TT.%1$tL " +
                "%4$s %2$s: %5$s%6$s%n");
//        int[] permits = new int[]{1, 1, 1, 1, 1, 1, 1, 1};
//        int[] delay = new int[]{0, 1, 10, 100, 1000, 10000, 100000, 1000000};
        int[] permits = new int[]{1};
        int[] delay = new int[]{0};
        for (int i = 0; i < permits.length; i++) {
            Runner example = new Runner();
            example.startServer(delay[i]);
            try {
                example.runClient(permits[i], delay[i]);
            } finally {
                example.stopServer();
            }
        }
    }

    private void startServer(int delay) throws IOException {
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        server = ServerBuilder.forPort(0)
                .addService(new SayHelloServiceImpl(delay))
                .build();
        server.start();
    }

    private void stopServer() throws InterruptedException {
        Server s = server;
        if (s == null) {
            throw new IllegalStateException("Already stopped");
        }
        server = null;
        s.shutdown();
        if (s.awaitTermination(10, TimeUnit.MILLISECONDS)) {
            return;
        }
        s.shutdownNow();
        if (s.awaitTermination(100, TimeUnit.MILLISECONDS)) {
            return;
        }
        throw new RuntimeException("Unable to shutdown server");
    }

    private void runClient(int permit, int serverDelay) throws InterruptedException {
        if (channel != null) {
            throw new IllegalStateException("Already started");
        }
        channel = ManagedChannelBuilder.forTarget("dns:///localhost:"+server.getPort()).usePlaintext().build();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            AtomicBoolean done = new AtomicBoolean();
            HelloServiceClient client = new HelloServiceClient(channel, permit);
            scheduler.schedule(() -> done.set(true), DURATION_SECONDS, TimeUnit.SECONDS);
            client.doClientWork(done);
            double qps = client.getRpcCount().doubleValue() / DURATION_SECONDS;
            logger.log(Level.INFO, "permit {3}:: {0} RPCs, {1} RPCs/s, Server Added Latency: {2} us",
                    new Object[]{
                            client.getRpcCount().get(),
                            qps,
                            (DURATION_SECONDS * 1000000L - client.getRpcCount().get() * serverDelay) / client.getRpcCount().get(),
                            permit});
            logger.log(Level.INFO, "server_total_time::{0} avg_server_latency::{1}",
                    new Object[]{client.getLatency().get(),
                            client.getLatency().get() / client.getRpcCount().get()});
        } finally {
            scheduler.shutdownNow();
            channel.shutdownNow();
        }
    }
}
