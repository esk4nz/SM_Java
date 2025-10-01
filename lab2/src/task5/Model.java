package task5;

import tasks1_2.*;

import java.util.ArrayList;

public class Model {
    private ArrayList<Element> list;
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
                    list.get(event).getName() +
                    ", time = " + tnext);
            for (Element e : list) {
                e.doStatistics(tnext - tcurr);
            }
            tcurr = tnext;
            for (Element e : list) {
                e.setTcurr(tcurr);
            }
            list.get(event).outAct();
            for (Element e : list) {
                if (e.getTnext() == tcurr) {
                    e.outAct();
                }
            }
            printInfo();
        }
        printResult();
    }
    public void printInfo() {
        for (Element e : list) {
            e.printInfo();
        }
    }
    public void printResult() {
        System.out.println("\n-------------RESULTS-------------");
        for (Element e : list) {
            e.printResult();
            if (e instanceof Process) {
                Process p = (Process) e;
                double attempts = p.getQuantity() + p.getFailure();
                System.out.println("mean length of queue = " +
                        p.getMeanQueue() / tcurr
                        + "\nfailure probability = " +
                        (attempts == 0 ? 0.0 : p.getFailure() / attempts)
                        + "\naverage load = " +
                        p.getAverageLoad(tcurr));
            }
        }
    }
}
