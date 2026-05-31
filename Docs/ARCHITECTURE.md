# ARCHITECTURE.md

## 1. Project Overview

This project is a modified fork of RootlessJamesDSP, redesigned into a deterministic audio DSP engine with an embedded EEL2 (NS-EEL) scripting VM.

The system is NOT a traditional audio effects app.

It is a:

> Real-time DSP execution engine with scriptable control layer and fixed safety constraints.

---

## 2. Core Design Philosophy

### 2.1 Deterministic Audio Pipeline
All audio processing must follow a deterministic, fixed execution model per buffer.

No dynamic graph execution is allowed in the audio thread.

---

### 2.2 Separation of Concerns

The system is split into three layers:

1. UI Layer (Android)
2. DSP Host Layer (C++ / Java bridge)
3. EEL2 Virtual Machine (embedded interpreter)

---

### 2.3 Single Source of Truth

All runtime parameters must be stored in a shared DSPState snapshot.

No DSP module is allowed to own independent global state affecting audio processing.

---

## 3. Audio Processing Model

### 3.1 Global Pipeline

Audio processing follows this fixed structure:


Input
↓
DSPChain (ordered, user-defined)
↓
Post-Processing Stage
↓
Limiter (ALWAYS LAST - HARD SAFETY STAGE)
↓
Output


---

### 3.2 DSPChain Rules

- DSPChain is an ordered list of processing modules.
- Order can be changed at runtime.
- Changes must be applied via atomic snapshot swap.
- No dynamic branching inside audio callback.

Example chain:


EQ → AnalogModel → EEL2 → Spatial


---

### 3.3 Post-Processing Stage

This stage exists outside DSPChain and is NOT user configurable.

Used for:
- final gain staging
- system safety normalization
- pre-limiter corrections (if needed)

---

### 3.4 Limiter (Hard Constraint)

The limiter is a mandatory final stage.

Rules:
- Always executed last
- Cannot be disabled
- Cannot be reordered
- Cannot be part of DSPChain

---

## 4. DSPState System

### 4.1 Definition

DSPState is a lock-free shared snapshot used across:

- Audio thread
- DSPChain modules
- EEL2 VM
- UI thread (read-only)

### 4.2 Structure

Example fields:

- slider1, slider2, ...
- rms
- peak
- headroom
- system_volume

### 4.3 Rules

- Must be updated atomically or via double-buffer swap
- No allocations in audio thread
- Read-only access in DSP execution loop
- Updated per audio block (@block system)

---

## 5. @slider System (Event-Driven Control)

### 5.1 Concept

@slider is NOT part of EEL2 language.

It is a host-level event system.

### 5.2 Behavior

When UI slider changes:


UI Event → DSPState update → immediate visibility in audio thread


### 5.3 Constraints

- No polling
- No timers
- No blocking operations
- No direct audio thread calls from UI

---

## 6. @block System (Audio Analysis Scheduler)

### 6.1 Concept

@block is a host-level scheduler executed every N samples.

### 6.2 Responsibilities

Executed per block:

- RMS calculation
- Peak detection
- Envelope tracking
- Headroom estimation

### 6.3 Output

Results are stored in DSPState snapshot.

### 6.4 Constraints

- Must be deterministic
- Must not allocate memory
- Must not call UI
- Must be independent from EEL execution

---

## 7. EEL2 VM Integration

### 7.1 Role

EEL2 is used as a runtime scripting engine for:

- DSP parameter modulation
- nonlinear processing logic
- control signal generation

### 7.2 Execution Model

- @init executed once
- @sample executed per audio sample

### 7.3 Binding Model

All variables are bound via direct memory pointers to DSPState.

No runtime string lookups are allowed.

---

## 8. DSP Modules

Each DSP module must follow:


interface DSPModule {
process(buffer, frames, DSPState)
}


### Rules:
- Stateless or explicitly state-bound via DSPState
- No hidden global state
- Must be reorderable within DSPChain

---

## 9. Threading Model

### 9.1 UI Thread

- Handles user input
- Emits events only
- Never touches audio buffers

### 9.2 Audio Thread

- Real-time DSP execution
- No allocations
- No locks
- No JNI/Java calls

### 9.3 State Sharing

- DSPState is shared via atomic snapshot or double buffering

---

## 10. Performance Constraints

- No dynamic memory allocation in audio thread
- No string operations in DSP loop
- No locking mechanisms in realtime path
- Constant-time DSP operations per block

---

## 11. Forbidden Patterns

The following are explicitly forbidden:

- Dynamic DSP graph execution in audio thread
- UI polling loops
- Per-sample heap allocations
- Multiple conflicting DSP pipelines
- Non-deterministic ordering of DSPChain
- Limiter inside DSPChain

---

## 12. Design Goal

The final system should behave like:

> A real-time DSP engine with a deterministic processing chain and an embedded scripting VM controlling behavior at runtime.

---

## 13. Future Extensions (Allowed)

- Advanced DSPChain reordering UI
- EEL2-based modulation routing
- Additional analysis metrics in @block system
- SIMD optimization of DSP modules
- External plugin node system (optional future layer)

---

## 14. Summary

This architecture transforms RootlessJamesDSP from a fixed-effects audio application into a deterministic, scriptable DSP runtime engine with:

- Ordered DSP execution chain
- Embedded EEL2 VM
- Centralized DSP state model
- Event-driven UI control system
- Hard safety limiter stage