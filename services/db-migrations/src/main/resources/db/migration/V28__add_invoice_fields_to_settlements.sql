ALTER TABLE settlements ADD COLUMN invoice_requested BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE settlements ADD COLUMN invoice_pdf_path VARCHAR(255);
ALTER TABLE settlements ADD COLUMN company_name VARCHAR(255);
ALTER TABLE settlements ADD COLUMN tax_id VARCHAR(50);
ALTER TABLE settlements ADD COLUMN company_address VARCHAR(255);
