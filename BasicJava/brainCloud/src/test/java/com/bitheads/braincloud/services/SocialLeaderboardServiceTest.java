package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.ReasonCodes;
import com.bitheads.braincloud.client.StatusCodes;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by prestonjennings on 15-09-02.
 */
public class SocialLeaderboardServiceTest extends TestFixtureBase
{
    private final String _globalLeaderboardId = "testLeaderboard";
    private final String _socialLeaderboardId = "testSocialLeaderboard";
    private final String _dynamicLeaderboardId = "testDynamicLeaderboard";
    
    @Test
    public void testGetSocialLeaderboard() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getSocialLeaderboard(
                _globalLeaderboardId,
                true,
                tr);

        tr.Run();
    }

    @Test
    public void testGetSocialLeaderboardByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getSocialLeaderboardByVersion(
                _globalLeaderboardId,
                true,
                0,
                tr);

        tr.Run();
    }

    @Test
    public void testGetMultiSocialLeaderboard() throws Exception
    {
        postScoreToDynamicLeaderboard();
        postScoreToNonDynamicLeaderboard();

        TestResult tr = new TestResult(_wrapper);

        String[] lbIds = new String[] {
                _globalLeaderboardId,
                _dynamicLeaderboardId + "_" + SocialLeaderboardService.SocialLeaderboardType.LAST_VALUE
        };
        _wrapper.getSocialLeaderboardService().getMultiSocialLeaderboard(
                lbIds,
                10,
                true,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardPageHigh() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardPage(
                _globalLeaderboardId,
                SocialLeaderboardService.SortOrder.HIGH_TO_LOW,
                0,
                10,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardPageLow() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardPage(
                _globalLeaderboardId,
                SocialLeaderboardService.SortOrder.LOW_TO_HIGH,
                0,
                10,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardPageFail() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardPage(
                "thisDoesNotExistLeaderboard",
                SocialLeaderboardService.SortOrder.LOW_TO_HIGH,
                0,
                10,
                tr);

        tr.RunExpectFail(StatusCodes.INTERNAL_SERVER_ERROR, 40499);
    }

    @Test
    public void testGetGlobalLeaderboardPageByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardPageByVersion(
                _globalLeaderboardId,
                SocialLeaderboardService.SortOrder.HIGH_TO_LOW,
                0,
                10,
                1,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardView() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardView(
                _globalLeaderboardId,
                SocialLeaderboardService.SortOrder.HIGH_TO_LOW,
                5,
                5,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardViewByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardViewByVersion(
                _globalLeaderboardId,
                SocialLeaderboardService.SortOrder.HIGH_TO_LOW,
                5,
                5,
                1,
                tr);

        tr.Run();
    }

    @Test
    public void testGetGlobalLeaderboardVersions() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardVersions(
                _globalLeaderboardId,
                tr);

        tr.Run();
    }

    @Test
    public void testPostScoreToLeaderboard() throws Exception
    {
        postScoreToNonDynamicLeaderboard();
    }

    @Test
    public void testPostScoreToDynamicLeaderboard() throws Exception
    {
        postScoreToDynamicLeaderboard();
    }

    @Test
    public void testPostScoreToDynamicLeaderboardDays() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToDynamicLeaderboardDays(
                _dynamicLeaderboardId + "_" + "DAYS",
                100,
                Helpers.createJsonPair("testDataKey", 400),
                SocialLeaderboardService.SocialLeaderboardType.LOW_VALUE.toString(),
                null,
                5,
                3,
                tr);

        tr.Run();
    }

    @Test
    public void testPostScoreToDynamicLeaderboardLowValue() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToDynamicLeaderboard(
                _dynamicLeaderboardId + "_" + SocialLeaderboardService.SocialLeaderboardType.LOW_VALUE,
                100,
                Helpers.createJsonPair("testDataKey", 400),
                SocialLeaderboardService.SocialLeaderboardType.LOW_VALUE.toString(),
                SocialLeaderboardService.RotationType.NEVER.toString(),
                null,
                5,
                tr);

        tr.Run();
    }

    @Test
    public void testPostScoreToDynamicLeaderboardCumulative() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToDynamicLeaderboard(
                _dynamicLeaderboardId + "_" + SocialLeaderboardService.SocialLeaderboardType.CUMULATIVE,
                100,
                Helpers.createJsonPair("testDataKey", 400),
                SocialLeaderboardService.SocialLeaderboardType.CUMULATIVE.toString(),
                SocialLeaderboardService.RotationType.WEEKLY.toString(),
                addDays(new Date(), 3),
                5,
                tr);

        tr.Run();
    }

    @Test
    public void testPostScoreToDynamicLeaderboardLastValue() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToDynamicLeaderboard(
                _dynamicLeaderboardId + "_" + SocialLeaderboardService.SocialLeaderboardType.LAST_VALUE,
                100,
                Helpers.createJsonPair("testDataKey", 400),
                SocialLeaderboardService.SocialLeaderboardType.LAST_VALUE.toString(),
                SocialLeaderboardService.RotationType.DAILY.toString(),
                addDays(new Date(), 2),
                3,
                tr);

        tr.Run();
    }

    @Test
    public void testPostScoreToDynamicLeaderboardNullRotationTime() throws Exception
    {
        postScoreToDynamicLeaderboard();
    }

    @Test
    public void testGetGroupSocialLeaderboard() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getGroupService().createGroup(
                "testGroup",
                "test",
                false,
                new GroupACL(GroupACL.Access.ReadWrite, GroupACL.Access.ReadWrite),
                Helpers.createJsonPair("testInc", 123),
                Helpers.createJsonPair("test", "test"),
                Helpers.createJsonPair("test", "test"),
                tr);

        tr.Run();

        JSONObject data = tr.m_response.getJSONObject("data");
        String groupId = data.getString("groupId");

        _wrapper.getSocialLeaderboardService().getGroupSocialLeaderboard(
                _socialLeaderboardId,
                groupId,
                tr);
        tr.Run();

        _wrapper.getGroupService().deleteGroup(
                groupId,
                -1,
                tr);
        tr.Run();
    }

    @Test
    public void testGetGroupSocialLeaderboardByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGroupSocialLeaderboardByVersion(
                _socialLeaderboardId,
                "_invalid_",
                0,
                tr);
        tr.RunExpectFail(400, ReasonCodes.MISSING_RECORD);
    }

    @Test
    public void testGetPlayersSocialLeaderboard() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getPlayersSocialLeaderboard(
                _socialLeaderboardId,
                new String[] { getUser(Users.UserA).profileId, getUser(Users.UserB).profileId },
                tr);

        tr.Run();
    }

    @Test
    public void testGetPlayersSocialLeaderboardByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getPlayersSocialLeaderboardByVersion(
                _socialLeaderboardId,

                new String[] { getUser(Users.UserA).profileId, getUser(Users.UserB).profileId },
                0,
                tr);

        tr.Run();
    }

    @Test
    public void testListAllLeaderboards() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().listAllLeaderboards(tr);

        tr.Run();
    }

    @Test
    public void getGlobalLeaderboardEntryCount() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardEntryCount(_globalLeaderboardId, tr);

        tr.Run();
    }

    @Test
    public void getGlobalLeaderboardEntryCountByVersion() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().getGlobalLeaderboardEntryCountByVersion(_globalLeaderboardId, 1, tr);

        tr.Run();
    }

    @Test
    public void testGetPlayerScore() throws Exception
    {
        postScoreToNonDynamicLeaderboard();

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getSocialLeaderboardService().getPlayerScore(
                _globalLeaderboardId,
                -1,
                tr);

        tr.Run();
    }

    @Test
    public void testRemovePlayerScore() throws Exception
    {
        postScoreToNonDynamicLeaderboard();

        TestResult tr = new TestResult(_wrapper);
        _wrapper.getSocialLeaderboardService().removePlayerScore(
                _globalLeaderboardId,
                -1,
                tr);

        tr.Run();
    }

    @Test
    public void testGetPlayerScoresFromLeaderboards() throws Exception
    {
        postScoreToDynamicLeaderboard();
        postScoreToNonDynamicLeaderboard();

        TestResult tr = new TestResult(_wrapper);

        String[] lbIds = new String[] {
                _globalLeaderboardId,
                _dynamicLeaderboardId + "_" + SocialLeaderboardService.SocialLeaderboardType.LAST_VALUE
        };
        _wrapper.getSocialLeaderboardService().getPlayerScoresFromLeaderboards(
                lbIds,
                tr);

        tr.Run();
    }

    public void postScoreToDynamicLeaderboard() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToDynamicLeaderboard(
                _dynamicLeaderboardId + "_" + (int)(Math.random() * 10000000),
                100,
                Helpers.createJsonPair("testDataKey", 400),
                SocialLeaderboardService.SocialLeaderboardType.LAST_VALUE.toString(),
                SocialLeaderboardService.RotationType.NEVER.toString(),
                addDays(new Date(), 3),
                5,
                tr);

        tr.Run();
    }

    public void postScoreToNonDynamicLeaderboard() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getSocialLeaderboardService().postScoreToLeaderboard(
                _globalLeaderboardId,
                1000,
                Helpers.createJsonPair("testDataKey", 400),
                tr);

        tr.Run();
    }

    public static Date addDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); //minus number would decrement the days
        return cal.getTime();
    }
}