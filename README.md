# cloud-itonami-isco-7231

Open Occupation Blueprint for **ISCO-08 7231**: Motor Vehicle Mechanics and Repairers.

This repository designs a forkable OSS business for an independent auto mechanic: a diagnostic and lift-assist robot performs vehicle scanning and component-handling work under a governor-gated actor, so the shop keeps its own service records instead of renting a closed shop-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a diagnostic and lift-assist robot performs vehicle scanning and component handling under an actor that proposes
actions and an independent **Auto Repair Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating a vehicle lift, or working near a running engine) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
service request + vehicle history + repair scope
        |
        v
Repair Advisor -> Auto Repair Governor -> repair, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7231`). Required capabilities:

- :robotics
- :forms
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/auto_repair/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `auto-repair.store` — `Store` protocol + `MemStore`: vehicles, repair
  orders, repair actions, invoices. A repair-action/invoice can only be
  recorded against a registered order on a registered vehicle (order
  provenance).
- `auto-repair.governor` — `AutoRepairGovernor`: `assess` gates a proposal
  against the order env. Hard invariants force `:hold` (no order,
  direct-write instead of `:propose`, or an `:engine-running` repair at
  below `:high` safety-class); engine-running repairs always require
  `:high`+ safety-class and thus `:human-approval` — they can never be
  auto-approved; low-confidence proposals also escalate.

```bash
clojure -M:test   # 7 tests, 13 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 12th `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210` and `-5223` (ADR-2607012000).

## License

AGPL-3.0-or-later.
