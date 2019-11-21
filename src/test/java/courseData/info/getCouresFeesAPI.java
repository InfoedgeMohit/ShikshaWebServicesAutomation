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
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCouresFeesAPI extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseFees";
	ArrayList<HashMap<String, String>> DBList = new ArrayList<HashMap<String, String>>();
	ArrayList<HashMap<String, String>> DBListforRankingLocation = new ArrayList<HashMap<String, String>>();
	HashMap<String, String> innerMap;
	String sqlQuery = null;
	String ExpectedActualListingId = null;
	String path = "data.courseFees.";

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "cityId", "localityId",
				"apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//GetCourseData.xlsx";
		PrintWriter pw = new PrintWriter(file +".txt");
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
	public void verifyGetCourseFeesdataApi(String courseId, String cityId,
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
		try {
			ResultSet rs = DatabaseCommon.getCourseFee(courseId);
			
			int resultCount = getCountOfResultSet(rs);
			if ( resultCount> 0) {
				while (rs.next()) {
					String dbFeeValue = String.valueOf(rs.getInt("fees_value"));
					String dbFeeUnit = String.valueOf(rs.getInt("fees_unit"));
					String dbFeeUnitName = DatabaseCommon
							.getCurrencyName(dbFeeUnit);
					String dbFeeType = rs.getString("fees_type");
					String dbFeeyear = String.valueOf(rs.getInt("batch_year"));
					String dbFeeCat = rs.getString("category");
					String dbFeePeriod = DatabaseCommon.getCourseFeePeriod(courseId);
					String period = rs.getString("period");
					String node = null;
					if (period.equals("overall")) {
						node = "totalFees";
					} else if (period.equals("otp")) {
						node = "oneTimePayment";
					} else {
						if (dbFeeType.equals("total")) {
							node = "fees";
						}
						else if (dbFeeType.equals("hostel")) {
							node = "hostelFees";
						}
					}

					String dbFeeOrder = String.valueOf(rs.getInt("order"));
					String dbTotalIncludesString = DatabaseCommon.getCourseFeeTotalIncludes(courseId);
					if (dbTotalIncludesString!=null&&dbTotalIncludesString.contains("Others")) {
						String dbOtherIncludes = dbTotalIncludesString.replace(
								"Others;other_text--", "");
						dbOtherIncludes = dbOtherIncludes.replace("|", ";");
						dbTotalIncludesString = dbOtherIncludes;
					}
					
					

					String jsonFeeYear = jsonPath.getString(path + "year");
					if (dbFeeyear.equals(jsonFeeYear)) {
						Reporter.log(
								"Fee year is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee year is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee year is not appear correct for courseId: "
										+ courseId, file);
						filewriter("Actual: " + jsonFeeYear + " Expected: "
								+ dbFeeyear, file);
						Assert.assertTrue(false, "Fee year is not appear correct for courseId: "+ courseId);
					}

					String jsonFeeUnit = jsonPath.getString(path + "feesUnit");
					if (dbFeeUnit.equals(jsonFeeUnit)) {
						Reporter.log(
								"Fee Unit is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee Unit is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee Unit is not appear correct for courseId: "
										+ courseId, file);
						filewriter("Actual: " + jsonFeeUnit + " Expected: "
								+ dbFeeUnit, file);
						Assert.assertTrue(false, "Fee Unit is not appear correct for courseId: "+ courseId);
					}

					String jsonFeeUnitName = jsonPath.getString(path
							+ "feesUnitName");
					if (dbFeeUnitName.equals(jsonFeeUnitName)) {
						Reporter.log(
								"Fee Unit Name is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee Unit Name is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee Unit Name is not appear correct for courseId: "
										+ courseId, file);
						filewriter("Actual: " + jsonFeeUnitName + " Expected: "
								+ dbFeeUnitName, file);
						Assert.assertTrue(false, "Fee Unit Name is not appear correct for courseId: "+ courseId);
					}
					if (dbFeePeriod != null) {
						String jsonFeePeriod = jsonPath.getString(path
								+ "fees.periodType");
						if (dbFeePeriod.equals(jsonFeePeriod)) {
							Reporter.log(
									"Fee Period is appear correct for courseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"Fee Period is not appear correct for courseId: "
											+ courseId, true);
							filewriter(
									"Fee Period is not appear correct for courseId: "
											+ courseId, file);
							filewriter("Actual: " + jsonFeePeriod
									+ " Expected: " + dbFeePeriod, file);
							Assert.assertTrue(false, "Fee Period is not appear correct for courseId: "+ courseId);
						}
					}

					String jsonFeeValue = jsonPath.getString(path + "fees."
							+ node + "." + dbFeeOrder + "." + dbFeeCat
							+ ".value");
					if (dbFeeValue.equals(dbFeeValue)) {
						Reporter.log(
								"Fee Value is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee Value is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee Value is not appear correct for courseId: "
										+ courseId + " period:" + dbFeePeriod
										+ " " + dbFeeOrder + " category: "
										+ dbFeeCat, file);
						filewriter("Actual: " + jsonFeeValue + " Expected: "
								+ dbFeeValue, file);
						Assert.assertTrue(false, "Fee Value is not appear correct for courseId: "+ courseId);
					}
					
					String jsonFeeCategory = jsonPath.getString(path + "fees."
							+ node + "." + dbFeeCat
							+ ".category");
					if(node.equals("fees")){
					jsonFeeCategory = jsonPath.getString(path + "fees."
								+ node + "." + dbFeeOrder + "." + dbFeeCat
								+ ".category");
					}
					
					if (dbFeeCat.equals(jsonFeeCategory)) {
						Reporter.log(
								"Fee Category is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee Category is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee Category is not appear correct for courseId: "
										+ courseId + " period:" + dbFeePeriod
										+ " " + dbFeeOrder + " category: "
										+ dbFeeCat, file);
						filewriter("Actual: " + jsonFeeCategory + " Expected: "
								+ dbFeeCat, file);
						Assert.assertTrue(false, "Fee Category is not appear correct for courseId: "+ courseId);
					}
					if(dbTotalIncludesString!=null){
					String[] dbTotalIncludesArr = dbTotalIncludesString.split(";");
					List<String> dbTotalIncludes = new ArrayList<String>();

					for (int i = 0; i < dbTotalIncludesArr.length; i++) {
						dbTotalIncludes.add(dbTotalIncludesArr[i]);
					}
					List<String> jsonTotalIncludes = new ArrayList<>(
							jsonPath.getList(path + "fees.totalIncludes"));
					if(dbTotalIncludes.equals(jsonTotalIncludes)){
						Reporter.log(
								"Fee Total Include is appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Fee Total Include is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Fee Total Include is not appear correct for courseId: "
										+ courseId + " period:" + dbFeePeriod
										+ " " + dbFeeOrder + " category: "
										+ dbFeeCat, file);
						filewriter("Actual: " + jsonTotalIncludes + " Expected: "
								+ dbTotalIncludes, file);
						Assert.assertTrue(false, "Fee Total Include is not appear correct for courseId: "+ courseId);
					}
					}
					else{
						if(jsonPath.getString(path + "fees.totalIncludes")==null){
							Reporter.log(
									"Fee Total Include is appear correct as appear 0 result in DB for courseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"Fee Total Include is not appear correct as appear 0 result in DB for courseId: "
											+ courseId, true);
							filewriter(
									"Fee Total Include is not appear correct as appear 0 result in DB for courseId: "
											+ courseId + " period:" + dbFeePeriod
											+ " " + dbFeeOrder + " category: "
											+ dbFeeCat, file);
							filewriter("Actual: " + jsonPath.getList(path + "fees.totalIncludes"), file);
							Assert.assertTrue(false, "Fee Total Include is not appear correct for courseId: "+ courseId);
						}
					}
				}
			} else {
				if (jsonPath.getString("data.courseFees") == null) {
					Reporter.log(
							"Course Fee appear null as appear 0 in db for courseId: "
									+ courseId, true);
				} else {
					Reporter.log(
							"Course Fee is not appear null as appear 0 in db for courseId: "
									+ courseId, true);
					filewriter(
							"Course Fee is not appear null as appear 0 in db for courseId: "
									+ courseId, file);
					filewriter(
							"Actual Result:"
									+ jsonPath.getString("data.courseFees"),
							file);
					Assert.assertTrue(false,
							"Course Fee is not appear null as appear 0 in db for courseId: "
									+ courseId);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetCourseFeesApi() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

}
