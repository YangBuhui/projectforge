package org.projectforge.test;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@ApplyLdifFiles("localTest.ldif")
@RunWith(FrameworkRunner.class)
@CreateLdapServer(
        transports = {
                @CreateTransport(protocol = "LDAP")
        })
public class JUnitLDAPTestWrapper extends AbstractLdapTestUnit {

    public static LdapServer ldapServerWrap;

    public void sdfsd() {
        // Get the SchemaManager, we need it for this addition
        SchemaManager schemaManager = ldapServerWrap.getDirectoryService().getSchemaManager();

        // Create the partition
        JdbmPartition sevenseasPartition = new JdbmPartition();
        sevenseasPartition.setSchemaManager(schemaManager);
        sevenseasPartition.setId("sevenseas");
        DN suffixDn = null;
        try {
            suffixDn = new DN("o=sevenseas" );
        } catch (LdapInvalidDnException e) {
            e.printStackTrace();
        }
        try {
            sevenseasPartition.setSuffix(suffixDn.toString());
        } catch (LdapInvalidDnException e) {
            e.printStackTrace();
        }
        sevenseasPartition.setCacheSize(1000);
        try {
            sevenseasPartition.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        sevenseasPartition.setPartitionDir(new File("/home/mhesse/zeug"));

// Create some indices (optional)
        try {
            sevenseasPartition.addIndexOn(new JdbmIndex<>("objectClass"));
            sevenseasPartition.addIndexOn( new JdbmIndex("o") );
        } catch (Exception e) {
            e.printStackTrace();
        }

// Initialize the partition
        try {
            sevenseasPartition.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

// create the context entry
        DefaultServerEntry contextEntry = new DefaultServerEntry( schemaManager, suffixDn, "o=sevenseas",
                "objectClass: top",
                "objectClass: organization",
                "o: sevenseas" );

// add the context entry
        try {
            sevenseasPartition.add( new AddOperationContext(null, contextEntry) );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInitLdap() {
        ldapServerWrap = ldapServer;
        //sdfsd();
    }
}
