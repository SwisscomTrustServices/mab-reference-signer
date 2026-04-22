# Demo Signer (MAB + ETSI Sign)

This project is a minimal end-to-end demo that shows how to integrate the Swisscom Trust Services Multiple Authentication Broker (MAB) APIs and the ETSI Sign API.

It is built as a reference implementation for:
- Using mTLS for all protected endpoints 
- Calling OIDC + PAR
- Calling OIDC + CIBA
- Exchanging the authorization code for an access token (SAD JWT)
- Polling CIBA token endpoint for an access token (SAD JWT)
- Calling the ETSI signDoc endpoint to sign a document digest 
- Providing a small browser UI for manual testing
- Preparing a PDF for signature with PDFBox
- Embedding the returned CMS signature back into the PDF to obtain a final, signed PDF without modifying the signed byte ranges
- Adding PAdES DSS/VRI structures to upgrade the signature to Baseline B-LT (LTV enabled)

## PAR Flow Sequence Diagram

![PAR Flow](docs/par-flow.png)

## CIBA Flow Sequence Diagram

![CIBA Flow](docs/ciba-flow.png)

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
- A configured redirect URL (either back to this UI or to an inspection endpoint for manual copy/paste)

## Configuration

This project is safe for public repositories and does not contain real secrets.
All values are injected via environment variables referenced from `src/main/resources/application.yaml`.

### application.yml (defaults)

The repository ships with safe defaults like:
- `CLIENT_ID=00000000-0000-0000-0000-000000000000`
- `MTLS_BASE_URL=https://example.invalid`

These are the exact variable names currently referenced by the app:

- `DISCOVERY_PATH`
- `CLIENT_ID`
- `CLIENT_SECRET`
- `REDIRECT_URI`
- `MTLS_BASE_URL`
- `MTLS_CLIENT_CERT`
- `MTLS_CLIENT_KEY`

They can be set for local development in application-dev.yaml.

### Using a local `.env` file

For local development, the app also loads a project-root `.env` file automatically at startup via Spring Boot config import. The `.env` file uses plain `KEY=value` entries, so you can keep local settings in one place instead of exporting them manually in every shell.

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
```

For `MTLS_CLIENT_CERT` and `MTLS_CLIENT_KEY`, use Spring resource syntax such as `file:/absolute/path/to/client-cert.pem` or `classpath:...`.

### Exported environment variables

If you prefer, you can still export the variables manually before starting the app.
If a variable is defined both in the `.env` file and as an exported OS environment variable, the OS environment variable takes precedence. This allows you to keep local defaults in `.env` while still being able to override them per shell or CI job.

```bash
export DISCOVERY_PATH="https://example.invalid/.well-known/openid-configuration"

export CLIENT_ID="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export CLIENT_SECRET="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
export REDIRECT_URI="https://webhook.site/<your-id>"

export MTLS_BASE_URL="https://example.invalid"
export MTLS_CLIENT_CERT="file:/absolute/path/to/client-cert.pem"
export MTLS_CLIENT_KEY="file:/absolute/path/to/client-key.pem"
```

For local development you can also point the redirect URI to any endpoint that lets you inspect the URL parameters and copy the authorization code.

## Run the Demo

The project can be built and run with Gradle. If you prefer shorter, more general commands, the project also supports Makefile.

### Gradle

```bash
./gradlew build
```

```bash
./gradlew test
```

```bash
./gradlew bootRun
```

### Makefile

```bash
make
```

This runs `./gradlew bootRun` via the project `Makefile`.

If you are using `.env`, no extra export step is required before running that command.

Additional shortcuts:

```bash
make build   # ./gradlew build
```
```bash
make test    # ./gradlew test
```
```bash
make clean   # ./gradlew clean
```

Server starts on:
- http://localhost:8081

Open the UI in a browser:
- http://localhost:8081/

### Important: Localhost and Redirect URLs

When running locally, be aware that `localhost` cannot be used as the redirect URI due to STS restrictions.
* **(Quick and easy)**
  * When the MAB answers with a redirect URI containing the access code, you can copy and paste the code manually from the browser tab for testing purposes (easiest for quick tests).
* **(More work but convenient)**
  * Use a local tunneling service such as [ngrok](https://ngrok.com/) or [localtunnel](https://github.com/localtunnel/localtunnel) to expose your local server to the internet. But mind security restrictions of such tunneling services in an enterprise context!

Example with ngrok:

```bash
ngrok http 8081
```

Set your OAuth2 redirect URI to the public ngrok URL (e.g., `https://randomid.ngrok.io/callback`).

## Security Notes

This demo is designed so it can be published safely:
- No real QTSP credentials are committed
- mTLS key material in the repository is demo-only
- Tokens are redacted in responses
- Session state is stored only in-memory (not production-safe)

Do not use this code as production signing software.

Use it as a reference integration blueprint.