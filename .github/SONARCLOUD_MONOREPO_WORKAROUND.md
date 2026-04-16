# SonarCloud monorepo workaround

At the moment, SonarCloud analysis in CI is enabled only for services that already have a configured SonarCloud project:

- `config-server`
- `reservation-service`
- `service-registry`

For the remaining services, SonarCloud is temporarily skipped in the CI matrix, while the rest of the quality pipeline still runs normally:

- Spotless
- `clean verify`
- SpotBugs

## Reason:
the SonarCloud monorepo configuration UI currently fails for our repository during setup of additional services. The page keeps bouncing between the project creation URLs and eventually ends on a blank screen, which blocks creating new SonarCloud projects for the remaining modules.

This is a temporary workaround. Once SonarCloud monorepo setup works again and missing projects are added, `sonar_enabled` should be switched to `true` in `.github/workflows/ci.yml` for the remaining services.