# ToneForge Architecture

ToneForge is a programmable real-time DSP host for Android. It originated as a
RootlessJamesDSP fork and still preserves the JamesDSP lineage, but the current
direction is broader: a deterministic DSP runtime with scriptable processing,
runtime module ordering, a central state snapshot model, and a fixed output
safety stage.

This document describes the architecture as it exists in the current tree. It
is intentionally conservative: it documents implemented behavior and calls out
future work separately.

## Project Identity

- Project name: ToneForge.
- Origin: RootlessJamesDSP and libjamesdsp.
- Current direction: a programmable DSP host environment for Android, with a
  future path toward shared desktop/VST3 runtime components.
- Core values: real-time safety, deterministic processing, scriptability,
  modular ordering, and user-programmable sound design.

Package names, JNI symbols, and source paths still follow the RootlessJamesDSP
heritage in many places. Do not rename those casually; user-visible branding can
evolve independently from ABI and package identity.

## Core Architecture

ToneForge separates control work from audio work.

- Android UI and control code receives user input, stores preferences, loads
  scripts, and calls JNI/control APIs.
- Native DSP code owns the audio buffers and executes the processing chain.
- LiveProg embeds an NS-EEL/EEL2 virtual machine for user scripts.
- `DSPState` is the control-to-audio snapshot used by the native engine.

### DSPState Snapshot System

`DSPState` is the central real-time configuration snapshot. It currently stores:

- runtime DSP execution order,
- active module enable flags,
- output gain and limiter coefficients,
- LiveProg slider values,
- Android/system variables for LiveProg.

The native engine owns two `DSPState` buffers:

```text
stateBuffers[0]
stateBuffers[1]
activeState -> one of the two buffers
```

Control-side updates copy the active snapshot into the inactive buffer, modify
only the intended fields, then publish the inactive buffer with an atomic
release store. Audio processing captures `activeState` with an atomic acquire
load at the beginning of each block and uses that same pointer through chain
execution, LiveProg state sync, output gain, and limiter processing.

This keeps configuration coherent across one audio buffer and avoids mixed-state
processing inside a block.

### Real-Time Safety Model

The audio callback must remain deterministic:

- no heap allocation in the audio thread,
- no file I/O in the audio thread,
- no Android framework calls from the audio thread,
- no string parsing in the audio thread,
- no JNI calls from the audio thread,
- no locks in the DSP hot path except existing legacy/script protection paths
  where the current implementation still requires them,
- no dynamic containers in sample/block processing.

All expensive or mutable work belongs on the control/UI side and must be
published through precomputed data, existing native module state, or `DSPState`.

## DSP Processing Pipeline

The active ToneForge processing model is:

```text
Input
  -> reorderable DSP modules
  -> output gain
  -> final limiter
  -> Output
```

The current intended user-facing signal flow is:

1. Analog Modeling / Vacuum Tube (`tube`)
2. Multimodal EQ (`m_eq`)
3. Arbitrary Response EQ (`arb_eq`)
4. LiveProg Runtime (`liveprog`)
5. Stereo Enhancement (`ster_enh`)
6. Final Limiter / Output Safety

The first five stages are represented by the native `dsp_execution_chain` table
when compiled into this build. The final limiter is not in that table. It is
hardcoded after chain execution as the mandatory safety stage.

### Limiter Rule

The limiter is a hard architectural constraint:

- it is always last,
- it is not reorderable,
- it is not persisted as part of execution order,
- it is not included in `setExecutionOrder()`,
- it exists to protect output level and user safety.

UI must show the limiter as the final output/safety stage and must not imply
that it processes before the reorderable modules.

### Hidden Legacy Modules

Some inherited modules remain disabled or hidden unless explicitly re-enabled:

- Compressor,
- Dynamic Bass Boost,
- ViPER-DDC,
- Convolver,
- Crossfeed,
- Reverb / Virtual Room.

They should not be shown or persisted as active user modules by accident.

## Module Reordering

The native engine supports runtime execution order through:

- `getModules()`,
- `getExecutionOrder()`,
- `setExecutionOrder()`,
- `resetExecutionOrder()`.

The reorderable order is stored natively as module indices inside `DSPState`.
Android persistence stores stable internal module names instead of numeric
indices. The current persisted key is:

```text
dsp_execution_order
```

The value is a comma-separated list such as:

```text
tube,m_eq,arb_eq,liveprog,ster_enh
```

On engine startup, Android validates the saved names against the current native
module list, maps names back to native indices, and calls `setExecutionOrder()`.
Invalid, duplicate, unknown, or incomplete saved orders are ignored and the
native default is restored.

### Main DSP Screen

The main DSP screen mirrors native execution order and exposes direct Up/Down
controls for the active reorderable modules. The visual order is refreshed from
the engine after applying a new order. The limiter/output safety card remains
fixed at the bottom and has no reorder controls.

### DSP Chain Inspector

The DSP Chain Inspector is a debug/diagnostic view of the native execution
table. It reads the same `getModules()` and `getExecutionOrder()` data and can
also reorder/reset the runtime chain. Inspector changes update the persisted
order when the current build includes the persistence helper.

## LiveProg Runtime

LiveProg embeds an NS-EEL/EEL2 VM as a programmable DSP module. It supports:

- optional `@init`,
- optional `@block`,
- required `@sample`.

Script load and parsing happen off the audio hot path. During audio processing,
LiveProg receives the current block's captured `DSPState`, synchronizes sliders
and system variables, computes block analysis values, executes `@block` if
present, then runs `@sample` once per sample.

LiveProg is one reorderable module in the DSP chain. The limiter still remains
outside and after LiveProg regardless of module order.

## Android Integration

ToneForge uses the RootlessJamesDSP-style rootless Android audio path. Instead
of relying only on Android's built-in effects, the app captures supported app
audio through Android's internal capture path, processes it through the native
DSP engine, and returns processed audio to output.

Android-side responsibilities include:

- preference storage,
- preset/script file management,
- LiveProg parameter UI,
- system variable observation,
- module order persistence,
- calls into JNI control APIs.

Native audio processing must not call back into Android. Android state is pushed
into native code from the control side and published to the audio side through
`DSPState`.

## Control Thread vs Audio Thread

Control/UI thread:

- may allocate,
- may parse scripts,
- may read/write preferences and files,
- may call JNI setters,
- prepares new `DSPState` values through native control APIs.

Audio thread:

- captures one `DSPState` pointer per block,
- executes the module chain,
- applies output gain and limiter using that same snapshot,
- must avoid allocation, file I/O, Android calls, JNI calls, and broad locks.

## Future Architecture

The planned architecture direction includes:

- glitch-free LiveProg hot reload,
- crossfaded runtime replacement,
- sample-accurate parameter automation and interpolation,
- `@slider` lifecycle support,
- shared memory / `gmem` between scripts,
- FFT helper layer,
- envelope follower and smoothing helpers,
- native M/S and band-split helpers,
- oversampling utilities for analog modeling,
- latency reporting and latency-aware processing modes,
- a VST3 compatibility layer that shares runtime concepts with Android.

These items are future work unless specifically documented as implemented in
`LIVEPROG_RUNTIME.md` or source code.
