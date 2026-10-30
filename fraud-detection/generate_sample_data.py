"""
Generates a SYNTHETIC bank_transactions.csv so the pipeline can be executed end to end.

This is a dry-run aid only. The numbers it produces are invented and must never be presented as results
of the exam dataset. Replace it with the real bank_transactions.csv as soon as it is available:

    python fraud_detection.py --data bank_transactions.csv
"""

import argparse
import uuid

import numpy as np
import pandas as pd

RANDOM_STATE = 42
COUNTRIES = ["MA", "FR", "ES", "US", "DE"]
CATEGORIES = ["GROCERY", "TRAVEL", "ELECTRONICS", "RESTAURANT", "FUEL", "ONLINE"]


def generate(rows, fraud_rate, seed=RANDOM_STATE):
    rng = np.random.default_rng(seed)
    fraud_count = int(rows * fraud_rate)
    legit_count = rows - fraud_count

    def block(count, fraud):
        if fraud:
            # Fraud here skews to larger amounts, night hours, rarer countries and bursts of activity.
            amount = rng.lognormal(mean=5.6, sigma=0.9, size=count)
            hour = rng.choice(range(24), size=count, p=_night_weighted())
            country = rng.choice(COUNTRIES, size=count, p=[0.20, 0.15, 0.15, 0.30, 0.20])
            last_24h = rng.poisson(7, size=count)
        else:
            amount = rng.lognormal(mean=3.6, sigma=0.8, size=count)
            hour = rng.choice(range(24), size=count, p=_day_weighted())
            country = rng.choice(COUNTRIES, size=count, p=[0.55, 0.20, 0.10, 0.05, 0.10])
            last_24h = rng.poisson(2, size=count)

        return pd.DataFrame(
            {
                "transaction_id": [str(uuid.uuid4()) for _ in range(count)],
                "account_id": [str(uuid.uuid4()) for _ in range(count)],
                "amount": np.round(amount, 2),
                "hour": hour,
                "country": country,
                "merchant_category": rng.choice(CATEGORIES, size=count),
                "transactions_last_24h": last_24h,
                "average_amount_last_7d": np.round(rng.lognormal(mean=3.6, sigma=0.5, size=count), 2),
                "is_fraud": 1 if fraud else 0,
            }
        )

    frame = pd.concat([block(legit_count, False), block(fraud_count, True)], ignore_index=True)
    frame = frame.sample(frac=1.0, random_state=seed).reset_index(drop=True)

    # A little missingness, so the imputation steps of the pipeline are actually exercised.
    for column, fraction in [("average_amount_last_7d", 0.03), ("country", 0.02)]:
        mask = rng.random(len(frame)) < fraction
        frame.loc[mask, column] = None
    return frame


def _night_weighted():
    weights = np.array([3.0 if h < 6 or h >= 22 else 1.0 for h in range(24)])
    return weights / weights.sum()


def _day_weighted():
    weights = np.array([0.3 if h < 6 or h >= 22 else 1.0 for h in range(24)])
    return weights / weights.sum()


def main():
    parser = argparse.ArgumentParser(description="Generate a synthetic dataset for a dry run")
    parser.add_argument("--rows", type=int, default=20000)
    parser.add_argument("--fraud-rate", type=float, default=0.015)
    parser.add_argument("--output", default="bank_transactions_SYNTHETIC.csv")
    args = parser.parse_args()

    frame = generate(args.rows, args.fraud_rate)
    frame.to_csv(args.output, index=False)
    print(f"Wrote {len(frame):,} SYNTHETIC rows to {args.output} (fraud rate {frame.is_fraud.mean():.2%})")
    print("These numbers are invented. Do not report them as results of the exam dataset.")


if __name__ == "__main__":
    main()
