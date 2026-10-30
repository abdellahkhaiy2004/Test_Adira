# Fraud detection (exam part H)

Standalone analytical experiment in Python. It is **not** a Maven module, is not deployed, and no
microservice calls it — the exam explicitly does not ask for deployment or integration.

## Setup
```powershell
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```
A `.venv` is already created here with pandas 3.1.0 and scikit-learn 1.9.1.

## Run
```powershell
.\.venv\Scripts\python.exe fraud_detection.py --data bank_transactions.csv
.\.venv\Scripts\python.exe fraud_detection.py --data bank_transactions.csv --model forest
```
Unix: `./.venv/bin/python fraud_detection.py --data bank_transactions.csv`

## Dataset

The supplied dataset is `bank_transactions - bank_transactions.csv` (5 000 rows) in this folder:

```powershell
.\.venv\Scripts\python.exe fraud_detection.py --data "bank_transactions - bank_transactions.csv"
```

**Results on it are in [`RESULTS.md`](RESULTS.md)** — real output, including the honest finding that the
model is not good enough to deploy (average precision 0.19 against a 0.051 baseline).

`generate_sample_data.py` remains for running the pipeline without the real file; its numbers are
invented and must never be reported as results.

Expected columns: `transaction_id`, `account_id`, `amount`, `hour`, `country`, `merchant_category`,
`transactions_last_24h`, `average_amount_last_7d`, `is_fraud`.

## What the script does
1. Loads the CSV and checks the expected columns are present.
2. Prints the fraud proportion, the class imbalance ratio and the missing values per column.
3. Stratified train/test split (80/20), so both sides keep the same fraud rate.
4. Drops `transaction_id` and `account_id`: they identify a row, they do not describe behaviour.
5. Pipeline — numeric: median imputation + standardisation; categorical: most-frequent imputation +
   one-hot encoding (`handle_unknown="ignore"`). Imbalance handled with `class_weight="balanced"`.
6. Trains logistic regression (default) or a random forest.
7. Evaluates on the held-out test set: precision, recall and F1 of the fraud class, the confusion matrix
   and the average precision (PR AUC).
8. Prints a threshold sweep and the answers to the four analysis questions.

## How leakage is avoided
- The whole pipeline (imputation, encoding, scaling) is fitted **inside** `fit()` on the training set
  only, so no test statistic reaches the transformers.
- The decision threshold is chosen by 5-fold stratified cross-validation **within the training set**
  (`cross_val_predict` out-of-fold probabilities), never from the test set. The test set is touched once,
  to produce the final numbers.
- The historical features (`transactions_last_24h`, `average_amount_last_7d`) are stated by the exam to
  be computed only from information available before each transaction, so they carry no future knowledge.

## Known limitation
The split is random, not temporal. The exam asks for a stratified split, so that is what the script does,
and analysis question 4 explains why a real deployment should validate over time instead.
