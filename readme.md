# QRDA Report Generator

## Usage
### Build docker image
Run the below command from project directory. Make sure docker running in local
```
mvn spring-boot:build-image
```

### Run application in local
Run the below command from project directory. Make sure Java 17 JDK installed.
```
mvn spring-boot:run
```

### API Usage
Below endpoints are available to use
1. POST {host:port}/qrda1
2. POST {host:port}/qrda3

- Use Basic Authentication with admin/admin. This is only for initial implementation.
- These endpoints accept JSON request as input. Use the JSON with Quality Measures outcomes.
- The API generates corresponding QRDA file in XML format

## Notes
1. [UnitTest.java](src/test/java/com/digitalhie/QRDAReportGenerator/UnitTest.java) 
    - This file is used for local debugging to find the relative fields mapping from template.
    - Do not delete this file
