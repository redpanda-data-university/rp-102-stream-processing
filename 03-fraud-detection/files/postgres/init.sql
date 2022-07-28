CREATE TABLE purchases (
    id                          SERIAL PRIMARY KEY,
    category                    VARCHAR(120),
    amount                      MONEY,
    credit_card                 VARCHAR(20),
    distance_from_billing_zip   FLOAT,
    timestamp                   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO purchases (category, amount, credit_card, distance_from_billing_zip) values ('Dining', 72.20, '1111-2222-3333-4444', 43);

CREATE TABLE travel_flags (
    id             SERIAL PRIMARY KEY,
    enabled        BOOLEAN,
    credit_card    VARCHAR(20),
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO travel_flags (enabled, credit_card) values (true, '1111-2222-3333-4444');

