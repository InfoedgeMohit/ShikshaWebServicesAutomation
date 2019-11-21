package homepage.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
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

import common.Common;

public class getHomePageFeaturedCollegeBanners extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result= false;
	HashMap<String, String> innerMap;
	ArrayList<HashMap<String, String>> PaidBannerList = new ArrayList<HashMap<String, String>>();
	ArrayList<HashMap<String, String>> freeBannerList = new ArrayList<HashMap<String, String>>();
	int freebannerLimit;
	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/homepageapi/v1/info/getHomePageFeaturedCollegeBanners";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "deviceType", "apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//homepage//info//getHomePageFeaturedCollegeBanners.xlsx";
		PrintWriter pw = new PrintWriter("Logs//GetHomePageFeaturedCollegeBanners.txt");
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
	public void verifyGetHomePageFeaturedCollegeBanners(String device,
			String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit apicompareCourseId
		if (device.equals("ignoreHeader")){
			return;
//			apiResponse = RestAssured.given().param("userId", userId).param("compareCourseIds[]",compareCourseId ).when().post(api).then().extract().response();
		}
		else if (device.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().post(api)
					.then().extract().response();
		}
		else{
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("deviceType", device).when().post(api)
					.then().extract().response();
		}
		

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(jsonPath, device);
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

	public void verifyPostiveCases(JsonPath jsonPath, String device) {
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
			
			ResultSet rs = exceuteDbQuery("select * from homePageFeaturedCollegeBanner where status ='live' and isDefault = 0 and showOn='"+device+"' and start_date<= curdate() and end_date>= curdate() limit 8;", "shiksha");
			int paidbannerRowCount = getCountOfResultSet(rs);
			freebannerLimit = 8-paidbannerRowCount;
			if(paidbannerRowCount>0){
				while(rs.next()){
					innerMap = new HashMap<String, String>();
					innerMap.put("id",
							String.valueOf(rs.getInt("id")));
					innerMap.put("bannerId", String.valueOf(rs.getInt("banner_id")));
					innerMap.put("collegeName",
							rs.getString("collegeName"));
					innerMap.put("imageUrl", rs.getString("image_url"));
					innerMap.put("isDefault", rs.getString("isDefault"));
					innerMap.put("targetUrl", "/trackCtr/"+rs.getInt("banner_id")+"?url="+rs.getString("target_url"));
					PaidBannerList.add(innerMap);
				}
				try{
					filewriter("PaidBannerList", "Logs//GetHomePageFeaturedCollegeBanners");
					if (!jsonPath.getList("data.paidBannerList")
							.isEmpty()) {
						List<HashMap<String, String>> JsonpaidBannerList = new ArrayList<HashMap<String, String>>(
								jsonPath.getList("data.paidBannerList"));
						List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>();
						for (int i = 0; i < JsonpaidBannerList.size(); i++) {
							HashMap<String, String> hashmap = JsonpaidBannerList
									.get(i);
							String imageUrl = hashmap.get("imageUrl");
							String targetUrl = hashmap.get("targetUrl");
							imageUrl = imageUrl
									.substring(imageUrl.indexOf(".com") + 4);
							targetUrl = targetUrl
									.substring(targetUrl.indexOf(".com/trackCtr/") + 4);
							hashmap.put("imageUrl", imageUrl);
							hashmap.put("targetUrl", targetUrl); 	
							jsonList.add(hashmap);
						}
						if(jsonList.size()==PaidBannerList.size()){
						for (int j = 0; j < jsonList.size(); j++) {
							HashMap<String, String> jsonTempMap = jsonList
									.get(j);
							HashMap<String, String> dbTempMap = PaidBannerList
									.get(j);

							for (Map.Entry<String, String> jsonmap : jsonTempMap
									.entrySet()) {
								String jsonkey = jsonmap.getKey();
								String jsonvalue = String.valueOf(jsonmap
										.getValue());
								String dbvalue = dbTempMap.get(jsonkey);

								if (jsonvalue.equals(dbvalue)) {
									Reporter.log(
											"GetHomePageFeaturedCollegeBanners Matches Successfully for key: "
													+ jsonkey, true);
									 Assert.assertTrue(true);
								} else {
									Reporter.log(
											"GetHomePageFeaturedCollegeBanners Matches Failed for key: "
													+ jsonkey, true);
									filewriter("Actual Result: JsonKey: "
											+ jsonkey + " JsonValue: "
											+ jsonvalue,
											"Logs//GetHomePageFeaturedCollegeBanners");
									filewriter("expected Result: dbKey: "
											+ jsonkey + " dbValue: " + dbvalue,
											"Logs//GetHomePageFeaturedCollegeBanners");
									filewriter("Actual Result: " + jsonList,
											"Logs//GetHomePageFeaturedCollegeBanners");
									filewriter("expected Result: "
											+ PaidBannerList,
											"Logs//GetHomePageFeaturedCollegeBanners");
									result = true;
								}
							}
						}
					}
						else{
							Reporter.log("Size of both list for PaidBanner are not same for Device:"+device);
							filewriter("Actual result: "+jsonList, "Logs//GetHomePageFeaturedCollegeBanners");
							filewriter("Expected result: "+PaidBannerList, "Logs//GetHomePageFeaturedCollegeBanners");
							result = true;
						}
					}
				}
				catch(NullPointerException e){
					Reporter.log("Paid Banner Map is null", true);
				}
				if(freebannerLimit>0){
					filewriter("FreeBannerlist", "Logs//GetHomePageFeaturedCollegeBanners");
					ResultSet rsFreebanner = exceuteDbQuery("select * from homePageFeaturedCollegeBanner where status ='live' and isDefault = 1 and showOn='"+device+"' and start_date<= curdate() and end_date>= curdate() limit "+freebannerLimit+";", "shiksha");
					int freebannerRowCount = getCountOfResultSet(rsFreebanner);
					if(freebannerRowCount>0){
						while(rs.next()){
							innerMap = new HashMap<String, String>();
							innerMap.put("id",
									String.valueOf(rs.getInt("id")));
							innerMap.put("bannerId", String.valueOf(rsFreebanner.getInt("banner_id")));
							innerMap.put("collegeName",
									rsFreebanner.getString("collegeName"));
							innerMap.put("imageUrl", rsFreebanner.getString("image_url"));
							innerMap.put("isDefault", rsFreebanner.getString("isDefault"));
							innerMap.put("targetUrl", "/trackCtr/"+rsFreebanner.getInt("banner_id")+"?url="+rsFreebanner.getString("target_url"));
							freeBannerList.add(innerMap);
						}
					List<HashMap<String, String>> JsonfreeBannerList = new ArrayList<HashMap<String, String>>(
							jsonPath.getList("data.freeBannerList"));
					List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>();
					for (int i = 0; i < JsonfreeBannerList.size(); i++) {
						HashMap<String, String> hashmap = JsonfreeBannerList
								.get(i);
						String imageUrl = hashmap.get("imageUrl");
						String targetUrl = hashmap.get("targetUrl");
						imageUrl = imageUrl
								.substring(imageUrl.indexOf(".com") + 4);
						targetUrl = targetUrl
								.substring(targetUrl.indexOf(".com/trackCtr") + 4);
						hashmap.put("imageUrl", imageUrl);
						hashmap.put("targetUrl", targetUrl); 	
						jsonList.add(hashmap);
					}
					if(jsonList.size()==freeBannerList.size()){
					for (int j = 0; j < jsonList.size(); j++) {
						HashMap<String, String> jsonTempMap = jsonList
								.get(j);
						HashMap<String, String> dbTempMap = PaidBannerList
								.get(j);

						for (Map.Entry<String, String> jsonmap : jsonTempMap
								.entrySet()) {
							String jsonkey = jsonmap.getKey();
							String jsonvalue = String.valueOf(jsonmap
									.getValue());
							String dbvalue = dbTempMap.get(jsonkey);

							if (jsonvalue.equals(dbvalue)) {
								Reporter.log(
										"freeBanner Matches Successfully for key: "
												+ jsonkey, true);
								 Assert.assertTrue(true);
							} else {
								Reporter.log(
										"freeBanner Matches Failed for key: "
												+ jsonkey, true);
								filewriter("Actual Result: JsonKey: "
										+ jsonkey + " JsonValue: "
										+ jsonvalue,
										"Logs//GetHomePageFeaturedCollegeBanners");
								filewriter("expected Result: dbKey: "
										+ jsonkey + " dbValue: " + dbvalue,
										"Logs//GetHomePageFeaturedCollegeBanners");
								filewriter("Actual Result: " + jsonList,
										"Logs//GetHomePageFeaturedCollegeBanners");
								filewriter("expected Result: "
										+ PaidBannerList,
										"Logs//GetHomePageFeaturedCollegeBanners");
								result = true;
							}
						}
					}
					}
					else{
						Reporter.log("Size of both list for Free are not same for Device:"+device);
						filewriter("Actual result: "+jsonList, "Logs//GetHomePageFeaturedCollegeBanners");
						filewriter("Expected result: "+freeBannerList, "Logs//GetHomePageFeaturedCollegeBanners");
						result = true;
					}
					}
					else{
						Reporter.log("no result found in Db: db Count is: "+freebannerRowCount);
						try{
							if(!jsonPath.getList("data.freeBannerList").isEmpty()){
							List<HashMap<String, String>>jsonFreeBannerList = new ArrayList<HashMap<String, String>>(jsonPath.getList("data.freeBannerList"));
							Reporter.log("Map is appear while there is limit appear: "+freebannerLimit+" JsonList: "+jsonFreeBannerList);
							result = true;
							}
						}
						catch(NullPointerException e){
							Reporter.log("Map is appear null for Free banner: "+jsonPath.getList("data.freeBannerList"));
						}
					}
				}
				else{
					try{
						List<HashMap<String, String>>jsonFreeBannerList = new ArrayList<HashMap<String, String>>(jsonPath.getList("data.freeBannerList"));
						if(jsonFreeBannerList.size()>0){
							Reporter.log("Map is appear while there is limit appear: "+freebannerLimit+" JsonList: "+jsonFreeBannerList);
							result = true;
						}
					}
					catch(NullPointerException e){
						Reporter.log("Map is appear null for Free banner: "+jsonPath.getList("data.freeBannerList"));
					}
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		Assert.assertTrue(!result, "getHomepageFeaturedCollegeBanners Failed");
	}

	 @Test(priority = 2)
	 public static void respnseTimeStats_GetHomePageFeaturedCollegeBanners() {
	 respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/") + 1));
	 }
}
