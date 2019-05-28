package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.StatusCodes;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.BrainCloudWrapper;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by prestonjennings on 15-08-31.
 */
public class AuthenticationServiceTest extends TestFixtureNoAuth
{

    @Test
    public void testAuthenticateAnonymous() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);
        String anonId = _client.getAuthenticationService().generateAnonymousId();
        _client.getAuthenticationService().authenticateAnonymous(anonId, true, tr);

        tr.Run();
    }

    @Test
    public void testAuthenticateUniversalInstance() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserA).id, getUser(Users.UserA).password, true, tr2);
        
        tr2.Run();
    }

    @Test
    public void testAuthenticateEmailPassword() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateEmailPassword(
            getUser(Users.UserA).email,
            getUser(Users.UserA).password,
            true,
            tr);

        tr.Run();
    }

    @Test
    public void testAuthenticateHandoff() throws Exception
    {

        TestResult tr = new TestResult(_wrapper);
        _client.getAuthenticationService().authenticateHandoff("invalid_handoffId", "invalid_securityToken", tr);

        tr.RunExpectFail(403, ReasonCodes.TOKEN_DOES_NOT_MATCH_USER);
    }

    @Test
    public void testAuthenticateExternal() throws Exception
    {

    }

    @Test
    public void testAuthenticateFacebook() throws Exception
    {

    }

    @Test
    public void testAuthenticateGoogle() throws Exception
    {

    }

    @Test
    public void testAuthenticateSteam() throws Exception
    {

    }

    @Test
    public void testAuthenticateTwitter() throws Exception
    {

    }

    @Test
    public void testAuthenticateUniversal() throws Exception {
        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().authenticateUniversal("abc", "abc", true, tr);
        tr.Run();
    }

    @Test
    public void testResetEmailPassword() throws Exception
    {
        String email = "braincloudunittest@gmail.com";

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().resetEmailPassword(
                email, tr);
        tr.Run();
    }

    @Test
    public void testResetEmailPasswordAdvanced() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        String content = "{\"fromAddress\": \"fromAddress\",\"fromName\": \"fromName\",\"replyToAddress\": \"replyToAddress\",\"replyToName\": \"replyToName\", \"templateId\": \"8f14c77d-61f4-4966-ab6d-0bee8b13d090\",\"subject\": \"subject\",\"body\": \"Body goes here\", \"substitutions\": { \":name\": \"John Doe\",\":resetLink\": \"www.dummuyLink.io\"}, \"categories\": [\"category1\",\"category2\" ]}";
        _wrapper.getClient().getAuthenticationService().resetEmailPasswordAdvanced(
                "braincloudunittest@gmail.com",
                content,
                tr2);

        tr2.RunExpectFail(StatusCodes.BAD_REQUEST, ReasonCodes.INVALID_FROM_ADDRESS);
    }

    @Test
    public void testResetUniversalIdPassword() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr2);
        
        tr2.Run();

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().resetUniversalIdPassword(
        //an example universal ID of userB
        "userb-1177370719", tr);
        tr.Run();
    }

    @Test
    public void testResetUniversalIdPasswordAdvanced() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr2);
        
        tr2.Run();

        TestResult tr = new TestResult(_wrapper);

        String content = "{\"templateId\": \"d-template-id-guid\", \"substitutions\": { \":name\": \"John Doe\",\":resetLink\": \"www.dummuyLink.io\"}, \"categories\": [\"category1\",\"category2\" ]}"; 
        _wrapper.getClient().getAuthenticationService().resetUniversalIdPasswordAdvanced(
            //an example universalId of userB
                "userb-1177370719",
                content,
                tr);

        tr.Run();
    }
}