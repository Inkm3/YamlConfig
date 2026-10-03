#!/usr/bin/env python3
"""Report paired JMH observations; no unstable wall-clock acceptance threshold."""
import hashlib
import json
import math
import sys
from pathlib import Path


def read_results(path):
    with path.open(encoding="utf-8") as stream:
        entries = json.load(stream)
    result = {}
    for item in entries:
        key = (item["benchmark"], tuple(sorted(item.get("params", {}).items())))
        if key in result:
            raise ValueError(f"Duplicate benchmark in {path.name}: {key}")
        score = item["primaryMetric"]["score"]
        if not math.isfinite(score) or score <= 0:
            raise ValueError(f"Invalid score in {path.name}")
        result[key] = item
    if not result:
        raise ValueError(f"Empty JMH result: {path.name}")
    return result


def summarize(directory):
    rows = []
    hashes = {}
    for pair in (1, 2):
        paths = {kind: directory / f"pair-{pair}-{kind}.json" for kind in ("reference", "current")}
        data = {kind: read_results(path) for kind, path in paths.items()}
        hashes.update({path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in paths.values()})
        if data["reference"].keys() != data["current"].keys():
            raise ValueError("Reference/current benchmark cases differ")
        for key, old in sorted(data["reference"].items()):
            new = data["current"][key]
            for field in ("jmhVersion", "jdkVersion", "vmName", "vmVersion", "forks", "warmupIterations", "measurementIterations", "mode"):
                if old.get(field) != new.get(field):
                    raise ValueError(f"Incompatible {field} in pair {pair}")
            a, b = old["primaryMetric"], new["primaryMetric"]
            if a["scoreUnit"] != b["scoreUnit"]:
                raise ValueError("Score units differ")
            old_bytes = old["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
            new_bytes = new["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
            row = {"pair": pair, "benchmark": key[0], "params": dict(key[1]), "unit": a["scoreUnit"],
                   "reference": a["score"], "current": b["score"], "referenceError": a["scoreError"],
                   "currentError": b["scoreError"], "timeChangePercent": 100 * (b["score"] / a["score"] - 1),
                   "referenceBytes": old_bytes, "currentBytes": new_bytes,
                   "allocationChangePercent": 100 * (new_bytes / old_bytes - 1)}
            rows.append(row)
            print("JMH_PAIR_RESULT|" + json.dumps(row, ensure_ascii=True, allow_nan=False))
    report = {"note": "Two reversed-order pairs on one shared Runner; observations, not a general speed guarantee.",
              "sha256": hashes, "results": rows}
    (directory / "summary.json").write_text(json.dumps(report, indent=2, allow_nan=False) + "\n", encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: summarize-sequence-jmh.py REPORT_DIRECTORY")
    summarize(Path(sys.argv[1]))
