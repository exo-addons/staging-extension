    alter table jbid_io 
        add index FK94860092CF26285 (REALM), 
        add constraint FK94860092CF26285 
        foreign key (REALM) 
        references jbid_realm (ID);

    alter table jbid_io 
        add index FK948600921900168B (IDENTITY_TYPE), 
        add constraint FK948600921900168B 
        foreign key (IDENTITY_TYPE) 
        references jbid_io_type (ID);

    alter table jbid_io_attr 
        add index FK4DC61D7EDE7E39CB (BIN_VALUE_ID), 
        add constraint FK4DC61D7EDE7E39CB 
        foreign key (BIN_VALUE_ID) 
        references jbid_attr_bin_value (BIN_VALUE_ID);

    alter table jbid_io_attr 
        add index FK4DC61D7E992317F0 (IDENTITY_OBJECT_ID), 
        add constraint FK4DC61D7E992317F0 
        foreign key (IDENTITY_OBJECT_ID) 
        references jbid_io (ID);

    alter table jbid_io_attr_text_values 
        add index FKC6F8C733577FACCB (TEXT_ATTR_VALUE_ID), 
        add constraint FKC6F8C733577FACCB 
        foreign key (TEXT_ATTR_VALUE_ID) 
        references jbid_io_attr (ATTRIBUTE_ID);

    alter table jbid_io_creden 
        add index FKF7FB3EC4F1FBD584 (BIN_VALUE_ID), 
        add constraint FKF7FB3EC4F1FBD584 
        foreign key (BIN_VALUE_ID) 
        references jbid_creden_bin_value (BIN_VALUE_ID);

    alter table jbid_io_creden 
        add index FKF7FB3EC4992317F0 (IDENTITY_OBJECT_ID), 
        add constraint FKF7FB3EC4992317F0 
        foreign key (IDENTITY_OBJECT_ID) 
        references jbid_io (ID);

    alter table jbid_io_creden 
        add index FKF7FB3EC429C64409 (CREDENTIAL_TYPE), 
        add constraint FKF7FB3EC429C64409 
        foreign key (CREDENTIAL_TYPE) 
        references jbid_io_creden_type (ID);

    alter table jbid_io_creden_props 
        add index FKBAD648353BD68084 (PROP_ID), 
        add constraint FKBAD648353BD68084 
        foreign key (PROP_ID) 
        references jbid_io_creden (ID);

    alter table jbid_io_props 
        add index FK6BCFF78372655F8D (PROP_ID), 
        add constraint FK6BCFF78372655F8D 
        foreign key (PROP_ID) 
        references jbid_io (ID);

    alter table jbid_io_rel 
        add index FKE9BC4F6C2D9E6CE8 (REL_TYPE), 
        add constraint FKE9BC4F6C2D9E6CE8 
        foreign key (REL_TYPE) 
        references jbid_io_rel_type (ID);

    alter table jbid_io_rel 
        add index FKE9BC4F6C4EF6CBA4 (NAME), 
        add constraint FKE9BC4F6C4EF6CBA4 
        foreign key (NAME) 
        references jbid_io_rel_name (ID);

    alter table jbid_io_rel 
        add index FKE9BC4F6CE5991E18 (TO_IDENTITY), 
        add constraint FKE9BC4F6CE5991E18 
        foreign key (TO_IDENTITY) 
        references jbid_io (ID);

    alter table jbid_io_rel 
        add index FKE9BC4F6C89DEF9C9 (FROM_IDENTITY), 
        add constraint FKE9BC4F6C89DEF9C9 
        foreign key (FROM_IDENTITY) 
        references jbid_io (ID);

    alter table jbid_io_rel_name 
        add index FK5856A77ECF26285 (REALM), 
        add constraint FK5856A77ECF26285 
        foreign key (REALM) 
        references jbid_realm (ID);

    alter table jbid_io_rel_name_props 
        add index FK224ABD6F3C564090 (PROP_ID), 
        add constraint FK224ABD6F3C564090 
        foreign key (PROP_ID) 
        references jbid_io_rel_name (ID);

    alter table jbid_io_rel_props 
        add index FKB2A23ADD93AFB8E5 (PROP_ID), 
        add constraint FKB2A23ADD93AFB8E5 
        foreign key (PROP_ID) 
        references jbid_io_rel (ID);

    alter table jbid_real_props 
        add index FK5F651EFBF40F776D (PROP_ID), 
        add constraint FK5F651EFBF40F776D 
        foreign key (PROP_ID) 
        references jbid_realm (ID);
