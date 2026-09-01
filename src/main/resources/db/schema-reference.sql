-- Reference schema for the assignment. Hibernate creates/updates tables by default in development.
CREATE TABLE product (
  id BIGSERIAL PRIMARY KEY,
  product_name VARCHAR(255) NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  created_on TIMESTAMP NOT NULL,
  modified_by VARCHAR(100),
  modified_on TIMESTAMP
);

CREATE TABLE item (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES product(id),
  quantity INTEGER NOT NULL
);

CREATE INDEX idx_product_created_on ON product(created_on);
CREATE INDEX idx_product_created_by ON product(created_by);
CREATE INDEX idx_item_product_id ON item(product_id);
