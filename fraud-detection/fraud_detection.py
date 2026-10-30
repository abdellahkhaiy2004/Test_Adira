"""
Bank fraud detection - binary classification experiment (exam part H).

Standalone analytical experiment: it is deliberately NOT part of the Maven application and is not
deployed or called by any microservice.

Run:
    python fraud_detection.py --data bank_transactions.csv

Constraints respected:
  * Identifiers are excluded from the explanatory variables.
  * Preprocessing is fitted on the training data only.
  * The test set is never used for training nor for selecting the decision threshold; the threshold is
    chosen by cross-validation inside the training set.
"""

import argparse
import sys
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    confusion_matrix,
    f1_score,
    precision_recall_curve,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import StratifiedKFold, cross_val_predict, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

TARGET = "is_fraud"
# Excluded from the features: they identify a row or an account, they do not describe behaviour.
IDENTIFIER_COLUMNS = ["transaction_id", "account_id"]
NUMERIC_FEATURES = ["amount", "hour", "transactions_last_24h", "average_amount_last_7d"]
CATEGORICAL_FEATURES = ["country", "merchant_category"]

RANDOM_STATE = 42
TEST_SIZE = 0.2


def rule(title):
    print("\n" + "=" * 78)
    print(title)
    print("=" * 78)


def load_data(path):
    rule("1. DATA LOADING")
    if not Path(path).exists():
        sys.exit(
            f"ERROR: dataset not found: {path}\n"
            "Provide the real bank_transactions.csv with --data, or generate a synthetic one for a dry "
            "run:\n    python generate_sample_data.py"
        )
    frame = pd.read_csv(path)
    print(f"File       : {path}")
    print(f"Rows       : {len(frame):,}")
    print(f"Columns    : {list(frame.columns)}")

    missing = [c for c in [TARGET] + NUMERIC_FEATURES + CATEGORICAL_FEATURES if c not in frame.columns]
    if missing:
        sys.exit(f"ERROR: missing expected column(s): {missing}")
    return frame


def explore(frame):
    rule("2. FRAUD PROPORTION AND MISSING VALUES")

    counts = frame[TARGET].value_counts().sort_index()
    total = len(frame)
    fraud_count = int(counts.get(1, 0))
    fraud_rate = fraud_count / total if total else 0.0

    print("Class distribution:")
    print(f"  legitimate (0): {int(counts.get(0, 0)):>8,}  ({1 - fraud_rate:6.2%})")
    print(f"  fraud      (1): {fraud_count:>8,}  ({fraud_rate:6.2%})")
    if fraud_rate:
        print(f"  imbalance ratio: 1 fraud for {(1 - fraud_rate) / fraud_rate:,.1f} legitimate transactions")

    print("\nMissing values per column:")
    missing = frame.isna().sum()
    if missing.sum() == 0:
        print("  none")
    else:
        for column, count in missing[missing > 0].items():
            print(f"  {column:<28} {count:>8,}  ({count / total:6.2%})")

    print("\nNumeric summary:")
    print(frame[NUMERIC_FEATURES].describe().round(2).to_string())
    return fraud_rate


def split(frame):
    rule("3. STRATIFIED TRAIN / TEST SPLIT")

    present_identifiers = [c for c in IDENTIFIER_COLUMNS if c in frame.columns]
    features = frame.drop(columns=[TARGET] + present_identifiers)
    target = frame[TARGET].astype(int)

    print(f"Identifiers excluded from the features: {present_identifiers}")
    print(f"Explanatory variables kept            : {list(features.columns)}")

    x_train, x_test, y_train, y_test = train_test_split(
        features, target, test_size=TEST_SIZE, stratify=target, random_state=RANDOM_STATE
    )
    print(f"\nTrain: {len(x_train):,} rows, fraud rate {y_train.mean():.2%}")
    print(f"Test : {len(x_test):,} rows, fraud rate {y_test.mean():.2%}")
    print("Stratification keeps the same fraud rate on both sides.")
    return x_train, x_test, y_train, y_test


def build_pipeline(model_name):
    """Preprocessing + model. Every step is fitted later on the training data only."""
    numeric = Pipeline(
        [("impute", SimpleImputer(strategy="median")), ("scale", StandardScaler())]
    )
    categorical = Pipeline(
        [
            ("impute", SimpleImputer(strategy="most_frequent")),
            ("encode", OneHotEncoder(handle_unknown="ignore")),
        ]
    )
    preprocessor = ColumnTransformer(
        [("numeric", numeric, NUMERIC_FEATURES), ("categorical", categorical, CATEGORICAL_FEATURES)]
    )

    if model_name == "logistic":
        model = LogisticRegression(max_iter=1000, class_weight="balanced", random_state=RANDOM_STATE)
    elif model_name == "forest":
        model = RandomForestClassifier(
            n_estimators=200, class_weight="balanced", random_state=RANDOM_STATE, n_jobs=-1
        )
    else:
        raise ValueError(f"unknown model: {model_name}")

    return Pipeline([("preprocess", preprocessor), ("model", model)])


def choose_threshold(pipeline, x_train, y_train):
    """
    Pick the decision threshold using cross-validation INSIDE the training set.

    The test set plays no part here: using it would make the reported performance optimistic, which is
    exactly what the exam forbids.
    """
    rule("5. THRESHOLD SELECTION (training data only)")

    folds = StratifiedKFold(n_splits=5, shuffle=True, random_state=RANDOM_STATE)
    oof_scores = cross_val_predict(pipeline, x_train, y_train, cv=folds, method="predict_proba")[:, 1]

    precisions, recalls, thresholds = precision_recall_curve(y_train, oof_scores)
    # f1 over the candidate thresholds; last precision/recall pair has no threshold attached.
    f1_scores = np.divide(
        2 * precisions[:-1] * recalls[:-1],
        precisions[:-1] + recalls[:-1],
        out=np.zeros_like(precisions[:-1]),
        where=(precisions[:-1] + recalls[:-1]) > 0,
    )
    best = int(np.argmax(f1_scores))
    threshold = float(thresholds[best])

    print("Method: 5-fold stratified cross-validation on the training set, out-of-fold probabilities.")
    print(f"Chosen threshold          : {threshold:.4f}")
    print(f"Cross-validated F1 (fraud): {f1_scores[best]:.4f}")
    print(f"Cross-validated precision : {precisions[best]:.4f}")
    print(f"Cross-validated recall    : {recalls[best]:.4f}")
    return threshold


def evaluate(pipeline, x_test, y_test, threshold):
    rule("6. EVALUATION ON THE HELD-OUT TEST SET")

    probabilities = pipeline.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= threshold).astype(int)

    average_precision = average_precision_score(y_test, probabilities)
    print(f"Average precision (PR AUC): {average_precision:.4f}")
    try:
        print(f"ROC AUC                   : {roc_auc_score(y_test, probabilities):.4f}")
    except ValueError:
        print("ROC AUC                   : undefined (a single class in the test set)")

    print(f"\n--- At the cross-validated threshold {threshold:.4f} ---")
    print(f"Precision (fraud): {precision_score(y_test, predictions, zero_division=0):.4f}")
    print(f"Recall    (fraud): {recall_score(y_test, predictions, zero_division=0):.4f}")
    print(f"F1        (fraud): {f1_score(y_test, predictions, zero_division=0):.4f}")

    print("\nClassification report:")
    print(classification_report(y_test, predictions, target_names=["legitimate", "fraud"], zero_division=0))

    matrix = confusion_matrix(y_test, predictions)
    tn, fp, fn, tp = matrix.ravel() if matrix.size == 4 else (0, 0, 0, 0)
    print("Confusion matrix:")
    print("                 predicted legit   predicted fraud")
    print(f"  actual legit   {tn:>15,}   {fp:>15,}")
    print(f"  actual fraud   {fn:>15,}   {tp:>15,}")
    print(f"\n  false positives (legitimate blocked) : {fp:,}")
    print(f"  false negatives (fraud missed)       : {fn:,}")

    # Shown only to illustrate the trade-off; the threshold above is the one that was selected.
    default_predictions = (probabilities >= 0.5).astype(int)
    print("\n--- For comparison, at the default threshold 0.5 ---")
    print(f"Precision (fraud): {precision_score(y_test, default_predictions, zero_division=0):.4f}")
    print(f"Recall    (fraud): {recall_score(y_test, default_predictions, zero_division=0):.4f}")
    print(f"F1        (fraud): {f1_score(y_test, default_predictions, zero_division=0):.4f}")

    return {
        "average_precision": average_precision,
        "precision": precision_score(y_test, predictions, zero_division=0),
        "recall": recall_score(y_test, predictions, zero_division=0),
        "f1": f1_score(y_test, predictions, zero_division=0),
        "false_positives": int(fp),
        "false_negatives": int(fn),
    }


def threshold_sweep(pipeline, x_test, y_test):
    """Illustrates question 3: what happens when the threshold is lowered."""
    rule("7. THRESHOLD SWEEP (supports analysis question 3)")

    probabilities = pipeline.predict_proba(x_test)[:, 1]
    print(f"{'threshold':>10} {'precision':>10} {'recall':>8} {'F1':>8} {'false pos':>10} {'missed':>8}")
    for threshold in [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9]:
        predictions = (probabilities >= threshold).astype(int)
        matrix = confusion_matrix(y_test, predictions)
        _, fp, fn, _ = matrix.ravel() if matrix.size == 4 else (0, 0, 0, 0)
        print(
            f"{threshold:>10.2f} {precision_score(y_test, predictions, zero_division=0):>10.4f} "
            f"{recall_score(y_test, predictions, zero_division=0):>8.4f} "
            f"{f1_score(y_test, predictions, zero_division=0):>8.4f} {fp:>10,} {fn:>8,}"
        )
    print("\nLowering the threshold raises recall and lowers precision: more fraud caught, more legitimate")
    print("transactions wrongly blocked. Average precision does not move, it summarises the ranking.")


def answer_questions(fraud_rate, results):
    rule("8. ANALYSIS QUESTIONS")

    majority_accuracy = 1 - fraud_rate
    print("1. Why is accuracy alone insufficient?")
    print(f"   The classes are heavily imbalanced: fraud is {fraud_rate:.2%} of the rows. A model that")
    print(f"   predicts 'legitimate' for everything scores {majority_accuracy:.2%} accuracy while catching")
    print("   zero fraud. Accuracy is dominated by the majority class and says nothing about the only")
    print("   class that matters here, so precision, recall and average precision on the fraud class are")
    print("   the metrics to read.")

    print("\n2. What is the trade-off between false positives and undetected fraud?")
    print(f"   At the selected threshold this run blocks {results['false_positives']:,} legitimate")
    print(f"   transactions and misses {results['false_negatives']:,} frauds. A false positive costs")
    print("   customer friction, support calls and possibly a lost sale; a false negative costs the")
    print("   fraudulent amount plus the handling of the dispute. The two move in opposite directions,")
    print("   so the threshold should be set from the relative cost of each, not from F1 alone.")

    print("\n3. What is the possible effect of lowering the decision threshold?")
    print("   More transactions are flagged: recall rises, precision falls, false positives rise and")
    print("   missed frauds fall. The ranking quality is unchanged, so average precision stays the same,")
    print("   which is why it is the right metric to compare models independently of the threshold.")

    print("\n4. Why prefer temporal validation on real banking data?")
    print("   Fraud patterns drift as fraudsters adapt, so a random split trains on transactions that")
    print("   happened after the ones it is scored on, which leaks future information and flatters the")
    print("   result. Several transactions of one account can also land on both sides of a random split.")
    print("   Splitting by time reproduces production conditions, where the model only ever sees the past.")


def main():
    parser = argparse.ArgumentParser(description="Bank fraud detection experiment")
    parser.add_argument("--data", default="bank_transactions.csv", help="path to the CSV dataset")
    parser.add_argument("--model", default="logistic", choices=["logistic", "forest"])
    args = parser.parse_args()

    frame = load_data(args.data)
    fraud_rate = explore(frame)
    x_train, x_test, y_train, y_test = split(frame)

    rule("4. PIPELINE AND TRAINING")
    pipeline = build_pipeline(args.model)
    print(f"Model          : {args.model}")
    print(f"Numeric        : {NUMERIC_FEATURES} -> median imputation + standardisation")
    print(f"Categorical    : {CATEGORICAL_FEATURES} -> most-frequent imputation + one-hot encoding")
    print("Class imbalance: handled with class_weight='balanced'")
    print("The whole pipeline is fitted on the training set only, so the test set leaks nothing.")

    threshold = choose_threshold(pipeline, x_train, y_train)
    pipeline.fit(x_train, y_train)

    results = evaluate(pipeline, x_test, y_test, threshold)
    threshold_sweep(pipeline, x_test, y_test)
    answer_questions(fraud_rate, results)

    rule("DONE")


if __name__ == "__main__":
    main()
