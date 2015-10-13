
    create table jbid_attr_bin_value (
        BIN_VALUE_ID numeric(19,0) identity not null,
        VALUE image null,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_creden_bin_value (
        BIN_VALUE_ID numeric(19,0) identity not null,
        VALUE image null,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_io (
        ID numeric(19,0) identity not null,
        IDENTITY_TYPE numeric(19,0) not null,
        NAME varchar(255) not null,
        REALM numeric(19,0) not null,
        primary key (ID),
        unique (IDENTITY_TYPE, NAME, REALM)
    );

    create table jbid_io_attr (
        ATTRIBUTE_ID numeric(19,0) identity not null,
        IDENTITY_OBJECT_ID numeric(19,0) not null,
        NAME varchar(255) null,
        ATTRIBUTE_TYPE varchar(255) null,
        BIN_VALUE_ID numeric(19,0) null,
        primary key (ATTRIBUTE_ID),
        unique (IDENTITY_OBJECT_ID, NAME)
    );

    create table jbid_io_attr_text_values (
        TEXT_ATTR_VALUE_ID numeric(19,0) not null,
        ATTR_VALUE varchar(255) null
    );

    create table jbid_io_creden (
        ID numeric(19,0) identity not null,
        BIN_VALUE_ID numeric(19,0) null,
        IDENTITY_OBJECT_ID numeric(19,0) not null,
        TEXT varchar(255) null,
        CREDENTIAL_TYPE numeric(19,0) not null,
        primary key (ID),
        unique (IDENTITY_OBJECT_ID, CREDENTIAL_TYPE)
    );

    create table jbid_io_creden_props (
        PROP_ID numeric(19,0) not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_creden_type (
        ID numeric(19,0) identity not null,
        NAME varchar(255) null unique,
        primary key (ID)
    );

    create table jbid_io_props (
        PROP_ID numeric(19,0) not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel (
        ID numeric(19,0) identity not null,
        FROM_IDENTITY numeric(19,0) not null,
        NAME numeric(19,0) null,
        TO_IDENTITY numeric(19,0) not null,
        REL_TYPE numeric(19,0) not null,
        primary key (ID),
        unique (FROM_IDENTITY, NAME, TO_IDENTITY, REL_TYPE)
    );

    create table jbid_io_rel_name (
        ID numeric(19,0) identity not null,
        NAME varchar(255) not null,
        REALM numeric(19,0) not null,
        primary key (ID),
        unique (NAME, REALM)
    );

    create table jbid_io_rel_name_props (
        PROP_ID numeric(19,0) not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_props (
        PROP_ID numeric(19,0) not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_type (
        ID numeric(19,0) identity not null,
        NAME varchar(255) not null unique,
        primary key (ID)
    );

    create table jbid_io_type (
        ID numeric(19,0) identity not null,
        NAME varchar(255) not null unique,
        primary key (ID)
    );

    create table jbid_real_props (
        PROP_ID numeric(19,0) not null,
        PROP_VALUE varchar(255) not null,
        PROP_NAME varchar(255) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_realm (
        ID numeric(19,0) identity not null,
        NAME varchar(255) not null,
        primary key (ID),
        unique (NAME)
    );
