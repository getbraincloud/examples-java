package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.StatusCodes;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.BrainCloudWrapper;

import java.util.Dictionary;
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

    long mostRecentPacket = -1000000;
    long secondMostRecentPacket = -1000000;

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
        String handoffId;
        String handoffToken;

        TestResult tr3 = new TestResult(_wrapper);
        String anonId = _client.getAuthenticationService().generateAnonymousId();
        _client.getAuthenticationService().authenticateAnonymous(anonId, true, tr3);
        tr3.Run();

        TestResult tr2 = new TestResult(_wrapper);
        _client.getScriptService().runScript("createHandoffId", 
        Helpers.createJsonPair("", ""),
         tr2);
        tr2.Run();
        handoffId = tr2.m_response.getJSONObject("data").getJSONObject("response").getString("handoffId");
        handoffToken = tr2.m_response.getJSONObject("data").getJSONObject("response").getString("securityToken");

        TestResult tr = new TestResult(_wrapper);
        _client.getAuthenticationService().authenticateHandoff(handoffId, handoffToken, tr);
        tr.Run();
    }
    
    @Test
    public void testAuthenticateSettopHandoff() throws Exception
    {
        String handoffCode;

        TestResult tr3 = new TestResult(_wrapper);
        String anonId = _client.getAuthenticationService().generateAnonymousId();
        _client.getAuthenticationService().authenticateAnonymous(anonId, true, tr3);
        tr3.Run();

        TestResult tr2 = new TestResult(_wrapper);
        _client.getScriptService().runScript("CreateSettopHandoffCode", 
        Helpers.createJsonPair("", ""),
         tr2);
        tr2.Run();
        handoffCode = tr2.m_response.getJSONObject("data").getJSONObject("response").getString("handoffCode");

        TestResult tr = new TestResult(_wrapper);
        _client.getAuthenticationService().authenticateSettopHandoff(handoffCode, tr);
        tr.Run();
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
    public void testResetEmailPasswordWithExpiry() throws Exception
    {
            TestResult tr2 = new TestResult(_wrapper);
            _wrapper.getClient().getAuthenticationService().authenticateUniversal("abc", "abc", true, tr2);
            tr2.Run();

        String email = "braincloudunittest@gmail.com";

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().resetEmailPasswordWithExpiry(
                email, 1 , tr);
        tr.Run();
    }

    @Test
    public void testResetEmailPasswordAdvancedWithExpiry() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().authenticateUniversal("abc", "abc", true, tr);
        tr.Run();
        
        TestResult tr2 = new TestResult(_wrapper);

        String content = "{\"fromAddress\": \"fromAddress\",\"fromName\": \"fromName\",\"replyToAddress\": \"replyToAddress\",\"replyToName\": \"replyToName\", \"templateId\": \"8f14c77d-61f4-4966-ab6d-0bee8b13d090\",\"subject\": \"subject\",\"body\": \"Body goes here\", \"substitutions\": { \":name\": \"John Doe\",\":resetLink\": \"www.dummuyLink.io\"}, \"categories\": [\"category1\",\"category2\" ]}";
        _wrapper.getClient().getAuthenticationService().resetEmailPasswordAdvancedWithExpiry(
                "braincloudunittest@gmail.com",
                content,
                1,
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

    @Test
    public void testResetUniversalIdPasswordWithExpiry() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr2);
        
        tr2.Run();

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getClient().getAuthenticationService().resetUniversalIdPasswordWithExpiry(
        //an example universal ID of userB
        "userb-1177370719", 1 , tr);
        tr.Run();
    }

    @Test
    public void testResetUniversalIdPasswordAdvancedWithExpiry() throws Exception
    {
        TestResult tr2 = new TestResult(_wrapper);

        _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr2);
        
        tr2.Run();

        TestResult tr = new TestResult(_wrapper);

        String content = "{\"templateId\": \"d-template-id-guid\", \"substitutions\": { \":name\": \"John Doe\",\":resetLink\": \"www.dummuyLink.io\"}, \"categories\": [\"category1\",\"category2\" ]}"; 
        _wrapper.getClient().getAuthenticationService().resetUniversalIdPasswordAdvancedWithExpiry(
            //an example universalId of userB
                "userb-1177370719",
                content,
                1,
                tr);

        tr.Run();
    }
    
    @Test
    public void testBadSig() throws Exception
    {
        //our problem is that users who refresh their app secret via the portal, the client would fail to read the response, and would retry infinitely.
        //This threatens our servers, because huge numbers of errors related to bad signature show up, and infinitely retry to get out of this error.
        //Instead of updating the signature via the portal, we will mimic a bad signature from the client.
        Map<String, String> originalAppSecretMap = new HashMap<String, String>();
        originalAppSecretMap.put(m_appId, m_secret);
        originalAppSecretMap.put(m_childAppId, m_childSecret);
        int numRepeatBadSigFailures = 0;
    
        // mess up app 
        Map<String, String> updatedAppSecretMap = new HashMap<String, String>();;
    
        for (Map.Entry<String, String> entry : originalAppSecretMap.entrySet())
        {
            updatedAppSecretMap.put(entry.getKey(), entry.getValue() + "123");
        }  
        /////////////////////Phase 1
            //first auth
            TestResult tr1 = new TestResult(_wrapper);
            _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr1);
            if(tr1.Run())
            {
               //Check the packet coming in and compare it to the last recevied packet. if they're both -1, we may be in a repeating scenario.
                if(mostRecentPacket == -1000000 && secondMostRecentPacket == -1000000)
                {
                    mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
                }
                else
                {
                    secondMostRecentPacket = mostRecentPacket;
                    mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
                }
        
                //Is there the sign of a repeat?
                if(mostRecentPacket == -1 && secondMostRecentPacket == -1)
                {
                    numRepeatBadSigFailures++;
                }
                //we shouldnt expect more than 2 times that most recent and second most recent are both bad sig errors for this test, else its repeating itself. 
                if (numRepeatBadSigFailures > 2) throw new Exception("Repeating Bad sig errors");
            }
        
            //check state
            _wrapper.getClient().getPlayerStateService().readUserState(tr1);
        
        //////////////////////////Phase 2
            TestResult tr3 = new TestResult(_wrapper);
            _wrapper.getClient().initializeWithApps(m_serverUrl, m_appId, updatedAppSecretMap, m_appVersion);
            _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr3);
            if(tr3.RunExpectFail(StatusCodes.FORBIDDEN, ReasonCodes.BAD_SIGNATURE))
            {
               //Check the packet coming in and compare it to the last recevied packet. if they're both -1, we may be in a repeating scenario.
               if(mostRecentPacket == -1000000 && secondMostRecentPacket == -1000000)
               {
                   mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
               }
               else
               {
                   secondMostRecentPacket = mostRecentPacket;
                   mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
               }
        
                //Is there the sign of a repeat?
                if(mostRecentPacket == -1 && secondMostRecentPacket == -1)
                {
                    numRepeatBadSigFailures++;
                }
                //we shouldnt expect more than 2 times that most recent and second most recent are both bad sig errors for this test, else its repeating itself. 
                if (numRepeatBadSigFailures > 2) throw new Exception("Repeating Bad sig errors");
            }
        
            //check state
            _wrapper.getClient().getPlayerStateService().readUserState(tr3);
        
            //wait a while
            Thread.sleep(5 * 1000);
        
            /////////////////////Phase 3
            TestResult tr5 = new TestResult(_wrapper);
            _wrapper.getClient().initializeWithApps(m_serverUrl, m_appId, originalAppSecretMap, m_appVersion);
            _wrapper.getClient().getAuthenticationService().authenticateUniversal(getUser(Users.UserB).id, getUser(Users.UserB).password, true, tr5);
            if(tr5.Run())
            {
                //Check the packet coming in and compare it to the last recevied packet. if they're both -1, we may be in a repeating scenario.
                if(mostRecentPacket == -1000000 && secondMostRecentPacket == -1000000)
                {
                   mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
                }
                else
                {
                   secondMostRecentPacket = mostRecentPacket;
                   mostRecentPacket = _wrapper.getClient().getRestClient().getLastReceivedPacketId();
                }
        
                //Is there the sign of a repeat?
                if(mostRecentPacket == -1 && secondMostRecentPacket == -1)
                {
                    numRepeatBadSigFailures++;
                }
                //we shouldnt expect more than 2 times that most recent and second most recent are both bad sig errors for this test, else its repeating itself. 
                if (numRepeatBadSigFailures > 2) throw new Exception("Repeating Bad sig errors");
            }
        
        //check state
        _wrapper.getClient().getPlayerStateService().readUserState(tr5);
    }
}
