#!/usr/bin/env python3
"""Lightweight deterministic validation for repository-local agent assets.

Uses Python standard library only. It validates structure, repository-path integration,
reference routing, JSON, suspicious Unicode, and executable-script review indicators.
It does not replace behavioral evals or human review of flagged executable code.
"""
from __future__ import annotations

import ast
import json
import re
import sys
from pathlib import Path

ALLOWED_TOP_LEVEL = {"name", "description", "license", "compatibility", "metadata", "allowed-tools"}
NAME_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
TOP_KEY_RE = re.compile(r"^([A-Za-z0-9_-]+):(?:\s|$)")
REFERENCE_RE = re.compile(r"references/[A-Za-z0-9_.\-/]+\.(?:md|json|ya?ml)")
DANGEROUS_UNICODE = {"\u202A", "\u202B", "\u202C", "\u202D", "\u202E", "\u2066", "\u2067", "\u2068", "\u2069", "\u200B", "\u200C", "\u200D", "\uFEFF"}
SCRIPT_EXTENSIONS = {".py", ".sh", ".ps1", ".js", ".ts"}
NON_PY_REVIEW_PATTERNS = {
    "network-or-upload": re.compile(r"\b(curl|wget|Invoke-WebRequest|requests\.|urllib|fetch\(|webhook|socket\.)", re.I),
    "process-execution": re.compile(r"\b(subprocess\.|os\.system\s*\(|bash\s+-c|sh\s+-c|powershell\b)", re.I),
    "package-install": re.compile(r"\b(pip|npm|yarn|pnpm|apt(?:-get)?|brew)\s+install\b", re.I),
}
NON_PY_HARD_FAIL_PATTERNS = {
    "destructive-shell": re.compile(r"\brm\s+-rf\b|\bgit\s+push\b[^\n]*--force|\bgit\s+reset\s+--hard\b|\bgit\s+clean\s+-f", re.I),
}


def extract_frontmatter(text: str):
    if not text.startswith("---\n"):
        return None
    end = text.find("\n---\n", 4)
    if end < 0:
        return None
    return text[4:end]


def top_level_keys(fm: str):
    out = []
    for line in fm.splitlines():
        if line.startswith((" ", "\t")) or not line.strip() or line.lstrip().startswith("#"):
            continue
        m = TOP_KEY_RE.match(line)
        if m:
            out.append(m.group(1))
    return out


def scalar_or_block(fm: str, key: str):
    lines = fm.splitlines()
    for i, line in enumerate(lines):
        if line.startswith(key + ":"):
            tail = line.split(":", 1)[1].strip()
            if tail in {">", "|", ">-", "|-", ">+", "|+"}:
                vals = []
                for x in lines[i + 1:]:
                    if x.startswith((" ", "\t")):
                        vals.append(x.strip())
                    else:
                        break
                return " ".join(vals).strip()
            if len(tail) >= 2 and tail[0] == tail[-1] and tail[0] in "\"'":
                return tail[1:-1].replace("''", "'") if tail[0] == "'" else tail[1:-1]
            return tail
    return None


def validate_skill(skill_dir: Path):
    errors, warnings = [], []
    f = skill_dir / "SKILL.md"
    if not f.exists():
        return [f"{skill_dir}: missing SKILL.md"], warnings
    text = f.read_text(encoding="utf-8")
    fm = extract_frontmatter(text)
    if fm is None:
        return [f"{f}: missing/invalid YAML frontmatter delimiters"], warnings
    keys = top_level_keys(fm)
    extra = set(keys) - ALLOWED_TOP_LEVEL
    if extra:
        errors.append(f"{f}: custom top-level keys {sorted(extra)}; move under metadata")
    name = scalar_or_block(fm, "name")
    desc = scalar_or_block(fm, "description")
    if not name:
        errors.append(f"{f}: name missing")
    else:
        if len(name) > 64:
            errors.append(f"{f}: name > 64 chars")
        if not NAME_RE.fullmatch(name):
            errors.append(f"{f}: invalid name format: {name!r}")
        if name != skill_dir.name:
            errors.append(f"{f}: name {name!r} != folder {skill_dir.name!r}")
    if not desc:
        errors.append(f"{f}: description missing/empty")
    elif len(desc) > 1024:
        errors.append(f"{f}: description > 1024 chars")
    n = len(text.splitlines())
    if n >= 500:
        warnings.append(f"{f}: {n} lines; use progressive disclosure")

    references_dir = skill_dir / "references"
    referenced = set(REFERENCE_RE.findall(text))
    for rel in sorted(referenced):
        if not (skill_dir / rel).is_file():
            errors.append(f"{f}: referenced file does not exist: {rel}")
    if references_dir.is_dir():
        for ref in sorted(p for p in references_dir.rglob("*") if p.is_file()):
            rel = ref.relative_to(skill_dir).as_posix()
            if rel not in referenced:
                warnings.append(f"{f}: orphan/unrouted reference: {rel}")
    return errors, warnings


def dotted_name(node):
    if isinstance(node, ast.Name):
        return node.id
    if isinstance(node, ast.Attribute):
        left = dotted_name(node.value)
        return f"{left}.{node.attr}" if left else node.attr
    return None


def scan_python_script(path: Path):
    errors, warnings = [], []
    try:
        tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
    except SyntaxError as ex:
        return [f"{path}: Python syntax error: {ex}"], warnings

    review_import_roots = {"subprocess", "requests", "urllib", "socket", "httpx"}
    review_calls = {"os.system", "eval", "exec", "subprocess.run", "subprocess.Popen", "subprocess.call", "subprocess.check_output", "subprocess.check_call"}
    network_call_prefixes = ("requests.", "urllib.request.", "socket.", "httpx.")

    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                root = alias.name.split(".", 1)[0]
                if root in review_import_roots:
                    warnings.append(f"{path}: REVIEW REQUIRED import: {alias.name}")
        elif isinstance(node, ast.ImportFrom):
            root = (node.module or "").split(".", 1)[0]
            if root in review_import_roots:
                warnings.append(f"{path}: REVIEW REQUIRED import: {node.module}")
        elif isinstance(node, ast.Call):
            name = dotted_name(node.func) or ""
            if name in review_calls or name.startswith(network_call_prefixes):
                warnings.append(f"{path}: REVIEW REQUIRED call: {name}")
            if name in {"open", "Path"} and node.args and isinstance(node.args[0], ast.Constant) and isinstance(node.args[0].value, str):
                target = node.args[0].value.lower().replace("\\", "/")
                if target.endswith("/.env") or target == ".env" or "id_rsa" in target or "private_key" in target:
                    warnings.append(f"{path}: REVIEW REQUIRED sensitive-path access: {node.args[0].value}")
    return errors, warnings


def scan_executable_scripts(agents: Path):
    errors, warnings = [], []
    scripts = agents / "scripts"
    if not scripts.is_dir():
        return errors, warnings
    for path in sorted(p for p in scripts.rglob("*") if p.is_file() and p.suffix.lower() in SCRIPT_EXTENSIONS):
        if path.suffix.lower() == ".py":
            e, w = scan_python_script(path)
            errors += e
            warnings += w
        else:
            text = path.read_text(encoding="utf-8", errors="replace")
            for rule, pattern in NON_PY_HARD_FAIL_PATTERNS.items():
                if pattern.search(text):
                    errors.append(f"{path}: unsafe executable-script pattern: {rule}")
            for rule, pattern in NON_PY_REVIEW_PATTERNS.items():
                if pattern.search(text):
                    warnings.append(f"{path}: REVIEW REQUIRED executable-script pattern: {rule}")
    return errors, warnings


def simple_top_scalar(text: str, key: str):
    m = re.search(rf"(?m)^{re.escape(key)}:\s*([^#\n]+?)\s*$", text)
    return m.group(1).strip().strip("\"'") if m else None


def simple_mapping_block(text: str, key: str):
    lines = text.splitlines()
    start = None
    for i, line in enumerate(lines):
        if line.rstrip() == key + ":" and not line.startswith((" ", "\t")):
            start = i + 1
            break
    if start is None:
        return {}
    out = {}
    for line in lines[start:]:
        if line and not line.startswith((" ", "\t")):
            break
        m = re.match(r"^\s{2}([A-Za-z0-9_-]+):\s*(.+?)\s*$", line)
        if m:
            out[m.group(1)] = m.group(2).strip().strip("\"'")
    return out


def validate_repo_contract(agents: Path):
    errors, warnings = [], []
    contract = agents / "repo-contract.yml"
    if not contract.is_file():
        return [f"missing {contract}"], warnings
    text = contract.read_text(encoding="utf-8")
    repo_root = agents.parent

    for key in ("agent_entrypoint", "policy_document", "document_register"):
        value = simple_top_scalar(text, key)
        if not value:
            errors.append(f"{contract}: missing {key}")
        elif not (repo_root / value).exists():
            errors.append(f"{contract}: {key} path does not exist: {value}")

    canonical = simple_mapping_block(text, "canonical_docs")
    if not canonical:
        errors.append(f"{contract}: canonical_docs mapping missing/empty")
    for name, rel in canonical.items():
        if not (repo_root / rel).exists():
            errors.append(f"{contract}: canonical_docs.{name} path does not exist: {rel}")
    return errors, warnings


def validate_eval_isolation(agents: Path):
    errors, warnings = [], []
    harness = agents / "scripts" / "eval-run.py"
    regression = agents / "scripts" / "tests" / "test_eval_run.py"
    contract = agents / "repo-contract.yml"
    if not harness.is_file():
        return [f"missing {harness}"], warnings

    harness_text = harness.read_text(encoding="utf-8")
    for token in (
        "ensure_disjoint_layout",
        "evaluator_artifacts_in_sut",
        "PREPARED_NOT_RUN",
        "evaluation_leakage",
    ):
        if token not in harness_text:
            errors.append(f"{harness}: missing eval-isolation control: {token}")
    if not regression.is_file():
        errors.append(f"missing eval-isolation regression test: {regression}")
    elif "test_tested_agent_must_not_access_evaluator_only_artifacts" not in regression.read_text(encoding="utf-8"):
        errors.append(f"{regression}: missing evaluator-only artifact access regression")

    contract_text = contract.read_text(encoding="utf-8") if contract.is_file() else ""
    for phrase in (
        "eval_isolation:",
        "harness_outside_evaluated_workspace: true",
        "hidden_artifacts_copied_to_sut: false",
    ):
        if phrase not in contract_text:
            errors.append(f"{contract}: missing eval-isolation contract: {phrase}")

    acceptance = agents / "evals" / "acceptance" / "acceptance-cases.json"
    if acceptance.is_file():
        cases = load_json_file(acceptance)
        a01 = next((case for case in cases.get("cases", []) if case.get("id") == "A01"), None)
        expected_prompt = (
            "Change FR-19 from ACTIVE to OUT_OF_SCOPE while FR-20 remains ACTIVE. "
            "Update the requirement documentation consistently without changing identifier history."
        )
        if not a01 or a01.get("prompt") != expected_prompt:
            errors.append(f"{acceptance}: A01 visible prompt is not the approved minimal prompt")
    return errors, warnings


def load_json_file(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main():
    agents = Path(sys.argv[1] if len(sys.argv) > 1 else ".agents")
    errors, warnings = [], []
    if not agents.is_dir():
        print(f"FAIL: {agents} not found")
        return 2

    for p in agents.rglob("*"):
        if not p.is_file():
            continue
        try:
            t = p.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if any(ch in t for ch in DANGEROUS_UNICODE):
            errors.append(f"{p}: suspicious bidi/zero-width control")

    skills = agents / "skills"
    if not skills.is_dir():
        errors.append("missing .agents/skills")
    else:
        for d in sorted(x for x in skills.iterdir() if x.is_dir()):
            e, w = validate_skill(d)
            errors += e
            warnings += w

    for p in agents.rglob("*.json"):
        try:
            json.loads(p.read_text(encoding="utf-8"))
        except Exception as ex:
            errors.append(f"{p}: invalid JSON: {ex}")

    route = agents / "evals/routing/routing-cases.json"
    if route.exists():
        try:
            n = len(json.loads(route.read_text(encoding="utf-8")).get("cases", []))
            if n < 16:
                errors.append(f"{route}: only {n} routing cases; repository minimum is 16")
        except Exception:
            pass

    acceptance = agents / "evals/acceptance/acceptance-cases.json"
    if acceptance.exists():
        try:
            n = len(json.loads(acceptance.read_text(encoding="utf-8")).get("cases", []))
            if n < 10:
                errors.append(f"{acceptance}: only {n} acceptance cases; repository minimum is 10")
        except Exception:
            pass

    required = [
        agents / "POLICY.md",
        agents / "repo-contract.yml",
        agents / "workflows/evaluator-optimizer.md",
        agents / "workflows/acceptance-evaluation.md",
        agents / "evals/runs/README.md",
        agents / "evals/acceptance/README.md",
    ]
    for p in required:
        if not p.exists():
            errors.append(f"missing required agent asset: {p}")

    policy = (agents / "POLICY.md").read_text(encoding="utf-8") if (agents / "POLICY.md").exists() else ""
    for phrase in ["GitHub Batch Approval", "Evaluator Separation", "Stable requirement"]:
        if phrase.lower() not in policy.lower():
            warnings.append(f"POLICY.md: expected concept not found: {phrase}")

    repo_root = agents.parent
    # A standalone .agents package cannot prove repository-path integration. Validate
    # contract paths only when the parent looks like an actual repository checkout.
    if (repo_root / ".git").exists() or (repo_root / "AGENTS.md").exists() or (repo_root / "README.md").exists():
        e, w = validate_repo_contract(agents)
        errors += e
        warnings += w

    e, w = scan_executable_scripts(agents)
    errors += e
    warnings += w

    e, w = validate_eval_isolation(agents)
    errors += e
    warnings += w

    # De-duplicate identical warnings while preserving order.
    seen = set()
    warnings = [x for x in warnings if not (x in seen or seen.add(x))]

    for w in warnings:
        print("WARN:", w)
    if errors:
        for e in errors:
            print("FAIL:", e)
        print(f"\nValidation failed: {len(errors)} error(s), {len(warnings)} warning(s).")
        return 1

    skill_count = len([x for x in skills.iterdir() if x.is_dir()]) if skills.exists() else 0
    print(f"PASS: agent assets valid ({skill_count} skill directories, {len(warnings)} warning(s)).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
