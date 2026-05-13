# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Easy Deploy is an IntelliJ Platform Plugin for deploying and upgrading services to remote servers via SSH/SFTP. It is inspired by Alibaba Cloud Toolkit but focuses only on server management and deployment.

## Build & Development Commands

- `./gradlew buildPlugin` — Build the plugin ZIP for deployment.
- `./gradlew runIde` — Launch a sandboxed IntelliJ IDEA instance with the plugin loaded.
- `./gradlew test` — Run the test suite (JUnit 5 via JUnit Platform).
- `./gradlew clean` — Delete the build directory.

To run a single test class: `./gradlew test --tests "tech.lin2j.idea.plugin.file.FilterTest"`

## Tech Stack

- **Build system:** Gradle with `org.jetbrains.intellij` plugin v1.17.4
- **Languages:** Java 17, Kotlin 2.0.20 (JVM target 17)
- **Target IDE:** IntelliJ IDEA 2024.1+ (build 241.14494.240+)
- **Key dependencies:**
  - `sshj` (0.39.0) — SSH/SFTP/SCP client library
  - `org.jetbrains.plugins.terminal` — IntelliJ terminal integration
  - `com.intellij.java` — Java plugin dependency

## Architecture

### Plugin Entry Point

`src/main/resources/META-INF/plugin.xml` declares all extensions, services, actions, and tool windows. Key registrations:
- `DeployConsoleToolWindowFactory` — Bottom tool window "Easy Deploy"
- `ConfigPersistence` — Application-level persistent state
- `SshjSshService` / `HotReloadServiceImpl` — Application services
- `DeployRunConfigurationType` — Run/Debug configuration for deployment

### Configuration & Persistence

- **`ConfigPersistence`** (`model/ConfigPersistence.java`) — A `PersistentStateComponent` that stores all plugin data in `deploy-helper-settings.xml`. Holds lists of `SshServer`, `Command`, `UploadProfile`, server tags, and `PluginSetting`.
- **`ConfigHelper`** (`model/ConfigHelper.java`) — Static facade that lazy-loads `ConfigPersistence` into memory and maintains cached maps (`SSH_SERVER_MAP`, `COMMAND_MAP`, `UPLOAD_PROFILE_MAP`) for fast lookups. All config reads/writes go through here. Call `ConfigHelper.refreshConfig()` after bulk changes.
- **Passwords** are stored via IntelliJ's `PasswordSafe`, not in the XML file. `SshServer.getPassword()` and `getPassPhrase()` use `@Transient` and load from `PasswordSafe` at runtime.

### Domain Models

- **`SshServer`** — SSH host config (IP, port, user, auth type, private key, proxy/jump host, success exit code). Supports password and private-key auth (`AuthType`).
- **`Command`** — Shell command tied to a server (title, working dir, content). Commands can be `sharable` across servers.
- **`UploadProfile`** — Upload configuration: local file/dir, remote destination, exclude patterns, optional pre/post commands, and a flag to use the upload path as the command execution directory.

### SSH Layer

- **`ISshService`** / **`SshjSshService`** — Service interface for SSH operations (connect, execute command, upload, download). The implementation uses `SshjConnection`.
- **`SshConnectionManager`** — Builds `SSHClient` chains from `SshServer`, handling jump host proxies. Validates against circular proxy dependencies.
- **`SshjConnection`** — Wraps an `sshj` `SSHClient`/`SFTPClient` and implements `SshConnection`. Supports both SFTP and SCP transfer modes (configurable in plugin settings).
- **`SshStatus`** — Result wrapper with success flag and message. `SshServer.isCommandSuccess()` allows per-server override of the "success" exit code (default 0).

### Deployment Runner

- **`DeployRunConfiguration`** — Run/Debug configuration that holds a list of `DeployProfile` strings (encoding server + upload profile) and a `parallelExec` flag.
- **`DeployRunProfileState`** — Sequential deployment.
- **`ParallelDeployRunProfileState`** — Parallel deployment.

### UI Architecture

- **`DashboardView`** — Main tool window panel showing a table of `SshServer` entries with tag filtering and search. Action cells contain buttons for Upload, Command, Terminal, and More (SFTP, Properties, Remove).
- **Dialogs** — All UI dialogs live under `ui/dialog/`. They extend IntelliJ's dialog classes (e.g., `HostSettingsDialog`, `UploadProfileDialog`, `AddCommandDialog`).
- **SFTP Panel** — `SFTPEditor`/`SFTPFileSystem` provide a file browser/editor for remote files. File actions are in `action/ftp/`.
- **Event System** — Custom pub/sub via `ApplicationContext`/`ApplicationPublisher`. Events like `TableRefreshEvent`, `CommandAddEvent`, `UploadProfileSelectedEvent` decouple UI components.

### i18n

- **`MessagesBundle`** — Loads strings from `messages_en.properties` or `messages_zh.properties` based on `PluginSetting.i18nType`.
- Add new keys to both `.properties` files.

### Icons

- **`MyIcons`** (`icons/MyIcons.java`) — Central icon registry. Icons are SVGs under `src/main/resources/icons/`.

## Important Build Notes

The `build.gradle` disables several IntelliJ plugin tasks due to compatibility issues with IJ 2024.1 and plugin v1.17.4:
- `initializeIntelliJPlugin` — disabled (GitHub API unreachable)
- `buildSearchableOptions` — disabled
- `instrumentCode` / `instrumentTestCode` — disabled

Do not re-enable these without verifying compatibility.

## Testing

Tests are in `src/test/java/`. The only test currently is `FilterTest`. Run via `./gradlew test`.
