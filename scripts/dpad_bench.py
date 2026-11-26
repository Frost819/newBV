#!/usr/bin/env python3
"""TV 遥控器方向键响应基准。

用法:
    python dpad_bench.py [包名] [轮数]

流程: 每轮 reset gfxinfo -> 在设备端连续发送方向键 -> 采集 framestats。
输出每帧 IntendedVsync -> FrameCompleted 的耗时分布，以及 gfxinfo 摘要指标。
优化前后请用相同的包名/轮数/动作序列对比。
"""
import os
import re
import shutil
import statistics
import subprocess
import sys


def find_adb():
    for candidate in (
        os.environ.get("ADB"),
        shutil.which("adb"),
        *(os.path.join(root, "platform-tools", "adb.exe" if os.name == "nt" else "adb")
          for root in (os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT"))
          if root),
    ):
        if candidate and (os.path.isfile(candidate) or shutil.which(candidate)):
            return candidate
    return "adb"


ADB = find_adb()
# 右右下下左左上上，覆盖横向与纵向焦点移动
KEYS = [22, 22, 20, 20, 21, 21, 19, 19] * 3
KEY_GAP = "0.10"


def adb(*args, capture=True):
    cmd = [ADB, *args]
    return subprocess.run(cmd, capture_output=capture, text=True, errors="replace")


def shell(cmd):
    return adb("shell", cmd).stdout


def main():
    pkg = sys.argv[1] if len(sys.argv) > 1 else "dev.frost819.newbv.opt"
    rounds = int(sys.argv[2]) if len(sys.argv) > 2 else 3

    results = []
    for r in range(rounds):
        shell(f"dumpsys gfxinfo {pkg} reset")
        seq = " ".join(f"input keyevent {k}; sleep {KEY_GAP};" for k in KEYS)
        shell(seq)
        raw = shell(f"dumpsys gfxinfo {pkg} framestats")
        summary = shell(f"dumpsys gfxinfo {pkg}")

        frames = parse_framestats(raw)
        janky = find(summary, r"Janky frames:\s*(\d+)\s*\((\d+\.\d+)%\)")
        high_input = find(summary, r"Number High input latency:\s*(\d+)")
        slow_ui = find(summary, r"Number Slow UI thread:\s*(\d+)")
        missed_vsync = find(summary, r"Number Missed Vsync:\s*(\d+)")
        total = find(summary, r"Total frames rendered:\s*(\d+)")
        p50s = find(summary, r"50th percentile:\s*([\d.]+)ms")
        p90s = find(summary, r"90th percentile:\s*([\d.]+)ms")

        d = {
            "round": r + 1,
            "frames": len(frames),
            "total_rendered": total,
            "janky_pct": janky[1] if janky else None,
            "high_input": high_input[0] if high_input else None,
            "slow_ui": slow_ui[0] if slow_ui else None,
            "missed_vsync": missed_vsync[0] if missed_vsync else None,
            "gfx_p50_ms": p50s[0] if p50s else None,
            "gfx_p90_ms": p90s[0] if p90s else None,
            "frame_ms": frames,
        }
        results.append(d)
        print(format_round(d))

    print("\n=== 汇总 ===")
    all_frames = [f for d in results for f in d["frame_ms"]]
    if all_frames:
        all_frames.sort()
        print(f"总采样帧: {len(all_frames)}")
        for p in (50, 90, 95, 99):
            i = min(len(all_frames) - 1, int(len(all_frames) * p / 100))
            print(f"  P{p}: {all_frames[i]:.1f} ms")
        over = sum(1 for f in all_frames if f > 16.7)
        print(f"  >16.7ms: {over}/{len(all_frames)} ({over * 100 / len(all_frames):.1f}%)")
    for key, label in (("high_input", "High input latency"),
                       ("slow_ui", "Slow UI thread"),
                       ("missed_vsync", "Missed Vsync"),
                       ("gfx_p90_ms", "gfxinfo 90th percentile(ms)")):
        vals = [d[key] for d in results if d[key] is not None]
        if vals:
            print(f"  {label}: {vals} (均值 {statistics.mean(vals):.1f})")


def find(text, pattern):
    m = re.search(pattern, text)
    if not m:
        return None
    return tuple(float(g) if re.match(r"^\d+\.\d+$", g) else int(g) for g in m.groups())


def parse_framestats(raw):
    """返回每帧 FrameCompleted - IntendedVsync，单位毫秒。"""
    lines = raw.splitlines()
    header_idx = next((i for i, l in enumerate(lines) if "IntendedVsync" in l), None)
    if header_idx is None:
        return []
    header = [c.strip() for c in lines[header_idx].split(",")]
    try:
        i_vsync = header.index("IntendedVsync")
        i_done = header.index("FrameCompleted")
    except ValueError:
        return []
    out = []
    for line in lines[header_idx + 1:]:
        cols = [c.strip() for c in line.split(",")]
        if len(cols) <= max(i_vsync, i_done):
            continue
        try:
            start, end = int(cols[i_vsync]), int(cols[i_done])
        except ValueError:
            continue
        if end > start > 0:
            ms = (end - start) / 1_000_000
            if 0 < ms < 2000:
                out.append(ms)
    return out


def format_round(d):
    ms = d["frame_ms"]
    s = (f"轮 {d['round']}: frames={d['frames']} total={d['total_rendered']} "
         f"janky={d['janky_pct']}% high_input={d['high_input']} "
         f"slow_ui={d['slow_ui']} missed_vsync={d['missed_vsync']} "
         f"gfx p50/p90={d['gfx_p50_ms']}/{d['gfx_p90_ms']}ms")
    if ms:
        ms = sorted(ms)
        s += f" | framestats P50={ms[len(ms)//2]:.1f}ms P90={ms[min(len(ms)-1, int(len(ms)*0.9))]:.1f}ms"
    return s


if __name__ == "__main__":
    main()
