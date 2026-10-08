# Joinara Project Context

## Project purpose
Joinara is a master's thesis web application.

Thesis topic:
"Застосування рекомендаційних алгоритмів для формування груп користувачів у соціальних мережах"

The application helps users:
- find people with similar interests
- discover events
- create events
- request participation
- receive recommendations
- form compatible groups for specific activities

This is not a dating application.

## Main academic focus
The core of the thesis is:
- recommendation algorithms
- user compatibility
- event recommendations
- automatic group formation

Social features exist mainly to support these scenarios.

## Architecture
Use a microservice architecture without unnecessary fragmentation.

Planned components:
- user-service
- event-service
- recommendation-service
- api-gateway
- frontend

Current work is focused on:
- user-service

Do not create additional microservices without a clear architectural reason.

## Backend stack
- Java 21
- Spring Boot
- Gradle
- REST API
- Spring Security
- JWT
- Flyway
- Docker
- PostgreSQL

## User Service responsibilities
The user-service owns:
- authentication
- registration
- user profile
- roles
- interests
- activity preferences
- languages
- city
- age
- later: availability

Initial registration fields:
- name
- email
- password
- age
- city

Gender / sex is not required.

## Recommendation concepts

### Event Match
Measures how well an event fits a specific user.

### Group Fit
Measures how well a user fits the current members of a group.

This value may change when group composition changes.

### Group Compatibility
Measures the overall compatibility of a complete group.

This is one of the main thesis concepts.

## Event participation
For normal events:
- users submit join requests
- organizer approves or rejects them
- the recommendation system may rank candidates
- already approved users must not be automatically replaced by later candidates
- full events may use a waiting list

## Automatic group formation
For "Find my group":
- users join a candidate pool for a specific activity
- matching happens after a defined recruitment period
- the recommendation algorithm forms compatible groups

## Development principles
- inspect existing code before modifying it
- explain significant architectural changes before implementing them
- prefer maintainable production-quality solutions
- avoid overengineering
- avoid unnecessary dependencies
- use DTOs instead of exposing JPA entities directly
- use Flyway for database schema changes
- validate API input
- add tests for important business logic
- preserve existing working behavior unless intentionally changing it

Some current code was inherited from another project and may contain obsolete functionality.
Review it before reusing it.