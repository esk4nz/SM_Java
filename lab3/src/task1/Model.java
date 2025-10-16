package task1;

import java.util.ArrayList;

public class Model {
    private final ArrayList<Element> list;
    private double tnext, tcurr;
    private int event;

    public Model(ArrayList<Element> elements) {
        list = elements;
        event = 0;
        tcurr = 0.0;
    }

    public void simulate(double time) {
        while (tcurr < time) {
            tnext = Double.MAX_VALUE;
            for (Element e : list) {
                if (e.getTnext() < tnext) {
                    tnext = e.getTnext();
                    event = e.getId();
                }
            }
            System.out.println("\nIt's time for event in " +
                    list.get(event).getName() + ", time = " + tnext);

            for (Element e : list) e.doStatistics(tnext - tcurr);
            tcurr = tnext;
            for (Element e : list) e.setTcurr(tcurr);

            list.get(event).outAct();

            for (Element e : list) {
                if (e.getTnext() == tcurr && e.getId() != event) {
                    e.outAct();
                }
            }

            boolean progressed;
            do {
                progressed = false;
                for (Element e : list) {
                    if (e.tryUnblock()) progressed = true;
                }
            } while (progressed);

            printInfo();
        }
        printResult();
    }

    public void printInfo() {
        for (Element e : list) e.printInfo();
    }

    public void printResult() {
        System.out.println("\n-------------RESULTS-------------");
        for (Element e : list) {
            e.printResult();
            if (e instanceof Process) {
                Process p = (Process) e;
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