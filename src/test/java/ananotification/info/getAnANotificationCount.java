package ananotification.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;	

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.ExtentTest;

import common.Common;

public class getAnANotificationCount  extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int userId;
	ExtentReports report;
	ExtentTest parent,child1;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("ananotification");
		// api path
		apiPath = "notification/api/v1/info/getAnANotificationCount";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "userIds", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//ananotification//info//ananotification.xlsx";
		
		 report =  createExtinctReport("getAnANotificationCount");
		parent = createParent(report, "getAnANotificationCount","");

	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetAnANotificationApi(String userIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		 child1 = createChild(report,userIds,"");
		// pass api params and hit api
		if (userIds.equals("ignoreHeader"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if (userIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api).then()
					.extract().response();
		else if (!userIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("userIds[]", userIds)
					.when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(userIds,jsonPath, report, parent, child1);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			fail(child1, "InCorrect Response Code : " + statusCode);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			fail(child1, "API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
			Assert.fail("API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			pass(child1, "API Response time : " + apiResponseActualTime + " MS");
			Reporter.log("API Response time : " + apiResponseActualTime + " MS", true);
		}
		closeChild(parent, child1, report);
	}

	public void verifyPostiveCases(String userIds, JsonPath jsonPath,
			ExtentReports report, ExtentTest parent, ExtentTest child) {
		try {
		// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
				pass(child, "Correct status [success]");
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]", true);
				fail(child, "InCorrect status [" + statusResponse + "]");
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
				pass(child, " Correct message [null]");
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]", true);
				fail(child, "InCorrect message [" + messageResponse + "]");
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}

			// loop for multiple streamIds in array
			String[] userIdsArr = userIds.split(",");
			for (int i = 0; i < userIdsArr.length; i++) {
				this.userId = Integer.parseInt(userIdsArr[i]);
				int jsonUserId = jsonPath.getInt("data."+userId+".userId");
				if(userId == jsonUserId){
				Reporter.log("Correct user id appear for userId: "+ userId, true);
				pass(child, "Correct user id appear for userId: "+ userId);
				}
				else{
					Reporter.log("inCorrect user id appear for userId: "+ userId, false);
					fail(child,"inCorrect user id appear for userId: "+ userId);
					Assert.assertFalse(true);
				}
				
				String notificationcount = jsonPath.getString("data."+userId+".notificationCount");
				if(!notificationcount.equals("null")){
					Reporter.log("Notification count is not appear null for userId:  "+ userId, true);
					pass(child, "Notification count is not appear null for userId:  "+ userId);
				}
				else{
					Reporter.log("Notification count is appear null for userId:"+ userId, false);	
					fail(child, "Notification count is appear null for userId:"+ userId);
					Assert.assertFalse(true);
				}
			
				}
			
		} catch (Exception e) {
			fail(child, "");
			e.printStackTrace();
			Assert.fail("Exception occured :" + e.getMessage());
		}
		finally{
			closeChild(parent, child, report);
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_Getananotification() {
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}