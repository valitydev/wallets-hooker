ALTER TABLE whook.webhook ALTER identity_id DROP NOT NULL;
ALTER TABLE whook.webhook DROP COLUMN identity_id;
ALTER TABLE whook.webhook ADD COLUMN party_id character varying;

CREATE TABLE IF NOT EXISTS whook.party_key
(
    id bigserial NOT NULL,
    party_id character varying(40) NOT NULL,
    pub_key character VARYING NOT NULL,
    priv_key character VARYING NOT NULL,
    CONSTRAINT pk_party_key PRIMARY KEY (id),
    CONSTRAINT key_party_id_key UNIQUE (party_id)
);

CREATE TABLE whook.withdrawal_reference
(
    withdrawal_id character varying(40) NOT NULL,
    party_id character varying(40) NOT NULL,
    wallet_id character varying(40) NOT NULL,
    event_id character varying(40) NOT NULL,
    external_id character varying,
    CONSTRAINT withdrawal_reference_pkey PRIMARY KEY (withdrawal_id)
);
