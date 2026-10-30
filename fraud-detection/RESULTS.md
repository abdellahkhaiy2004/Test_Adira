# Fraud detection — results on the supplied dataset

Dataset: `bank_transactions - bank_transactions.csv`, 5 000 rows, supplied by the project owner.
Reproduce with:

```powershell
.\.venv\Scripts\python.exe fraud_detection.py --data "bank_transactions - bank_transactions.csv"
```

All numbers below are the real output of that command. Nothing here is estimated or rounded by hand.

## 1. Dataset

| | |
|---|---|
| Rows | 5 000 |
| Legitimate (`is_fraud=0`) | 4 743 — 94.86 % |
| Fraud (`is_fraud=1`) | 257 — **5.14 %** |
| Imbalance | 1 fraud per 18.5 legitimate transactions |

Missing values: `merchant_category` 84 (1.68 %), `average_amount_last_7d` 75 (1.50 %),
`country` 66 (1.32 %). All three are imputed inside the pipeline, fitted on the training split only.

`amount` is strongly skewed: median 70.68, mean 128.21, max 3 224.93.

## 2. Split

Stratified 80/20: train 4 000 rows (5.15 % fraud), test 1 000 rows (5.10 % fraud).
`transaction_id` and `account_id` are dropped from the explanatory variables.
Six features remain: `amount`, `hour`, `country`, `merchant_category`, `transactions_last_24h`,
`average_amount_last_7d`.

## 3. Model comparison

Threshold chosen by 5-fold cross-validation **inside the training set**; the test set is used once.

| Model | Average precision | ROC AUC | Precision (fraud) | Recall (fraud) | F1 (fraud) |
|---|---|---|---|---|---|
| **Logistic regression** | **0.1886** | 0.7234 | 0.2083 | 0.1961 | 0.2020 |
| Random forest | 0.0798 | 0.6570 | 0.0839 | 0.2549 | 0.1262 |

A random classifier scores an average precision equal to the fraud rate, **0.0510**. Logistic
regression reaches 0.1886, about 3.7× better than chance; the random forest, at 0.0798, is barely
above it and overfits the 4 000 training rows.

**Logistic regression is the model to keep here.**

## 4. Logistic regression in detail

Chosen threshold: **0.7765** (cross-validated F1 0.2512 on the training folds).

Confusion matrix on the 1 000 held-out rows:

| | predicted legitimate | predicted fraud |
|---|---|---|
| **actual legitimate** | 911 | 38 |
| **actual fraud** | 41 | 10 |

38 legitimate transactions blocked, 41 frauds missed.

Threshold sweep on the test set:

| threshold | precision | recall | F1 | false positives | frauds missed |
|---|---|---|---|---|---|
| 0.10 | 0.0512 | 1.0000 | 0.0974 | 945 | 0 |
| 0.30 | 0.0638 | 0.8824 | 0.1190 | 660 | 6 |
| 0.50 | 0.1221 | 0.6275 | 0.2045 | 230 | 19 |
| 0.70 | 0.1688 | 0.2549 | 0.2031 | 64 | 38 |
| 0.80 | 0.2439 | 0.1961 | 0.2174 | 31 | 41 |
| 0.90 | 0.4000 | 0.0784 | 0.1311 | 6 | 47 |

## 5. Honest reading of these numbers

This model is **not good enough to deploy**, and the figures should be presented as such.

- An average precision of 0.19 means that when the model ranks transactions by risk, the top of that
  ranking is only about one-fifth fraud. Roughly four investigations in five would be wasted.
- The six available features carry limited signal. There is no merchant identity, no device, no
  location of the cardholder, no velocity beyond a 24-hour count, and no history of the account's own
  behaviour relative to the population.
- 4 000 training rows with 206 frauds is very little for learning an interaction-heavy problem, which
  is why the random forest does worse than the linear model rather than better.
- The cross-validated threshold (0.7765) optimises F1, which weights precision and recall equally.
  A real bank would not: it would set the threshold from the cost of a blocked customer against the
  cost of a lost amount, and would likely operate nearer 0.50, accepting 230 false positives to catch
  63 % of fraud instead of 20 %.

The method is sound — no leakage, stratified split, threshold selected without the test set — so these
numbers are a trustworthy measurement of a genuinely weak signal, not an artefact of the pipeline.

## 6. Analysis questions

**1. Why is accuracy alone insufficient?**
Fraud is 5.14 % of the rows, so predicting "legitimate" for everything scores 94.86 % accuracy while
catching zero fraud. This run reaches 92 % accuracy with an F1 of 0.20 on the fraud class — *lower*
accuracy than the useless model, and far more useful. Accuracy is dominated by the majority class and
is blind to the only class that matters.

**2. What trade-off exists between false positives and undetected fraud?**
At the selected threshold: 38 legitimate transactions blocked against 41 frauds missed. A false
positive costs customer friction, a support call and possibly a lost sale; a false negative costs the
fraudulent amount plus the dispute handling. They move in opposite directions, so the threshold is a
business decision about the relative cost of each, not a statistical one.

**3. What is the possible effect of lowering the decision threshold?**
More transactions are flagged: recall rises, precision falls. Measured here, going from 0.80 to 0.50
takes recall from 0.20 to 0.63 while false positives grow from 31 to 230 — three times the fraud
caught for seven times the friction. Average precision does not change, because it summarises the
ranking rather than any single cut-off, which is what makes it the right metric for comparing models.

**4. Why prefer temporal validation on real banking data?**
Fraud patterns drift as fraudsters adapt and as the bank's own defences change. A random split trains
on transactions that occurred after the ones it is scored on, leaking future information and flattering
the result; several transactions of one account can also straddle the split, so the model recognises
the account rather than the behaviour. Splitting by time reproduces production, where the model only
ever sees the past, and it exposes the decay that tells you when to retrain.
