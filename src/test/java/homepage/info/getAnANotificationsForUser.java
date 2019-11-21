package homepage.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.*;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.testng.*;
import org.testng.annotations.*;

import common.Common;

public class getAnANotificationsForUser extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result= false;
	HashMap<String, String> innerMap;
	ArrayList<HashMap<String, String>> UserNotificationList = new ArrayList<HashMap<String, String>>();
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/commonapi/v1/info/getAnANotificationsForUser";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "userId", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//homepage//info//getAnANotificationsForUser.xlsx";
		PrintWriter pw = new PrintWriter("Logs//getAnANotificationsForUser.txt");
		pw.close();
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
	public void verifygetAnANotificationsForUser(String UserId,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		UserId = "53106";
		// pass api params and hit apicompareCourseId
		if (UserId.equals("ignoreHeader")){
			return;
//			apiResponse = RestAssured.given().param("userId", userId).param("compareCourseIds[]",compareCourseId ).when().post(api).then().extract().response();
		}
		else if (UserId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api)
					.then().extract().response();
		}
		else{
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("userId", UserId).when().post(api)
					.then().extract().response();
		}
		

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath, UserId);
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

	public void verifyPostiveCases(JsonPath jsonPath, String userId) {
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
			UserNotificationList.clear();
		
			ResultSet rs = exceuteDbQuery("select * from notificationsInAppQueue where userId = "+userId+" order by modificationTime desc limit 20;", "shiksha");
			int userNotificationRowCount = getCountOfResultSet(rs);
			if(userNotificationRowCount>0){
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				while(rs.next()){
					innerMap = new HashMap<String, String>();
					String modificationtime = sdf.format(rs.getTimestamp("modificationTime"));
					innerMap.put("commandType",
							String.valueOf(rs.getInt("commandType")));
					innerMap.put("creationTime", sdf.format(rs.getTimestamp("creationTime")));
					innerMap.put("landingPage", String.valueOf(rs.getInt("landing_page")));
					innerMap.put("message", rs.getString("message"));
					innerMap.put("modificationTime", modificationtime);
					innerMap.put("notificationType", String.valueOf(rs.getInt("notificationType")));
					innerMap.put("primaryId", String.valueOf(rs.getInt("primaryId")));
					innerMap.put("primaryIdType", rs.getString("primaryIdType"));
					innerMap.put("readStatus", rs.getString("readStatus"));
					innerMap.put("secondaryData", rs.getString("secondaryData"));
					innerMap.put("secondaryDataId", String.valueOf(rs.getInt("secondaryDataId")));
					innerMap.put("title", rs.getString("title"));
					innerMap.put("trackingUrl", String.valueOf(rs.getInt("trackingUrl")));
					innerMap.put("userId",  String.valueOf(rs.getInt("userId")));
					innerMap.put("displayTime", displayTime(modificationtime));
					UserNotificationList.add(innerMap);
				}
				try{
					
					Reporter.log("UserNotificationList for UserId: "+userId, true);
					if (!jsonPath.getList("data.UserNotificationList")
							.isEmpty()) {
						List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>(jsonPath.getList("data"));
						if(jsonList.size()==UserNotificationList.size()){
						for (int j = 0; j < jsonList.size(); j++) {
							HashMap<String, String> jsonTempMap = jsonList
									.get(j);
							HashMap<String, String> dbTempMap = UserNotificationList
									.get(j);

							for (Map.Entry<String, String> jsonmap : jsonTempMap
									.entrySet()) {
								String jsonkey = jsonmap.getKey();
								String jsonvalue = String.valueOf(jsonmap
										.getValue());
								if(jsonvalue.equals("")){
									jsonvalue = "0";
								}
								String dbvalue = dbTempMap.get(jsonkey);
								if(dbvalue.equals("")){
									dbvalue = "0";
								}
								if (jsonvalue.equals(dbvalue)) {
									Reporter.log(
											"getAnANotificationsForUser Matches Successfully for key: "
													+ jsonkey, true);
									 Assert.assertTrue(true);
								} else {
									Reporter.log(
											"getAnANotificationsForUser Matches Failed for key: "
													+ jsonkey, true);
									filewriter("userId: "+userId,
											"Logs//getAnANotificationsForUser");
									filewriter("Actual Result: JsonKey: "
											+ jsonkey + " JsonValue: "
											+ jsonvalue,
											"Logs//getAnANotificationsForUser");
									filewriter("expected Result: dbKey: "
											+ jsonkey + " dbValue: " + dbvalue,
											"Logs//getAnANotificationsForUser");
									filewriter("Actual Result: " + jsonList,
											"Logs//getAnANotificationsForUser");
									filewriter("expected Result: "
											+ UserNotificationList,
											"Logs//getAnANotificationsForUser");
									result = true;
								}
							}
						}
					}
						else{
							Reporter.log("Size of both list for notification are not same for userId:"+userId, true);
							filewriter("Size of both list for notification are not same for userId:"+userId, "Logs//getAnANotificationsForUser");
							filewriter("Actual Size: "+jsonList.size()+" Actual result: "+jsonList, "Logs//getAnANotificationsForUser");
							filewriter("Expected Size: "+UserNotificationList.size()+" Expected result: "+UserNotificationList, "Logs//getAnANotificationsForUser");
							result = true;
						}
					}
				}
				catch(NullPointerException e){
					Reporter.log(" Map is null", true);
				}
			}
			else{
				Reporter.log("no result found in Db: db Count is: "+userNotificationRowCount, true);
				try{
					if(!jsonPath.getList("data.freeBannerList").isEmpty()){
					List<HashMap<String, String>>jsonList = new ArrayList<HashMap<String, String>>(jsonPath.getList("data"));
					Reporter.log("Map is appear for UserId "+userId+" JsonList: "+jsonList, true);
					result = true;
					}
				}
				catch(NullPointerException e){
					Reporter.log("Map is appear null: "+jsonPath.getList("data"), true);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		Assert.assertTrue(!result, "getAnANotificationsForUser Failed");
	}	

	 @Test(priority = 2)
	 public static void respnseTimeStats_getAnANotificationsForUser() {
	 respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
