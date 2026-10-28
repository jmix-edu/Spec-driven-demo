# Booking

This is a web application based on the [Jmix](https://www.jmix.io) framework.

## Getting Started

The newly created project requires Java 21 and uses the embedded HSQL database.

Use the following resources to learn more about Jmix:
* [Jmix Documentation](https://docs.jmix.io)
* [Online Demo Applications](https://www.jmix.io/live-demo)
* [Jmix AI Assistant](https://ai-assistant.jmix.io) (also available in the **Jmix AI** tool window of IntelliJ IDEA)

## Development

- [Setup](https://docs.jmix.io/jmix/setup.html) your development environment.
- [Open](https://docs.jmix.io/jmix/studio/project.html#opening-existing-project) the project in the IDE.
- If you want to use AI agents to develop the application, check out the [Jmix AI Agent Guidelines](https://github.com/jmix-framework/jmix-agent-guidelines) repository.

## Running

To start the application, use the **Booking Jmix Application** run configuration in your IDE, or run the following command in the project root directory:

```bash
./gradlew bootRun
```

The application will be available at <http://localhost:8080>.

The default user credentials are:
* Login: `admin`
* Password: `admin`

Without an active profile the application runs in the `dev` profile: embedded HSQLDB, demo data
(building, rooms, users `emma`, `oliver`, `maria` with password = username) and a prefilled login form.
The AI chat needs the `OPENROUTER_API_KEY` environment variable.

## Deployment

The `prod` profile takes everything environment-specific from environment variables
(`DB_URL`, `DB_USER`, `DB_PASSWORD`, `OPENROUTER_API_KEY`), uses PostgreSQL, logs less and creates
no demo data.

1. Build the production JAR on the host (Studio's `productionMode = false` in `build.gradle` stays):

   ```bash
   ./gradlew -Pvaadin.productionMode=true bootJar
   ```

2. Copy `.env.example` to `.env` (git-ignored) and fill in real values.
3. Start PostgreSQL and the application:

   ```bash
   docker compose up -d --build
   ```

4. Open <http://localhost:8080>, log in as `admin` / `admin` and **change the admin password at once** -
   every new database starts with this account.
5. As admin, re-apply the Dynamic Model definition (`specs/05_addons/02-catering-dynamic-model.yaml`):
   the model is stored in the database.

Stop with `docker compose down`; the data stays in the `booking-db-data` volume. Do not run
`docker compose config` - it prints the secrets.

The application is packaged as an executable JAR with an embedded server. A WAR is only needed to deploy
into an existing external servlet container; this project does not build one.
