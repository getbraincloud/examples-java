package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by prestonjennings on 15-09-02.
 */
public class TimeServiceTest extends TestFixtureBase
{

    @Test
    public void testReadServerTime() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getTimeService().readServerTime(
                tr);

        tr.Run();
    }
}