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

## Demo Flow

The demo executes the following end-to-end sequence:
1. User uploads a PDF in the UI
2. Backend prepares the PDF using PDFBox:
   - Adds a signature placeholder
   - Computes the exact byte range to be signed
   - Hashes that byte range
3. Backend creates a PAR request (mTLS) containing the document digest
4. User opens the authorization URL and completes authentication
5. On redirect back, the UI can read `code` + `state` from the callback URL query parameters
6. User can still copy/paste `code` manually if using an external redirect inspector
7. Backend exchanges the authorization code for a SAD JWT (mTLS)
8. Backend calls ETSI signDoc (mTLS) using:
- the SAD JWT as SAD in the request body
- the previously computed document digest
- the aud claim from the token to determine the correct ETSI sign endpoint
9. Backend embeds the returned CMS signature into the PDF
10. Backend upgrades to PAdES Baseline LT (LTV)
11. The UI receives:
- metadata about the signature
- the final signed PDF (Base64), which can be downloaded

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
- Thymeleaf
  - Minimal browser UI for manual end-to-end testing
  - Manual end-to-end testing
  - Copy/paste Authorization Code flow

## Prerequisites

To run this demo against Swisscom preprod you need:

- mTLS client certificate + key issued by STS 
- client_id and client_secret issued by STS 
- A configured redirect URL (either back to this UI or to an inspection endpoint for manual copy/paste)

## Configuration

This project is safe for public repositories and does not contain real secrets.
All values are injected via environment variables referenced from `src/main/resources/application.yaml`.

For local development, the app also loads a project-root `.env` file automatically at startup via Spring Boot config import. The `.env` file uses plain `KEY=value` entries, so you can keep local settings in one place instead of exporting them manually in every shell.

### application.yml (defaults)

The repository ships with safe defaults like:
- `CLIENT_ID=00000000-0000-0000-0000-000000000000`
- `MTLS_BASE_URL=https://example.invalid`

### Variable names the app expects

These are the exact variable names currently referenced by the app:

- `DISCOVERY_PATH`
- `CLIENT_ID`
- `CLIENT_SECRET`
- `REDIRECT_URI`
- `MTLS_BASE_URL`
- `MTLS_CLIENT_CERT`
- `MTLS_CLIENT_KEY`
- `SPRING_PROFILES_ACTIVE`

### Using a local `.env` file

Create a `.env` file in the project root. You can start from the checked-in example:

```bash
cp .env.example .env
```

Sample `.env`:

```dotenv
DISCOVERY_PATH=https://example.invalid/.well-known/openid-configuration
CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
CLIENT_SECRET=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
REDIRECT_URI=https://webhook.site/<your-id>
MTLS_BASE_URL=https://example.invalid
MTLS_CLIENT_CERT=file:/absolute/path/to/client-cert.pem
MTLS_CLIENT_KEY=file:/absolute/path/to/client-key.pem
SPRING_PROFILES_ACTIVE=dev
```

For `MTLS_CLIENT_CERT` and `MTLS_CLIENT_KEY`, use Spring resource syntax such as `file:/absolute/path/to/client-cert.pem` or `classpath:...`.

### Exported environment variables

If you prefer, you can still export the variables manually before starting the app.
If the same variable is defined in both places, the exported OS environment variable wins over the value from `.env`. This keeps local defaults convenient while preserving the usual ability to override values per shell or CI job.

```bash
export DISCOVERY_PATH="https://example.invalid/.well-known/openid-configuration"

export CLIENT_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export CLIENT_SECRET="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export REDIRECT_URI="https://webhook.site/<your-id>"

export MTLS_BASE_URL="https://example.invalid"
export MTLS_CLIENT_CERT="file:/absolute/path/to/client-cert.pem"
export MTLS_CLIENT_KEY="file:/absolute/path/to/client-key.pem"
```

For local development you can also point redirect URI to any endpoint that lets you inspect the URL parameters and copy the code.

## Run the Demo

```bash
make
```

This runs `./gradlew bootRun` via the project `Makefile`.

If you are using `.env`, no extra export step is required before running that command.

Additional shortcuts:

```bash
make build   # ./gradlew build
make test    # ./gradlew test
make clean   # ./gradlew clean
```

Server starts on:
- http://localhost:8081

Open the UI in a browser:
- http://localhost:8081/

### Important: Localhost and Redirect URLs

When running locally, be aware that `localhost` cannot be used as the redirect URI.

For local development testing, use a reverse proxy solution is required.

## Security Notes (important)

This demo is designed so it can be published safely:
- No real QTSP credentials are committed
- mTLS key material in the repository is demo-only 
- Tokens are redacted in responses 
- Session state is stored only in-memory (not production-safe)

Do not use this code as production signing software.

Use it as a reference integration blueprint.
