package task2;

import task1.Element;

import java.util.ArrayList;

public class SimTask2 {
    public static void main(String[] args) {
        Process lane1 = new Process("LANE1", 0.3);
        Process lane2 = new Process("LANE2", 0.3);
        lane1.setServers(1); lane1.setMaxqueue(3); lane1.setDistribution("exp");
        lane2.setServers(1); lane2.setMaxqueue(3); lane2.setDistribution("exp");

        lane1.linkLanePair(lane2); lane2.linkLanePair(lane1);
        lane1.setLaneRebalanceEnabled(true); lane2.setLaneRebalanceEnabled(true);
        lane1.setLaneSwitchThreshold(2);     lane2.setLaneSwitchThreshold(2);

        Create src = new Create("ARRIVALS", 0.5);
        src.setDistribution("exp");
        src.setTnext(0.1);

        src.addPriorityRoute(
                lane1,
                10,
                true,
                (from, to) -> lane1.getQueue() > lane2.getQueue()
        );

        src.addPriorityRoute(
                lane2,
                5,
                false,
                null
        );

        seedInitial(lane1, 0.1, 0.3, 2);
        seedInitial(lane2, 0.1, 0.3, 2);

        int initialSeeded = (lane1.getBusy() + lane1.getQueue()) + (lane2.getBusy() + lane2.getQueue());

        ArrayList<Element> list = new ArrayList<>();
        list.add(src);
        list.add(lane1);
        list.add(lane2);

        Model model = new Model(list);
        model.simulate(1000.0);

        // Підсумки
        double T = lane1.getTcurr();
        int served = lane1.getQuantity() + lane2.getQuantity();
        int arrivals = src.getQuantity();
        int losses = src.getRouteLoss();
        int attempted = initialSeeded + arrivals;

        double areaN = (lane1.getMeanQueue() + lane1.getBusyTime()) + (lane2.getMeanQueue() + lane2.getBusyTime());
        double avgN = T > 0 ? areaN / T : 0.0;
        double avgInterDeparture = served > 0 ? T / served : 0.0;
        double avgTimeInBank = served > 0 ? areaN / served : 0.0;

        int unfinishedAtEnd = (lane1.getBusy() + lane1.getQueue()) + (lane2.getBusy() + lane2.getQueue());


        int laneChanges = lane1.getLaneChanges() + lane2.getLaneChanges();

        System.out.println("\n============= TASK2 SUMMARY (aggregated) =============");
        System.out.println("Total time                 = " + T);
        System.out.println("Arrivals (Create)          = " + arrivals);
        System.out.println("Initial (seeded)           = " + initialSeeded);
        System.out.println("Total attempted            = " + attempted);
        System.out.println("Served total               = " + served);
        System.out.println("Lost at source (Create)    = " + losses);
        System.out.println("Unfinished at end          = " + unfinishedAtEnd);
        System.out.println("Avg number in bank         = " + avgN);
        System.out.println("Avg inter-departure time   = " + avgInterDeparture);
        System.out.println("Avg time in bank (Little)  = " + avgTimeInBank);
        System.out.println("Lane changes               = " + laneChanges);
        System.out.println("======================================================");
    }

    private static void seedInitial(Process lane, double firstServMean, double firstServDev, int initialQueue) {
        String prevDistr = lane.getDistribution();
        System.out.println(prevDistr);
        double prevMean  = lane.getDelayMean();

        lane.setDistribution("norm");
        lane.setDelayMean(firstServMean);
        lane.setDelayDev(firstServDev);
        lane.inAct();

        lane.setDistribution(prevDistr);
        lane.setDelayMean(prevMean);

        for (int i = 0; i < initialQueue; i++) lane.inAct();
    }
}