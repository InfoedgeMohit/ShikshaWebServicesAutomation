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

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCourseDataRecuirtCompany extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseDataPlacements";
	ArrayList<HashMap<String, String>> dbList = new ArrayList<>();
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
	public void verifyGetCoursedataPlacementsApi(String courseId, String cityId,
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
			ResultSet rs = DatabaseCommon.getCourseRecurtmentCompany(courseId);
			if(getCountOfResultSet(rs)>0){
				dbList.clear();
				while(rs.next()){
					innerMap = new HashMap<String, String>();
					innerMap.put("companyName", rs.getString("company_name"));
					innerMap.put("logoUrl", rs.getString("logo_url"));
					dbList.add(innerMap);
				}
				List<HashMap<String, String>> jsonList = new ArrayList<>(jsonPath.getList("data.recruitmentCompanies"));
				if(jsonList.size()==dbList.size()){
					for(int i=0; i<jsonList.size();i++){
						HashMap<String, String> jsonTempMap = jsonList.get(i);
						String url = jsonTempMap.get("logoUrl");
						if(url!=null){
							url = url.substring(url.indexOf(".com/")+4);
							jsonTempMap.put("logoUrl", url);
						}
						HashMap<String, String> dbTempMap = dbList.get(i);
						for(Map.Entry<String, String> entry: jsonTempMap.entrySet()){
							String key = entry.getKey();
							String jsonValue = entry.getValue();
							String dbValue = dbTempMap.get(key);
							if(StringUtils.equals(jsonValue, dbValue)||(dbValue==null && jsonValue=="null")){
								Reporter.log("Recuritment Companies is appear correct for courseId: "+courseId+" Verified key: "+key, true);
							}
							else{
								Reporter.log("Recuritment Companies is not appear correct for courseId: "+courseId+" Verified key: "+key, true);
								filewriter("Recuritment Companies is not appear correct for courseId: "+courseId+" Verified key: "+key, file);
								filewriter("Actual Value: "+jsonValue, file);
								filewriter("Expected Value: "+dbValue, file);
								Assert.assertTrue(false, "Recuritment Companies is appear correct for courseId: "+courseId+" Verified key: "+key);
							}
						}
					}
				}
				else{
					Reporter.log("Size of Db and json is appear different for courseId: "+courseId, true);
					filewriter("Size of Db and json is appear different for courseId: "+courseId, file);
					filewriter("Actual: "+jsonList.size()+" Expected: "+dbList.size(), file);
				}
			}
			else{
				if(jsonPath.getList("data.recruitmentCompanies").size()==0){
					Reporter.log("Recruitmenet companies is appear as null while db return 0 for courseId: "+courseId, true);
				}
				else{
					Reporter.log("Recruitmenet companies is not appear correct while db return 0 for courseId: "+courseId, true);
					filewriter("Recruitmenet companies is not appear correct while db return 0 for courseId: "+courseId, file);
					filewriter("Actual: "+jsonPath.getList("data.recruitmentCompanies"), file);
					Assert.assertTrue(false, "Recruitmenet companies is not appear correct while db return 0 for courseId: "+courseId);
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			}
	}

	
	@Test(priority = 2)
	public static void respnseTimeStats_GetCoursedataPlacements() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}  
}
