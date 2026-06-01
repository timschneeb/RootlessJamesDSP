# ToneForge Roadmap

ToneForge is moving from a RootlessJamesDSP-derived effects app toward a
programmable, deterministic DSP host. This roadmap describes the architectural
direction without changing the current real-time safety contract.

## Phase 1 - Runtime Foundation

Status: mostly implemented.

Completed:

- DSPState snapshot system.
- Double-buffered native `DSPState` storage.
- Atomic active-state publishing.
- Block-level snapshot capture for DSP chain, output gain, limiter, and
  LiveProg sync.
- Table-driven native DSP execution chain.
- Runtime module ordering APIs.
- Persistent Android module order using stable internal names.
- Main DSP screen order controls.
- DSP Chain Inspector.
- LiveProg runtime with optional `@init`, optional `@block`, and required
  `@sample`.
- 128-slider host path.
- Custom parameter aliases.
- Name-based LiveProg parameter persistence.
- Android system variables exposed to LiveProg through `DSPState`.
- PRNG variables for LiveProg.
- Analog noise helper variables for LiveProg.
- Hardcoded final limiter outside the reorderable chain.

Still to audit or harden:

- Native parser robustness.
- Remaining legacy module dormant/hidden state.
- Denormal protection across all DSP modules.
- Real-time lock audit for modules that still require legacy protection.

## Phase 2 - Runtime Expansion

Remaining work:

- Parser hardening with comment-aware and section-aware scanning.
- `@slider` lifecycle support.
- Dirty tracking for slider/parameter changes.
- Sample-accurate parameter interpolation.
- Native envelope follower helpers.
- Native smoothing utilities.
- State reset API for scripts/modules.
- Seeded deterministic PRNG helper functions.
- `gmem` / shared memory between scripts.
- FFT helper layer.
- M/S helper utilities.
- Band-split helper utilities.
- DSP safety/debug meters.

Goal: make LiveProg a stronger musical DSP runtime while keeping all expensive
work outside the audio callback and all per-sample work deterministic.

## Phase 3 - DSP Module Modernization

### Analog Modeling

Direction: replace or heavily rework the legacy Vacuum Tube implementation with
a new NFX-style analog modeling architecture.

Potential elements:

- tube preamp and power-stage models,
- dynamic bias,
- sag,
- transformer saturation,
- grid conduction,
- slew behavior,
- asymmetry drift,
- dynamic high-frequency rolloff,
- analog noise,
- crackle and wear/era controls,
- oversampling where needed.

### Stereo Enhancement

Direction: migrate Dynamics Ultimate-inspired concepts into a safer modular
stereo processor.

Potential elements:

- M/S utilities,
- mono-safe low-end behavior,
- harmonic side generation,
- per-band width,
- frequency-dependent width,
- transient-aware width control,
- width skew processing.

### Multimodal EQ

Direction: evolve Multimodal EQ into a clearer musical EQ module.

Targets:

- GraphicEQ mode,
- Parametric EQ mode,
- unified filter infrastructure,
- stable UI/parameter model,
- separation from Arbitrary Response EQ.

### Arbitrary Response EQ

Direction: keep Arbitrary Response EQ as a separate target-response/correction
module rather than merging it into Multimodal EQ.

## Phase 4 - Low Latency Architecture

Direction: introduce explicit latency-aware processing without compromising
system-wide DSP stability.

Planned concepts:

- Low Latency / Video mode.
- Ultra Low Latency / Gaming mode.
- Quality / Music mode.
- Per-module latency reporting.
- Total chain latency reporting.
- Latency classification by active module set.
- Latency-aware processing choices for FIR/lookahead/oversampling modules.
- Optional user-facing video synchronization guidance.

Global Android DSP should not blindly implement video delay. The system should
prefer latency reporting and latency-aware modes.

## Phase 5 - Hot Reload

Direction: make LiveProg editing fast without causing audio dropouts.

Planned concepts:

- background script parse and compile,
- validation before publishing,
- glitch-free runtime replacement,
- crossfaded script replacement where needed,
- preserved parameter state across reloads,
- failure-safe rollback to the previous compiled script.

## Phase 6 - ToneForge VST3

Direction: share the core runtime concepts with a desktop plugin host.

Planned concepts:

- shared DSP runtime architecture,
- portable LiveProg-compatible scripting layer,
- reusable DSP modules where feasible,
- desktop DSP host wrapper,
- VST3 parameter bridge,
- code sharing between Android and VST3 without forcing Android package/JNI
  renames.

## Design Principles

- Real-time safety first.
- Deterministic DSP execution.
- One coherent state snapshot per audio block.
- Scriptability without compromising the audio thread.
- Modular architecture with clear safety boundaries.
- Limiter always final.
- User programmability as a core feature.
- Preserve compatibility with RootlessJamesDSP heritage where ABI/package
  stability matters.
