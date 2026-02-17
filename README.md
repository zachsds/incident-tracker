# SDS Weather Incident Tracker

Incident Tracker is an internal analytics tool developed by SDS Weather to track component failures and incidents across physical weather stations.

---

## Overview

The application provides a centralized interface for logging, tracking, and reviewing hardware-related incidents occurring across deployed weather monitoring systems.

It is a JavaFX desktop application that communicates with a PostgreSQL-backed REST API hosted on a Rock Pi server.

---

## Architecture

- Frontend: Java 21 + JavaFX 21
- Build System: Maven
- Backend: REST API (PostgreSQL)
- Server Host: 192.168.0.237
- Server Port: 3000
- Transport Security: Self-signed SSL certificate
- Email Integration: Jakarta Mail (com.sun.mail)

The application initializes through `com.sdsweather.App`, which bootstraps the JavaFX runtime and displays the login page on startup.

---

## Project Structure

- `com.sdsweather.App` – JavaFX entry point
- `com.sdsweather.navigation` – View navigation management
- `com.sdsweather.ui` – UI pages and components
- Maven-managed dependencies and plugins for build and packaging

---

## Requirements

- JDK 21
- Maven 3.9+
- Network access to 192.168.0.237:3000
- Trust configured for the bundled self-signed SSL certificate

---

## Dependencies

- javafx-controls 21.0.4
- sqlite-jdbc 3.46.1.0 (retained for compatibility)
- jakarta.mail 2.0.1

---

## Build Instructions

To compile the project:

- mvn clean compile

To run during development:

- mvn javafx:run

To package the application:

- mvn clean package

This will:

- Create the executable JAR in the `target/` directory
- Copy runtime dependencies into `target/libs/`

---

## Running the Packaged Application

After packaging:

- Navigate to the `target/` directory
- Ensure the `libs/` folder exists alongside the JAR
- Run:

- java -jar incident-tracker-1.0-SNAPSHOT.jar

---

## Security Notes

- The application communicates with an internal REST API secured with a self-signed SSL certificate.
- The certificate must be trusted on the client machine.
- This tool is intended for internal SDS Weather use only.

---

## Version

- Version: 1.0
- Author: Zachary Sneed
- Since: 2026-02-16
