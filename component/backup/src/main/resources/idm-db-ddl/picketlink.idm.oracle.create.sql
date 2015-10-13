
    create table jbid_attr_bin_value (
        BIN_VALUE_ID number(19,0) not null,
        VALUE blob,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_creden_bin_value (
        BIN_VALUE_ID number(19,0) not null,
        VALUE blob,
        primary key (BIN_VALUE_ID)
    );

    create table jbid_io (
        ID number(19,0) not null,
        IDENTITY_TYPE number(19,0) not null,
        NAME varchar2(255 char) not null,
        REALM number(19,0) not null,
        primary key (ID),
        unique (IDENTITY_TYPE, NAME, REALM)
    );

    create table jbid_io_attr (
        ATTRIBUTE_ID number(19,0) not null,
        IDENTITY_OBJECT_ID number(19,0) not null,
        NAME varchar2(255 char),
        ATTRIBUTE_TYPE varchar2(255 char),
        BIN_VALUE_ID number(19,0),
        primary key (ATTRIBUTE_ID),
        unique (IDENTITY_OBJECT_ID, NAME)
    );

    create table jbid_io_attr_text_values (
        TEXT_ATTR_VALUE_ID number(19,0) not null,
        ATTR_VALUE varchar2(255 char)
    );

    create table jbid_io_creden (
        ID number(19,0) not null,
        BIN_VALUE_ID number(19,0),
        IDENTITY_OBJECT_ID number(19,0) not null,
        TEXT varchar2(255 char),
        CREDENTIAL_TYPE number(19,0) not null,
        primary key (ID),
        unique (IDENTITY_OBJECT_ID, CREDENTIAL_TYPE)
    );

    create table jbid_io_creden_props (
        PROP_ID number(19,0) not null,
        PROP_VALUE varchar2(255 char) not null,
        PROP_NAME varchar2(255 char) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_creden_type (
        ID number(19,0) not null,
        NAME varchar2(255 char) unique,
        primary key (ID)
    );

    create table jbid_io_props (
        PROP_ID number(19,0) not null,
        PROP_VALUE varchar2(255 char) not null,
        PROP_NAME varchar2(255 char) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel (
        ID number(19,0) not null,
        FROM_IDENTITY number(19,0) not null,
        NAME number(19,0),
        TO_IDENTITY number(19,0) not null,
        REL_TYPE number(19,0) not null,
        primary key (ID),
        unique (FROM_IDENTITY, NAME, TO_IDENTITY, REL_TYPE)
    );

    create table jbid_io_rel_name (
        ID number(19,0) not null,
        NAME varchar2(255 char) not null,
        REALM number(19,0) not null,
        primary key (ID),
        unique (NAME, REALM)
    );

    create table jbid_io_rel_name_props (
        PROP_ID number(19,0) not null,
        PROP_VALUE varchar2(255 char) not null,
        PROP_NAME varchar2(255 char) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_props (
        PROP_ID number(19,0) not null,
        PROP_VALUE varchar2(255 char) not null,
        PROP_NAME varchar2(255 char) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_io_rel_type (
        ID number(19,0) not null,
        NAME varchar2(255 char) not null unique,
        primary key (ID)
    );

    create table jbid_io_type (
        ID number(19,0) not null,
        NAME varchar2(255 char) not null unique,
        primary key (ID)
    );

    create table jbid_real_props (
        PROP_ID number(19,0) not null,
        PROP_VALUE varchar2(255 char) not null,
        PROP_NAME varchar2(255 char) not null,
        primary key (PROP_ID, PROP_NAME)
    );

    create table jbid_realm (
        ID number(19,0) not null,
        NAME varchar2(255 char) not null,
        primary key (ID),
        unique (NAME)
    );

    create sequence hibernate_sequence;
