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
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getCourseEligibilityCutOffQuota extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseEligibilityCutOffData";
	ArrayList<HashMap<String, String>> DBList = new ArrayList<HashMap<String, String>>();
	ArrayList<HashMap<String, String>> DBListforRankingLocation = new ArrayList<HashMap<String, String>>();
	HashMap<String, String> innerMap;
	String sqlQuery = null;
	String ExpectedActualListingId = null;
	String path = "data.eligibility.";

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
	public void verifyGetCourseEligiblityCutOffQuotaApi(String courseId, String cityId,
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
		Map<String, String> categoryList = DatabaseCommon
				.getEligibiltyCategoryList(courseId);
		for (Map.Entry<String, String> catentry : categoryList.entrySet()) {
			String catvalue = catentry.getValue();
			try {
				String path = "data.eligibility.";
				HashMap<String, String> StandardMap = new HashMap<String, String>();
				StandardMap.put("X", "tenthDetails");
				StandardMap.put("XII", "twelthDetails");
				StandardMap.put("graduation", "graduationDetails");
				StandardMap.put("postgraduation", "postGraduationDetails");
				for (Map.Entry<String, String> entry : StandardMap.entrySet()) {
					String standardkey = entry.getKey();
					String standardvalue = entry.getValue();

					ResultSet rs = DatabaseCommon.getEligibilityScoreData(
							courseId, standardkey, catvalue);
					int resultCount = getCountOfResultSet(rs);
					if (resultCount > 0) {
						while (rs.next()) {
							String DBcategory = rs.getString("category");
							String DBunit = rs.getString("unit");
							String DBvalue = rs.getString("value");
							String DBmaxValue = rs.getString("max_value");

							String jsonScoreType = jsonPath.getString(path
									+ standardvalue + ".scoreType");
							if (DBunit.equals(jsonScoreType)) {
								Reporter.log(
										"Score type is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
							} else {
								Reporter.log(
										"Score type is not appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
								filewriter(
										"Score type is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, file);
								filewriter("Actual Result: " + jsonScoreType,
										file);
								filewriter("Expected Result: " + DBunit, file);
								Assert.assertTrue(false);
							}

							String jsoncategory = jsonPath.getString(path
									+ standardvalue + ".categoryWiseScores."
									+ catvalue + ".category");
							if (DBcategory.equals(jsoncategory)) {
								Reporter.log(
										"Category is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
							} else {
								Reporter.log(
										"Category is not appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
								filewriter(
										"Category is noy appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, file);
								filewriter("Actual Result: " + jsoncategory,
										file);
								filewriter("Expected Result: " + DBcategory,
										file);
								Assert.assertTrue(false);
							}

							String jsonValue = jsonPath.getString(path
									+ standardvalue + ".categoryWiseScores."
									+ catvalue + ".score");
							if (DBvalue.equals(jsonValue)) {
								Reporter.log(
										"Value is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
							} else {
								Reporter.log(
										"Value is not appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
								filewriter(
										"Value is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, file);
								filewriter("Actual Result: " + jsonValue, file);
								filewriter("Expected Result: " + DBvalue, file);
								Assert.assertTrue(false);
							}

							String jsonMaxValue = jsonPath.getString(path
									+ standardvalue + ".categoryWiseScores."
									+ catvalue + ".maxScore");
							if (DBmaxValue.equals(jsonMaxValue)) {
								Reporter.log(
										"Max_value is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
							} else {
								Reporter.log(
										"Max_value is not appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, true);
								filewriter(
										"Max_value is appear correct in Eligibility for CourseId: "
												+ courseId + " standard: "
												+ standardvalue + " category: "
												+ catvalue, file);
								filewriter("Actual Result: " + jsonMaxValue,
										file);
								filewriter("Expected Result: " + DBmaxValue,
										file);
								Assert.assertTrue(false);
							}
						}
					} else {
						if (jsonPath.getString(path + standardvalue) != null) {
							if (jsonPath.getString(path + standardvalue
									+ ".scoreType") != null) {
								Reporter.log(
										"Eligiblity is not appear null while count is 0 for CourseId: "
												+ courseId + " Standard: "
												+ standardkey + " category: "
												+ catvalue, true);
								filewriter(
										"Eligiblity is not appear null while count is 0 for CourseId: "
												+ courseId + " Standard: "
												+ standardkey + " category: "
												+ catvalue, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ standardvalue), file);
								Assert.assertTrue(false);
							} else {
								Reporter.log(
										"Eligiblity is appear Correct while count is 0 for CourseId: "
												+ courseId + " Standard: "
												+ standardkey + " category: "
												+ catvalue, true);
							}
						} else {
							Reporter.log(
									"Eligiblity is appear Correct while count is 0 for CourseId: "
											+ courseId + " Standard: "
											+ standardkey + " category: "
											+ catvalue, true);
						}
					}
				}
				ResultSet rs = DatabaseCommon.getEligibilityCutOffDataforQuota(
						courseId, catvalue);
				int resultCount = getCountOfResultSet(rs);
				if (resultCount > 0) {
					path = path + "twelthDetails.cutoff." + catvalue;
					while (rs.next()) {
						String DBquota = rs.getString("quota");
						String DBcategory = rs.getString("category");
						String DBCutoff = rs.getString("cut_off_value");

						if (DBquota.equals(jsonPath.getString(path + "."
								+ DBquota + ".category"))) {
							Reporter.log(
									"Category is appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
						} else {
							Reporter.log(
									"Category is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
							filewriter(
									"Category is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, file);
							filewriter(
									"Actual Result: "
											+ jsonPath.getString(path + "."
													+ DBquota + ".category"),
									file);
							filewriter("Expected Result: " + DBquota, file);
							Assert.assertTrue(false);
						}

						if (DBCutoff.equals(jsonPath.getString(path + "."
								+ DBquota + ".score"))) {
							Reporter.log(
									"Score is appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
						} else {
							Reporter.log(
									"Score is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
							filewriter(
									"Score is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, file);
							filewriter(
									"Actual Result: "
											+ jsonPath.getString(path
													+ DBcategory + "."
													+ DBquota + ".score"), file);
							filewriter("Expected Result: " + DBCutoff, file);
							Assert.assertTrue(false);
						}

						if ("100".equals(jsonPath.getString(path + "."
								+ DBquota + ".maxScore"))) {
							Reporter.log(
									"MaxScore is appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
						} else {
							Reporter.log(
									"MaxScore is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, true);
							filewriter(
									"MaxScore is not appear correct on courseId: "
											+ courseId + " category: "
											+ catvalue, file);
							filewriter(
									"Actual Result: "
											+ jsonPath.getString(path + "."
													+ DBquota + ".score"), file);
							filewriter("Expected Result: " + DBCutoff, file);
							Assert.assertTrue(false);
						}
					}
				} else {
					if (jsonPath.getString(path + "twelthDetails") != null) {
						if (jsonPath.getMap(path + "twelthDetails.cutoff")
								.size() == 0
								|| jsonPath.getMap(
										path + "twelthDetails.cutoff").size() == 0) {
							Reporter.log(
									"CutOff is appear as empty while result count is 0 as well so Passed for courseId"
											+ courseId
											+ " category: "
											+ catvalue, true);
						} else {
							Reporter.log(
									"CutOff is not appear empty while result count is 0 as well for courseId"
											+ courseId + " category: "
											+ catvalue, true);
							filewriter(
									"CutOff is not appear empty while result count is 0 as well for courseId"
											+ courseId + " category: "
											+ catvalue, file);
							filewriter(
									"Actual Result"
											+ jsonPath.getMap(path
													+ "twelthDetails.cutoff"),
									file);
							Assert.assertTrue(false);
						}
					} else {
						Reporter.log(
								"TwelthDetails is appear null while result count is 0 as well so Passed for courseId"
										+ courseId + " category: " + catvalue,
								true);
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}



	@Test(priority = 2)
	public static void respnseTimeStats_getCourseEligibilityCutOffQuota() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}

}
