CREATE TABLE purchases (
    id             SERIAL PRIMARY KEY,
    category       VARCHAR(120),
    amount         MONEY,
    credit_card    VARCHAR(20),
    timestamp      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO purchases (category, amount, credit_card) values ('Dining', 72.20, '1111-2222-3333-4444');
INSERT INTO purchases (category, amount, credit_card) values ('Lodging', 744.90, '1111-2222-3333-4444');
INSERT INTO purchases (category, amount, credit_card) values ('Merchandise', 203.12, '1111-2222-3333-4444');
