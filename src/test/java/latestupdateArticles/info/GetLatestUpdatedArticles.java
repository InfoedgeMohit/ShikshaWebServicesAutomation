package latestupdateArticles.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.relevantcodes.extentreports.*;

import common.Common;

public class GetLatestUpdatedArticles extends Common  {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result= false;
	String value, key;
	String[] resultkey = null;
	ExtentReports report;
	ExtentTest parent,child1;
	
	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("latestupdatearticels");
		// api path
		apiPath = "article/api/v1/info/getLatestUpdatedArticles";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "Ids", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//latestupdateArticles//info//getLatestArticles.xlsx";
		report =  createExtinctReport("GetLatestUpdatedArticles");
		parent = createParent(report, "GetLatestUpdatedArticles","");
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same
		// no. of
		// times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetLatestUpdateArticlesApi(String Ids,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1 = createChild(report,Ids,"");
		// pass api params and hit apicompareCourseId
		if (Ids.equals("ignoreHeader")){
//			return;
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		}
		else if (Ids.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api)
					.then().extract().response();
		} 
		

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath, report, parent, child1);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		} else {
			fail(child1, "InCorrect Response Code : " + statusCode);
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			 fail(child1,"API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
						+ apiResponseExpectedTime + " MS)");
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {	pass(child1," API Response time : " + apiResponseActualTime + " MS");
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(JsonPath jsonPath, ExtentReports report, ExtentTest parent, ExtentTest child) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {
				fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else {
				fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
			
			
			ResultSet rs = exceuteDbQuery("select blogTitle, url from blogTable where status = 'live' order by lastModifiedDate desc limit 3;", "shiksha");
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			String urlvalue = null, titlevalue = null;
			List<String> jsonurllist = new ArrayList<String>(jsonPath.getList("data.url"));
			List<String> jsontitlelist = new ArrayList<String>(jsonPath.getList("data.blogTitle"));
			Map<String, String> jsonMap = new HashMap<String, String>();
			Map<String, String> dbMap = new HashMap<String, String>();
			for(int i=0;i<jsonurllist.size();i++){
				urlvalue = jsonurllist.get(i);
				titlevalue = jsontitlelist.get(i);
				urlvalue = urlvalue.substring(urlvalue.indexOf(".com")+4);
				jsonMap.put(titlevalue, urlvalue);
			}
			if (rowcount > 0) {
				while (rs.next()) {
					dbMap.put(rs.getString("blogTitle"), rs.getString("url"));
				}
			}
			
			if(jsonMap.equals(dbMap)){
				pass(child, "getLestArticlesandCountParams Api Passed");
				Reporter.log("getLestArticlesandCountParams Api Passed");
			}
			else
			{
				fail(child, "getLestArticlesandCountParams Api Failed");
				Reporter.log("getLestArticlesandCountParams Api Failed");
				Reporter.log("Json Map :"+jsonMap +" DB Map :"+dbMap);
				Assert.assertTrue(false, "Json Map :"+jsonMap +" DB Map :"+dbMap);
			}
			
			
		} catch (Exception e) {
			fail(child, "");
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		finally{
			closeChild(parent, child, report);
		}
	}

	 @Test(priority = 2)
	 public static void respnseTimeStats_GetLatestArticles() {
	 respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
