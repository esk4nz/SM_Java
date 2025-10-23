import java.util.ArrayList;

public class Model {
    private final ArrayList<Element> list;
    private double tnext, tcurr;
    private int eventIndex;

    private boolean verbose = true;

    private long iterations = 0;
    private long opCount   = 0;

    public Model(ArrayList<Element> elements) {
        this.list = elements;
        this.eventIndex = 0;
        this.tcurr = 0.0;
    }

    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public long getIterations() { return iterations; }
    public long getOpCount() { return opCount; }
    public double getTcurr() { return tcurr; }
    public int getElementCount() { return list.size(); }

    public void simulate(double time) {
        iterations = 0;
        opCount = 0;

        while (tcurr < time) {
            tnext = Double.MAX_VALUE;

            for (int i = 0; i < list.size(); i++) {
                Element e = list.get(i);
                opCount++;
                if (e.getTnext() < tnext) {
                    tnext = e.getTnext();
                    eventIndex = i;
                }
            }

            if (verbose) {
                System.out.println("\nIt's time for event in " +
                        list.get(eventIndex).getName() + ", time = " + tnext);
            }

            double delta = tnext - tcurr;
            for (Element e : list) { e.doStatistics(delta); opCount++; }

            tcurr = tnext;
            for (Element e : list) { e.setTcurr(tcurr); opCount++; }

            list.get(eventIndex).outAct();
            iterations++;

            for (int i = 0; i < list.size(); i++) {
                if (i == eventIndex) continue;
                Element e = list.get(i);
                opCount++;
                if (e.getTnext() == tcurr) {
                    e.outAct();
                    iterations++;
                }
            }

            boolean progressed;
            do {
                progressed = false;
                for (Element e : list) {
                    opCount++;
                    if (e.tryUnblock()) {
                        progressed = true;
                    }
                }
            } while (progressed);

            if (verbose) printInfo();
        }
        if (verbose) printResult();
    }

    public void printInfo() {
        for (Element e : list) e.printInfo();
    }

    public void printResult() {
        System.out.println("\n-------------RESULTS-------------");
        for (Element e : list) {
            e.printResult();
            if (e instanceof Process p) {
                double attempts = p.getQuantity() + p.getFailure();
                System.out.println("  mean length of queue = " + p.getMeanQueue() / tcurr
                        + "\n  failure probability (at target input) = " + (attempts == 0 ? 0.0 : p.getFailure() / attempts)
                        + "\n  average load = " + p.getAverageLoad(tcurr)
                        + "\n  block count = " + p.getBlockCount()
                        + "\n  blocked time = " + p.getBlockedTime()
                        + "\n  route loss (at sources, policy=drop) = " + p.getRouteLoss());
            }
        }
    }
}