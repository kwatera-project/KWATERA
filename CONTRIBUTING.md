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

Use one of the standard closing keywords, for example:

`Closes #14`

Preferred convention:
- one PR should address one main issue
- if needed, additional related issues can be referenced in the description
- if the PR is not intended to close the issue yet, reference the issue without a closing keyword

## Recommended workflow

1. Create an issue using the correct issue template.
2. Define context, scope, and Definition of Done.
3. Create a branch for the issue.
4. Implement only the changes required by that issue.
5. Open a pull request using the shared PR template.
6. Link the issue in the PR description.
7. Provide testing status and complete the checklist.
8. Request review.
9. Merge after review is complete.