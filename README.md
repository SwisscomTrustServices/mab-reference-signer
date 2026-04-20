# Demo Signer (MAB + ETSI Sign)

This project is a minimal end-to-end demo that shows how to integrate the Swisscom Trust Services Multiple Authentication Broker (MAB) APIs (OIDC + PAR + Token) and the ETSI Sign API.

It is built as a reference implementation for:
- Using mTLS for all protected endpoints 
- Calling OIDC + PAR (Pushed Authorization Request)
- Exchanging the authorization code for an access token (SAD JWT)
- Calling the ETSI signDoc endpoint to sign a document digest 
- Providing a small browser UI for manual testing (copy/paste auth code)
- Preparing a PDF for signature with PDFBox
- Embedding the returned CMS signature back into the PDF to obtain a final, signed PDF without modifying the signed byte ranges
- Adding PAdES DSS/VRI structures to upgrade the signature to Baseline B-LT (LTV enabled)
- Providing a minimal browser UI for manual testing (copy/paste authorization code flow)

## Demo Flow

The demo executes the following end-to-end sequence:
1. User uploads a PDF in the UI
2. Backend prepares the PDF using PDFBox:
   - Adds a signature placeholder
   - Computes the exact byte range to be signed
   - Hashes that byte range
3. Backend creates a PAR request (mTLS) containing the document digest
4. User opens the authorization URL and completes authentication
5. User copies the code from the redirect URL back into the demo UI
6. Backend exchanges the authorization code for a SAD JWT (mTLS)
7. Backend calls ETSI signDoc (mTLS) using:
   - the SAD JWT as SAD in the request body
   - the previously computed document digest
   - the aud claim from the token to determine the correct ETSI sign endpoint
8. Backend embeds the returned CMS signature into the PDF
9. Backend upgrades to PAdES Baseline LT (LTV)
10. The UI receives:
   - metadata about the signature
   - the final signed PDF (Base64), which can be downloaded

## CIBA Flow (UI-driven polling)

In addition to the PAR authorization-code flow above, the demo also supports a CIBA flow in the browser UI:

1. User selects a CIBA family journey (`CIBA_AES` or `CIBA_QES`)
2. UI calls Webfinger check (`/api/ciba/webfinger`)
3. Backend starts CIBA auth (`/api/ciba/auth`) with journey mapping:
   - not onboarded -> `*_IDENT`
   - onboarded -> `*_SIGN`
4. UI starts token polling against `/api/ciba/token`
   - polling loop runs in the UI
   - backend returns `status` (`PENDING` or `READY`) and always includes `nextPollInSec`
5. On `READY`, UI stores the SAD token and the flow continues with ETSI signing (step 4 in the UI)

## Tech Stack

- Java 21 
- Spring Boot (WebFlux)
- Project Reactor (Mono / reactive flows)
- Netty HTTP client with mTLS (ReactorClientHttpConnector)
- Apache PDFBox
  - External PDF signing
  - CMS embedding into PDF signature placeholders
  - Incremental update handling
  - Manual construction of PAdES DSS + VRI structures
  - PAdES Baseline B-LT (LTV enabled) support
- OpenAPI-generated clients (MAB + ETSI)
- Static HTML/CSS/JavaScript UI
  - Minimal browser UI for manual end-to-end testing
  - Copy/paste Authorization Code flow

## Prerequisites

To run this demo against Swisscom preprod you need:

- mTLS client certificate + key issued by STS 
- client_id and client_secret issued by STS 
- A configured redirect URL (for this demo, a “copy/paste code” workflow is used)

## Configuration

This project is safe for public repositories and does not contain real secrets.
All values are injected via environment variables.

### application.yml (defaults)

The repository ships with safe defaults like:
- QTSP_CLIENT_ID=00000000-0000-0000-0000-000000000000 
- QTSP_MTLS_BASE_URL=https://example.invalid

### Required environment variables

Set these before starting the app:

```bash
export DISCOVERY_PATH="https://example.invalid/.well-known/openid-configuration"

export CLIENT_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export CLIENT_SECRET="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export REDIRECT_URI="https://webhook.site/<your-id>"

export MTLS_BASE_URL="https://example.invalid"
export MTLS_CLIENT_CERT="classpath:test-client.pem"
export MTLS_CLIENT_KEY="classpath:test-client.key"
```

For local development you can also point redirect URI to any endpoint that lets you inspect the URL parameters and copy the code.

## Run the Demo

```bash
./gradlew bootRun
```

Server starts on:
- http://localhost:8081

Open the UI in a browser:
- http://localhost:8081/

## Security Notes (important)

This demo is designed so it can be published safely:
- No real QTSP credentials are committed
- mTLS key material in the repository is demo-only 
- Tokens are redacted in responses 
- Session state is stored only in-memory (not production-safe)

Do not use this code as production signing software.

Use it as a reference integration blueprint.