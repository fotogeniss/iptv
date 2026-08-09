# Google Play Billing release setup

The Android client is wired for one non-consumable product. Price, currency and
ownership are read from Google Play; they are never hard-coded in Prelude+.

## Product contract

Create this product in Play Console under **Monetize > Products > In-app
products**:

| Field | Required value |
| --- | --- |
| Product ID | `prelude_plus_lifetime` |
| Type | Non-consumable one-time product |
| Name | PRELUDE+ Premium |
| Status | Active |

The ID is an API contract. Changing it in Play Console also requires changing
`BillingProductCatalog.kt` and testing purchase restoration from an older app
version.

## Play Console steps

1. Configure the product and its price/regions, then activate it.
2. Upload a production-signed Android App Bundle to an internal testing track.
3. Add license testers and opt each tester into the internal track.
4. Install Prelude+ from the Play Store test link. A sideloaded APK cannot fully
   validate the real purchase flow.
5. Exercise successful, cancelled and pending test payments.
6. Confirm that **Restore purchases** works after clearing app data and on a
   second device using the same Play account.
7. Confirm that a successful Play query with no owned product returns the user to
   the free tier (refund/revocation path).

## Client guarantees

- One application-scoped `BillingClient` prevents duplicate purchase callbacks.
- Product details are queried again before opening the purchase UI, avoiding
  stale offer tokens.
- `PENDING` never grants Premium.
- Owned purchases are queried at startup, resume and explicit restore.
- A successful empty ownership response revokes the cached entitlement.
- A temporary Play/network error keeps the last known entitlement until a
  successful query can resolve it.
- New purchases are acknowledged; failed acknowledgement is retried when the
  purchase is queried again.

## Verification boundary

Prelude+ has no publisher backend. `DevicePurchaseVerifier` accepts only purchase
evidence delivered by BillingClient for the application package and the known
product ID, with a nonblank purchase token and Play payload. Only a verified
`PURCHASED` result grants Premium; pending, missing and unrelated purchases do
not.

`VerificationLevel.SERVER` remains readable solely for compatibility with any
previously persisted entitlement value. The current client does not emit that
level and this release plan does not promise a server-verification phase. A
future change to the store threat model would be a separate product, security,
legal and Play-compliance decision.

## Release evidence

For every release that touches billing, attach to the release QA record:

- Play track and version code;
- tester account class (never the email address);
- successful purchase and restore results;
- pending-payment result;
- refund/revocation result;
- device-verification result for the expected package and product ID.
