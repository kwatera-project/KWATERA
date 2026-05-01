# Contributing to KWATERA

## Issues

Use the available issue templates when creating a new task:

- **Feature / functional task**
  Use for new features, functional changes, business logic, API work, UI flows, validations, and user-facing behavior.

- **Research / analysis task**
  Use for analysis, comparisons, architecture discussions, requirements work, diagrams, and design decisions.

- **Chore / DevOps / process task**
  Use for repository setup, CI/CD, Docker, testing setup, documentation, and workflow improvements.

When creating an issue:
- choose the template that best matches the task type
- fill in all required fields
- keep the scope specific and actionable
- define a clear Definition of Done

## Pull requests

Each pull request must use the shared PR template and include at least:

- **Summary**
- **Linked issue**
- **Scope of changes**
- **Testing status**
- **Checklist**

Minimum PR completion rules:
- the PR must be linked to an issue
- the PR scope must match the linked issue
- unrelated changes must not be included
- testing status must be provided
- documentation must be updated if needed
- the PR must be ready for review before requesting review

## Linking pull requests with issues

Every pull request should be linked to its issue in the PR description.

Use a closing keyword only when the pull request fully completes the issue, for example:

`Closes #14`

If the pull request implements only part of the issue, use a non-closing reference instead, for example:

`Refs #14`

or

`Part of #14`

Preferred convention:
- one issue may be completed by one or more pull requests
- smaller focused PRs are preferred when an issue covers multiple independent changes
- use `Closes #...` only in the PR that completes the issue
- use `Refs #...` or `Part of #...` for partial PRs
- unrelated changes must not be included in a PR, even if they are small

## CODEOWNERS maintenance

When a PR introduces a new service, module, directory, or clearly separated feature area, update `.github/CODEOWNERS` in the same PR.

## Recommended workflow

1. Create an issue using the correct issue template.
2. Define context, scope, and Definition of Done.
3. Create a branch for the first focused part of the issue.
4. Implement only the changes required by that branch scope.
5. Check whether the change introduces a new service, module, directory, or clearly separated feature area.
6. Update `.github/CODEOWNERS` in the same PR if ownership changes are needed.
7. Run local quality checks with `.\scripts\quality\pre-PR-check.ps1`.
8. Open a pull request using the shared PR template.
9. Link the issue in the PR description:
   - use `Refs #...` or `Part of #...` for partial PRs
   - use `Closes #...` only when the PR completes the issue
10. Provide testing status and complete the checklist.
11. Request review.
12. Merge after review is complete.
13. Continue with another focused PR if the issue is not fully completed yet.


## Local quality checks before PR

Before opening a pull request, contributors should run the local quality check script:

```powershell
.\scripts\quality\pre-PR-check.ps1
```

The script currently runs the following checks for all Java services in the repository:

- `spotless:check`
- `clean verify`
- `spotbugs:check`

Purpose:

- catch formatting or build problems before opening a PR
- reduce avoidable CI failures
- keep pull requests focused on implementation instead of basic quality fixes

JaCoCo coverage reports are generated during `verify` and can be viewed locally in:

- `services/config-server/target/site/jacoco/index.html`
- `services/service-registry/target/site/jacoco/index.html`
- `services/reservation-service/target/site/jacoco/index.html`

These local checks are recommended before every PR, **but the final source of truth remains the GitHub Actions CI workflow**.