# Open Source Usage and License Compliance Guide

## Overview

CORDAL (Configuration Orchestrated REST Dynamic API Layer) is an open source project licensed under the **Apache License 2.0**. This document outlines the open source components used, license requirements, and compliance guidelines.

CORDAL is a revolutionary framework that enables developers to create powerful REST APIs using only YAML configuration files - no Java coding required!

## Project License

**License:** Apache License 2.0
**Copyright:** 2025 Mark Andrew Ray-Smith Cityline Ltd
**License File:** [LICENSE](LICENSE)
**Attribution File:** [NOTICE](NOTICE)

### Apache License 2.0 Summary

**Permissions:**
- Commercial use
- Modification
- Distribution
- Patent use
- Private use

**Conditions:**
- License and copyright notice
- State changes
- Include NOTICE file

**Limitations:**
- Trademark use
- Liability
- Warranty

## Required License Headers

All Java source files must include the following license header:

```java
/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Third-Party Dependencies

### Runtime Dependencies

#### Core Framework
- **Java 21 Runtime Environment** - Oracle No-Fee Terms and Conditions (NFTC)
- **Javalin Web Framework** (6.1.3) - Apache License 2.0
- **Google Guice Dependency Injection** (7.0.0) - Apache License 2.0

#### Database & Connection Management
- **H2 Database Engine** (2.2.224) - Mozilla Public License 2.0 / EPL 1.0
- **PostgreSQL JDBC Driver** (42.6.0) - BSD 2-Clause License
- **HikariCP Connection Pool** (5.0.1) - Apache License 2.0

#### JSON Processing & Configuration
- **Jackson Databind** (2.15.2) - Apache License 2.0
- **Jackson Core** (2.15.2) - Apache License 2.0
- **Jackson Annotations** (2.15.2) - Apache License 2.0
- **SnakeYAML** (2.2) - Apache License 2.0

#### Logging
- **SLF4J API** (2.0.9) - MIT License
- **Logback Classic** (1.4.11) - EPL 1.0 / LGPL 2.1
- **Logback Core** (1.4.11) - EPL 1.0 / LGPL 2.1

#### Utilities
- **Apache Commons Lang** (3.13.0) - Apache License 2.0

### Test Dependencies

#### Testing Frameworks
- **JUnit Jupiter** (5.10.1) - Eclipse Public License 2.0
- **JUnit Platform Suite** (1.10.1) - Eclipse Public License 2.0
- **AssertJ Core** (3.24.2) - Apache License 2.0
- **Mockito Core** (5.7.0) - MIT License

#### Integration Testing
- **TestContainers** (1.18.3) - MIT License
- **TestContainers JUnit Jupiter** (1.18.3) - MIT License
- **TestContainers PostgreSQL** (1.18.3) - MIT License

#### Build Tools
- **Maven** (3.9.5) - Apache License 2.0
- **Maven Compiler Plugin** (3.11.0) - Apache License 2.0
- **Maven Surefire Plugin** (3.2.2) - Apache License 2.0

## License Compatibility Matrix

| License | Compatible with Apache 2.0 | Notes |
|---------|----------------------------|-------|
| Apache 2.0 | Yes | Same license |
| MIT | Yes | Permissive, compatible |
| BSD 2-Clause | Yes | Permissive, compatible |
| EPL 1.0/2.0 | Yes | Compatible with Apache 2.0 |
| MPL 2.0 | Yes | Compatible with Apache 2.0 |
| LGPL 2.1 | Conditional | Dynamic linking only |
| Oracle NFTC | Yes | No-fee terms compatible |

## Compliance Requirements

### For Distribution

1. **Include License File:** Copy of Apache License 2.0
2. **Include NOTICE File:** Attribution notices for all dependencies
3. **Preserve Copyright Notices:** Keep all existing copyright headers
4. **Document Changes:** If you modify the code, document the changes

### For Commercial Use

**Allowed:**
- Use in commercial products
- Sell products containing CORDAL
- Modify for commercial purposes
- Create proprietary derivatives

**Required:**
- Include license and copyright notices
- Include NOTICE file in distributions
- Don't use "CORDAL" trademark without permission

### For Modification

**Allowed:**
- Modify source code
- Create derivative works
- Distribute modifications

**Required:**
- Mark modified files with change notices
- Include original license headers
- Include NOTICE file

## Attribution Requirements

When using CORDAL in your project, include:

### In Documentation
```
This product includes CORDAL - Configuration Orchestrated REST Dynamic API Layer
(https://github.com/your-repo/cordal-api-mesh)
Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
Licensed under the Apache License 2.0
```

### In Software
- Include the NOTICE file in your distribution
- Preserve all copyright headers in source code
- Include Apache License 2.0 text

## Automated Compliance

### Header Management Script

Use the provided script to ensure all files have proper headers:

```powershell
# Check current status
.\update-java-headers.ps1 -DryRun

# Update headers with license information
.\update-java-headers.ps1
```

### Maven License Plugin

Consider adding the Maven License Plugin to your build:

```xml
<plugin>
    <groupId>com.mycila</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <version>4.2</version>
    <configuration>
        <header>LICENSE-HEADER.txt</header>
        <includes>
            <include>**/*.java</include>
        </includes>
    </configuration>
</plugin>
```

## Frequently Asked Questions

### Q: Can I use CORDAL in my commercial product?
**A:** Yes, the Apache License 2.0 explicitly allows commercial use.

### Q: Do I need to open source my modifications?
**A:** No, Apache License 2.0 does not require derivative works to be open source.

### Q: Can I remove the license headers?
**A:** No, you must preserve all copyright and license notices.

### Q: Do I need to contribute back my changes?
**A:** No, but contributions are welcome and appreciated.

### Q: Can I use the "CORDAL" name for my product?
**A:** The license doesn't grant trademark rights. Contact the copyright holder for trademark usage.

### Q: Can I build commercial APIs using CORDAL?
**A:** Yes, CORDAL is designed for commercial use. You can build and sell APIs created with CORDAL.

## Implementation Status

**Complete Implementation:**
- Apache License 2.0 headers added to all Java files across all modules
- LICENSE file created with full Apache License 2.0 text
- NOTICE file created with third-party attribution for CORDAL dependencies
- POM.xml files updated with license metadata across all modules
- Type-safe DTOs implemented eliminating unsafe casts
- Comprehensive documentation with step-by-step examples
- Production-ready monitoring and health checks
- Multi-module Maven project structure

## Contact

For license questions or trademark permissions:
- **Copyright Holder:** Mark Andrew Ray-Smith Cityline Ltd
- **Project Repository:** [Your Repository URL]
- **License Questions:** [Your Contact Email]

## Resources

- [Apache License 2.0 Full Text](https://www.apache.org/licenses/LICENSE-2.0)
- [Apache License FAQ](https://www.apache.org/foundation/license-faq.html)
- [Open Source Initiative](https://opensource.org/licenses/Apache-2.0)
- [SPDX License Identifier](https://spdx.org/licenses/Apache-2.0.html)

---

**Note:** This document provides general guidance. For specific legal questions, consult with a qualified attorney familiar with open source licensing.
