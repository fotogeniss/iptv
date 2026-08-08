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

## Mandatory backend gate before public paid release

The current `DevicePurchaseVerifier` is a deliberately isolated interim adapter.
Before public monetization, replace it with a `PurchaseVerifier` implementation
that sends the purchase token to the Prelude backend. The backend must:

1. Treat the purchase token as the unique transaction key.
2. Verify it with Google Play Developer API `Purchases.products:get`.
3. Check package name, product ID, purchase state and account ownership.
4. Grant entitlement only for a valid `PURCHASED` response.
5. Acknowledge valid purchases server-side.
6. Consume Real-time Developer Notifications and Voided Purchases so refunds and
   revocations propagate while the app is closed.

The backend response should map to `VerificationLevel.SERVER`; no UI or
BillingClient rewrite is required.

## Release evidence

For every release that touches billing, attach to the release QA record:

- Play track and version code;
- tester account class (never the email address);
- successful purchase and restore results;
- pending-payment result;
- refund/revocation result;
- backend verification/RTDN evidence once the server integration is enabled.
