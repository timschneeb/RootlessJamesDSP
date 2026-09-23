#!/usr/bin/env python3
"""
Latency / A-V sync test loop for the rootless JamesDSP capture engine.

What it measures
  The latency probe app plays isolated pulses through an ordinary media AudioTrack and logs when
  AudioTrack.getTimestamp() says each pulse is presented. That is the clock video players sync
  their frames to. The engine (debug build, trace on) logs when the processed pulse is actually
  presented by its own output track. Both are CLOCK_MONOTONIC, so

      A/V offset = engine_present - probe_present

  is exactly how late the audible sound is relative to what a video app believes.

Commands
  setup     grant permissions/app-ops needed for unattended runs (run once per install)
  tune      set engine latency knobs (debug receiver), e.g. tune read_frames=480 low_latency=true
  run       one measured run                    run --label baseline
  ab        alternating A/B runs (ABBA order)   ab --label lowlat --tune-b low_latency=true
  soak      long run for drift                  soak --minutes 10 --label drift
  analyze   re-analyze a saved events.jsonl
  compare   compare two summary.json files
  selftest  offline check of the pairing/statistics code

See tools/latency/README.md.
"""
from __future__ import annotations

import argparse
import csv
import json
import math
import os
import re
import shutil
import statistics
import subprocess
import sys
import threading
import time
from datetime import datetime
from pathlib import Path

ENGINE_PKG = "me.timschneeberger.rootlessjamesdsp.debug"
PROBE_PKG = "me.timschneeberger.latencyprobe"
APP_NS = "me.timschneeberger.rootlessjamesdsp"
TUNING_RECEIVER = APP_NS + ".receiver.LatencyTuningReceiver"
LAUNCHER_ACTIVITY = APP_NS + ".activity.EngineLauncherActivity"
PROBE_ACTIVITY = "me.timschneeberger.latencyprobe.ProbeActivity"
LOG_TAGS = ("JdspLatency", "JdspProbe")

# Mirrors LatencyTuning.kt keys
TUNING_KEYS = {
    "trace": bool,
    "read_frames": int,
    "track_frames": int,
    "low_latency": bool,
    "urgent_priority": bool,
    "max_queue_frames": int,
    "app_settings": bool,   # true: the app's Low-latency mode setting decides (no override)
}

HERE = Path(__file__).resolve().parent
DEFAULT_OUT = HERE / "results"

# Pairing window relative to the reference (probe) time
PAIR_LEAD_NS = 50_000_000


class HarnessError(RuntimeError):
    pass


# --------------------------------------------------------------------------------------------
# adb plumbing


def find_adb(explicit: str | None) -> str:
    candidates = [explicit, os.environ.get("ADB")]
    names = ("adb.exe", "adb") if os.name == "nt" else ("adb",)
    candidates += [shutil.which(n) for n in names]
    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(env):
            candidates.append(str(Path(os.environ[env]) / "platform-tools" / names[0]))
    for c in candidates:
        if c and Path(c).is_file():
            return c
    raise HarnessError("adb not found; pass --adb or set ADB / ANDROID_HOME")


class Adb:
    def __init__(self, exe: str, serial: str | None):
        self.exe = exe
        self.serial = serial

    def argv(self, *args: str) -> list[str]:
        base = [self.exe]
        if self.serial:
            base += ["-s", self.serial]
        return base + list(args)

    def cmd(self, *args: str, check: bool = True, timeout: float = 60) -> str:
        proc = subprocess.run(self.argv(*args), capture_output=True, text=True,
                              encoding="utf-8", errors="replace", timeout=timeout)
        if check and proc.returncode != 0:
            raise HarnessError(f"adb {' '.join(args)} failed ({proc.returncode}): {proc.stderr.strip()}")
        # Unchecked callers inspect the text for errors, and am/appops report them on stderr
        return proc.stdout if check else proc.stdout + proc.stderr

    def shell(self, command: str, check: bool = True, timeout: float = 60) -> str:
        return self.cmd("shell", command, check=check, timeout=timeout)

    def ensure_device(self) -> None:
        state = self.cmd("get-state", check=False).strip()
        if state != "device":
            raise HarnessError("no authorized device (adb get-state != device); connect a phone with USB debugging")

    def device_info(self) -> dict:
        def prop(name: str) -> str:
            return self.shell(f"getprop {name}", check=False).strip()
        return {
            "manufacturer": prop("ro.product.manufacturer"),
            "model": prop("ro.product.model"),
            "sdk": prop("ro.build.version.sdk"),
            "build": prop("ro.build.fingerprint"),
        }


class LogStream:
    """Streams JdspLatency/JdspProbe JSON lines from logcat into memory and a raw .jsonl file."""

    def __init__(self, adb: Adb, raw_path: Path):
        adb.cmd("logcat", "-c", check=False)
        filters = [f"{tag}:I" for tag in LOG_TAGS]
        self.proc = subprocess.Popen(adb.argv("logcat", "-v", "raw", "-s", *filters),
                                     stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                                     text=True, encoding="utf-8", errors="replace", bufsize=1)
        self.raw_path = raw_path
        self.events: list[dict] = []
        self.lock = threading.Lock()
        self.thread = threading.Thread(target=self._pump, daemon=True)
        self.thread.start()

    def _pump(self) -> None:
        with open(self.raw_path, "w", encoding="utf-8") as raw:
            for line in self.proc.stdout:
                line = line.strip()
                if not line.startswith("{"):
                    continue
                try:
                    event = json.loads(line)
                except json.JSONDecodeError:
                    continue
                raw.write(line + "\n")
                raw.flush()
                with self.lock:
                    self.events.append(event)

    def wait_for(self, predicate, timeout: float, start_index: int = 0) -> dict | None:
        deadline = time.monotonic() + timeout
        index = start_index
        while time.monotonic() < deadline:
            with self.lock:
                pending = self.events[index:]
                index = len(self.events)
            for event in pending:
                if predicate(event):
                    return event
            time.sleep(0.1)
        return None

    def mark(self) -> int:
        with self.lock:
            return len(self.events)

    def snapshot(self) -> list[dict]:
        with self.lock:
            return list(self.events)

    def close(self) -> None:
        self.proc.terminate()
        try:
            self.proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            self.proc.kill()
        self.thread.join(timeout=5)


# --------------------------------------------------------------------------------------------
# engine / probe control


def parse_tune(items: list[str] | None) -> dict:
    tuning = {}
    for item in items or []:
        if "=" not in item:
            raise HarnessError(f"bad tuning '{item}', expected key=value")
        key, value = item.split("=", 1)
        key = key.strip()
        if key not in TUNING_KEYS:
            raise HarnessError(f"unknown tuning key '{key}' (known: {', '.join(TUNING_KEYS)})")
        if TUNING_KEYS[key] is bool:
            lowered = value.strip().lower()
            if lowered not in ("true", "false", "1", "0", "on", "off"):
                raise HarnessError(f"'{key}' expects true/false")
            tuning[key] = lowered in ("true", "1", "on")
        else:
            tuning[key] = int(value)
    return tuning


def engine_tune(adb: Adb, pkg: str, tuning: dict, reset: bool = True) -> None:
    # The harness force-stops the engine first; stopped packages only get flagged broadcasts
    parts = [f"am broadcast --include-stopped-packages -n {pkg}/{TUNING_RECEIVER}"]
    if reset:
        parts.append("--ez reset true")
    for key, value in tuning.items():
        if TUNING_KEYS[key] is bool:
            parts.append(f"--ez {key} {'true' if value else 'false'}")
        else:
            parts.append(f"--ei {key} {int(value)}")
    out = adb.shell(" ".join(parts), check=False)
    if "Broadcast completed" not in out:
        raise HarnessError(f"tuning broadcast failed (is the DEBUG engine build installed?):\n{out.strip()}")


def engine_stop(adb: Adb, pkg: str) -> None:
    adb.shell(f"am force-stop {pkg}", check=False)


def engine_running(adb: Adb, pkg: str) -> bool:
    return ".service.RootlessAudioProcessorService" in adb.shell(f"dumpsys activity services {pkg}", check=False)


def is_engine_config(tuning: dict):
    """Matches the engine's config line once the loop runs with exactly this tuning."""
    def predicate(event: dict) -> bool:
        if event.get("src") != "jdsp" or event.get("ev") != "config":
            return False
        applied = event.get("tuning", {})
        if tuning.get("app_settings"):
            return applied.get("override") is False
        return all(applied.get(key) == value for key, value in tuning.items())
    return predicate


def engine_start(adb: Adb, pkg: str) -> None:
    out = adb.shell(f"am start -n {pkg}/{LAUNCHER_ACTIVITY}", check=False)
    if "Error" in out or "Exception" in out:
        raise HarnessError(f"could not start engine launcher:\n{out.strip()}")


def probe_start(adb: Adb, probe_pkg: str, run_id: str, args) -> None:
    extras = (f"--es run_id {run_id} --ei count {args.count} --ei interval_ms {args.interval_ms} "
              f"--ei lead_in_ms {args.lead_in_ms} --ez acoustic {str(args.acoustic).lower()} "
              f"--ez low_latency {str(args.probe_low_latency).lower()} --ei buffer_ms {args.probe_buffer_ms}")
    out = adb.shell(f"am start -S -n {probe_pkg}/{PROBE_ACTIVITY} {extras}", check=False)
    if "Error" in out or "Exception" in out:
        raise HarnessError(f"could not start probe (is it installed?):\n{out.strip()}")


# --------------------------------------------------------------------------------------------
# analysis (pure functions, covered by selftest)


def percentile(sorted_values: list[float], q: float) -> float:
    if not sorted_values:
        return math.nan
    pos = (len(sorted_values) - 1) * q
    lo = math.floor(pos)
    hi = math.ceil(pos)
    return sorted_values[lo] + (sorted_values[hi] - sorted_values[lo]) * (pos - lo)


def stats(values: list[float]) -> dict:
    values = [v for v in values if v is not None and not math.isnan(v)]
    if not values:
        return {"n": 0}
    ordered = sorted(values)
    return {
        "n": len(ordered),
        "median": statistics.median(ordered),
        "mean": statistics.fmean(ordered),
        "p05": percentile(ordered, 0.05),
        "p95": percentile(ordered, 0.95),
        "min": ordered[0],
        "max": ordered[-1],
        "stdev": statistics.stdev(ordered) if len(ordered) > 1 else 0.0,
    }


def slope_per_minute(times_ns: list[int], values: list[float]) -> float | None:
    """Least-squares slope of values over time, in value units per minute."""
    if len(times_ns) < 3:
        return None
    xs = [(t - times_ns[0]) / 60e9 for t in times_ns]
    mean_x = statistics.fmean(xs)
    mean_y = statistics.fmean(values)
    sxx = sum((x - mean_x) ** 2 for x in xs)
    if sxx == 0:
        return None
    return sum((x - mean_x) * (y - mean_y) for x, y in zip(xs, values)) / sxx


def match_in_window(refs: list[dict], ref_key: str, cands: list[dict], cand_key: str,
                    window_ns: int) -> list[tuple[dict, dict]]:
    """Greedy in-order pairing: each ref takes the first unused candidate within
    [ref - PAIR_LEAD_NS, ref + window_ns - PAIR_LEAD_NS)."""
    pairs = []
    j = 0
    for ref in refs:
        t = ref[ref_key]
        while j < len(cands) and cands[j][cand_key] < t - PAIR_LEAD_NS:
            j += 1
        if j < len(cands) and cands[j][cand_key] < t + window_ns - PAIR_LEAD_NS:
            pairs.append((ref, cands[j]))
            j += 1
    return pairs


def last_event(events: list[dict], src: str, ev: str) -> dict | None:
    found = [e for e in events if e.get("src") == src and e.get("ev") == ev]
    return found[-1] if found else None


def analyze(events: list[dict], engine_on: bool = True) -> dict:
    pcfg = last_event(events, "probe", "probe_config") or {}
    jcfg = last_event(events, "jdsp", "config") or {}
    interval_ms = pcfg.get("interval_ms", 1000)
    sample_rate = jcfg.get("sample_rate") or pcfg.get("sample_rate") or 48000
    window_ns = int(interval_ms * 1e6)

    probe = sorted((e for e in events if e.get("src") == "probe" and e.get("ev") == "probe_marker"),
                   key=lambda e: e["t_present_ns"])
    engine = sorted((e for e in events if e.get("src") == "jdsp" and e.get("ev") == "marker"
                     and e.get("t_present_ns", -1) > 0), key=lambda e: e["t_present_ns"])
    mic = sorted((e for e in events if e.get("src") == "probe" and e.get("ev") == "probe_mic"
                  and e.get("t_capture_ns", -1) > 0), key=lambda e: e["t_capture_ns"])
    health = [e for e in events if e.get("src") == "jdsp" and e.get("ev") == "health"]
    done = last_event(events, "probe", "probe_done") or {}

    pairs = match_in_window(probe, "t_present_ns", engine, "t_present_ns", window_ns)
    offsets = [(j["t_present_ns"] - p["t_present_ns"]) / 1e6 for p, j in pairs]

    def stage(fn):
        values = []
        for p, j in pairs:
            try:
                values.append(fn(p, j))
            except (KeyError, TypeError):
                pass
        return stats(values)

    breakdown = {
        # Negative is normal: the tee copies audio before the (muted) original reaches the speaker
        "source_present_to_capture_ms": stage(
            lambda p, j: (j["t_capture_ns"] - p["t_present_ns"]) / 1e6 if j["t_capture_ns"] > 0 else None),
        "capture_to_read_ms": stage(
            lambda p, j: (j["t_read_ns"] - j["t_capture_ns"]) / 1e6 if j["t_capture_ns"] > 0 else None),
        "read_to_written_ms": stage(lambda p, j: (j["t_written_ns"] - j["t_read_ns"]) / 1e6),
        "written_to_present_ms": stage(lambda p, j: (j["t_present_ns"] - j["t_written_ns"]) / 1e6),
        "dsp_delay_ms": stage(
            lambda p, j: j["dsp_delay_frames"] * 1000.0 / sample_rate if j["dsp_delay_frames"] >= 0 else None),
    }

    acoustic_pairs = match_in_window(probe, "t_present_ns", mic, "t_capture_ns", window_ns)
    acoustic = [(m["t_capture_ns"] - p["t_present_ns"]) / 1e6 for p, m in acoustic_pairs]

    def health_values(key):
        return [h[key] for h in health if key in h]

    health_summary = {
        "windows": len(health),
        "internal_ms": stats(health_values("internal_ms")),
        "track_queue_ms": stats(health_values("track_queue_ms")),
        "rec_backlog_ms": stats(health_values("rec_backlog_ms")),
        "block_frames": stats(health_values("block_frames_avg")),
        "read_wait_ms_avg": stats(health_values("read_wait_ms_avg")),
        "dsp_ms_max": max(health_values("dsp_ms_max"), default=None),
        "write_wait_ms_avg": stats(health_values("write_wait_ms_avg")),
        "underruns": sum(health_values("underruns_delta")),
        "internal_drift_ms_per_min": slope_per_minute(
            [h["t_ns"] for h in health if "internal_ms" in h], health_values("internal_ms")),
    }

    warnings = []
    if engine_on and not jcfg:
        warnings.append("no engine config line: engine loop not running or trace disabled")
    if engine_on and probe and len(pairs) < 0.8 * len(probe):
        warnings.append(f"only {len(pairs)}/{len(probe)} pulses paired: probe audio not captured "
                        "(excluded/blocklisted app?), other audio playing, or offset > interval")
    if offsets and statistics.median(offsets) > 0.8 * interval_ms:
        warnings.append("median offset is close to the pulse interval; rerun with a larger --interval-ms")
    if pcfg and len(probe) < pcfg.get("count", 0):
        warnings.append(f"probe resolved {len(probe)}/{pcfg.get('count')} pulses")
    if health and not all(h.get("trk_ts_ok") and h.get("rec_ts_ok") for h in health[1:]):
        warnings.append("some health windows lacked valid AudioRecord/AudioTrack timestamps")
    if health_summary["underruns"]:
        warnings.append(f"{health_summary['underruns']} output underruns (audible glitches)")

    return {
        "engine_on": engine_on,
        "sample_rate": sample_rate,
        "probe_config": pcfg,
        "engine_config": jcfg,
        "pulses": len(probe),
        "engine_markers": len(engine),
        "paired": len(pairs),
        "av_offset_ms": stats(offsets),
        "av_offset_drift_ms_per_min": slope_per_minute([p["t_present_ns"] for p, _ in pairs], offsets),
        "breakdown": breakdown,
        "acoustic_ms": stats(acoustic),
        "health": health_summary,
        "probe_queue_ms": done.get("probe_queue_ms_avg"),
        "warnings": warnings,
    }


# --------------------------------------------------------------------------------------------
# reporting


def fmt(value, digits: int = 1) -> str:
    if value is None or (isinstance(value, float) and math.isnan(value)):
        return "-"
    return f"{value:.{digits}f}"


def print_summary(summary: dict) -> None:
    meta = summary.get("meta", {})
    device = meta.get("device", {})
    print()
    print(f"== {meta.get('run_id', '?')}  engine={'on' if summary['engine_on'] else 'off'}  "
          f"{device.get('manufacturer', '')} {device.get('model', '')} sdk {device.get('sdk', '?')}")
    print(f"   tuning: {json.dumps(meta.get('tuning', {}))}")
    jc = summary["engine_config"]
    if jc:
        print(f"   engine: sr={jc.get('sample_rate')} read={jc.get('read_frames')}f "
              f"rec_buf={jc.get('rec_buffer_frames')}f trk_buf={jc.get('trk_buffer_frames')}/"
              f"{jc.get('trk_capacity_frames')}f perf_mode={jc.get('trk_perf_mode')} "
              f"hal_fpb={jc.get('hal_frames_per_buffer')} prio={jc.get('thread_priority')}")
    av = summary["av_offset_ms"]
    if av.get("n"):
        print(f"   A/V offset: median {fmt(av['median'])} ms  p95 {fmt(av['p95'])}  sd {fmt(av['stdev'])}  "
              f"range {fmt(av['min'])}..{fmt(av['max'])}  n={summary['paired']}/{summary['pulses']}  "
              f"drift {fmt(summary['av_offset_drift_ms_per_min'], 2)} ms/min")
        b = summary["breakdown"]
        print("   breakdown (median ms): "
              f"src->capture {fmt(b['source_present_to_capture_ms'].get('median'))} | "
              f"capture->read {fmt(b['capture_to_read_ms'].get('median'))} | "
              f"read->written {fmt(b['read_to_written_ms'].get('median'))} | "
              f"written->present {fmt(b['written_to_present_ms'].get('median'))} | "
              f"dsp delay {fmt(b['dsp_delay_ms'].get('median'))}")
    h = summary["health"]
    if h["windows"]:
        print(f"   health: internal {fmt(h['internal_ms'].get('median'))} ms (max {fmt(h['internal_ms'].get('max'))}) "
              f"queue {fmt(h['track_queue_ms'].get('median'))} backlog {fmt(h['rec_backlog_ms'].get('median'))} "
              f"block {fmt(h['block_frames'].get('median'), 0)}f dsp_max {fmt(h['dsp_ms_max'], 2)} ms "
              f"underruns {h['underruns']}")
    ac = summary["acoustic_ms"]
    if ac.get("n"):
        print(f"   acoustic (mic - source present): median {fmt(ac['median'])} ms  p95 {fmt(ac['p95'])}  n={ac['n']}")
    for w in summary["warnings"]:
        print(f"   WARNING: {w}")


HISTORY_FIELDS = ["time", "run_id", "label", "engine", "device", "sdk", "tuning", "paired", "pulses",
                  "av_median_ms", "av_p95_ms", "av_sd_ms", "av_drift_ms_min", "internal_median_ms",
                  "underruns", "acoustic_median_ms", "summary"]


def append_history(out_dir: Path, summary: dict, summary_path: Path) -> None:
    history = out_dir / "history.csv"
    new = not history.exists()
    meta = summary.get("meta", {})
    row = {
        "time": meta.get("time"),
        "run_id": meta.get("run_id"),
        "label": meta.get("label"),
        "engine": "on" if summary["engine_on"] else "off",
        "device": meta.get("device", {}).get("model"),
        "sdk": meta.get("device", {}).get("sdk"),
        "tuning": json.dumps(meta.get("tuning", {}), separators=(",", ":")),
        "paired": summary["paired"],
        "pulses": summary["pulses"],
        "av_median_ms": fmt(summary["av_offset_ms"].get("median"), 2),
        "av_p95_ms": fmt(summary["av_offset_ms"].get("p95"), 2),
        "av_sd_ms": fmt(summary["av_offset_ms"].get("stdev"), 2),
        "av_drift_ms_min": fmt(summary["av_offset_drift_ms_per_min"], 3),
        "internal_median_ms": fmt(summary["health"]["internal_ms"].get("median"), 2),
        "underruns": summary["health"]["underruns"],
        "acoustic_median_ms": fmt(summary["acoustic_ms"].get("median"), 2),
        "summary": str(summary_path),
    }
    with open(history, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=HISTORY_FIELDS)
        if new:
            writer.writeheader()
        writer.writerow(row)


# --------------------------------------------------------------------------------------------
# commands


def slug(text: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "-", text).strip("-") or "run"


def run_once(args, adb: Adb, label: str, engine_on: bool, tuning: dict) -> dict:
    out_dir = Path(args.out)
    run_id = f"{datetime.now().strftime('%Y%m%d-%H%M%S')}-{slug(label)}"
    run_dir = out_dir / run_id
    run_dir.mkdir(parents=True, exist_ok=True)
    full_tuning = {"trace": True, **tuning}
    meta = {"run_id": run_id, "label": label, "time": datetime.now().isoformat(timespec="seconds"),
            "device": adb.device_info(), "tuning": full_tuning, "engine_on": engine_on,
            "args": {k: v for k, v in vars(args).items() if k not in ("func",)}}

    print(f"\n-- {run_id}: engine {'on' if engine_on else 'off'}, tuning {json.dumps(tuning)}")
    stream = LogStream(adb, run_dir / "events.jsonl")
    try:
        if not engine_on:
            engine_stop(adb, args.pkg)
            engine_tune(adb, args.pkg, full_tuning, reset=True)
        elif engine_running(adb, args.pkg) and not args.fresh:
            # Android 14+ cannot reuse a projection token, so keep the service (and its projection)
            # alive: the tuning broadcast hard-reboots only the capture loop.
            mark = stream.mark()
            engine_tune(adb, args.pkg, full_tuning, reset=True)
            if not stream.wait_for(is_engine_config(full_tuning), timeout=args.start_timeout, start_index=mark):
                raise HarnessError("engine loop did not restart with the new tuning")
        else:
            engine_stop(adb, args.pkg)
            time.sleep(1.0)
            engine_tune(adb, args.pkg, full_tuning, reset=True)
            mark = stream.mark()
            engine_start(adb, args.pkg)
            print("   waiting for the engine; accept the screen-capture dialog on the phone if it appears")
            if not stream.wait_for(is_engine_config(full_tuning), timeout=args.start_timeout, start_index=mark):
                raise HarnessError("engine did not start its loop (no config line). Run `setup`, finish "
                                   "onboarding once in the app, and accept the capture consent dialog.")
        time.sleep(args.settle)

        probe_start(adb, args.probe_pkg, run_id, args)
        run_s = (args.lead_in_ms + args.count * args.interval_ms) / 1000.0
        # AudioFlinger snapshot mid-run, while all tracks exist
        is_done = lambda e: e.get("src") == "probe" and e.get("ev") == "probe_done"
        done = stream.wait_for(is_done, timeout=args.lead_in_ms / 1000.0 + run_s / 2)
        if not done:
            (run_dir / "audio_flinger.txt").write_text(
                adb.shell("dumpsys media.audio_flinger", check=False, timeout=30), encoding="utf-8")
            done = stream.wait_for(is_done, timeout=run_s + 20)
        if not done:
            print("   WARNING: probe_done not seen before timeout")
        time.sleep(1.0)
    finally:
        stream.close()

    summary = analyze(stream.snapshot(), engine_on=engine_on)
    summary["meta"] = meta
    summary_path = run_dir / "summary.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")
    append_history(out_dir, summary, summary_path)
    print_summary(summary)
    return summary


def connect(args) -> Adb:
    adb = Adb(find_adb(args.adb), args.serial)
    adb.ensure_device()
    return adb


def cmd_setup(args) -> None:
    adb = connect(args)
    steps = [
        f"pm grant {args.probe_pkg} android.permission.RECORD_AUDIO",
        f"pm grant {args.pkg} android.permission.RECORD_AUDIO",
        f"pm grant {args.pkg} android.permission.DUMP",
        f"pm grant {args.pkg} android.permission.POST_NOTIFICATIONS",
        f"appops set {args.pkg} PROJECT_MEDIA allow",
    ]
    for step in steps:
        out = adb.shell(step, check=False).strip()
        print(f"  {step}{'  -> ' + out.splitlines()[0] if out else ''}")
    if "allow" not in adb.shell(f"appops get {args.pkg} PROJECT_MEDIA", check=False):
        print("  NOTE: this device does not let adb set PROJECT_MEDIA (OEM restriction). Runs keep the engine "
              "alive between runs, so accept the screen-capture dialog once per engine start.")
    granted = adb.shell(f"dumpsys package {args.pkg}", check=False)
    if "android.permission.RECORD_AUDIO: granted=true" not in granted:
        print("  ERROR: RECORD_AUDIO is not granted to the engine; the service stops right after it starts.\n"
              "  adb cannot grant runtime permissions on this device (OnePlus/ColorOS 'permission monitoring').\n"
              "  Open the debug engine app once and finish onboarding (microphone + notifications), or enable\n"
              "  Developer options > 'Disable permission monitoring' and rerun setup.")
    engine_tune(adb, args.pkg, {"trace": True}, reset=True)
    print("Done. Open the engine app once and finish onboarding if you have not yet; "
          "keep the probe app off the engine's app blocklist.")


def cmd_tune(args) -> None:
    adb = connect(args)
    tuning = parse_tune(args.values)
    engine_tune(adb, args.pkg, {"trace": True, **tuning}, reset=args.reset)
    print(f"tuning sent: {json.dumps(tuning)} (reset={args.reset})")


def cmd_run(args) -> None:
    adb = connect(args)
    run_once(args, adb, args.label, args.engine == "on", parse_tune(args.tune))


def cmd_soak(args) -> None:
    args.count = max(3, int(args.minutes * 60_000 / args.interval_ms))
    cmd_run(args)


def cmd_ab(args) -> None:
    adb = connect(args)
    sides = {"A": parse_tune(args.tune_a), "B": parse_tune(args.tune_b)}
    results: dict[str, list[dict]] = {"A": [], "B": []}
    for rep in range(args.repeats):
        order = ("A", "B") if rep % 2 == 0 else ("B", "A")   # ABBA cancels slow drift in conditions
        for side in order:
            results[side].append(run_once(args, adb, f"{args.label}-{side}", True, sides[side]))

    print(f"\n== A/B {args.label}: A={json.dumps(sides['A'])}  B={json.dumps(sides['B'])}")
    verdict = {}
    for side in ("A", "B"):
        medians = [r["av_offset_ms"].get("median") for r in results[side] if r["av_offset_ms"].get("n")]
        underruns = sum(r["health"]["underruns"] for r in results[side])
        verdict[side] = {"run_medians_ms": medians, "underruns": underruns,
                         "pooled_median_ms": statistics.median(medians) if medians else None}
        print(f"   {side}: run medians {[fmt(m) for m in medians]}  pooled {fmt(verdict[side]['pooled_median_ms'])} ms  "
              f"underruns {underruns}")
    a, b = verdict["A"], verdict["B"]
    if a["run_medians_ms"] and b["run_medians_ms"]:
        delta = b["pooled_median_ms"] - a["pooled_median_ms"]
        separated = max(b["run_medians_ms"]) < min(a["run_medians_ms"]) or \
            min(b["run_medians_ms"]) > max(a["run_medians_ms"])
        print(f"   B - A = {fmt(delta)} ms ({'runs do not overlap' if separated else 'runs overlap: inconclusive'}); "
              f"underruns A={a['underruns']} B={b['underruns']}")
        verdict["delta_ms"] = delta
        verdict["separated"] = separated
    out = Path(args.out) / f"ab-{datetime.now().strftime('%Y%m%d-%H%M%S')}-{slug(args.label)}.json"
    out.write_text(json.dumps({"tuning": sides, "verdict": verdict,
                               "runs": {s: [r["meta"]["run_id"] for r in results[s]] for s in results}},
                              indent=2), encoding="utf-8")
    print(f"   saved {out}")


def cmd_analyze(args) -> None:
    events = []
    with open(args.events, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("{"):
                try:
                    events.append(json.loads(line))
                except json.JSONDecodeError:
                    pass
    summary = analyze(events, engine_on=not args.engine_off)
    summary["meta"] = {"run_id": Path(args.events).parent.name}
    print_summary(summary)
    if args.json:
        print(json.dumps(summary, indent=2))


def cmd_compare(args) -> None:
    a = json.loads(Path(args.a).read_text(encoding="utf-8"))
    b = json.loads(Path(args.b).read_text(encoding="utf-8"))
    rows = [
        ("A/V offset median ms", lambda s: s["av_offset_ms"].get("median")),
        ("A/V offset p95 ms", lambda s: s["av_offset_ms"].get("p95")),
        ("A/V offset sd ms", lambda s: s["av_offset_ms"].get("stdev")),
        ("A/V drift ms/min", lambda s: s["av_offset_drift_ms_per_min"]),
        ("capture->read ms", lambda s: s["breakdown"]["capture_to_read_ms"].get("median")),
        ("written->present ms", lambda s: s["breakdown"]["written_to_present_ms"].get("median")),
        ("internal ms", lambda s: s["health"]["internal_ms"].get("median")),
        ("underruns", lambda s: s["health"]["underruns"]),
        ("acoustic ms", lambda s: s["acoustic_ms"].get("median")),
    ]
    print(f"{'metric':<24}{'A':>12}{'B':>12}{'B-A':>12}")
    for name, get in rows:
        va, vb = get(a), get(b)
        delta = vb - va if isinstance(va, (int, float)) and isinstance(vb, (int, float)) else None
        print(f"{name:<24}{fmt(va):>12}{fmt(vb):>12}{fmt(delta):>12}")


def cmd_selftest(_args) -> None:
    """Synthetic run: 20 pulses 1 s apart, engine output 180 ms late plus 0.5 ms/min drift."""
    sr = 48000
    base = 10_000_000_000
    events = [
        {"src": "probe", "ev": "probe_config", "interval_ms": 1000, "count": 20, "sample_rate": sr},
        {"src": "jdsp", "ev": "config", "sample_rate": sr, "read_frames": 4096},
    ]
    for k in range(20):
        t_src = base + k * 1_000_000_000
        offset_ns = 180_000_000 + int(0.5e6 * k / 60)            # 0.5 ms per minute
        events.append({"src": "probe", "ev": "probe_marker", "seq": k, "t_present_ns": t_src})
        events.append({"src": "probe", "ev": "probe_mic", "t_capture_ns": t_src + offset_ns + 2_000_000})
        if k == 7:
            continue                                                # one lost engine marker
        events.append({"src": "jdsp", "ev": "marker", "seq": k, "t_capture_ns": t_src - 10_000_000,
                       "t_read_ns": t_src + 60_000_000, "t_written_ns": t_src + 61_000_000,
                       "t_present_ns": t_src + offset_ns, "dsp_delay_frames": 192})
    events.append({"src": "jdsp", "ev": "marker", "seq": 99, "t_present_ns": base - 5_000_000_000,
                   "dsp_delay_frames": -1})                         # stray, must not pair
    for s in range(20):
        events.append({"src": "jdsp", "ev": "health", "t_ns": base + s * 1_000_000_000, "internal_ms": 190.0,
                       "underruns_delta": 1 if s == 5 else 0, "trk_ts_ok": True, "rec_ts_ok": True})

    summary = analyze(events)
    av = summary["av_offset_ms"]
    assert summary["paired"] == 19, summary["paired"]
    assert abs(av["median"] - 180.1) < 0.2, av
    assert abs(summary["av_offset_drift_ms_per_min"] - 0.5) < 1e-3, summary["av_offset_drift_ms_per_min"]
    assert abs(summary["breakdown"]["source_present_to_capture_ms"]["median"] + 10.0) < 1e-6
    assert abs(summary["breakdown"]["capture_to_read_ms"]["median"] - 70.0) < 1e-6
    assert abs(summary["breakdown"]["dsp_delay_ms"]["median"] - 4.0) < 1e-6
    assert summary["acoustic_ms"]["n"] == 20
    assert summary["health"]["underruns"] == 1
    assert any("underruns" in w for w in summary["warnings"])
    summary["meta"] = {"run_id": "selftest"}
    print_summary(summary)
    print("\nselftest OK")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--adb", help="path to adb (default: PATH / ANDROID_HOME)")
    parser.add_argument("--serial", help="device serial (adb -s)")
    parser.add_argument("--pkg", default=ENGINE_PKG, help=f"engine package (default {ENGINE_PKG})")
    parser.add_argument("--probe-pkg", default=PROBE_PKG)
    parser.add_argument("--out", default=str(DEFAULT_OUT), help="results directory")
    sub = parser.add_subparsers(dest="command", required=True)

    def add_run_options(p):
        p.add_argument("--label", required=True)
        p.add_argument("--count", type=int, default=30, help="pulses per run")
        p.add_argument("--interval-ms", type=int, default=1000, help="pulse spacing; must exceed the offset")
        p.add_argument("--lead-in-ms", type=int, default=3000, help="probe silence before the first pulse")
        p.add_argument("--settle", type=float, default=2.0, help="seconds between engine start and probe")
        p.add_argument("--start-timeout", type=float, default=60.0)
        p.add_argument("--fresh", action="store_true",
                       help="cold-start the engine even if it runs (needs the consent dialog on Android 14+)")
        p.add_argument("--acoustic", action="store_true", help="also time pulses via the mic (speaker output)")
        p.add_argument("--probe-low-latency", action="store_true")
        p.add_argument("--probe-buffer-ms", type=int, default=0, help="probe track buffer (0 = 2x min)")

    p = sub.add_parser("setup")
    p.set_defaults(func=cmd_setup)

    p = sub.add_parser("tune")
    p.add_argument("values", nargs="*", help="key=value; keys: " + ", ".join(TUNING_KEYS))
    p.add_argument("--reset", action="store_true", help="reset to legacy defaults before applying")
    p.set_defaults(func=cmd_tune)

    p = sub.add_parser("run")
    add_run_options(p)
    p.add_argument("--engine", choices=("on", "off"), default="on")
    p.add_argument("--tune", nargs="*", default=[], help="key=value knobs for this run (others reset)")
    p.set_defaults(func=cmd_run)

    p = sub.add_parser("ab")
    add_run_options(p)
    p.add_argument("--tune-a", nargs="*", default=[], help="knobs for A (default: legacy)")
    p.add_argument("--tune-b", nargs="*", default=[], required=True, help="knobs for B")
    p.add_argument("--repeats", type=int, default=2, help="A/B pairs, run in ABBA order")
    p.set_defaults(func=cmd_ab)

    p = sub.add_parser("soak")
    add_run_options(p)
    p.add_argument("--minutes", type=float, default=10)
    p.add_argument("--engine", choices=("on", "off"), default="on")
    p.add_argument("--tune", nargs="*", default=[])
    p.set_defaults(func=cmd_soak)

    p = sub.add_parser("analyze")
    p.add_argument("events", help="events.jsonl from a run directory")
    p.add_argument("--engine-off", action="store_true")
    p.add_argument("--json", action="store_true")
    p.set_defaults(func=cmd_analyze)

    p = sub.add_parser("compare")
    p.add_argument("a")
    p.add_argument("b")
    p.set_defaults(func=cmd_compare)

    p = sub.add_parser("selftest")
    p.set_defaults(func=cmd_selftest)

    args = parser.parse_args()
    if hasattr(args, "label"):
        args.label = slug(args.label)
    try:
        args.func(args)
    except HarnessError as ex:
        print(f"error: {ex}", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    main()
