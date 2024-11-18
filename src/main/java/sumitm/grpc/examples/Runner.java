package sumitm.grpc.examples;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This Runner simulates a grpc server and grpc client with serialization library as chronicle wire
 */
public final class Runner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);

    private static final long DURATION_SECONDS = 10L;

    private Server server;
    private ManagedChannel channel;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
//        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$TT.%1$tL %4$s %2$s: %5$s%6$s%n");
        int[][] inputData = new int[][]{{1, 0}, {1, 0}, {1, 0}, {100, 0}, {500, 0}, {1000, 0}, {1, 100}, {100, 100}, {500, 100}, {1000, 100}, {1, 1000}, {100, 1000}, {500, 1000}, {1000, 1000},};
        logger.info("Started");
        logger.info("Clients, RPCs, RPCs/s, sum(us), avg(us), simulated_delay(us), delta(us)");
        for (int[] input : inputData) {
            Runner example = new Runner();
            example.startServer(input[1]);
            try {
                example.runClient(input[0], input[1]);
            } finally {
                example.stopServer();
            }
        }
    }

    private void startServer(int delay) throws IOException {
        if (server != null) {
            throw new IllegalStateException("Already started");
        }
        server = ServerBuilder.forPort(0).addService(new SayHelloServiceImpl(delay)).build();
        server.start();
    }

    private void stopServer() {
        Server s = server;
        if (s == null) {
            throw new IllegalStateException("Already stopped");
        }
        server = null;
        s.shutdown();
    }

    /**
     * Builds a channel with server on defined target. Builds a  {@link HelloServiceClient client} which runs for {@code DURATION_SECONDS}
     *
     * @param permit      Number of parallel requests client is allowed to make
     * @param serverDelay The simulated delay (in us) in server side response, used here only for building analysis
     * @throws InterruptedException if interrupted during client execution
     */
    private void runClient(int permit, int serverDelay) throws InterruptedException {
        if (channel != null) {
            throw new IllegalStateException("Already started");
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        channel = ManagedChannelBuilder.forTarget("dns:///localhost:" + server.getPort()).usePlaintext().executor(executor).build();
        try {
            HelloServiceClient client = new HelloServiceClient(channel, permit);
            client.doTimedClientWork(DURATION_SECONDS);
            long rpcCount = client.getRpcCount().get();
            double qps = (double) rpcCount / DURATION_SECONDS;
            long totalServerTime = client.getLatency().get();
            logger.info("{}, {}, {}, {}, {}, {}, {}", String.format("%4d", permit), String.format("%6d", rpcCount), String.format("%8.2f", qps), String.format("%11.2f", totalServerTime / 1000f), String.format("%8.2f", (double) totalServerTime / (rpcCount * 1000)), String.format("%4d", serverDelay), String.format("%8.2f", (totalServerTime / 1000f - rpcCount * serverDelay) / rpcCount));
        } finally {
            channel.shutdown();
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
        }
    }
}
