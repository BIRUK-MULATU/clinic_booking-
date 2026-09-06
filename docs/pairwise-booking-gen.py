"""
All-pairs covering array generator for BookingPairwiseIT.
Greedy: repeatedly emit the row covering the most still-uncovered value pairs.
Seeded with the three approve-path rows so age x coverage fee combos are
also exercised on a successful booking. Run: python3 docs/pairwise-booking-gen.py
"""
import itertools
from decimal import Decimal, ROUND_HALF_UP

factors = {
    "age":      ["CHILD", "ADULT", "SENIOR"],
    "slotFree": ["FREE", "TAKEN"],
    "balance":  ["CLEAR", "OWING"],
    "notice":   ["ENOUGH", "SHORT"],
    "suspend":  ["CLEAR", "SUSPENDED"],
    "coverage": ["C0", "C50", "C100"],
}
names = list(factors)

def all_pairs():
    s = set()
    for i, j in itertools.combinations(range(len(names)), 2):
        for a in factors[names[i]]:
            for b in factors[names[j]]:
                s.add((i, a, j, b))
    return s

seeds = [
    ("CHILD", "FREE", "CLEAR", "ENOUGH", "CLEAR", "C0"),
    ("ADULT", "FREE", "CLEAR", "ENOUGH", "CLEAR", "C50"),
    ("SENIOR", "FREE", "CLEAR", "ENOUGH", "CLEAR", "C100"),
]

uncovered = all_pairs()
rows = list(seeds)
for r in rows:
    for i, j in itertools.combinations(range(len(names)), 2):
        uncovered.discard((i, r[i], j, r[j]))

while uncovered:
    best, best_cov = None, -1
    for combo in itertools.product(*[factors[n] for n in names]):
        cov = sum(1 for i, j in itertools.combinations(range(len(names)), 2)
                  if (i, combo[i], j, combo[j]) in uncovered)
        if cov > best_cov:
            best_cov, best = cov, combo
    rows.append(best)
    for i, j in itertools.combinations(range(len(names)), 2):
        uncovered.discard((i, best[i], j, best[j]))

fee = {"CHILD": 100, "ADULT": 250, "SENIOR": 150}
pct = {"C0": 0, "C50": 50, "C100": 100}
for idx, r in enumerate(rows, 1):
    d = dict(zip(names, r))
    if d["suspend"] == "SUSPENDED":
        out = "SUSPENDED_NO_SHOWS"
    elif d["slotFree"] == "TAKEN":
        out = "SLOT_UNAVAILABLE"
    elif d["balance"] == "OWING":
        out = "OUTSTANDING_BALANCE"
    elif d["notice"] == "SHORT":
        out = "INSUFFICIENT_NOTICE"
    else:
        f = Decimal(fee[d["age"]])
        waived = (f * pct[d["coverage"]] / 100).quantize(Decimal("0.01"), ROUND_HALF_UP)
        out = f"APPROVE fee={f} net={(f - waived).quantize(Decimal('0.01'))}"
    print(f"{idx:2}  " + "  ".join(f"{k}={v}" for k, v in d.items()) + f"  ->  {out}")

# assert full pairwise coverage
uc = all_pairs()
for r in rows:
    for i, j in itertools.combinations(range(len(names)), 2):
        uc.discard((i, r[i], j, r[j]))
assert not uc, uc
print(f"\n{len(rows)} rows, all {len(all_pairs())} value-pairs covered")
