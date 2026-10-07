CREATE TABLE users
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    first_name    VARCHAR(255),
    last_name     VARCHAR(255),
    sex           VARCHAR(50),
    avatar        TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_username UNIQUE (username)
) ENGINE = InnoDB;