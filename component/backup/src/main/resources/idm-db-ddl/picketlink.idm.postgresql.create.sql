
    create table jbid_attr_bin_value (
        BIN_VALUE_ID int8 not null,
        VALUE oid,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_creden_bin_value (
        BIN_VALUE_ID int8 not null,
        VALUE oid,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_io (
        ID int8 not null,
        IDENTITY_TYPE int8 not null,
        NAME varchar(255) not null,
        REALM int8 not null,
        primary key (ID),
        unique (IDENTITY_TYPE, NAME, REALM)
    );

    create table jbid_io_attr (
        ATTRIBUTE_ID int8 not null,
        IDENTITY_OBJECT_ID int8 not null,
        NAME varchar(255),
        ATTRIBUTE_TYPE varchar(255),
        BIN_VALUE_ID int8,
        primary key (ATTRIBUTE_ID),
        unique (IDENTITY_OBJECT_ID, NAME)
    );

    create table jbid_io_attr_text_values (
        TEXT_ATTR_VALUE_ID int8 not null,
        ATTR_VALUE varchar(255)
    );

    create table jbid_io_creden (
        ID int8 not null,
        BIN_VALUE_ID int8,
        IDENTITY_OBJECT_ID int8 not null,
        TEXT varchar(255),
        CREDENTIAL_TYPE int8 not null,
        primary key (ID),
        unique (IDENTITY_OBJECT_ID, CREDENTIAL_TYPE)
    );

    create table jbid_io_creden_props (
        PROP_ID int8 not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_creden_type (
        ID int8 not null,
        NAME varchar(255) unique,
        primary key (ID)
    );

    create table jbid_io_props (
        PROP_ID int8 not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel (
        ID int8 not null,
        FROM_IDENTITY int8 not null,
        NAME int8,
        TO_IDENTITY int8 not null,
        REL_TYPE int8 not null,
        primary key (ID),
        unique (FROM_IDENTITY, NAME, TO_IDENTITY, REL_TYPE)
    );

    create table jbid_io_rel_name (
        ID int8 not null,
        NAME varchar(255) not null,
        REALM int8 not null,
        primary key (ID),
        unique (NAME, REALM)
    );

    create table jbid_io_rel_name_props (
        PROP_ID int8 not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_props (
        PROP_ID int8 not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_type (
        ID int8 not null,
        NAME varchar(255) not null unique,
        primary key (ID)
    );

    create table jbid_io_type (
        ID int8 not null,
        NAME varchar(255) not null unique,
        primary key (ID)
    );

    create table jbid_real_props (
        PROP_ID int8 not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_realm (
        ID int8 not null,
        NAME varchar(255) not null,
        primary key (ID),
        unique (NAME)
    );

    -- create sequence hibernate_sequence;
