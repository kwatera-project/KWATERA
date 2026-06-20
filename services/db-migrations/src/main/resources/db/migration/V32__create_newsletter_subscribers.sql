CREATE TABLE newsletter_subscribers (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_newsletter_subscribers_email ON newsletter_subscribers (email);
CREATE INDEX idx_newsletter_subscribers_token ON newsletter_subscribers (token);
