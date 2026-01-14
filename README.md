# Demo Signer – MAB & ETSI Integration Reference

## Purpose of this Demo

This project demonstrates a correct and minimal integration with the Swisscom Trust Services MAB (Multiple Authentication Broker) and ETSI Remote Signing (RDSC) APIs.

The goal is not to provide a full signing SDK, but to show:
- Correct use of OIDC / PAR
- Correct token exchange sequence
- Correct invocation of ETSI signDoc
- Proper separation of concerns between:
  - OAuth / OIDC
  - SAD issuance
  - ETSI signing
  - Document handling (out of scope)

This demo is intentionally simple and transparent to facilitate technical review and audit.