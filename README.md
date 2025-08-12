# Janus

Janus is a **Spring/Angular**-based application for managing company time records. It facilitates the tracking of employees' working hours, helping to comply with labor regulations and optimize time management.

## Features

- **Time Logging**: Allows employees to easily record their clock-in and clock-out times.
- **User Management**: Administer profiles with different roles and permissions.
- **Custom Reports**: Generate detailed reports on worked hours, overtime, and absences.
- **Integration**: Compatible with other systems via RESTful APIs.

## Prerequisites

- **Java 24** or higher.
- A PostgreSQL relational database.

## Installation

1. **Clone the repository**:

```bash
git clone https://github.com/nivel36/janus.git
```

3. **Build the project**:

```bash
mvn clean install
```

4. **Start the app**:

```bash
java -jar Janus.jar
```

## Usage

Access the application via a web browser:

```
http://localhost:8080/janus
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).
