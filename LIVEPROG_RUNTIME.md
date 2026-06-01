# LiveProg Runtime

LiveProg is ToneForge's programmable DSP runtime. It embeds an NS-EEL/EEL2 VM
inside the native DSP engine and lets users run JSFX-style scripts as one
module in the ToneForge processing chain.

LiveProg is real-time audio code. Script loading, parsing, compilation, and file
access happen outside the audio hot path. Per-block and per-sample execution use
pre-registered VM variables and the block's captured `DSPState` snapshot.

## Overview

LiveProg currently supports three script sections:

- `@init` - optional, executed once when the script is loaded.
- `@block` - optional, executed once per audio block.
- `@sample` - required, executed once per sample.

The runtime is JSFX-inspired, but it is not a full REAPER JSFX clone. There is
no `@gfx`, no DAW transport, no tempo source, and no host timeline.

## Execution Model

### Load Time

When a script is loaded:

1. the control side reads the script file,
2. metadata and parameter declarations are parsed,
3. native LiveProg splits the source into `@init`, `@block`, and `@sample`,
4. VM variables are registered,
5. `@init` is compiled and executed if present,
6. `@block` is compiled if present,
7. `@sample` is compiled and must be present.

If `@sample` is missing, the script does not compile as an audio processor.
`@init` and `@block` are optional.

### Per Audio Block

For each audio block where LiveProg is enabled and compiled:

1. LiveProg receives the same `DSPState` snapshot captured for the whole DSP
   block.
2. `slider1` through `slider128` are copied from `DSPState`.
3. custom parameter aliases are copied from their mapped slider slots.
4. block analysis helpers are computed from the incoming block at the point
   where LiveProg appears in the chain.
5. Android/system variables are copied from `DSPState`.
6. `samplesblock` is updated.
7. `@block` executes if present.
8. `@sample` executes once for every stereo sample.

### Per Sample

Before each `@sample` execution:

- `spl0` receives the current left sample,
- `spl1` receives the current right sample,
- PRNG helper variables are refreshed,
- analog noise helper variables are refreshed.

After `@sample`, `spl0` and `spl1` are copied back to the DSP buffers. NaN and
infinite script outputs are clamped to zero to avoid permanent audio loss.

## Built-In Host Variables

### Audio Variables

- `srate` - current DSP sample rate.
- `num_ch` - channel count, currently stereo-oriented and set to `2`.
- `samplesblock` - number of samples in the current audio block.
- `spl0` - current left sample during `@sample`.
- `spl1` - current right sample during `@sample`.

### Analysis Variables

These are computed once per block before `@block` and before per-sample script
processing:

- `block_rms_l` - RMS value of the left channel for the current block.
- `block_rms_r` - RMS value of the right channel for the current block.
- `block_peak_l` - peak absolute left-channel sample in the current block.
- `block_peak_r` - peak absolute right-channel sample in the current block.
- `block_dc_l` - average left-channel DC offset in the current block.
- `block_dc_r` - average right-channel DC offset in the current block.

### Android System Variables

These are updated by Android/control code and published to the audio side
through `DSPState`:

- `device_volume`
- `device_volume_db`
- `device_muted`
- `headset_connected`
- `bluetooth_audio`
- `audio_route`

The audio thread does not poll Android APIs. It only reads the captured
snapshot.

### PRNG Variables

These variables are refreshed during per-sample execution using native PRNG
state:

- `rand_uniform` - nominal 0..1 random value.
- `rand_bipolar` - nominal -1..1 random value.
- `rand_gauss` - Gaussian-like random value generated from uniform draws.

The current implementation exposes these as variables, not callable seeded
functions.

### Analog Noise Helpers

These variables provide low-rate and character noise sources for analog-style
scripts:

- `noise_slow_l`
- `noise_slow_r`
- `noise_bias_l`
- `noise_bias_r`
- `noise_crackle_l`
- `noise_crackle_r`

The left/right helpers are updated from separate native PRNG state.

## Parameter System

### Sliders

LiveProg supports:

```text
slider1 ... slider128
```

The native slider count is `JDSP_EEL_SLIDER_COUNT`, currently `128`.

Android can push slider values through the engine's `setSlider(index, value)`
path. Native code stores those values in `DSPState.sliders[]`; LiveProg copies
them into VM variables at the start of each block.

### Custom Parameter Aliases

LiveProg discovers JSFX-style parameter declarations such as:

```eel
gain:1<0,2,0.01>Output Gain
body_mode:1<0,2,1{Soft,Medium,Hard}>Punch Mode
slider1:0<-12,12,0.1>Input Trim
```

Custom names are registered as EEL variables and mapped to host slider slots.
If the declared name is `sliderN`, it maps to canonical slot `N - 1`. Other
custom names map to sequential host slots.

### Persistence

Android persists LiveProg parameters by stable parameter name. The current UI
uses a per-script key:

```text
liveprog_params_<scriptIdentity>
```

Values are stored as a JSON map from parameter name to numeric value. The
legacy semicolon-separated `key_liveprog_sliders` string is still maintained for
engine synchronization compatibility and migration.

### Stable Script Identity

A script can define a stable identity with:

```eel
// @id my_script_id
```

If present, that ID is preferred for parameter persistence. If no explicit ID is
present, the runtime falls back to a file/path-derived identity. This lets users
rename files or reorder parameters without necessarily losing saved values when
`@id` remains stable.

## Native Helper Functions

ToneForge currently exposes LiveProg host helpers primarily as VM variables, not
as custom callable native functions.

Currently implemented ToneForge-specific helper variables:

- block analysis: `block_rms_l/r`, `block_peak_l/r`, `block_dc_l/r`,
- Android/system state: `device_volume`, `device_volume_db`, `device_muted`,
  `headset_connected`, `bluetooth_audio`, `audio_route`,
- PRNG values: `rand_uniform`, `rand_bipolar`, `rand_gauss`,
- analog noise values: `noise_slow_l/r`, `noise_bias_l/r`,
  `noise_crackle_l/r`,
- sliders and custom parameter aliases.

No ToneForge-specific seeded callable helpers such as `rand_uniform_seed(seed)`
are currently registered in the LiveProg VM. Scripts also inherit the bundled
NS-EEL/EEL2 language and standard math/operator behavior from the embedded
engine.

## Real-Time Safety Rules

LiveProg must preserve the audio-thread contract:

- no heap allocation during `@sample`,
- no file I/O during `@sample` or `@block`,
- no Android calls from script execution,
- no string parsing in the audio callback,
- no JNI calls from the audio callback,
- no dynamic container use in the DSP hot path,
- deterministic per-block and per-sample behavior.

The script VM and all needed VM variables are prepared during load/compile.
During processing, the runtime uses already-registered variable pointers and the
captured `DSPState` snapshot.

## Current Limitations

- `@sample` is required.
- `@init` and `@block` are optional.
- `@gfx` is not supported.
- There is no DAW transport, tempo, beat, or timeline source.
- Parser hardening is still needed; section-marker parsing can be improved.
- Seeded PRNG helper functions are planned but not currently exposed.
- Shared memory / `gmem` is not implemented.
- FFT helper functions for scripts are not implemented as a ToneForge layer.

## Planned Runtime Features

Planned work includes:

- parser hardening,
- `@slider` lifecycle support,
- dirty tracking for slider/parameter updates,
- sample-accurate parameter interpolation,
- native envelope follower helpers,
- state reset API,
- native smoothing utilities,
- native M/S utilities,
- band-split helpers,
- `gmem` / shared memory between scripts,
- FFT helper layer,
- oversampling helpers,
- latency reporting,
- DSP debugging and safety meters,
- glitch-free hot reload and crossfaded script replacement.
