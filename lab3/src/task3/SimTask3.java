package task3;

import task1.Element;
import task2.Model;

import java.util.ArrayList;
import java.util.List;

public class SimTask3 {
    public static void main(String[] args) {
        final List<Double> labArrTimes = new ArrayList<>();
        final long[] done = new long[3];
        final double[] sumTime = new double[3];

        Process reception = new Process(
                "RECEPTION",
                2,
                p -> {
                    switch (p.type) {
                        case T1: return RandUtil.exp(15.0);
                        case T2: return RandUtil.exp(40.0);
                        case T3: return RandUtil.exp(30.0);
                        default: return RandUtil.exp(15.0);
                    }
                }
        );
        reception.setPriorityType(Patient.Type.T1);

        Process escort = new Process(
                "ESCORT",
                3,
                p -> RandUtil.unif(3.0, 8.0)
        );
        escort.setOnExit(p -> {
            int idx = (p.orig == Patient.Type.T1 ? 0 : p.orig == Patient.Type.T2 ? 1 : 2);
            done[idx]++; sumTime[idx] += (escort.getTcurr() - p.t0);
        });

        Process toLab = new Process(
                "TO_LAB",
                512,
                p -> RandUtil.unif(2.0, 5.0)
        );
        toLab.setOnCompleteHook((self, p) -> labArrTimes.add(self.getTcurr()));

        Process labReg = new Process(
                "LAB_REG",
                1,
                p -> RandUtil.erlang(4.5, 3)
        );

        Process labAn = new Process(
                "LAB_ANALYSIS",
                2,
                p -> RandUtil.erlang(4.0, 2)
        );
        labAn.setChangeT2toT1OnComplete(true);
        labAn.setOnExit(p -> {
            // вихід для T3
            int idx = 2;
            done[idx]++; sumTime[idx] += (labAn.getTcurr() - p.t0);
        });

        Process backToRec = new Process(
                "BACK_TO_RECEPTION",
                512,
                p -> RandUtil.unif(2.0, 5.0)
        );

        reception.setNextT1(escort);
        reception.setNextT2(toLab);
        reception.setNextT3(toLab);

        toLab.setNextT1(labReg);
        toLab.setNextT2(labReg);
        toLab.setNextT3(labReg);

        labReg.setNextT1(labAn);
        labReg.setNextT2(labAn);
        labReg.setNextT3(labAn);

        labAn.setNextT1(null);
        labAn.setNextT2(backToRec);
        labAn.setNextT3(null);

        backToRec.setNextT1(reception);
        backToRec.setNextT2(reception);
        backToRec.setNextT3(reception);

        // Джерело
        Create src = new Create(15.0, 0.5, 0.1, 0.4, reception);

        // Модель
        ArrayList<Element> list = new ArrayList<>();
        list.add(src);
        list.add(reception);
        list.add(escort);
        list.add(toLab);
        list.add(labReg);
        list.add(labAn);
        list.add(backToRec);

        Model model = new Model(list);
        model.simulate(1000.0);

        double T = reception.getTcurr();

        int unfinished =
                reception.getBusyCount() + reception.getQueueSize() +
                        escort.getBusyCount()    + escort.getQueueSize() +
                        toLab.getBusyCount()     + toLab.getQueueSize() +
                        labReg.getBusyCount()    + labReg.getQueueSize() +
                        labAn.getBusyCount()     + labAn.getQueueSize() +
                        backToRec.getBusyCount() + backToRec.getQueueSize();

        // Вивід
        System.out.println("\n============= TASK3 SUMMARY =============");
        System.out.println("Total time (T)                       = " + T);
        System.out.println("Arrivals total (Create.quantity)     = " + src.getQuantity());
        System.out.println("Done to ward (T1) count              = " + done[0]
                + ", avg time = " + (done[0] > 0 ? (sumTime[0] / done[0]) : 0.0));
        System.out.println("Done to ward (T2) count              = " + done[1]
                + ", avg time = " + (done[1] > 0 ? (sumTime[1] / done[1]) : 0.0));
        System.out.println("Left after lab (T3) count            = " + done[2]
                + ", avg time = " + (done[2] > 0 ? (sumTime[2] / done[2]) : 0.0));
        System.out.println("Lab arrivals count                    = " + labArrTimes.size());
        if (labArrTimes.size() >= 2) {
            double s = 0.0;
            for (int i = 1; i < labArrTimes.size(); i++) s += (labArrTimes.get(i) - labArrTimes.get(i - 1));
            System.out.println("Avg interarrival to lab               = " + (s / (labArrTimes.size() - 1)));
        } else {
            System.out.println("Avg interarrival to lab               = 0.0");
        }
        double areaN =
                (reception.getAreaQueue() + reception.getAreaBusy()) +
                        (escort.getAreaQueue()    + escort.getAreaBusy()) +
                        (labReg.getAreaQueue()    + labReg.getAreaBusy()) +
                        (labAn.getAreaQueue()     + labAn.getAreaBusy());
        System.out.println("Avg number in system (approx)         = " + (T > 0 ? areaN / T : 0.0));
        System.out.println("Unfinished at end                     = " + unfinished);
        System.out.println("========================================");
    }
}