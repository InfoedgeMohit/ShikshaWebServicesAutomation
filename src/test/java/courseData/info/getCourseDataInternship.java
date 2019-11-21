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

public class getCourseDataInternship extends Common{


	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseDataIntern";
	HashMap<String, String> dbMap;
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
	public void verifyGetCoursedataInternApi(String courseId, String cityId,
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
			ResultSet rs = DatabaseCommon.getCourseInternData(courseId);
			if(getCountOfResultSet(rs)>0){
				while(rs.next()){
					dbMap = new HashMap<>();
					dbMap.put("type", rs.getString("type"));
					dbMap.put("course", rs.getString("course"));
					dbMap.put("course_type",rs.getString("course_type"));
					dbMap.put("avg_salary", rs.getString("avg_salary"));
					dbMap.put("batch_year", rs.getString("batch_year"));
					dbMap.put("max_salary", rs.getString("max_salary"));
					dbMap.put("median_salary",rs.getString("median_salary"));
					dbMap.put("min_salary", rs.getString("min_salary"));
					if(rs.getFloat("percentage_batch_placed")==0.0){
						dbMap.put("percentage_batch_placed", null);
					}else{
					dbMap.put("percentage_batch_placed", String.valueOf(rs.getFloat("percentage_batch_placed")));
					}
					dbMap.put("salary_unit", rs.getString("salary_unit"));
					dbMap.put("salary_unit_name", DatabaseCommon.getCurrencyName(rs.getString("salary_unit")));
					dbMap.put("total_international_offers", rs.getString("total_international_offers"));
					dbMap.put("report_url", rs.getString("report_url"));
					dbMap.put("max_international_salary",rs.getString("max_international_salary"));
					dbMap.put("max_international_salary_unit", rs.getString("max_international_salary_unit"));
					dbMap.put("max_international_salary_unit_name", DatabaseCommon.getCurrencyName(rs.getString("max_international_salary_unit")));
					
				Map<String, String> jsonMap =new HashMap<String, String>(jsonPath.getMap("data.intership"));
				String reportUrl = jsonMap.get("report_url");
				if(reportUrl!=null){
					reportUrl = reportUrl.substring(reportUrl.indexOf(".com/")+4);
					jsonMap.put("report_url", reportUrl);
				}
				if(jsonMap.size()==dbMap.size()){
					for(Map.Entry<String, String> entry : jsonMap.entrySet()){
						String key = entry.getKey();
						String jsonValue = String.valueOf(entry.getValue());
						String dbValue = dbMap.get(key);
						if(StringUtils.equals(jsonValue, dbValue)||(dbValue==null && jsonValue=="null")){
							Reporter.log("internship value is appear correct for courseId: "+courseId+" Verified key: "+key, true);
						}
						else{
							Reporter.log("internship value is not appear correct for courseId: "+courseId+" Verified key: "+key, true);
							filewriter("internship value is not appear correct for courseId: "+courseId+" Verified key: "+key, file);
							filewriter("Actual Value: "+jsonValue, file);
							filewriter("Expected Value: "+dbValue, file);
							Assert.assertTrue(false, "internship value is not appear correct for courseId: "+courseId+" Verified key: "+key);
						}
					}
				}
				else{
					Reporter.log("Json size and Db size is appear different for course: "+courseId, true);
					filewriter("Json size and Db size is appear different for course: "+courseId, file);
					filewriter("Actual Size: "+jsonMap.size()+" expected Size: "+dbMap.size(), file);
					Assert.assertTrue(false, "Json size and Db size is appear different for course: "+courseId);
				}
			}
			}
			else{
				if(jsonPath.getString("data.intership")==null){
					Reporter.log("intership is appear null while result from DB is 0 for Courseid: "+courseId, true);
				}
				else{
					Reporter.log("intership is not appear null while result from DB is 0 for Courseid: "+courseId, true);
					filewriter("intership is not appear null while result from DB is 0 for Courseid: "+courseId, file);
					filewriter("Actual result: "+jsonPath.getMap("data.intership"), file);
					Assert.assertTrue(false, "intership is not appear null while result from DB is 0 for Courseid: "+courseId);
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
