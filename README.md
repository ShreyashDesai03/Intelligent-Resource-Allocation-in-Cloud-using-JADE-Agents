# Intelligent Resource Allocation in Cloud using JADE Agents

## 📌 Overview

This project implements an **intelligent cloud resource allocation framework** using **JADE (Java Agent DEvelopment Framework)** agents integrated with **CloudSim / CloudSim Plus** concepts. The system simulates **EC2-like virtual machine (VM) lifecycles** and demonstrates how **multi-agent systems** can be used for **load-aware scheduling, dynamic resource allocation, and real-time monitoring** in cloud environments.

The project is designed as an **academic + research-oriented simulation**, suitable for **final-year projects, cloud computing labs, and agent-based system studies**.

---

## 🎯 Objectives

* Simulate cloud VM lifecycle management (ALLOCATE, START, STOP, TERMINATE)
* Implement **agent-based decision making** using JADE
* Perform **load-aware scheduling** of cloud tasks
* Demonstrate **manager–resource–client agent coordination**
* Provide **real-time monitoring and analytics** via a web dashboard

---

## 🧠 System Architecture (High Level)

The system follows a **multi-agent architecture**:

* **ManagerAgent**
  Central coordinator responsible for:

  * Maintaining global resource state
  * Load-aware VM allocation decisions
  * Scheduling tasks to ResourceAgents

* **ResourceAgent(s)**
  Represent cloud hosts/VM providers:

  * Register with ManagerAgent
  * Maintain VM state (RUNNING / STOPPED / TERMINATED)
  * Execute tasks and report metrics

* **ClientAgent**
  Acts as a cloud user:

  * Submits workload / task requests
  * Receives execution results

* **Cloud Simulation Layer**
  Uses CloudSim-style abstractions for:

  * VM creation
  * Task execution
  * Performance measurement

* **Dashboard (Frontend)**
  React-based UI for:

  * Monitoring VM states
  * Viewing analytics and trends

---

## 🛠️ Tech Stack

### Core Technologies

* **Java** – Agent logic and simulation
* **JADE Framework** – Multi-agent system
* **CloudSim / CloudSim Plus concepts** – Cloud simulation

### Backend & Frontend

* **Node.js + Express** – Backend API
* **React.js** – Dashboard UI
* **Tailwind CSS** – UI styling

### Tools

* Git & GitHub
* Windows PowerShell

---

## 📂 Project Structure

```
Intelligent-Resource-Allocation-in-Cloud-using-JADE-Agents/
│
├── src/                    # JADE agents and cloud simulation code
│   ├── agents/             # ManagerAgent, ResourceAgent, ClientAgent
│   └── cloud/              # CloudSimEngine and simulation logic
│
├── backend/                # Node.js backend services
│
├── dashboard/              # React frontend (monitoring dashboard)
│
├── JADE-all-4.6.0/          # JADE framework (for local execution)
│
├── run.bat                 # Script to launch agents
├── .gitignore
└── README.md
```

---

## ▶️ How to Run (High Level)

1. **Start JADE platform**

   * Use `run.bat` or JADE boot command

2. **Launch agents**

   * ManagerAgent
   * One or more ResourceAgents
   * ClientAgent

3. **Start backend server**

   ```bash
   cd backend
   npm install
   npm start
   ```

4. **Run dashboard**

   ```bash
   cd dashboard
   npm install
   npm start
   ```

5. Open browser and monitor system behavior

---

## 📊 Key Features

* EC2-like VM lifecycle simulation
* Load-aware resource allocation
* Multi-agent coordination using JADE
* Auto-registration of ResourceAgents
* Real-time analytics dashboard
* Auto-scaling policies
* Modular and extensible design

---

## 🎓 Academic Relevance

This project is useful for:

* Cloud Computing courses
* Multi-Agent Systems
* Distributed Systems
* Final Year Engineering Projects

---

## 🚀 Future Enhancements

* Fault-tolerant agents
* Advanced scheduling algorithms
* Cloud cost optimization models
* Deployment on real cloud infrastructure

---

## 👤 Author

**Shreyash D Desai**
Computer Science Engineering Undergraduate

---

## 📜 License

This project is intended for **academic and educational use**.
