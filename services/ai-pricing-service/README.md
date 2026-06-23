[← Back to the project README](../../README.md)

# AI Pricing Service

The AI Pricing Service suggests the price per night for accommodation units. It combines property and unit data with date-derived features and runs inference through a CatBoost model.

## Main responsibilities

- Load the trained CatBoost prediction model.
- Retrieve property and unit inputs from the Property Service.
- Return price suggestions through the API Gateway.

## Default port

`8087`

## Useful local URLs

- Health: [http://localhost:8087/actuator/health](http://localhost:8087/actuator/health)
- Swagger UI: [http://localhost:8087/swagger-ui.html](http://localhost:8087/swagger-ui.html)
- Gateway path: `http://localhost:8090/api/predict`

## Configuration notes

Download the [CatBoost prediction model release](https://github.com/kwatera-project/KWATERA/releases/tag/v1.0-catboost-prediction-model) and place it at:

```text
services/ai-pricing-service/src/main/resources/catboost_model_v1.cbm
```

The Docker image copies it to `/app/catboost_model_v1.cbm`, the path loaded by `CatBoostConfig`. The service also requires PostgreSQL credentials and access to the Property Service.

## Local verification

See the [root quality-check instructions](../../README.md#local-quality-checks).
