# Skills Guide for Mifos Self-Service Plugin

## Quick Reference (Essential Patterns)

- Controller pattern: `@Path` + `@Component` + `@RequiredArgsConstructor` (see [JAX-RS Endpoints](#jax-rs-endpoints))
- Service pattern: `@Service` + constructor injection + transactional write methods (see [Transaction Management](#transaction-management))
- Validation pattern: validator component + required field checks + typed exceptions (see [Input Validation](#input-validation))
- Security pattern: `@PreAuthorize` + self-service access validation via security context (see [Security Implementation](#security-implementation))
- Testing pattern: Mockito-based unit tests and Testcontainers integration tests (see [Testing Patterns](#testing-patterns))

Use this checklist first, then follow the detailed sections below for full examples and conventions.

This document defines procedural rules and patterns for AI agents working with the Mifos Self Service Plugin codebase.

## Java Coding Standards

### File Headers
All Java files must include the standard MPL-2.0 license header:
```java
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.example;
```

### Package Structure
Follow the established package structure:
```text
org.apache.fineract.selfservice.*
  - security/           # Authentication, authorization, security context
  - useradministration/ # User management and roles
  - client/            # Client operations and data
  - savings/           # Savings account operations
  - loanaccount/       # Loan account operations
  - products/          # Product browsing and discovery
  - registration/      # User registration workflows
  - config/            # Configuration and beans
  - runreport/         # Reporting functionality
  - spm/               # Survey participation management
  - shareaccounts/     # Share account operations
```

### Class Naming Conventions
- **API Resources**: `SelfXxxApiResource` (e.g., `SelfSavingsApiResource`)
- **Services**: `XxxService` / `XxxServiceImpl` (e.g., `SelfSavingsService`)
- **Data Validators**: `XxxDataValidator` (e.g., `SelfSavingsDataValidator`)
- **Repositories**: `XxxRepository` (e.g., `SelfSavingsRepository`)
- **Domain Objects**: `Xxx` (e.g., `SelfSavingsAccount`)

### Dependency Injection
Use Lombok's `@RequiredArgsConstructor` for constructor injection:
```java
@Service
@RequiredArgsConstructor
public class SelfSavingsServiceImpl implements SelfSavingsService {
    private final SelfSavingsRepository repository;
    private final SelfSavingsDataValidator validator;
}
```

### API Design Patterns

#### JAX-RS Endpoints
```java
@Path("/v1/self/savingsaccounts")
@Component
@Tag(name = "Self Savings Account", description = "Self-service savings account operations")
@RequiredArgsConstructor
public class SelfSavingsApiResource {
    
    @GET
    @Path("/{accountId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSavingsAccount(@PathParam("accountId") Long accountId) {
        // Implementation
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSavingsAccount(String jsonRequestBody) {
        // Implementation
    }
}
```

#### Response Patterns
Always use proper HTTP status codes and Response objects:
```java
return Response.ok().entity(responseData).build();
return Response.status(Response.Status.CREATED).entity(createdData).build();
return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
```

### Security Implementation

#### Method-Level Security
```java
@PreAuthorize("hasAuthority('READ_SAVINGSACCOUNT')")
public SavingsAccountData getSavingsAccount(Long accountId) {
    // Implementation
}
```

#### Security Context
Inject and use the security context properly:
```java
@RequiredArgsConstructor
public class SelfSavingsService {
    private final PlatformSelfServiceSecurityContext securityContext;
    
    public void validateAccess(Long accountId) {
        securityContext.validateSelfServiceUserAccess(accountId);
    }
}
```

### Data Validation Patterns

#### Input Validation
```java
@Component
public class SelfSavingsDataValidator {
    private final FromJsonHelper fromJsonHelper;
    
    public void validateCreate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new PlatformApiDataValidationException("validation.msg.empty.json");
        }
        
        Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> data = fromJsonHelper.extractObject(json, typeOfMap);
        
        validateRequiredFields(data);
        validateFieldFormats(data);
    }
    
    private void validateRequiredFields(Map<String, Object> data) {
        List<String> missingFields = new ArrayList<>();
        
        if (!data.containsKey("clientId")) {
            missingFields.add("clientId");
        }
        
        if (!missingFields.isEmpty()) {
            throw new PlatformApiDataValidationException(
                "validation.msg.required.fields.missing",
                String.join(", ", missingFields)
            );
        }
    }
}
```

### Database Operations

#### Repository Pattern
```java
@Repository
public interface SelfSavingsRepository {
    SelfSavingsAccount findById(Long id);
    
    @Query("SELECT s FROM SelfSavingsAccount s WHERE s.clientId = :clientId")
    List<SelfSavingsAccount> findByClientId(@Param("clientId") Long clientId);
    
    SelfSavingsAccount save(SelfSavingsAccount account);
    
    void delete(SelfSavingsAccount account);
}
```

#### Transaction Management
```java
@Service
@Transactional
public class SelfSavingsServiceImpl implements SelfSavingsService {
    
    @Transactional
    public CommandProcessingResult createSavingsAccount(Command command) {
        validateCommand(command);
        SelfSavingsAccount account = createAccountFromCommand(command);
        SelfSavingsAccount savedAccount = repository.save(account);
        return new CommandProcessingResultBuilder().withEntityId(savedAccount.getId()).build();
    }
}
```

### Error Handling

#### Custom Exceptions
```java
public class SelfSavingsException extends RuntimeException {
    public SelfSavingsException(String message) {
        super(message);
    }
    
    public SelfSavingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### Global Exception Handling
Use Fineract's standard exception handling patterns:
```java
@ExceptionHandler(SelfSavingsException.class)
public Response handleSelfSavingsException(SelfSavingsException ex) {
    return Response.status(Response.Status.BAD_REQUEST)
                   .entity(ApiGlobalExceptionHandler.createErrorResponse(ex.getMessage()))
                   .build();
}
```

### Testing Patterns

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class SelfSavingsServiceImplTest {
    
    @Mock
    private SelfSavingsRepository repository;
    
    @Mock
    private SelfSavingsDataValidator validator;
    
    @InjectMocks
    private SelfSavingsServiceImpl service;
    
    @Test
    void shouldCreateSavingsAccount() {
        // Given
        Command command = createValidCommand();
        SelfSavingsAccount expectedAccount = createExpectedAccount();
        when(repository.save(any())).thenReturn(expectedAccount);
        
        // When
        CommandProcessingResult result = service.createSavingsAccount(command);
        
        // Then
        assertThat(result.getEntityId()).isEqualTo(expectedAccount.getId());
        verify(validator).validateCreate(any());
        verify(repository).save(any());
    }
}
```

#### Integration Tests
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SelfSavingsApiResourceIntegrationTest extends SelfServiceIntegrationTestBase {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("fineract_test")
            .withUsername("mifos")
            .withPassword("password");
    
    @Test
    @DisplayName("GET /v1/self/savingsaccounts/{id} should return account details")
    void shouldReturnSavingsAccountDetails() {
        // Test implementation
    }
}
```

## Configuration Guidelines

### Application Properties
```properties
# Self Service Plugin Configuration
fineract.selfservice.enabled=true
fineract.selfservice.base-path=/v1/self
fineract.security.basicauth.enabled=true
fineract.security.oauth.enabled=false
```

### Bean Configuration
```java
@Configuration
@EnableWebSecurity
public class SelfServiceSecurityConfig {
    
    @Bean
    public SecurityFilterChain selfServiceFilterChain(HttpSecurity http) throws Exception {
        // Security configuration
    }
}
```

## Build and Deployment

### Maven Configuration
Ensure proper plugin configuration in `pom.xml`:
```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.17.0</version>
                <style>GOOGLE</style>
            </googleJavaFormat>
        </java>
    </configuration>
</plugin>
```

### Plugin Deployment
This is a library/plugin that extends Apache Fineract. It does not run as a standalone Docker container.

#### Deployment Options:
```bash
# As dependency in Apache Fineract (Docker)
The JAR is loaded via -Dloader.path parameter when running Fineract

# As dependency in Apache Fineract (Tomcat)
Copy JAR to $TOMCAT_HOME/webapps/fineract-provider/WEB-INF/lib/
```

## Best Practices

1. **Always validate input data** before processing
2. **Use proper security annotations** on all service methods
3. **Follow RESTful conventions** for API design
4. **Write comprehensive tests** for all functionality
5. **Handle exceptions gracefully** with meaningful error messages
6. **Use proper logging** for debugging and monitoring
7. **Follow Fineract coding standards** for consistency
8. **Document all public APIs** with proper Javadoc

## Common Pitfalls to Avoid

1. **Don't expose sensitive data** in API responses
2. **Don't skip security validation** for any endpoint
3. **Don't use hardcoded values** - use configuration properties
4. **Don't ignore transaction boundaries** in service methods
5. **Don't forget to validate user permissions** before operations
6. **Don't use deprecated APIs** - prefer current JAX-RS annotations
7. **Don't mix concerns** - keep services focused on single responsibilities
