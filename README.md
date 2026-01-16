# Demo Signer (Swisscom Trust Services MAB + ETSI Sign)

This project is a minimal end-to-end demo that shows how to integrate the Swisscom Trust Services Multiple Authentication Broker (MAB) APIs (OIDC + PAR + Token) together with the ETSI Sign API.

It is intentionally built as a reference implementation:
- Uses mTLS for all protected endpoints 
- Uses OIDC + PAR (Pushed Authorization Request)
- Exchanges the authorization code for an access token (SAD JWT)
- Calls the ETSI signDoc endpoint to sign a document digest 
- Provides a small browser UI for manual testing (copy/paste auth code)

Out of scope: PDF signature placeholder embedding (removed intentionally to keep the focus on MAB + ETSI flows).

## Demo Flow (what it proves)

The demo executes this sequence:
1.	Backend creates a PAR request (mTLS) and returns an authorizationUrl to the UI
2.	User opens the authorization URL and completes authentication
3.	User copies the code from the redirect URL back into the demo UI
4. Backend exchanges the code for a token (mTLS)
5. Backend calls ETSI signDoc (mTLS) using:
   - the SAD JWT as SAD in the request body
   - the aud claim from the token to determine the correct ETSI sign endpoint
6.	The UI displays the signing response (SignatureObject etc.)

## Tech Stack

- Java 21 
- Spring Boot (WebFlux)
- Reactor (Mono)
- Netty mTLS (ReactorClientHttpConnector)
- OpenAPI-generated clients (MAB + ETSI)

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
export DISCOVERY_PATH="https://rax-preprod.scapp.swisscom.com/api/auth/realms/broker/.well-known/openid-configuration"

export CLIENT_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export CLIENT_SECRET="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export REDIRECT_URI="https://webhook.site/<your-id>"

export MTLS_BASE_URL="https://rax-preprod.mtls-scapp.swisscom.com"
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

## License

This repository is provided as a reference implementation for integration testing.
No warranty. Use at your own risk.