# Janus

Janus is a Jakarta EE-based application for managing company time records. It facilitates the tracking of employees' working hours, helping to comply with labor regulations and optimize time management.

## Features

- **Time Logging**: Allows employees to easily record their clock-in and clock-out times.
- **User Management**: Administer profiles with different roles and permissions.
- **Custom Reports**: Generate detailed reports on worked hours, overtime, and absences.
- **Integration**: Compatible with other systems via RESTful APIs.

## Prerequisites

- **Java 21** or higher.
- An application server compatible with Jakarta EE (WildFly, GlassFish, etc.).
- A relational database (MySQL, PostgreSQL, etc.).

## Installation

1. **Clone the repository**:

   ```bash
   git clone https://github.com/nivel36/janus.git
   ```

3. **Build the project**:

   ```bash
   mvn clean install
   ```

4. **Deploy to the application server**:

   - Copy the generated `janus.war` file to your server's deployment directory.

## Usage

Access the application via a web browser:

```
http://localhost:8080/janus
```

## License

This project is licensed under the [Apache License 2.0](LICENSE).
