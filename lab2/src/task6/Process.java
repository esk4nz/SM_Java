package task6;

import tasks1_2.Element;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Process extends Element {
    private int queue = 0;
    private int maxqueue = Integer.MAX_VALUE;
    private int failure = 0;

    private int servers = 1;
    private int busy = 0;
    private double[] finishTimes = new double[1];

    private double meanQueue = 0.0;
    private double busyTime = 0.0;

    private final List<Element> nextElements = new ArrayList<>();
    private final List<Double> nextProbs = new ArrayList<>();
    private boolean useProbRouting = false;

    public Process(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setServers(1);
        super.setTnext(Double.MAX_VALUE);
    }

    public void addNextElement(Element next, double probability) {
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        if (probability < 0 || probability > 1) throw new IllegalArgumentException("probability must be in [0,1]");
        nextElements.add(next);
        nextProbs.add(probability);
        useProbRouting = true;
    }

    private Element chooseNext() {
        if (useProbRouting && !nextElements.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (double p : nextProbs) sum = sum.add(BigDecimal.valueOf(p));
            if (sum.compareTo(BigDecimal.ONE) != 0) {
                throw new IllegalStateException("Sum of routing probabilities must be 1.0, got " + sum);
            }

            double r = Math.random();
            double acc = 0.0;
            for (int i = 0; i < nextElements.size()-1; i++) {
                acc += nextProbs.get(i);
                if (r < acc) return nextElements.get(i);
            }
            return nextElements.get(nextElements.size() - 1);
        }
        return super.getNextElement();
    }

    // --- Multi-server logic (task5) ---
    public void setServers(int servers) {
        if (servers <= 0) throw new IllegalArgumentException("servers must be >= 1");
        this.servers = servers;
        this.finishTimes = new double[servers];
        for (int i = 0; i < servers; i++) finishTimes[i] = Double.MAX_VALUE;
        this.busy = 0;
        super.setState(0);
        super.setTnext(Double.MAX_VALUE);
    }

    public int getServers() { return servers; }
    public int getBusy() { return busy; }

    @Override
    public void inAct() {
        if (busy < servers) {
            startServiceNow();
        } else {
            if (queue < maxqueue) {
                queue++;
            } else {
                failure++;
            }
        }
    }

    @Override
    public void outAct() {
        super.outAct();
        int idx = indexOfMinFinishTime();
        finishTimes[idx] = Double.MAX_VALUE;
        busy--;

        // Route finished request
        Element next = chooseNext();
        if (next != null) next.inAct();

        // Start new from queue while there are free servers
        while (queue > 0 && busy < servers) {
            queue--;
            startServiceNow();
        }
        updateAggregateStateFromServers();
    }

    private void startServiceNow() {
        int freeIdx = indexOfFreeServer();
        busy++;
        double compTime = super.getTcurr() + super.getDelay();
        finishTimes[freeIdx] = compTime;
        updateAggregateStateFromServers();
    }

    private void updateAggregateStateFromServers() {
        super.setState(busy > 0 ? 1 : 0);
        int idxMin = indexOfMinFinishTime();
        super.setTnext(finishTimes[idxMin]);
    }

    private int indexOfFreeServer() {
        for (int i = 0; i < servers; i++) {
            if (finishTimes[i] == Double.MAX_VALUE) return i;
        }
        return -1;
    }

    private int indexOfMinFinishTime() {
        double min = Double.MAX_VALUE;
        int idx = 0;
        for (int i = 0; i < servers; i++) {
            if (finishTimes[i] < min) {
                min = finishTimes[i];
                idx = i;
            }
        }
        return idx;
    }

    @Override
    public void doStatistics(double delta) {
        meanQueue += queue * delta;
        busyTime += busy * delta;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("servers = " + servers +
                ", busy = " + busy +
                ", queue = " + queue +
                ", failure = " + failure);
    }

    public int getFailure() { return failure; }
    public int getQueue() { return queue; }
    public int getMaxqueue() { return maxqueue; }
    public void setMaxqueue(int maxqueue) { this.maxqueue = maxqueue; }
    public double getMeanQueue() { return meanQueue; }
    public double getBusyTime() { return busyTime; }

    public double getAverageLoad(double tcurr) {
        if (tcurr <= 0 || servers <= 0) return 0.0;
        return busyTime / (tcurr * servers);
    }
}