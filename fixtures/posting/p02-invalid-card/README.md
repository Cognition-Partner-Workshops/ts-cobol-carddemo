# p02-invalid-card
One transaction carries a card number with no CARDXREF entry.
CBTRN02C rejects it with reason 100 (INVALID CARD NUMBER FOUND) and ends with RC 4;
the other transaction posts normally.
