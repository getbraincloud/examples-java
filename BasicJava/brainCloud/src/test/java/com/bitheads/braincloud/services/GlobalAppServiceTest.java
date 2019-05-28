package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by prestonjennings on 15-09-02.
 */
public class GlobalAppServiceTest extends TestFixtureBase
{

    @Test
    public void testReadProperties() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getGlobalAppService().readProperties(
                tr);

        tr.Run();
    }
}