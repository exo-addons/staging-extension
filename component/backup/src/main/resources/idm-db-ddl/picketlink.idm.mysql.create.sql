create table jbid_attr_bin_value (BIN_VALUE_ID bigint not null auto_increment, VALUE longblob, primary key (BIN_VALUE_ID));

create table jbid_creden_bin_value (
    BIN_VALUE_ID bigint not null auto_increment,
    VALUE longblob,
    primary key (BIN_VALUE_ID)
);

create table jbid_io (
    ID bigint not null auto_increment,
    IDENTITY_TYPE bigint not null,
    NAME varchar(255) not null,
    REALM bigint not null,
    primary key (ID),
    unique (IDENTITY_TYPE, NAME, REALM)
);

create table jbid_io_attr (
    ATTRIBUTE_ID bigint not null auto_increment,
    IDENTITY_OBJECT_ID bigint not null,
    NAME varchar(255),
    ATTRIBUTE_TYPE varchar(255),
    BIN_VALUE_ID bigint,
    primary key (ATTRIBUTE_ID),
    unique (IDENTITY_OBJECT_ID, NAME)
);

create table jbid_io_attr_text_values (
    TEXT_ATTR_VALUE_ID bigint not null,
    ATTR_VALUE varchar(255)
);

create table jbid_io_creden (
    ID bigint not null auto_increment,
    BIN_VALUE_ID bigint,
    IDENTITY_OBJECT_ID bigint not null,
    TEXT varchar(255),
    CREDENTIAL_TYPE bigint not null,
    primary key (ID),
    unique (IDENTITY_OBJECT_ID, CREDENTIAL_TYPE)
);

create table jbid_io_creden_props (
    PROP_ID bigint not null,
    PROP_VALUE varchar(255) not null,
    PROP_NAME varchar(255) not null,
    primary key (PROP_ID, PROP_NAME)
);

create table jbid_io_creden_type (
    ID bigint not null auto_increment,
    NAME varchar(255) unique,
    primary key (ID)
);

create table jbid_io_props (
    PROP_ID bigint not null,
    PROP_VALUE varchar(255) not null,
    PROP_NAME varchar(255) not null,
    primary key (PROP_ID, PROP_NAME)
);

create table jbid_io_rel (
    ID bigint not null auto_increment,
    FROM_IDENTITY bigint not null,
    NAME bigint,
    TO_IDENTITY bigint not null,
    REL_TYPE bigint not null,
    primary key (ID),
    unique (FROM_IDENTITY, NAME, TO_IDENTITY, REL_TYPE)
);

create table jbid_io_rel_name (
    ID bigint not null auto_increment,
    NAME varchar(255) not null,
    REALM bigint not null,
    primary key (ID),
    unique (NAME, REALM)
);

create table jbid_io_rel_name_props (
    PROP_ID bigint not null,
    PROP_VALUE varchar(255) not null,
    PROP_NAME varchar(255) not null,
    primary key (PROP_ID, PROP_NAME)
);

create table jbid_io_rel_props (
    PROP_ID bigint not null,
    PROP_VALUE varchar(255) not null,
    PROP_NAME varchar(255) not null,
    primary key (PROP_ID, PROP_NAME)
);

create table jbid_io_rel_type (
    ID bigint not null auto_increment,
    NAME varchar(255) not null unique,
    primary key (ID)
);

create table jbid_io_type (
    ID bigint not null auto_increment,
    NAME varchar(255) not null unique,
    primary key (ID)
);

create table jbid_real_props (
    PROP_ID bigint not null,
    PROP_VALUE varchar(255) not null,
    PROP_NAME varchar(255) not null,
    primary key (PROP_ID, PROP_NAME)
);

create table jbid_realm (
    ID bigint not null auto_increment,
    NAME varchar(255) not null,
    primary key (ID),
    unique (NAME)
);
