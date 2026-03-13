# Preemptive HRRN CPU Scheduler (Java)

This program simulates a **preemptive variant of the Highest Response Ratio Next (HRRN) CPU scheduling algorithm**.
It schedules processes dynamically based on their response ratio and reports the completion time and performance metrics for each process.

---

# What the Program Does

* Simulates **100 processes** using the workload described in the assignment.
* Uses **Preemptive HRRN scheduling**, recalculating priority every time unit.
* Selects the process with the **highest response ratio**:

```
Response Ratio = (waiting time + remaining time) / remaining time
```

* Prints scheduling results including:

  * arrival time
  * burst time
  * deadlines
  * completion time
  * turnaround time
  * waiting time
  * deadline status.

---

# Main Logic

The scheduler works as follows:

* Only processes that have **arrived and are not finished** are considered.
* The **response ratio is computed** for each ready process.
* The process with the **highest response ratio runs for one time unit**.
* The scheduler **re-evaluates priorities after each time unit**, making it preemptive.

---

# Customizing the Input (For Peer Testing)

To test a different arrival scheme, edit the constants near the top of `Main.java`.

```java
static final int PROCESS_COUNT = 100;
static final int INITIAL_GROUP_SIZE = 10;
static final int ARRIVAL_STEP = 5;

static final int[] BURST_CYCLE = {5,10,15,20,25,30,35,40,45,50};
static final int[] DEADLINE_CYCLE = {10,20,30,40,50,60,70,80,90,100};
```

You can change:

* **PROCESS_COUNT** → total number of processes
* **INITIAL_GROUP_SIZE** → how many processes arrive at time 0
* **ARRIVAL_STEP** → time gap between later arrivals
* **BURST_CYCLE** → repeating burst times
* **DEADLINE_CYCLE** → repeating deadlines

After modifying these values, recompile and run the program again.

---
