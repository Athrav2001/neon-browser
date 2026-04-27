#!/usr/bin/env python3
from __future__ import annotations

import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path


REPO = Path("/root/Neo-downloader")
OUT_DIR = REPO / "docs" / "migration"
TARGET_ROOT = REPO / "app" / "src" / "main" / "kotlin"

SOURCE_ROOTS = [
    REPO / "shared",
    REPO / "downloader",
]

SOURCE_SET_MARKERS = ("commonMain", "androidMain")

PACKAGE_RE = re.compile(r"^\s*package\s+([A-Za-z0-9_.]+)\s*$", re.MULTILINE)
IMPORT_RE = re.compile(r"^\s*import\s+([A-Za-z0-9_.]+)", re.MULTILINE)


@dataclass(frozen=True)
class Entry:
    path: Path
    module: str
    source_set: str
    package_name: str
    class_name: str
    fqcn: str
    suggested_target: Path
    imports: tuple[str, ...]
    content: str


def detect_module_and_sourceset(path: Path) -> tuple[str, str]:
    text = str(path)
    source_set = "unknown"
    for marker in SOURCE_SET_MARKERS:
        if f"/src/{marker}/" in text:
            source_set = marker
            break

    # module key like shared:app, downloader:core
    parts = path.parts
    try:
        idx = parts.index("shared")
        module = f"shared:{parts[idx + 1]}"
    except ValueError:
        try:
            idx = parts.index("downloader")
            module = f"downloader:{parts[idx + 1]}"
        except ValueError:
            module = "unknown"
    return module, source_set


def parse_file(path: Path) -> Entry | None:
    content = path.read_text(encoding="utf-8")
    package_match = PACKAGE_RE.search(content)
    if not package_match:
        return None

    package_name = package_match.group(1).strip()
    class_name = path.stem
    fqcn = f"{package_name}.{class_name}"
    imports = tuple(IMPORT_RE.findall(content))
    module, source_set = detect_module_and_sourceset(path)
    suggested_target = TARGET_ROOT / Path(package_name.replace(".", "/")) / path.name
    return Entry(
        path=path,
        module=module,
        source_set=source_set,
        package_name=package_name,
        class_name=class_name,
        fqcn=fqcn,
        suggested_target=suggested_target,
        imports=imports,
        content=content,
    )


def load_entries() -> list[Entry]:
    entries: list[Entry] = []
    for source in SOURCE_ROOTS:
        if not source.exists():
            continue
        for path in source.rglob("*.kt"):
            if "/build/" in str(path):
                continue
            # Migration scope: commonMain + androidMain only
            p = str(path)
            if "/src/commonMain/" not in p and "/src/androidMain/" not in p:
                continue
            item = parse_file(path)
            if item is not None:
                entries.append(item)
    return entries


def build_report(entries: list[Entry]) -> str:
    by_module = Counter(e.module for e in entries)
    by_source_set = Counter(e.source_set for e in entries)
    imports = Counter()
    for e in entries:
        imports.update(e.imports)

    fqcn_map = defaultdict(list)
    target_map = defaultdict(list)
    expect_count = 0
    actual_count = 0
    optics_count = 0
    string_source_count = 0
    remember_string_count = 0
    koin_count = 0

    for e in entries:
        fqcn_map[e.fqcn].append(e)
        target_map[str(e.suggested_target)].append(e)
        expect_count += len(re.findall(r"\bexpect\b", e.content))
        actual_count += len(re.findall(r"\bactual\b", e.content))
        optics_count += len(re.findall(r"@optics|arrow\.optics", e.content))
        string_source_count += len(re.findall(r"StringSource", e.content))
        remember_string_count += len(re.findall(r"rememberString", e.content))
        koin_count += len(re.findall(r"org\.koin|@Single|@Factory|@Module", e.content))

    fqcn_conflicts = {k: v for k, v in fqcn_map.items() if len(v) > 1}
    target_conflicts = {k: v for k, v in target_map.items() if len(v) > 1}

    def format_conflicts(conflicts: dict[str, list[Entry]], limit: int = 30) -> str:
        lines = []
        for idx, (name, rows) in enumerate(sorted(conflicts.items(), key=lambda x: (-len(x[1]), x[0]))):
            if idx >= limit:
                lines.append(f"- ... and {len(conflicts) - limit} more")
                break
            lines.append(f"- `{name}` ({len(rows)} files)")
            for r in rows[:4]:
                lines.append(f"  - `{r.path.relative_to(REPO)}` [{r.source_set}]")
            if len(rows) > 4:
                lines.append(f"  - ... +{len(rows) - 4} more")
        return "\n".join(lines) if lines else "- none"

    top_risky_imports = [
        item for item in imports.items()
        if item[0].startswith("ir.neo.") or item[0].startswith("com.neo.downloader.shared")
    ]
    top_risky_imports.sort(key=lambda x: (-x[1], x[0]))

    lines = [
        "# Migration Dry-Run Report",
        "",
        f"- Total Kotlin files scanned: **{len(entries)}**",
        f"- Modules scanned: **{len(by_module)}**",
        "",
        "## Breakdown",
        "",
        "### By module",
    ]
    for module, count in sorted(by_module.items()):
        lines.append(f"- `{module}`: {count}")
    lines += [
        "",
        "### By source set",
    ]
    for ss, count in sorted(by_source_set.items()):
        lines.append(f"- `{ss}`: {count}")

    lines += [
        "",
        "## High-Risk Signals",
        "",
        f"- `expect` keywords: **{expect_count}**",
        f"- `actual` keywords: **{actual_count}**",
        f"- Arrow/optics markers: **{optics_count}**",
        f"- StringSource usage: **{string_source_count}**",
        f"- rememberString usage: **{remember_string_count}**",
        f"- Koin markers/imports: **{koin_count}**",
        "",
        "## Conflict Preview",
        "",
        f"- Duplicate FQCN candidates: **{len(fqcn_conflicts)}**",
        f"- Target-path collisions (`app/src/main/kotlin/...`): **{len(target_conflicts)}**",
        "",
        "### Duplicate FQCN sample",
        format_conflicts(fqcn_conflicts, limit=25),
        "",
        "### Target-path collision sample",
        format_conflicts(target_conflicts, limit=25),
        "",
        "## Risky import families (top 40)",
    ]
    if top_risky_imports:
        for name, count in top_risky_imports[:40]:
            lines.append(f"- `{name}`: {count}")
    else:
        lines.append("- none")

    lines += [
        "",
        "## Ordered Migration Batches",
        "",
        "1. `shared:utils`, `shared:config`, `shared:resources:contracts`",
        "2. `shared:resources`, `shared:compose-utils`",
        "3. `downloader:monitor`, `downloader:core`",
        "4. `shared:updater`, `shared:auto-start`",
        "5. `shared:app` (last, largest UI integration surface)",
        "",
        "## Notes",
        "",
        "- This report is dry-run only; no source mutation.",
        "- Package remap is intentionally deferred until compile parity is green.",
    ]
    return "\n".join(lines) + "\n"


def build_batches(entries: list[Entry]) -> str:
    module_counter = Counter(e.module for e in entries)
    ordered = [
        "shared:utils",
        "shared:config",
        "shared:resources:contracts",
        "shared:resources",
        "shared:compose-utils",
        "downloader:monitor",
        "downloader:core",
        "shared:updater",
        "shared:auto-start",
        "shared:app",
    ]
    lines = [
        "# Migration Batch Checklist",
        "",
        "Use this exact order; compile gate after each batch.",
        "",
    ]
    for idx, module in enumerate(ordered, start=1):
        count = module_counter.get(module, 0)
        lines.append(f"## Batch {idx}: `{module}` ({count} files)")
        lines.append("- Copy module source files into `app/src/main/kotlin` preserving package.")
        lines.append("- Do not remap package names in this stage.")
        lines.append("- Run compile gate (`:app:compileDebugKotlin`) and fix only batch-local breaks.")
        lines.append("")
    return "\n".join(lines) + "\n"


def main() -> None:
    entries = load_entries()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "MIGRATION_REPORT.md").write_text(build_report(entries), encoding="utf-8")
    (OUT_DIR / "MIGRATION_BATCHES.md").write_text(build_batches(entries), encoding="utf-8")
    print(f"Wrote: {OUT_DIR / 'MIGRATION_REPORT.md'}")
    print(f"Wrote: {OUT_DIR / 'MIGRATION_BATCHES.md'}")


if __name__ == "__main__":
    main()
