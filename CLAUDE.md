# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Maven MCP Server — a Java-based MCP (Model Context Protocol) server that wraps Maven CLI operations (`compile`, `test`, `package`, `clean`) and returns structured, parsed output (errors with file/line/column, test results with pass/fail details) to AI agents via stdio transport.

**Current status:** Pre-implementation specification phase. `REQUIREMENTS.md` is finalized. `SPEC.md` (architecture/design) is pending. No source code exists yet.

## Agent Roles (System Prompts)

This repo uses role-specific system prompts that define agent behavior:

- **`ANALYST.md`** — Requirements Analyst role. Conducts discovery conversations and produces `REQUIREMENTS.md`. Does NOT make technology/architecture decisions.
- **`ARCHITECT.md`** — Solution Architect role. Reads `REQUIREMENTS.md`, conducts interactive design sessions, and produces `SPEC.md`. Does NOT write implementation code.

When loaded as a system prompt, follow the role's rules strictly (e.g., Architect must never generate runnable code; Analyst must never recommend frameworks).

## OpenSpec Workflow

The project uses a spec-driven artifact workflow managed via `/opsx:*` slash commands:

| Command | Purpose |
|---------|---------|
| `/opsx:explore` | Think through ideas, investigate problems (no implementation) |
| `/opsx:new` | Start a new change (creates proposal → specs → design → tasks) |
| `/opsx:continue` | Create the next artifact in an existing change |
| `/opsx:ff` | Fast-forward: generate all artifacts at once |
| `/opsx:apply` | Implement tasks from a change |
| `/opsx:verify` | Validate implementation matches change artifacts |
| `/opsx:sync` | Sync delta specs to main specs |
| `/opsx:archive` | Archive a completed change |

Artifact sequence per change: `proposal.md` → `specs/*.md` → `design.md` → `tasks.md` → implementation.

Changes live in `openspec/changes/`, archived ones in `openspec/changes/archive/`.

## Key Technical Decisions (from REQUIREMENTS.md)

- **Language:** Java 25+, built with Maven (dogfooding)
- **MCP SDK:** `io.modelcontextprotocol.sdk:mcp` v0.17+ (official Java SDK, stdio transport)
- **Architecture:** Wrapper CLI — spawns Maven as external process via `ProcessBuilder`, parses stdout/stderr
- **Scope:** Single-module projects only (no reactor/multi-module)
- **Tools exposed:** `maven_compile`, `maven_test`, `maven_package`, `maven_clean`
- **Maven detection:** Prefers `./mvnw` wrapper, falls back to system `mvn`

## Open Architecture Questions (for Architect)

- Stack trace format in test results — truncate to N lines or return full? (Open Question #2)
- Cache `mvnw` vs `mvn` detection or check every invocation? (Open Question #3)

## Language

Requirements and stakeholder communication are in Polish. Code comments, API, and technical docs should be in English.
