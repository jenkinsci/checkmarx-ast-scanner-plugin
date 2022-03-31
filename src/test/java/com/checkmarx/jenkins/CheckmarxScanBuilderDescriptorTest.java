package com.checkmarx.jenkins;

import com.checkmarx.jenkins.credentials.DefaultCheckmarxApiToken;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CheckmarxScanBuilderDescriptorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor instance;

    @Before
    public void setUp() {
        this.instance = new CheckmarxScanBuilder.CheckmarxScanBuilderDescriptor();
    }

    @Test
    public void doFillCheckmarxTokenItems_shouldAddCurrentValue_ifPresent() {
        final ListBoxModel model = this.instance.doFillCredentialsIdItems(null, "current-value");
        assertEquals(2, model.size());
        assertSame(2, model.size());

        final ListBoxModel.Option firstItem = model.get(0);
        assertEquals("", firstItem.value);
        assertSame("", firstItem.value);


        final ListBoxModel.Option secondItem = model.get(1);
        assertEquals("current-value", secondItem.value);
        assertSame("current-value", secondItem.value);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnError_ifTokenIsEmpty() {
        final FormValidation.Kind checkmarxTokenIdValidation = this.instance.doCheckCredentialsId(null, null).kind;

        assertEquals(FormValidation.Kind.ERROR, checkmarxTokenIdValidation);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnError_ifTokenNotFound() {
        final FormValidation.Kind checkmarxTokenIdValidation = this.instance.doCheckCredentialsId(null, "any-token").kind;

        assertEquals(FormValidation.Kind.ERROR, checkmarxTokenIdValidation);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnOK_ifTokenFound() throws Exception {
        final DefaultCheckmarxApiToken checkmarxToken = new DefaultCheckmarxApiToken(CredentialsScope.GLOBAL, "id", "", "checkmarx-clientId", "checkmarx-client-secret");
        CredentialsProvider.lookupStores(this.jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), checkmarxToken);

        final FormValidation.Kind checkmarxTokenIdValidation = this.instance.doCheckCredentialsId(null, "id").kind;

        assertEquals(FormValidation.Kind.OK, checkmarxTokenIdValidation);
    }
}
