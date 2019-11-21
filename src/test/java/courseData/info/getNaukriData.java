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
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import common.DatabaseCommon;

public class getNaukriData extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getNaukriData";

	HashMap<String, String> innerMap;
	ArrayList<HashMap<String, String>> DBList;
	HashMap<String, HashMap<String, String>> outerMap;
	Map<String, HashMap<String, HashMap<String, String>>> dbListSpec = new HashMap<String, HashMap<String, HashMap<String, String>>>();

	String sqlQuery = null;
	String ExpectedActualListingId = null;

	@BeforeClass
	public void doItBeforeTest() throws FileNotFoundException {
		// apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("apifacade");
		// api path
		apiPath = "apigateway/courseapi/v1/info/getCourseNaukriData";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[] { "courseId", "instituteId",
				"apiResponseMsgExpected" };
		// test data file
		testDataFilePath = "//src//test//resources//courseData//info//getNaukriData.xlsx";
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
	public void verifyGetNaukridataApi(String courseId, String instituteId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
//		courseId = "130504";
//		instituteId="28499";
		// pass api params and hit apicompareCourseId
		// pass api params and hit api
		if (courseId.equals("ignoreHeader")){
			apiResponse = RestAssured.given().when().post(api).then().extract()
					.response();
		}
			else if(courseId.equals("ignoreInput")){
				if(instituteId.equals("ignoreInput")){
					apiResponse = RestAssured.given()
							.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
							.when().post(api).then()
							.extract().response();
				}
			else{
				apiResponse = RestAssured.given()
						.header("AUTHREQUEST", "INFOEDGE_SHIKSHA")
						.param("instituteId", instituteId).when().post(api).then()
						.extract().response();
			}
			}
		else {
			if(instituteId.equals("ignoreInput")){
			apiResponse = RestAssured.given()
					.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("courseId", courseId).when().post(api)
					.then().extract().response();
			}
			else{
				apiResponse = RestAssured.given()
						.header("AUTHREQUEST", "INFOEDGE_SHIKSHA").param("courseId", courseId).and()
						.param("instituteId", instituteId).when().post(api).then()
						.extract().response();
			}
		}
		
		

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if (statusCode == 200) { // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(courseId, jsonPath);
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

	public void verifyPostiveCases(String courseId, JsonPath jsonPath) {
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
		if (DatabaseCommon.getNaukricheckStatus(courseId) == true) {

			// Check Salary Bucket and value
			try {
				ResultSet rs = DatabaseCommon
						.getNaukriDataSalaryBucket(courseId);
				if (getCountOfResultSet(rs) > 0) {
					DBList = new ArrayList<HashMap<String, String>>();
					DBList.clear();
					float previousvalue = 0;
					while (rs.next()) {
						result = false;
						float value = rs.getFloat("ctc50");
						int totalEmp = rs.getInt("tot_emp");
						innerMap = new HashMap<String, String>();
						innerMap.put("bucket", rs.getString("exp_bucket"));
						innerMap.put("value", String.valueOf(value));

						if (previousvalue < value && totalEmp > 10) {
							result = true;
						}
						if (result == true) {
							DBList.add(innerMap);
						}
						previousvalue = value;
					}
					List<HashMap<String, String>> jsonSalaryBucket = new ArrayList<>(
							jsonPath.getList("data.salaryBuckets"));
					if (DBList.size() == jsonSalaryBucket.size()) {
						for (int i = 0; i < jsonSalaryBucket.size(); i++) {
							HashMap<String, String> jsonTemp = jsonSalaryBucket
									.get(i);
							HashMap<String, String> dbTemp = DBList.get(i);
							if (dbTemp.get("bucket").equals(
									jsonTemp.get("bucket"))
									&& dbTemp.get("value").equals(
											String.valueOf(jsonTemp
													.get("value")))) {
								Reporter.log(
										"Salary bucket value is appear correct for courseId: "
												+ courseId + " salary Bucket: "
												+ jsonTemp.get("bucket"), true);
							} else {
								Reporter.log(
										"Salary bucket value is not appear correct for courseId: "
												+ courseId + " salary Bucket: "
												+ jsonTemp.get("bucket"), true);
								filewriter(
										"Salary bucket value is appear correct for courseId: "
												+ courseId + " salary Bucket: "
												+ jsonTemp.get("bucket"), file);
								filewriter(
										"Actual Result: Bucket: "
												+ jsonTemp.get("bucket")
												+ " value: "
												+ String.valueOf(jsonTemp
														.get("value")), file);
								filewriter(
										"Actual Result: Bucket: "
												+ dbTemp.get("bucket")
												+ " value: "
												+ dbTemp.get("value"), file);
								Assert.assertTrue(false);
							}
						}
					} else {
						Reporter.log(
								"size of both salary bucket is not same check in getNaukriData.txt for courseId: "
										+ courseId, true);
						filewriter(
								"size of both salary bucket is not same check in getNaukriData.txt for courseId: "
										+ courseId, file);
						filewriter("db Map: " + DBList, file);
						filewriter("jsonList: " + jsonSalaryBucket, file);
						Assert.assertTrue(false);
					}

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			// Check alumniCountBySalaries
			try {
				ResultSet rs = DatabaseCommon
						.getNaukriDataAlumniCountBySalaries(courseId);
				if (getCountOfResultSet(rs) > 0) {
					while (rs.next()) {
						int dbTotalAlumani = rs.getInt("sums");
						int jsonTotalAlumani = jsonPath
								.getInt("data.alumniCountBySalaries");
						if (dbTotalAlumani == jsonTotalAlumani) {
							Reporter.log(
									"alumniCountBySalaries is appear correct for courseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"alumniCountBySalaries is not appear correct for courseId: "
											+ courseId, true);
							filewriter(
									"alumniCountBySalaries is not appear correct for courseId: "
											+ courseId, file);
							filewriter("Actual: " + jsonTotalAlumani
									+ " Expected: " + dbTotalAlumani, file);
							Assert.assertTrue(false);
						}
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			// Check alumniCountByPlacements
			try {
				ResultSet rs = DatabaseCommon
						.getNaurkiAlumniCountByPlacements(courseId);
				if (getCountOfResultSet(rs) > 0) {
					while (rs.next()) {
						int dbTotalAlumani = rs.getInt("sums");
						int jsonTotalAlumani = jsonPath
								.getInt("data.alumniCountByPlacements");
						if (dbTotalAlumani == jsonTotalAlumani) {
							Reporter.log(
									"alumniCountByPlacements is appear correct for courseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"alumniCountByPlacements is not appear correct for courseId: "
											+ courseId, true);
							filewriter(
									"alumniCountByPlacements is not appear correct for courseId: "
											+ courseId, file);
							filewriter("Actual: " + jsonTotalAlumani
									+ " Expected: " + dbTotalAlumani, file);
							Assert.assertTrue(false);
						}
					}
				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

			// check CompanyData in Specializtion
			try {
				ResultSet rs = DatabaseCommon
						.getNaurkiSpecializationCompanyData(courseId);
				if (getCountOfResultSet(rs) > 0) {
					dbListSpec.clear();
					while (rs.next()) {
						innerMap = new HashMap<String, String>();
						outerMap = new HashMap<String, HashMap<String, String>>();
					
						String specString = rs.getString("specialization");
						String comp_lableString = rs.getString("comp_label");
						String empCount = String.valueOf(rs.getInt("count"));
						innerMap.put("name", comp_lableString);
						innerMap.put("count", empCount);
						if (dbListSpec.get(specString) != null) {
							outerMap = dbListSpec.get(specString);

							outerMap.put(comp_lableString, innerMap);
							dbListSpec.put(specString, outerMap);

						} else {
							outerMap.put(comp_lableString, innerMap);
							dbListSpec.put(specString, outerMap);
						}
					}
					for (Entry<String, HashMap<String, HashMap<String, String>>> entry : dbListSpec
							.entrySet()) {
						String specKey = entry.getKey();
						HashMap<String, HashMap<String, String>> dbTempSpecList = entry
								.getValue();

						HashMap<Object, Object> jsonSpecTempMap = new HashMap<>(
								jsonPath.getMap("data.specializationData"));

						@SuppressWarnings("unchecked")
						List<HashMap<String, String>> jsonSpecTempMapCompanyList = (List<HashMap<String, String>>) parseJsonData(
								jsonSpecTempMap, specKey, "companyData");
						if (dbTempSpecList.size() == jsonSpecTempMapCompanyList
								.size()) {
							for (int i = 0; i < jsonSpecTempMapCompanyList
									.size(); i++) {
								HashMap<String, String> jsonTemp = jsonSpecTempMapCompanyList
										.get(i);
								for (Map.Entry<String, String> entryTemp : jsonTemp
										.entrySet()) {
									String keyName = entryTemp.getKey();
									String jsonValue = String.valueOf(entryTemp
											.getValue());
									HashMap<String, String> dbTemp = dbTempSpecList
											.get(jsonTemp.get("name"));
//									filewriter(courseId+" keyName: "+keyName+" jsontemp: "+entryTemp+"dbTemp: "+dbTemp, file);
									String dbValue = dbTemp.get(keyName);
									if (jsonValue.equalsIgnoreCase(dbValue)) {
										Reporter.log(
												"Company List for Company "
														+ keyName
														+ " is appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey, true);
									} else {
										Reporter.log(
												"Company List for Company "
														+ keyName
														+ " is not appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey, true);
										filewriter(
												"Company List for Company "
														+ keyName
														+ " is not appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey
														+ " CompanyName :"
														+ jsonTemp.get("name"),
												file);
										filewriter(
												"Actual Value: " + jsonValue,
												file);
										filewriter("Expected Value: ", file);
										Assert.assertTrue(result);
									}
								}
							}
						} else {
							Reporter.log(
									"Size of Company list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey, true);
							filewriter(
									"Size of Company list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey, file);
							filewriter("Actual Size: "
									+ jsonSpecTempMapCompanyList.size()
									+ " Expected size: " + dbListSpec.size(),
									file);
							Assert.assertTrue(false,
									"Size of Comapany list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey);
						}

					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			try {
				ResultSet rs = DatabaseCommon
						.getNaurkiSpecializationfunctionalDate(courseId);
				if (getCountOfResultSet(rs) > 0) {
					dbListSpec.clear();
					while (rs.next()) {
						innerMap = new HashMap<String, String>();
						outerMap = new HashMap<String, HashMap<String, String>>();
					

						String specString = rs.getString("specialization");
						String funAreaString = rs.getString("functional_area");
						String empCount = String.valueOf(rs.getInt("count"));
						innerMap.put("name", funAreaString);
						innerMap.put("count", empCount);
						if (dbListSpec.get(specString) != null) {
							outerMap = dbListSpec.get(specString);

							outerMap.put(funAreaString, innerMap);
							dbListSpec.put(specString, outerMap);

						} else {
							// DBList.add(innerMap);
							outerMap.put(funAreaString, innerMap);
//							outerList.add(outerMap);
							dbListSpec.put(specString, outerMap);
						}
					}
					for (Entry<String, HashMap<String, HashMap<String, String>>> entry : dbListSpec
							.entrySet()) {
						String specKey = entry.getKey();
						HashMap<String, HashMap<String, String>> dbTempSpecList = entry
								.getValue();

						HashMap<Object, Object> jsonSpecTempMap = new HashMap<>(
								jsonPath.getMap("data.specializationData"));

						@SuppressWarnings("unchecked")
						List<HashMap<String, String>> jsonSpecTempMapCompanyList = (List<HashMap<String, String>>) parseJsonData(
								jsonSpecTempMap, specKey, "functionalAreaData");
						if (dbTempSpecList.size() == jsonSpecTempMapCompanyList
								.size()) {
							for (int i = 0; i < jsonSpecTempMapCompanyList
									.size(); i++) {
								HashMap<String, String> jsonTemp = jsonSpecTempMapCompanyList
										.get(i);
								for (Map.Entry<String, String> entryTemp : jsonTemp
										.entrySet()) {
									String keyName = entryTemp.getKey();
									String jsonValue = String.valueOf(entryTemp
											.getValue());
									HashMap<String, String> dbTemp = dbTempSpecList
											.get(jsonTemp.get("name"));
//									filewriter(courseId+" keyName: "+keyName+" jsontemp: "+entryTemp+"dbTemp: "+dbTemp, file);
									String dbValue = dbTemp.get(keyName);
									if (jsonValue.equalsIgnoreCase(dbValue)) {
										Reporter.log(
												"Funcational Area List for Company "
														+ keyName
														+ " is appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey, true);
									} else {
										Reporter.log(
												"Funcational Area List for Company "
														+ keyName
														+ " is not appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey, true);
										filewriter(
												"Funcational Area List for Company "
														+ keyName
														+ " is not appear correct for courseId: "
														+ courseId + " Spec: "
														+ specKey
														+ " CompanyName :"
														+ jsonTemp.get("name"),
												file);
										filewriter(
												"Actual Value: " + jsonValue,
												file);
										filewriter("Expected Value: ", file);
										Assert.assertTrue(result);
									}
								}
							}
						} else {
							Reporter.log(
									"Size of Funcational Area list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey, true);
							filewriter(
									"Size of Funcational Area list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey, file);
							filewriter("Actual Size: "
									+ jsonSpecTempMapCompanyList.size()
									+ " Expected size: " + dbListSpec.size(),
									file);
							Assert.assertTrue(false,
									"Size of Funcational Area list in Specialization is appear different for courseId: "
											+ courseId + " Specialization: "
											+ specKey);
						}

					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Test(priority = 2)
	public static void respnseTimeStats_GetCoursedata() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
