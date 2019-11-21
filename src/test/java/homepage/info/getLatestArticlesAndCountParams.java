package homepage.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import common.Common;

public class getLatestArticlesAndCountParams extends Common {
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
	@BeforeClass
	public void doItBeforeTest() {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/homepageapi/v1/info/getLatestArticlesAndCountParams";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "Ids", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//homepage//info//getLatestArticlesAndCountParams.xlsx";
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
	public void verifyGetLatestArticlesAndCountParamsApi(String Ids,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit apicompareCourseId
		if (Ids.equals("ignoreHeader")){
			return;
//			apiResponse = RestAssured.given().param("userId", userId).param("compareCourseIds[]",compareCourseId ).when().post(api).then().extract().response();
		}
		else if (Ids.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		} 
		

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath);
		} else if (statusCode == 400) { // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected);
		} else if (statusCode == 403) { // unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath);
		} else {
			Reporter.log(", InCorrect Response Code : " + statusCode, true);
			Assert.fail("InCorrect Response Code : " + statusCode);
		}

		long apiResponseActualTime = apiResponse
				.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int) apiResponseActualTime);

		if (apiResponseActualTime > apiResponseExpectedTime) {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)", true);
			Assert.fail("API Response time : " + apiResponseActualTime
					+ " MS is greater than expected time ("
					+ apiResponseExpectedTime + " MS)");
		} else {
			Reporter.log("<br> API Response time : " + apiResponseActualTime
					+ " MS", true);
		}
	}

	public void verifyPostiveCases(JsonPath jsonPath) {
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if (statusResponse.contentEquals("success")) {
				Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status [" + statusResponse + "]",
						true);
				Assert.fail("InCorrect status [" + statusResponse + "]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if (messageResponse == null) {
				Reporter.log(", Correct message [null]", true);
			} else {
				Reporter.log(", InCorrect message [" + messageResponse + "]",
						true);
				Assert.fail("InCorrect message [" + messageResponse + "]");
			}
			
			
			ResultSet rs = exceuteDbQuery("select blogTitle, url from blogTable where status = 'live'  order by lastModifiedDate desc limit 3;", "shiksha");
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);
			String urlvalue = null, titlevalue = null;
			List<String> jsonurllist = new ArrayList<String>(jsonPath.getList("data.latestArticles.url"));
			List<String> jsontitlelist = new ArrayList<String>(jsonPath.getList("data.latestArticles.blogTitle"));
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
				Assert.assertTrue(true);
				Reporter.log("getLestArticlesandCountParams Api Passed");
			}
			else
			{
				Assert.assertTrue(false, "Json Map :"+jsonMap +" DB Map :"+dbMap);
				Reporter.log("getLestArticlesandCountParams Api Failed");
				Reporter.log("Json Map :"+jsonMap +" DB Map :"+dbMap);
			}
			Map<String, String> jsonHPcountMap = new HashMap<String, String>(jsonPath.getMap("data.nationalHPParams"));
			for(Map.Entry<String, String> entry : jsonHPcountMap.entrySet()){
				value = String.valueOf(entry.getValue());
				key = entry.getKey();
				
				if(value.length()<1){
					result = true;
				}
				if(result==true){
				Reporter.log("Result Found null for Key: "+ key);
				}
			}
			if(result==true){
			Assert.assertTrue(!result);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
	}

	 @Test(priority = 2)
	 public static void respnseTimeStats_GetLatestArticlesandCountParams() {
	 respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
