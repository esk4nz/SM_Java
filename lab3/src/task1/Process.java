package task1;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Process extends Element {
    // Черга/відмови (атрибуція у ЦІЛЬ)
    private int queue = 0;
    private int maxqueue = Integer.MAX_VALUE;
    private int failure = 0;

    // Сервери
    private int servers = 1;
    private int busy = 0;
    private double[] finishTimes = new double[1];

    // Блокування
    private boolean[] blocked;
    private Element[] blockedTarget;
    private int blockCount = 0;
    private double blockedTime = 0.0;

    // Дефолтна політика для маршрутів (для сумісності зі старим setBlockOnFull)
    private boolean defaultRouteBlockOnFull = true; // true=BLOCK, false=DROP

    // Втрати джерела при DROP
    private int routeLoss = 0;

    // Статистика
    private double meanQueue = 0.0;
    private double busyTime = 0.0;

    // Роутинг: або пріоритети, або ймовірності
    private final List<Element> prioElements = new ArrayList<>();
    private final List<Integer> prioValues   = new ArrayList<>();
    private final List<Boolean> prioBlockOnFull = new ArrayList<>();
    private boolean usePriorityRouting = false;

    private final List<Element> nextElements = new ArrayList<>();
    private final List<Double>  nextProbs    = new ArrayList<>();
    private final List<Boolean> nextBlockOnFull = new ArrayList<>();
    private boolean useProbRouting = false;

    public Process(String nameOfElement, double delay) {
        super(nameOfElement, delay);
        setServers(1);
        super.setTnext(Double.MAX_VALUE);
    }

    // -------------------- Конфіг --------------------
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

    // Залишено для сумісності: встановлює дефолтну політику для маршрутів без явної policy
    public void setBlockOnFull(boolean blockOnFull) {
        this.defaultRouteBlockOnFull = blockOnFull;
    }

    public int  getServers() { return servers; }
    public int  getBusy()    { return busy; }

    // -------------------- Роутинг API --------------------
    // Пріоритет: більше = вищий; явна політика для маршруту
    public void addPriorityRoute(Element next, int priority, Boolean blockOnFullPerRoute) {
        if (useProbRouting) throw new IllegalStateException("Cannot mix priority and probability routing");
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        prioElements.add(next);
        prioValues.add(priority);
        prioBlockOnFull.add(blockOnFullPerRoute != null ? blockOnFullPerRoute : defaultRouteBlockOnFull);
        usePriorityRouting = true;
    }
    // Back-compat: без політики → використовуємо дефолтну
    public void addPriorityRoute(Element next, int priority) {
        addPriorityRoute(next, priority, null);
    }

    // Імовірнісний: сума = 1.0; явна політика для маршруту
    public void addNextElement(Element next, double probability, Boolean blockOnFullPerRoute) {
        if (usePriorityRouting) throw new IllegalStateException("Cannot mix priority and probability routing");
        if (next == null) throw new IllegalArgumentException("next element cannot be null");
        if (probability < 0 || probability > 1) throw new IllegalArgumentException("probability must be in [0,1]");
        nextElements.add(next);
        nextProbs.add(probability);
        nextBlockOnFull.add(blockOnFullPerRoute != null ? blockOnFullPerRoute : defaultRouteBlockOnFull);
        useProbRouting = true;
    }
    // Back-compat: без політики → використовуємо дефолтну
    public void addNextElement(Element next, double probability) {
        addNextElement(next, probability, null);
    }

    private boolean hasAnyRoute() {
        if (usePriorityRouting && !prioElements.isEmpty()) return true;
        if (useProbRouting && !nextElements.isEmpty()) return true;
        return getNextElement() != null;
    }

    // -------------------- Події --------------------
    @Override
    public void inAct() {
        if (busy < servers) {
            startServiceNow();
        } else {
            if (queue < maxqueue) queue++;
            else failure++; // відмова на вході у ЦІЛЬ
        }
    }

    @Override
    public void outAct() {
        int idx = indexOfMinFinishTime();
        if (finishTimes[idx] == Double.MAX_VALUE) return;

        super.outAct();

        // SINK: немає виходів
        if (!hasAnyRoute()) {
            finishTimes[idx] = Double.MAX_VALUE;
            busy--;
            while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
            updateAggregateStateFromServers();
            return;
        }

        Element target = null;
        boolean policyBlock = defaultRouteBlockOnFull; // на випадок single nextElement
        boolean routeAvailable = true;

        if (usePriorityRouting && !prioElements.isEmpty()) {
            // Пріоритети: зверху вниз
            TreeSet<Integer> levelsDesc = new TreeSet<>(Comparator.reverseOrder());
            levelsDesc.addAll(prioValues);

            Element available = null;
            Element dropTarget = null;
            Element lastBlockCandidate = null;

            outer:
            for (Integer level : levelsDesc) {
                for (int i = 0; i < prioElements.size(); i++) {
                    if (!prioValues.get(i).equals(level)) continue;
                    Element e = prioElements.get(i);
                    if (e == null) continue;

                    boolean isBlock = prioBlockOnFull.get(i);

                    if (e.canAccept()) {
                        available = e;
                        break outer; // знайшли доступного
                    } else {
                        if (!isBlock) {
                            // DROP-маршрут і ціль переповнена → одразу фіксуємо відмову на цій цілі
                            dropTarget = e;
                            break outer;
                        } else {
                            // BLOCK-маршрут переповнений → йдемо далі, запам'ятовуємо останнього блочного кандидата
                            lastBlockCandidate = e;
                        }
                    }
                }
            }

            if (available != null) {
                target = available;
                policyBlock = true; // значення неважливе, бо ціль доступна
                routeAvailable = true;
            } else if (dropTarget != null) {
                target = dropTarget;
                policyBlock = false; // виконаємо DROP
                routeAvailable = false;
            } else {
                // Всі були BLOCK і всі переповнені → блокуємось на останньому (найнижчому) пріоритеті
                target = lastBlockCandidate;
                policyBlock = true;
                routeAvailable = false;
            }

        } else if (useProbRouting && !nextElements.isEmpty()) {
            // Ймовірнісний вибір однієї гілки
            int chosenIdx = chooseProbabilityIndex();
            target = nextElements.get(chosenIdx);
            policyBlock = nextBlockOnFull.get(chosenIdx);
            routeAvailable = target != null && target.canAccept();

        } else {
            // Single nextElement (сумісність)
            target = getNextElement();
            policyBlock = defaultRouteBlockOnFull;
            routeAvailable = target != null && target.canAccept();
        }

        if (routeAvailable && target != null) {
            // Передаємо далі
            finishTimes[idx] = Double.MAX_VALUE;
            busy--;
            target.inAct();
            while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
        } else {
            // Ціль недоступна → діємо за політикою маршруту
            if (policyBlock) {
                // Блокування на конкретній цілі
                blocked[idx] = true;
                blockedTarget[idx] = target;
                finishTimes[idx] = Double.MAX_VALUE; // сервер зайнятий, але завершення не планується
                blockCount++;
            } else {
                // DROP: атрибуція відмови у target
                routeLoss++;
                if (target != null) target.onDropFrom(this); // у цілі => failure++
                finishTimes[idx] = Double.MAX_VALUE;
                busy--;
                while (queue > 0 && busy < servers) { queue--; startServiceNow(); }
            }
        }

        updateAggregateStateFromServers();
    }

    private int chooseProbabilityIndex() {
        double sum = 0.0;
        for (double p : nextProbs) sum += p;
        if (Math.abs(sum - 1.0) > 1e-9) throw new IllegalStateException("Sum of routing probabilities must be 1.0");

        double r = Math.random(), acc = 0.0;
        for (int i = 0; i < nextElements.size() - 1; i++) {
            acc += nextProbs.get(i);
            if (r < acc) return i;
        }
        return nextElements.size() - 1;
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

    // -------------------- Розблокування --------------------
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

    // -------------------- Статистика --------------------
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
                ", defaultRouteBlockOnFull = " + defaultRouteBlockOnFull);
    }

    // Getters/Setters
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

    @Override
    public void onDropFrom(Element from) {
        // Відмова на вході в цей процес, коли джерело змушене було дропнути клієнта
        failure++;
    }
}