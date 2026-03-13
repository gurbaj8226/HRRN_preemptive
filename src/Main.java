import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {

    static class Process {
        int id;
        int arrivalTime;
        int burstTime;
        int relativeDeadline;
        int absoluteDeadline;

        int remainingTime;
        int executedTime;
        int startTime;
        int completionTime;

        Process(int id, int arrivalTime, int burstTime, int relativeDeadline) {
            this.id = id;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.relativeDeadline = relativeDeadline;
            this.absoluteDeadline = arrivalTime + relativeDeadline;

            this.remainingTime = burstTime;
            this.executedTime = 0;
            this.startTime = -1;
            this.completionTime = -1;
        }

        int turnaroundTime() {
            return completionTime - arrivalTime;
        }

        int waitingTime() {
            return turnaroundTime() - burstTime;
        }

        double responseRatioAt(int currentTime) {
            int waiting = currentTime - arrivalTime - executedTime;
            return (waiting + remainingTime) / (double) remainingTime;
        }

        boolean metDeadline() {
            return completionTime <= absoluteDeadline;
        }

        int lateness() {
            return Math.max(0, completionTime - absoluteDeadline);
        }
    }

    /*
     * =========================
     * EASY INPUT CHANGES HERE
     * =========================
     *
     * A classmate can change these numbers directly without making a CSV file.
     */

    static final int PROCESS_COUNT = 100;

    // First group arrives together at time 0
    static final int INITIAL_GROUP_SIZE = 10;

    // After the first group, one new process arrives every ARRIVAL_STEP units
    static final int ARRIVAL_STEP = 5;

    // Cycles used for burst times and relative deadlines
    static final int[] BURST_CYCLE = {5, 10, 15, 20, 25, 30, 35, 40, 45, 50};
    static final int[] DEADLINE_CYCLE = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};

    public static void main(String[] args) {
        List<Process> processes = generateWorkload();
        schedulePreemptiveHRRN(processes);
        printReport(processes);
    }

    /*
     * Default workload interpretation from the assignment:
     * - 100 processes total
     * - P0 to P9 arrive at time 0
     * - after that, one new process arrives every 5 time units
     * - burst times cycle 5,10,15,...,50
     * - relative deadlines cycle 10,20,30,...,100
     *
     * A peer can change PROCESS_COUNT, INITIAL_GROUP_SIZE, ARRIVAL_STEP,
     * BURST_CYCLE, or DEADLINE_CYCLE above to test another arrival scheme.
     */
    static List<Process> generateWorkload() {
        List<Process> processes = new ArrayList<>();

        for (int i = 0; i < PROCESS_COUNT; i++) {
            int arrivalTime = (i < INITIAL_GROUP_SIZE) ? 0 : (i - INITIAL_GROUP_SIZE + 1) * ARRIVAL_STEP;
            int burstTime = BURST_CYCLE[i % BURST_CYCLE.length];
            int relativeDeadline = DEADLINE_CYCLE[i % DEADLINE_CYCLE.length];

            processes.add(new Process(i, arrivalTime, burstTime, relativeDeadline));
        }

        return processes;
    }

    static void schedulePreemptiveHRRN(List<Process> processes) {
        int currentTime = 0;
        int completed = 0;

        while (completed < processes.size()) {
            Process chosen = null;
            double bestRatio = -1.0;

            for (Process p : processes) {
                if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                    double ratio = p.responseRatioAt(currentTime);

                    if (chosen == null
                            || ratio > bestRatio
                            || (Math.abs(ratio - bestRatio) < 1e-12
                            && p.arrivalTime < chosen.arrivalTime)
                            || (Math.abs(ratio - bestRatio) < 1e-12
                            && p.arrivalTime == chosen.arrivalTime
                            && p.id < chosen.id)) {
                        chosen = p;
                        bestRatio = ratio;
                    }
                }
            }

            if (chosen == null) {
                int nextArrival = Integer.MAX_VALUE;

                for (Process p : processes) {
                    if (p.remainingTime > 0 && p.arrivalTime > currentTime) {
                        nextArrival = Math.min(nextArrival, p.arrivalTime);
                    }
                }

                if (nextArrival == Integer.MAX_VALUE) {
                    break;
                }

                currentTime = nextArrival;
                continue;
            }

            if (chosen.startTime == -1) {
                chosen.startTime = currentTime;
            }

            // Preemptive version: run for one time unit, then choose again
            chosen.remainingTime--;
            chosen.executedTime++;
            currentTime++;

            if (chosen.remainingTime == 0) {
                chosen.completionTime = currentTime;
                completed++;
            }
        }
    }

    static void printReport(List<Process> processes) {
        processes.sort(Comparator.comparingInt(p -> p.id));

        double totalTurnaround = 0.0;
        double totalWaiting = 0.0;
        int metDeadlineCount = 0;

        System.out.println("Algorithm: Preemptive Highest Response Ratio Next (HRRN) Variant");
        System.out.println("Response Ratio = (waiting time + remaining time) / remaining time");
        System.out.println();
        System.out.println("PID | Arr | Burst | RelDL | AbsDL | Start | Complete | Turnaround | Wait | MetDL | LateBy");
        System.out.println("----+-----+-------+-------+-------+-------+----------+------------+------+-------+-------");

        for (Process p : processes) {
            totalTurnaround += p.turnaroundTime();
            totalWaiting += p.waitingTime();

            if (p.metDeadline()) {
                metDeadlineCount++;
            }

            System.out.printf(
                    "P%02d | %3d | %5d | %5d | %5d | %5d | %8d | %10d | %4d | %5s | %5d%n",
                    p.id,
                    p.arrivalTime,
                    p.burstTime,
                    p.relativeDeadline,
                    p.absoluteDeadline,
                    p.startTime,
                    p.completionTime,
                    p.turnaroundTime(),
                    p.waitingTime(),
                    p.metDeadline() ? "Yes" : "No",
                    p.lateness()
            );
        }

        System.out.println();
        System.out.printf("Average turnaround time: %.2f%n", totalTurnaround / processes.size());
        System.out.printf("Average waiting time: %.2f%n", totalWaiting / processes.size());
        System.out.printf("Processes meeting deadline: %d/%d%n", metDeadlineCount, processes.size());
        System.out.printf("Processes missing deadline: %d/%d%n",
                processes.size() - metDeadlineCount, processes.size());
    }
}