package courseData.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import common.DatabaseCommon;

public class getCourseDataScholarShips extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseDataScholarships";
	ArrayList<HashMap<String, String>> DBList = new ArrayList<HashMap<String, String>>();
	HashMap<String, String> innerMap;
	String sqlQuery = null;
	String ExpectedActualListingId = null;

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "cityId", "localityId",
				"apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//GetCourseData.xlsx";
		PrintWriter pw = new PrintWriter(file + ".txt");
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
	public void verifyGetCoursedataScholarShipsApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		// pass api params and hit apicompareCourseId
		if (courseId.equals("ignoreHeader")) {
			return;
			// apiResponse = RestAssured.given().param("userId",
			// userId).param("compareCourseIds[]",compareCourseId
			// ).when().post(api).then().extract().response();
		} else if (courseId.equals("ignoreInput")) {
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").when().get(api)
					.then().extract().response();
		} else if (!courseId.equals("ignoreInput")) {
			if (!cityId.equals("ignoreInput")) {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId).then()
							.extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&cityId="+cityId+"&localityId="+localityId)
							.then().extract().response();
				}
			} else {
				if (localityId.equals("ignoreInput")) {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId)
							.then().extract().response();
				} else {
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().get(api+"?courseId="+courseId+"&localityId="+localityId)
							.then().extract().response();
				}
			}
		}

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();
		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(courseId, cityId, localityId, jsonPath);
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

	public void verifyPostiveCases(String courseId, String cityId,
			String localityId, JsonPath jsonPath) {
		// verify status is success
		String statusResponse = jsonPath.getString("status");
		if (statusResponse.contentEquals("success")) {
			Reporter.log(", Correct status [success]", true);
		} else {
			Reporter.log(", InCorrect status [" + statusResponse + "]", true);
			Assert.fail("InCorrect status [" + statusResponse + "]");
		}

		// verify message is null
		String messageResponse = jsonPath.getString("message");
		if (messageResponse == null) {
			Reporter.log(", Correct message [null]", true);
		} else {
			Reporter.log(", InCorrect message [" + messageResponse + "]", true);
			Assert.fail("InCorrect message [" + messageResponse + "]");
		}
		try{
		ResultSet rsParentId = DatabaseCommon.getParentInstituteId(courseId);
		if(getCountOfResultSet(rsParentId)>0){
			while(rsParentId.next()){
				int ParentId = rsParentId.getInt("parent_id");
				String ParentType = rsParentId.getString("parent_type");
				
				ResultSet rs = DatabaseCommon.getCourseScholarShips(ParentId, ParentType);
				DBList.clear();
				if(getCountOfResultSet(rs)>0){
					while(rs.next()){
						innerMap = new HashMap<String, String>();
						innerMap.put("listingId", String.valueOf(rs.getInt("listing_id")));
						innerMap.put("description", rs.getString("description"));
						innerMap.put("scholarship_type_id", String.valueOf(rs.getInt("scholarship_type_id")));
						innerMap.put("scholarship_type_name", DatabaseCommon.getbaseAttributeListName(rs.getInt("scholarship_type_id")));
						DBList.add(innerMap);
					}
					List<HashMap<String, String>> jsonScholarMap = new ArrayList<>(jsonPath.getList("data.scholarshipData"));
					if(DBList.size()==jsonScholarMap.size()){
						for(int i =0;i<jsonScholarMap.size();i++){
							HashMap<String, String> jsonTempMap = jsonScholarMap.get(i);
							HashMap<String, String> DBTempMap = DBList.get(i);
							for(Map.Entry<String, String> entry: jsonTempMap.entrySet()){
								String jsonkey = entry.getKey();
								String jsonValue = String.valueOf(entry.getValue());
								String dBValue = DBTempMap.get(jsonkey);
								if(jsonValue.equals(dBValue)){
									Reporter.log("ScholarShip value is appear correct for courseId: "+courseId+" Verified key: "+jsonkey, true);
								}
								else{
									Reporter.log("ScholarShip value is not appear correct for courseId: "+courseId+" Verified key: "+jsonkey, true);
									filewriter("ScholarShip value is not appear correct for courseId: "+courseId+" Verified key: "+jsonkey, file);
									filewriter("Actual Value: "+jsonValue, file);
									filewriter("Expected Value: "+dBValue, file);
									Assert.assertTrue(false, "ScholarShip value is appear correct for courseId: "+courseId+" Verified key: "+jsonkey);
								}
							}
						}
					}
					else{
						Reporter.log("Size of DB scholar Map and json Map is appear different so failed for courseId: "+courseId, true);
						filewriter("Size of DB scholar Map and json Map is appear different so failed for courseId: "+courseId, file);
						filewriter("Actual Value: "+jsonScholarMap, file);
						filewriter("Expected Value: "+DBList, file);
						Assert.assertTrue(false, "Size of DB scholar Map and json Map is appear different so failed for courseId: "+courseId);
					}
				}
				else{
					if(jsonPath.getList("data.scholarshipData").size()==0){
						Reporter.log("DB Result is 0 and Json Result is also 0 so, Passed for courseId: "+courseId, true);
					}
					else{
						Reporter.log("DB Result is 0 but json Returns result so failed for courseId: "+courseId, true);
						filewriter("DB Result is 0 but json Returns result so failed for courseId: "+courseId, file);
						filewriter("Json Map Size: "+jsonPath.getList("data.scholarshipData").size(), file);
						Assert.assertTrue(false, "DB Result is 0 but json Returns result so failed for courseId: "+courseId);
					}
				}
			}
		}
		}
		catch(SQLException e){
			e.printStackTrace();
			}
	}

	
	@Test(priority = 2)
	public static void respnseTimeStats_GetCoursedataScholarShips() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

}
