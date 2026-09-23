# Latency / A-V sync test loop

Measures how late the rootless engine's audible output is compared with what a video player
believes. This is the A/V offset YouTube and similar apps suffer.

## How it works

| Piece | Where | Role |
|---|---|---|
| Latency probe APK | `latencyprobe/` | Separate package (own UID) that plays isolated 2 ms pulses through a normal media `AudioTrack`. For each pulse it logs the presentation time `AudioTrack.getTimestamp()` reports: the clock ExoPlayer & co. sync video to. |
| `LatencyTracer` | `app/.../latency/LatencyTracer.kt` | In the engine loop. Detects each pulse pre-DSP and logs its capture, read, write, DSP-delay and presentation times, plus 1 Hz health lines (internal latency, queue depth, underruns). |
| `LatencyTuning` + debug receiver | `app/.../latency/LatencyTuning.kt`, `app/src/debug/...` | Buffer settings, adjustable over adb in debug builds. Defaults match the legacy loop. |
| `run_latency_test.py` | here | Orchestrates runs over adb, pairs probe and engine events, and writes a summary per run plus `results/history.csv`. |

All timestamps are `CLOCK_MONOTONIC`, so:

```
A/V offset = engine output present time - probe (source) present time
```

The breakdown splits that into:
- source → capture: tee plus remote submix. Usually negative, because the tee copies the audio before the muted original is due at the speaker.
- capture → read: `AudioRecord` buffering and block size.
- read → written: DSP time plus blocking write.
- written → present: output track queue plus HAL/device.
- DSP algorithmic delay.

## One-time setup

```bash
./gradlew assembleRootlessFullDebug :latencyprobe:assembleDebug

adb install -r app/build/outputs/apk/rootlessFull/debug/JamesDSP-*-rootless-full-arm64-v8a-debug.apk
adb install -r latencyprobe/build/outputs/apk/debug/latencyprobe-debug.apk
python tools/latency/run_latency_test.py setup
```

Stop any other JamesDSP install before measuring (`adb shell am force-stop me.timschneeberger.rootlessjamesdsp`).
Otherwise audio is processed twice. Pause all other media: the probe needs silence between pulses.

## The loop

```bash
# Baseline with legacy settings (engine on)
python tools/latency/run_latency_test.py run --label baseline

# One change at a time, A = legacy, B = the change; ABBA order, 2 pairs
python tools/latency/run_latency_test.py ab --label trackbuf --tune-b track_frames=3840
python tools/latency/run_latency_test.py ab --label lowlat --tune-b low_latency=true

# Drift over 10 minutes (latency creep from clock mismatch)
python tools/latency/run_latency_test.py soak --minutes 10 --label drift --tune low_latency=true

# Ground-truth cross-check on the phone speaker (probe records the mic)
python tools/latency/run_latency_test.py run --label spk-off --engine off --acoustic
python tools/latency/run_latency_test.py run --label spk-on --acoustic

# Android EQ mode (no capture loop): switch it on in the app, then time it acoustically
python tools/latency/run_latency_test.py run --label eq --acoustic --engine keep

python tools/latency/run_latency_test.py compare results/<runA>/summary.json results/<runB>/summary.json
```

Knobs (`--tune key=value`, anything not given is reset to legacy):

| key | legacy | meaning |
|---|---|---|
| `read_frames` | 0 (= whole buffer, 4096 frames at default settings) | frames per `AudioRecord.read()`; keep it constant (the engine re-plans convolution partitions when block size changes) |
| `track_frames` | 0 (= full capacity) | `AudioTrack.setBufferSizeInFrames()` cap on the output queue |
| `low_latency` | false | `PERFORMANCE_MODE_LOW_LATENCY` output track (fast/primary path instead of deep buffer) |
| `urgent_priority` | false | recorder thread at `THREAD_PRIORITY_URGENT_AUDIO` |
| `max_queue_frames` | 0 (off) | latency controller: drop one faded block when the output track holds more than this |
| `app_settings` | - | `true`: no override; the app's Low-latency mode setting decides (measures what ships) |
| `trace` | false | telemetry on (the harness always turns it on) |

Any other `--tune` value is stored as a debug override on top of the app setting. The shipped preset is
`LatencyTuning.LOW_LATENCY` (read 960, track 3840, fast track, controller 2880).

Each run directory holds:
- `events.jsonl`: raw telemetry
- `summary.json`
- `audio_flinger.txt`: mid-run `dumpsys media.audio_flinger`, showing thread latencies, `FrmCnt`/`FrmRdy` per track and underruns

## Manual tracing (e.g. real YouTube playback)

```bash
adb shell am broadcast -n me.timschneeberger.rootlessjamesdsp.debug/me.timschneeberger.rootlessjamesdsp.receiver.LatencyTuningReceiver --ez trace true
adb logcat -v raw -s JdspLatency:I
```

The `health` lines report `internal_ms`, which is capture → presentation inside the engine, continuously. That covers the whole A/V offset except the tee/submix part.

## Offline

```bash
python tools/latency/run_latency_test.py selftest                    # pairing/statistics check
python tools/latency/run_latency_test.py analyze results/<run>/events.jsonl
```
