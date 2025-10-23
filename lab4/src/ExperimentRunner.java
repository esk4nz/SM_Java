import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;

public class ExperimentRunner {

    enum Topology { LINE, STAR }

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        double T = 10_000.0;
        int[] Ns = {10, 25, 50, 100, 200, 400, 800, 1000};
        int replicates = 3;
        double meanInterarrival = 6.0;
        double meanService      = 3.0;
        double lambda = 1.0 / meanInterarrival;
        double mu = 1.0 / meanService;
        double C_fixed = 4.0;

        runTopologyAndSaveCSV(Topology.LINE, Ns, T, replicates, meanInterarrival, meanService, lambda, mu, C_fixed, "results_line.csv");
        runTopologyAndSaveCSV(Topology.STAR, Ns, T, replicates, meanInterarrival, meanService, lambda, mu, C_fixed, "results_star.csv");

        System.out.println("Готово. Файли results_line.csv та results_star.csv");
    }

    private static void runTopologyAndSaveCSV(
            Topology topo, int[] Ns, double T, int replicates,
            double meanInterarrival, double meanService,
            double lambda, double mu, double C_fixed,
            String fileName
    ) throws IOException {

        Path path = Path.of(fileName);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {

            out.println("topology,N,M,timeMod,lambda,mu,replicate,elapsed_ms,events,emp_v,opCount,emp_C,emp_k,ops_theory_fixedC");

            for (int N : Ns) {
                warmup(topo, N, T * 0.1, meanInterarrival, meanService);

                double sumMs = 0.0;
                double sumV  = 0.0;
                double sumC  = 0.0;

                for (int r = 1; r <= replicates; r++) {
                    ArrayList<Element> list = buildNetwork(topo, N, meanInterarrival, meanService);

                    Model m = new Model(list);
                    m.setVerbose(false);

                    long t0 = System.nanoTime();
                    m.simulate(T);
                    long t1 = System.nanoTime();

                    double elapsedMs = (t1 - t0) / 1_000_000.0;
                    long events = m.getIterations();
                    long opCount = m.getOpCount();

                    int M = m.getElementCount();
                    double v_emp = events / T;
                    double C_emp = (events == 0 || M == 0) ? 0.0 : (opCount / (double) events) / M;
                    double k_emp = C_emp * M;

                    double ops_theory_fixedC = lambda * T * C_fixed * M * M;

                    out.printf(Locale.US,
                            "%s,%d,%d,%.0f,%.6f,%.6f,%d,%.3f,%d,%.6f,%d,%.6f,%.6f,%.3f%n",
                            topo.name(), N, M, T, lambda, mu, r, elapsedMs, events, v_emp, opCount, C_emp, k_emp, ops_theory_fixedC
                    );

                    sumMs += elapsedMs;
                    sumV  += v_emp;
                    sumC  += C_emp;
                }

                double avgMs = sumMs / replicates;
                double avgV  = sumV  / replicates;
                double avgC  = sumC  / replicates;
                int M = N + 1;
                double k_avg = avgC * M;
                double ops_theory_fixedC = lambda * T * C_fixed * M * M;

                out.printf(Locale.US,
                        "%s,%d,%d,%.0f,%.6f,%.6f,%s,%.3f,%s,%.6f,%s,%.6f,%.6f,%.3f%n",
                        topo.name(), N, M, T, lambda, mu, "AVG", avgMs, "", avgV, "", avgC, k_avg, ops_theory_fixedC
                );
            }
        }
    }

    private static void warmup(Topology topo, int N, double T, double meanInterarrival, double meanService) {
        ArrayList<Element> list = buildNetwork(topo, N, meanInterarrival, meanService);
        Model m = new Model(list);
        m.setVerbose(false);
        m.simulate(T);
    }

    private static ArrayList<Element> buildNetwork(Topology topology, int N, double meanInterarrival, double meanService) {
        ArrayList<Element> list = new ArrayList<>();

        Create c = new Create("CREATOR", meanInterarrival);
        c.setDistribution("exp");
        list.add(c);

        switch (topology) {
            case LINE -> {
                Process prev = null;
                for (int i = 1; i <= N; i++) {
                    Process p = new Process("P" + i, meanService);
                    p.setDistribution("exp");
                    p.setServers(1);
                    p.setMaxqueue(Integer.MAX_VALUE);
                    list.add(p);
                    if (i == 1) c.setNextElement(p);
                    if (prev != null) prev.setNextElement(p);
                    prev = p;
                }
            }
            case STAR -> {
                Process router = new Process("ROUTER", meanService);
                router.setDistribution("exp");
                router.setServers(1);
                router.setMaxqueue(Integer.MAX_VALUE);
                list.add(router);
                c.setNextElement(router);

                ArrayList<Process> services = new ArrayList<>();
                for (int i = 1; i <= N; i++) {
                    Process p = new Process("S" + i, meanService);
                    p.setDistribution("exp");
                    p.setServers(1);
                    p.setMaxqueue(Integer.MAX_VALUE);
                    list.add(p);
                    services.add(p);
                }
                if (N == 1) {
                    router.addNextElement(services.get(0), 1.0);
                } else {
                    double prob = 1.0 / N;
                    double acc = 0.0;
                    for (int i = 0; i < N - 1; i++) {
                        router.addNextElement(services.get(i), prob);
                        acc += prob;
                    }
                    router.addNextElement(services.get(N - 1), 1.0 - acc);
                }
            }
        }
        return list;
    }
}