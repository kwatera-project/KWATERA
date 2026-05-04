# Ruff configuration for OCR service

This document explains the Ruff configuration used by `services/ocr-service/pyproject.toml`.

## Purpose

Ruff is used as the Python linter and formatter for the OCR service. It provides a fast quality gate for style, import order, docstrings and type annotation hygiene.

## Configured scope

```toml
[tool.ruff]
target-version = "py312"
line-length = 100
src = ["app", "tests"]
```

## Enabled lint rule groups

```toml
[tool.ruff.lint]
select = ["E", "F", "I", "D", "ANN"]
```

Enabled groups:

- `E` - pycodestyle errors, for example invalid whitespace, indentation and basic style violations.
- `F` - Pyflakes rules, for example unused imports, undefined names and invalid variable usage.
- `I` - isort-compatible import ordering rules.
- `D` - pydocstyle docstring rules.
- `ANN` - flake8-annotations rules, requiring type annotations for functions and methods.

## Disabled rules

```toml
ignore = ["D203", "D213"]
```

Disabled rules:

- `D203` - requires one blank line before a class docstring.
- `D213` - requires a multi-line docstring summary to start on the second line.

These two rules are disabled because they conflict with the Google-style docstring convention used in the project. Ruff commonly recommends ignoring `D203` and `D213` when using Google-style docstrings.

## Docstring convention

```toml
[tool.ruff.lint.pydocstyle]
convention = "google"
```

The OCR service uses Google-style docstrings. This keeps the style readable and consistent for service-level documentation.

## Import sorting

```toml
[tool.ruff.lint.isort]
known-first-party = ["app", "tests"]
```

This tells Ruff that imports from `app` and `tests` are first-party imports. It prevents them from being grouped incorrectly with third-party dependencies.

## Per-file ignores

```toml
[tool.ruff.lint.per-file-ignores]
"tests/**/*.py" = ["D", "ANN"]
"**/__init__.py" = ["D104"]
```

Configured exceptions:

- Test files do not require docstrings or full annotation strictness.
- `__init__.py` files do not require module docstrings.

## Local commands

Run these commands from `services/ocr-service`:

```bash
ruff check .
ruff format --check .
```

To automatically format files:

```bash
ruff format .
```