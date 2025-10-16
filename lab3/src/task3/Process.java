package task3;

import task1.Element;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Process extends Element {

    private final int servers;
    private final double[] finishTimes;
    private final Patient[] onServer;
    private int busy = 0;

    private final Deque<Patient> queue = new ArrayDeque<>();

    private final Function<Patient, Double> timeFn;
    private BiConsumer<Process, Patient> onCompleteHook; // опційно
    private java.util.function.Consumer<Patient> onExit; // опційно

    private Process nextT1, nextT2, nextT3;
    private boolean changeT2toT1OnComplete = false;
    private Patient.Type priorityType = null;

    private double areaQueue = 0.0;
    private double areaBusy  = 0.0;

    public Process(String name, int servers, Function<Patient, Double> timeFn) {
        super(name, 0.0);
        if (servers <= 0) throw new IllegalArgumentException("servers must be >= 1");
        this.servers = servers;
        this.timeFn = timeFn;
        this.finishTimes = new double[servers];
        this.onServer = new Patient[servers];
        for (int i = 0; i < servers; i++) finishTimes[i] = Double.MAX_VALUE;
        setTnext(Double.MAX_VALUE);
    }

    public void inAct(Patient p) {
        int free = indexOfFreeServer();
        if (free >= 0) {
            startOn(free, p);
            return;
        }
        if (priorityType != null && p.type == priorityType) {
            queue.addFirst(p);
        } else {
            queue.addLast(p);
        }
    }

    @Override
    public void outAct() {
        int idx = indexOfEarliest();
        if (idx < 0) return;

        super.outAct(); // quantity++

        Patient p = onServer[idx];

        onServer[idx] = null;
        finishTimes[idx] = Double.MAX_VALUE;
        busy--;

        if (onCompleteHook != null) onCompleteHook.accept(this, p);

        Patient.Type typeBefore = p.type;
        Process next = routeForType(typeBefore);

        if (changeT2toT1OnComplete && typeBefore == Patient.Type.T2) {
            p.type = Patient.Type.T1;
        }

        if (next == null) {
            if (onExit != null) onExit.accept(p);
        } else {
            next.inAct(p);
        }

        while (!queue.isEmpty()) {
            int free = indexOfFreeServer();
            if (free < 0) break;
            startOn(free, queue.pollFirst());
        }

        updateTnext();
    }

    private Process routeForType(Patient.Type t) {
        switch (t) {
            case T1: return nextT1;
            case T2: return nextT2;
            case T3: return nextT3;
            default: return null;
        }
    }

    private void startOn(int idx, Patient p) {
        onServer[idx] = p;
        busy++;
        double dt = timeFn.apply(p);
        finishTimes[idx] = getTcurr() + dt;
        updateTnext();
    }

    private int indexOfFreeServer() {
        for (int i = 0; i < servers; i++) {
            if (onServer[i] == null && finishTimes[i] == Double.MAX_VALUE) return i;
        }
        return -1;
    }

    private int indexOfEarliest() {
        double min = Double.MAX_VALUE;
        int idx = -1;
        for (int i = 0; i < servers; i++) {
            if (finishTimes[i] < min) { min = finishTimes[i]; idx = i; }
        }
        return idx;
    }

    private void updateTnext() {
        double min = Double.MAX_VALUE;
        for (double t : finishTimes) if (t < min) min = t;
        setTnext(min);
    }

    @Override
    public void doStatistics(double delta) {
        areaQueue += queue.size() * delta;
        areaBusy  += busy * delta;
    }

    public void setPriorityType(Patient.Type t) { this.priorityType = t; }
    public void setOnCompleteHook(BiConsumer<Process, Patient> hook) { this.onCompleteHook = hook; }
    public void setOnExit(java.util.function.Consumer<Patient> onExit) { this.onExit = onExit; }
    public void setNextT1(Process n) { this.nextT1 = n; }
    public void setNextT2(Process n) { this.nextT2 = n; }
    public void setNextT3(Process n) { this.nextT3 = n; }
    public void setChangeT2toT1OnComplete(boolean v) { this.changeT2toT1OnComplete = v; }

    public double getAreaQueue() { return areaQueue; }
    public double getAreaBusy()  { return areaBusy; }
    public int getBusyCount() { return busy; }
    public int getQueueSize() { return queue.size(); }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("servers=" + servers + ", busy=" + busy + ", queue=" + queue.size());
    }
}