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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ScanBuilderDescriptorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private DescriptorImpl instance;

    @Before
    public void setUp() { instance = new DescriptorImpl(); }

    @Test
    public void doFillCheckmarxTokenItems_shouldAddCurrentValue_ifPresent() {
        ListBoxModel model = instance.doFillCredentialsIdItems(null, "current-value");
        assertEquals(2,model.size());
        assertSame(2,model.size());

        ListBoxModel.Option firstItem = model.get(0);
        assertEquals("",firstItem.value);
        assertSame("",firstItem.value);


        ListBoxModel.Option secondItem = model.get(1);
        assertEquals("current-value",secondItem.value);
        assertSame("current-value",secondItem.value);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnError_ifTokenIsEmpty() {
        FormValidation.Kind checkmarxTokenIdValidation = instance.doCheckCredentialsId(null,null).kind;

        assertEquals(FormValidation.Kind.ERROR,checkmarxTokenIdValidation);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnError_ifTokenNotFound() {
        FormValidation.Kind checkmarxTokenIdValidation = instance.doCheckCredentialsId(null,"any-token").kind;

        assertEquals(FormValidation.Kind.ERROR,checkmarxTokenIdValidation);
    }

    @Test
    public void doCheckCheckmarxTokenId_shouldReturnOK_ifTokenFound() throws Exception {
        DefaultCheckmarxApiToken checkmarxToken = new DefaultCheckmarxApiToken(CredentialsScope.GLOBAL, "id", "", "checkmarx-token");
        CredentialsProvider.lookupStores(jenkins.getInstance()).iterator().next().addCredentials(Domain.global(), checkmarxToken);

        FormValidation.Kind checkmarxTokenIdValidation = instance.doCheckCredentialsId(null,"id").kind;

        assertEquals(FormValidation.Kind.OK,checkmarxTokenIdValidation);
    }


}
