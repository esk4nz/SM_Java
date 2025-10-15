package task1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Process extends Element {
    // Черга/відмови (атрибутуються ЦІЛІ, у яку не потрапили)
    private int queue = 0;
    private int maxqueue = Integer.MAX_VALUE;
    private int failure = 0;

    // Сервери
    private int servers = 1;
    private int busy = 0;
    private double[] finishTimes = new double[1];

    // Блокування на виході
    private boolean[] blocked;
    private Element[] blockedTarget;
    private int blockCount = 0;
    private double blockedTime = 0.0;
    private boolean blockOnFull = true; // true=чекати, false=дропнути

    // Втрати на виході цього процесу (джерела)
    private int routeLoss = 0;

    // Статистика
    private double meanQueue = 0.0;
    private double busyTime = 0.0;

    // Роутинг
    private final List<Element> nextElements = new ArrayList<>();
    private final List<Double> nextProbs = new ArrayList<>();
    private boolean useProbRouting = false;

    private final List<Element> prioElements = new ArrayList<>();
    private final List<Integer> prioValues  = new ArrayList<>();
    private boolean usePriorityRouting = false;

    public Process(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setServers(1);
        super.setTnext(Double.MAX_VALUE);
    }

    // -------------------- НАЛАШТУВАННЯ --------------------
    public void setServers(int servers) {
        if (servers <= 0) throw new IllegalArgumentException("servers must be >= 1");
        this.servers = servers;
        this.finishTimes = new double[servers];
        this.blocked     = new boolean[servers];
        this.blockedTarget = new Element[servers];
        for (int i = 0; i < servers; i++) {
            finishTimes[i] = Double.MAX_VALUE;
            blocked[i] = false;
            blockedTarget[i] = null;
        }
        this.busy = 0;
        super.setState(0);
        super.setTnext(Double.MAX_VALUE);
    }
    public void setBlockOnFull(boolean blockOnFull) { this.blockOnFull = blockOnFull; }
    public int  getServers() { return servers; }
    public int  getBusy()    { return busy; }

    // -------------------- РОУТИНГ --------------------
    public void addNextElement(Element next, double probability) {
        if (usePriorityRouting) throw new IllegalStateException("Cannot mix priority and probability routing");
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        if (probability < 0 || probability > 1) throw new IllegalArgumentException("probability must be in [0,1]");
        nextElements.add(next);
        nextProbs.add(probability);
        useProbRouting = true;
    }

    public void addPriorityRoute(Element next, int priority) {
        if (useProbRouting) throw new IllegalStateException("Cannot mix priority and probability routing");
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        prioElements.add(next);
        prioValues.add(priority);
        usePriorityRouting = true;
    }

    private Element chooseNextProbability() {
        double sum = 0.0;
        for (double p : nextProbs) sum += p;
        if (Math.abs(sum - 1.0) > 1e-9) throw new IllegalStateException("Sum of routing probabilities must be 1.0");
        double r = Math.random(), acc = 0.0;
        for (int i = 0; i < nextElements.size() - 1; i++) {
            acc += nextProbs.get(i);
            if (r < acc) return nextElements.get(i);
        }
        return nextElements.get(nextElements.size() - 1);
    }

    private boolean hasAnyRoute() {
        if (usePriorityRouting && !prioElements.isEmpty()) return true;
        if (useProbRouting && !nextElements.isEmpty()) return true;
        return getNextElement() != null;
    }

    // -------------------- ПОДІЇ --------------------
    @Override
    public void inAct() {
        if (busy < servers) {
            startServiceNow();
        } else {
            if (queue < maxqueue) queue++;
            else failure++; // відмова зараховується ЦІЛІ, у яку намагаємось увійти
        }
    }

    @Override
    public void outAct() {
        int idx = indexOfMinFinishTime();
        if (finishTimes[idx] == Double.MAX_VALUE) return;

        super.outAct();

        // SINK: немає виходів — випускаємо клієнта
        if (!hasAnyRoute()) {
            finishTimes[idx] = Double.MAX_VALUE;
            busy--;
            while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
            updateAggregateStateFromServers();
            return;
        }

        Element target = null;
        boolean routeAvailable = true;

        if (usePriorityRouting && !prioElements.isEmpty()) {
            // ПРІОРИТЕТИ:
            // - йдемо від найвищого до найнижчого
            // - якщо знайшли доступний — беремо його
            // - якщо жоден не доступний — fallback = ОСТАННІЙ перевірений (найнижчий пріоритет),
            //   і саме ЙОМУ атрибутуємо блок/дроп (щоб "резервний" отримував свої фейли)
            TreeSet<Integer> levelsDesc = new TreeSet<>(Comparator.reverseOrder());
            levelsDesc.addAll(prioValues);

            Element available = null;
            Element fallbackLowest = null;

            outer:
            for (Integer level : levelsDesc) {
                for (int i = 0; i < prioElements.size(); i++) {
                    if (!prioValues.get(i).equals(level)) continue;
                    Element e = prioElements.get(i);
                    if (e == null) continue;

                    // останній переглянутий — це "резерв" найнижчого пріоритету
                    fallbackLowest = e;

                    if (e.canAccept()) {
                        available = e;
                        break outer;
                    }
                }
            }

            if (available != null) {
                target = available;
                routeAvailable = true;
            } else {
                target = fallbackLowest;  // усі забиті — атрибуція піде у найнижчий пріоритет
                routeAvailable = false;
            }

        } else if (useProbRouting && !nextElements.isEmpty()) {
            target = chooseNextProbability();
            routeAvailable = target != null && target.canAccept();
        } else {
            target = getNextElement();
            routeAvailable = target != null && target.canAccept();
        }

        if (routeAvailable && target != null) {
            // передаємо далі і звільняємо сервер
            finishTimes[idx] = Double.MAX_VALUE;
            busy--;
            target.inAct();
            while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
        } else {
            // маршрут недоступний
            if (blockOnFull) {
                // БЛОКУВАННЯ: тримаємо клієнта на сервері (ціль має бути не null)
                blocked[idx] = true;
                blockedTarget[idx] = (target != null ? target : getNextElement());
                if (blockedTarget[idx] == null) {
                    // перестраховка: якщо таки нема цілі — трактуємо як SINK
                    blocked[idx] = false;
                    finishTimes[idx] = Double.MAX_VALUE;
                    busy--;
                    while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
                } else {
                    finishTimes[idx] = Double.MAX_VALUE;
                    blockCount++;
                }
            } else {
                // DROP: джерело фіксує свої втрати, а ЦІЛІ зараховується відмова через onDropFrom
                routeLoss++;
                if (target != null) target.onDropFrom(this);
                finishTimes[idx] = Double.MAX_VALUE;
                busy--;
                while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
            }
        }

        updateAggregateStateFromServers();
    }

    private void startServiceNow() {
        int freeIdx = indexOfFreeServer();
        if (freeIdx < 0) return;
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
            if (!blocked[i] && finishTimes[i] == Double.MAX_VALUE) return i;
        }
        return -1;
    }

    private int indexOfMinFinishTime() {
        double min = Double.MAX_VALUE;
        int idx = 0;
        for (int i = 0; i < servers; i++) {
            if (finishTimes[i] < min) { min = finishTimes[i]; idx = i; }
        }
        return idx;
    }

    // -------------------- АТРИБУЦІЯ DROP ДО ЦІЛІ --------------------
    @Override
    public void onDropFrom(Element from) {
        // як у минулій лабі — це відмова ЦІЛІ
        failure++;
    }

    // -------------------- РОЗБЛОКУВАННЯ --------------------
    @Override
    public boolean tryUnblock() {
        boolean progressed = false;
        for (int i = 0; i < servers; i++) {
            if (blocked[i]) {
                Element t = blockedTarget[i];
                if (t != null && t.canAccept()) {
                    t.inAct();
                    blocked[i] = false;
                    blockedTarget[i] = null;
                    busy--;
                    if (queue > 0 && busy < servers) { queue--; startServiceNow(); }
                    progressed = true;
                }
            }
        }
        if (progressed) updateAggregateStateFromServers();
        return progressed;
    }

    // -------------------- СТАТИСТИКА --------------------
    @Override
    public void doStatistics(double delta) {
        meanQueue  += queue * delta;
        busyTime   += busy * delta;
        blockedTime+= countBlockedServers() * delta;
    }
    private int countBlockedServers() {
        int b = 0;
        if (blocked == null) return 0;
        for (boolean v : blocked) if (v) b++;
        return b;
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("servers = " + servers +
                ", busy = " + busy +
                ", queue = " + queue +
                ", failure(in/target) = " + failure +
                ", blockedServers = " + countBlockedServers() +
                ", routeLoss(out/source) = " + routeLoss +
                ", policy.blockOnFull = " + blockOnFull);
    }

    // Getters
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
    public int getBlockCount() { return blockCount; }
    public double getBlockedTime() { return blockedTime; }
    public int getRouteLoss() { return routeLoss; }

    @Override
    public boolean canAccept() {
        return (busy < servers) || (queue < maxqueue);
    }
}