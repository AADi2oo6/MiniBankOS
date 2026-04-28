# MiniBankOS Scheduler Demonstration Scenarios

This document outlines different test cases you can build in the **OS Visualizer Dashboard** to showcase the functionality of our new **True Preemptive Round Robin & Priority** scheduling simulation to your professor. 

We now support **Arrival Times** and true **Time-Slicing** (Quantum), meaning the CPU will chop processes into blocks if they don't finish in one go, simulating context switches accurately!

---

## Scenario 1: Pure Round Robin Fairness (Time Slicing)
**Objective:** Demonstrate that when processes have the identical priority, the scheduler grants CPU time slices equally without starvation, preempting processes after their quantum expires.

**How to build in the Dashboard:**
Set **Quantum** to `3`. Leave the "Priority" as `1` and "Arrival" as `0` for all tasks.

| Order | Task Type       | Priority | Burst Time | Arrival Time |
|-------|-----------------|----------|------------|--------------|
| 1     | Deposit         | 1        | 7          | 0            |
| 2     | Check Balance   | 1        | 3          | 0            |
| 3     | Apply Loan      | 1        | 5          | 0            |

**Expected Gantt Chart Output:**
The Gantt chart will dynamically fragment execution:
1. **Deposit (3cs)** [Preempted]
2. **Check Balance (3cs)** [Finished!]
3. **Apply Loan (3cs)** [Preempted]
4. **Deposit (3cs)** [Preempted]
5. **Apply Loan (2cs)** [Finished!]
6. **Deposit (1cs)** [Finished!]

---

## Scenario 2: Strict Priority Preemption
**Objective:** Prove that high-priority tasks always execute first. With our update, a higher number = higher priority.

**How to build in the Dashboard:**
Set **Quantum** to `3` or higher. Notice we put the highest priority (10) *last*.

| Order | Task Type       | Priority | Burst Time | Arrival Time |
|-------|-----------------|----------|------------|--------------|
| 1     | Withdraw        | 2        | 6          | 0            |
| 2     | Transfer        | 5        | 4          | 0            |
| 3     | Deposit         | 10       | 5          | 0            |

**Expected Gantt Chart Output:**
Because priority 10 is the highest, the chart groups by priority order regardless of insertion:
1. **Deposit (5cs) - Priority 10** [Finished!]
2. **Transfer (4cs) - Priority 5** [Finished!]
3. **Withdraw (6cs) - Priority 2** [Finished!]

*(Note: They might be split into 3cs chunks if Quantum is 3, but P3 will still fully finish before P2 starts, and P2 before P1 starts!)*

---

## Scenario 3: Preemptive Arrival Interruption (The Hybrid Test)
**Objective:** Show the *real* power of preemption. A low-priority process is running, but suddenly a high-priority process *arrives* later in time. The CPU must kick out the lower-priority process and give the CPU to the new arrival!

**How to build in the Dashboard:**
Set **Quantum** to `4`.

| Order | Task Type       | Priority | Burst Time | Arrival Time |
|-------|-----------------|----------|------------|--------------|
| 1     | Apply Loan      | 2        | 10         | 0            |
| 2     | Withdraw        | 10       | 3          | 4            |

**Expected Gantt Chart Output:**
1. **Apply Loan (4cs)** [Runs from 0cs to 4cs]
2. **Withdraw (3cs)** [At 4cs, Withdraw arrives! Since it's priority 10, it instantly steals the CPU and finishes]
3. **Apply Loan (4cs)** [Resumes]
4. **Apply Loan (2cs)** [Finished!]

---

## Scenario 4: The Convoy Effect (Famous OS Concept)
**Objective:** Show your professor you understand OS pitfalls. The Convoy Effect happens when a massive process hogs the CPU, drastically inflating the Average Waiting Time for tiny processes behind it.

**How to build in the Dashboard:**
Set **Quantum** to `50` (effectively disabling Round Robin by making the slice huge). All priority `1`.

| Order | Task Type       | Priority | Burst Time | Arrival Time |
|-------|-----------------|----------|------------|--------------|
| 1     | Apply Loan      | 1        | 35         | 0            |
| 2     | Check Balance   | 1        | 2          | 1            |
| 3     | Check Balance   | 1        | 2          | 2            |

**Expected Analytics & Output:**
Execute this, and point out the **Average Wait Time (WT)** metric. Because the massive 35cs loan arrived first, the tiny balance checks have to wait 30+ cycles. The Gantt chart will show one massive block followed by two tiny ones.

**The Fix:** Change the **Quantum** back to `3` and simulate again! The Gantt chart will chop the Loan process up, allowing the tiny tasks to finish much faster, significantly dropping the Average Wait Time. This proves why Round Robin is powerful!
