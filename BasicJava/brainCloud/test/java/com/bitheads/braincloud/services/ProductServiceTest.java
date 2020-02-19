package com.bitheads.braincloud.services;

import com.bitheads.braincloud.client.BrainCloudClient;
import com.bitheads.braincloud.client.ReasonCodes;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by prestonjennings on 15-09-02.
 */
public class ProductServiceTest extends TestFixtureBase
{
    private final String _currencytype = "credits";
    private final String _platform = "windows";
    private final String _productCatagory = "Test";

    @Test
    public void testGetCurrency() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().getCurrency(
                _currencytype,
                tr);

        tr.Run();
    }

    @Ignore("Used in Virtual Currency Service") 
    @Test
    public void testAwardCurrency() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().awardCurrency(
                _currencytype,
                200,
                tr);

        tr.RunExpectFail(403, ReasonCodes.CURRENCY_SECURITY_ERROR);
    }

    @Ignore("Used in Virtual Currency Service") 
    @Test
    public void testConsumeCurrency() throws Exception
    {
        testAwardCurrency();
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().consumeCurrency(
                _currencytype,
                100,
                tr);

        tr.RunExpectFail(403, ReasonCodes.CURRENCY_SECURITY_ERROR);
    }

    @Ignore("Used in Virtual Currency Service") 
    @Test
    public void testResetCurrency() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().resetCurrency(
                tr);

        tr.RunExpectFail(403, ReasonCodes.CURRENCY_SECURITY_ERROR);
    }

    @Test
    public void testGetSalesInventory() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().getSalesInventory(
                _platform,
                "CAD",
                tr);

        tr.Run();
    }

    @Test
    public void testGetSalesInventoryByCategory() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().getSalesInventoryByCategory(
                _platform,
                "CAD",
                _productCatagory,
                tr);

        tr.Run();
    }

    @Test
    public void testGetEligiblePromotions() throws Exception
    {
        TestResult tr = new TestResult(_wrapper);

        _wrapper.getProductService().getEligiblePromotions(
                tr);

        tr.Run();
    }

    @Test
    public void testConfirmGooglePlayPurchase() throws Exception
    {
// TODO: 15-09-03  
    }

}