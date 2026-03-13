import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    }

    public static void main(String[] args) throws Exception {
        List<Process> processes;

        if (args.length == 1) {
            processes = loadFromCsv(args[0]);
        } else {
            processes = generateProfessorWorkload(100);
        }

        schedulePreemptiveHRRN(processes);
        printReport(processes);
    }

    /*
     * Default workload based on the assignment prompt.
     *
     * Interpretation used:
     * - burst times cycle as 5, 10, 15, ..., 50
     * - relative deadlines cycle as 10, 20, 30, ..., 100
     * - first 10 processes arrive at time 0
     * - after that, one new process arrives every 5 time units
     *
     * The prompt example appears to contain a typo, so deadlines are treated
     * as relative deadlines and absoluteDeadline = arrivalTime + relativeDeadline.
     * This matches the example (20,55,5,65).
     */
    static List<Process> generateProfessorWorkload(int count) {
        List<Process> processes = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int arrivalTime = (i < 10) ? 0 : (i - 9) * 5;
            int burstTime = ((i % 10) + 1) * 5;          // 5,10,...,50
            int relativeDeadline = ((i % 10) + 1) * 10; // 10,20,...,100

            processes.add(new Process(i, arrivalTime, burstTime, relativeDeadline));
        }

        return processes;
    }

    /*
     * CSV format:
     * id,arrival,burst,relativeDeadline
     *
     * Example:
     * 0,0,5,10
     * 1,0,10,20
     * 2,5,15,30
     */
    static List<Process> loadFromCsv(String fileName) throws IOException {
        List<Process> processes = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of(fileName));

        for (String raw : lines) {
            String line = raw.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s*,\\s*");
            if (parts.length < 4) {
                continue;
            }

            try {
                int id = Integer.parseInt(parts[0]);
                int arrival = Integer.parseInt(parts[1]);
                int burst = Integer.parseInt(parts[2]);
                int relativeDeadline = Integer.parseInt(parts[3]);
                processes.add(new Process(id, arrival, burst, relativeDeadline));
            } catch (NumberFormatException e) {
                // Skip header or malformed line
            }
        }

        processes.sort(Comparator.comparingInt(p -> p.id));
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
                currentTime = nextArrival;
                continue;
            }

            if (chosen.startTime == -1) {
                chosen.startTime = currentTime;
            }

            // Run for one time unit because this is the preemptive version.
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

        System.out.println("Algorithm: Preemptive Highest Response Ratio Next (HRRN)");
        System.out.println("Response Ratio = (waiting time + remaining time) / remaining time");
        System.out.println();
        System.out.println("PID | Arr | Burst | RelDL | AbsDL | Complete | Turnaround | Wait");
        System.out.println("----+-----+-------+-------+-------+----------+------------+------");

        for (Process p : processes) {
            totalTurnaround += p.turnaroundTime();
            totalWaiting += p.waitingTime();

            System.out.printf(
                "P%02d | %3d | %5d | %5d | %5d | %8d | %10d | %4d%n",
                p.id,
                p.arrivalTime,
                p.burstTime,
                p.relativeDeadline,
                p.absoluteDeadline,
                p.completionTime,
                p.turnaroundTime(),
                p.waitingTime()
            );
        }

        System.out.println();
        System.out.printf("Average turnaround time: %.2f%n", totalTurnaround / processes.size());
        System.out.printf("Average waiting time: %.2f%n", totalWaiting / processes.size());
    }
}
