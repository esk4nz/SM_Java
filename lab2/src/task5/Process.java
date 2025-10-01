package task5;

import tasks1_2.Element;

public class Process extends Element {
    private int queue = 0;
    private int maxqueue = Integer.MAX_VALUE;
    private int failure = 0;

    private int servers = 1;
    private int busy = 0;
    private double[] finishTimes = new double[1];

    private double meanQueue = 0.0;
    private double busyTime = 0.0;

    public Process(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setServers(1);
        super.setTnext(Double.MAX_VALUE);
    }

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
        // Тепер завжди буде коректний індекс!
        finishTimes[idx] = Double.MAX_VALUE;
        busy--;
        if (super.getNextElement() != null) {
            super.getNextElement().inAct();
        }
        while (queue > 0 && busy < servers) {
            queue--;
            startServiceNow();
        }
        updateAggregateStateFromServers();
    }

    private void startServiceNow() {
        int freeIdx = indexOfFreeServer();
        // В цій моделі після busy < servers це завжди коректний індекс!
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