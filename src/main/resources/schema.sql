CREATE TABLE clients (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_number VARCHAR(255) NOT NULL UNIQUE,
    currency VARCHAR NOT NULL CHECK (currency IN ('RUB', 'USD', 'EUR')),
    status VARCHAR NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    balance NUMERIC (19, 2) NOT NULL DEFAULT 0 CHECK ( balance >= 0 ),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    client_id BIGINT NOT NULL ,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE transfers (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amount NUMERIC (19, 2) NOT NULL CHECK ( amount > 0 ),
    status VARCHAR NOT NULL CHECK ( status IN ('CREATED', 'SUCCESS', 'FAILED')),
    comment VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    from_account_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    CONSTRAINT chk_different_accounts
        CHECK (from_account_id <> to_account_id),
    FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    FOREIGN KEY (to_account_id) REFERENCES accounts(id)
);