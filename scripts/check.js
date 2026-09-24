// Compiles all Java sources with javac and runs the dependency-free test runner.
// Usage: node scripts/check.js
const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const out = path.join(root, "build", "classes");

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const p = path.join(dir, e.name);
    return e.isDirectory() ? walk(p) : p.endsWith(".java") ? [p] : [];
  });
}

fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });
const sources = [...walk(path.join(root, "src", "main")), ...walk(path.join(root, "src", "test"))];
execFileSync("javac", ["-Xlint:all", "-Werror", "-d", out, ...sources], { stdio: "inherit" });
execFileSync("java", ["-cp", out, "lab.TestRunner"], { stdio: "inherit" });
