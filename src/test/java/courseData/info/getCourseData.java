package courseData.info;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import common.*;

import org.apache.commons.lang3.StringUtils;
import org.testng.*;
import org.testng.annotations.*;

import common.Common;

public class getCourseData extends Common {
	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();
	boolean result;
	String file = "Logs//getCourseData";
	ArrayList<HashMap<String, String>> DBList = new ArrayList<HashMap<String, String>>();
	ArrayList<HashMap<String, String>> DBListforRankingLocation = new ArrayList<HashMap<String, String>>();
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
				"apiResponseMsgExpected","query","ActualListingId"};
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
	public void verifyGetCoursedataApi(String courseId, String cityId,
			String localityId, String apiResponseMsgExpected, String query, String actuallistingId) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		sqlQuery = query;
		ExpectedActualListingId = actuallistingId;
		
		
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
		result = verifyBasicInfo(jsonPath, courseId, cityId, localityId);
		Assert.assertTrue(!result,
				"verifyBasicInfo Failed please Check getCourseData.txt for more details..");
		result = verifylocationDetails(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifylocationDetails Failed please Check getCourseData.txt for more details..");
		result = verifyEntryTypeInfo(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifyEntryTypeInfo Failed please Check getCourseData.txt for more details..");
		result = verifymediumOfInstruction(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifymediumOfInstruction Failed please Check getCourseData.txt for more details..");
		result = verifyHighlights(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifyHighlights Failed please Check getCourseData.txt for more details..");
		result = verifyCourseStructure(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifyCourseStructure Failed please Check getCourseData.txt for more details..");
		result = verifyCurrentLocationandContactDetails(jsonPath, courseId, cityId, localityId);
		Assert.assertTrue(!result,
				"verifyCurrentLocationandContactDetails Failed please Check getCourseData.txt for more details..");
		result = verifyRankingBySourceAndLocation(jsonPath, courseId);
		Assert.assertTrue(!result,
				"verifyRankingBySourceAndLocation Failed please Check getCourseData.txt for more details..");
	}

	public boolean verifyBasicInfo(JsonPath jsonPath, String courseId, String cityId,
			String localityId) {
		 boolean result = false;
		try {
			ResultSet rs = exceuteDbQuery(
					"select sc.education_type, sc.affiliated_university_id, sc.affiliated_university_name, sc.affiliated_university_scope, sc.time_of_learning, si.accreditation as instituteaccreditation,sc.parent_id ,sc.duration_disclaimer, "
							+ "sc.duration_unit, sc.duration_value, sc.difficulty_level, lm.listing_type_id, si.name "
							+ "as instituteName,lm.pack_type, lm.listing_seo_url as instituteUrl, sc.course_variant, "
							+ "sc.course_id, sc.name as coursename from shiksha_institutes as si join shiksha_courses as sc"
							+ " on si.listing_id = sc.parent_id join listings_main as lm on lm.listing_type_id = sc.course_id "
							+ "where sc.course_id = "
							+ courseId
							+ " and sc.status ='live' and si.status = 'live' and lm.status = 'live' "
							+ "and lm.listing_type = 'course';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					// Fetch Data From DB with respect to course Id
					String DBInstituteId = String.valueOf(rs
							.getInt("parent_id"));
					String DBInstituteName = rs.getString("instituteName");
					String DBCourseId = String.valueOf(rs.getInt("course_id"));
					String DBCourseName = rs.getString("coursename");
					String DBPackType = String.valueOf(rs.getInt("pack_type"));
					String DBEducatioTypeId = String.valueOf(rs.getInt("education_type")); 
					String DBDifficultylevelId = rs
							.getString("difficulty_level");
					if (DBDifficultylevelId == null) {
						DBDifficultylevelId = "0";
					}
					String DBCourseVariant = String.valueOf(rs
							.getInt("course_variant"));
					String DBDurationUnit = rs.getString("duration_unit");
					String DBDurationValue = String.valueOf(rs
							.getString("duration_value"));
					int DBDurationDisclaimervalue = rs
							.getInt("duration_disclaimer");
					String DBinstituteaccreditation = rs
							.getString("instituteaccreditation");
					String DBDuratonDisclaimer;
					if (DBDurationDisclaimervalue == 1) {
						DBDuratonDisclaimer = "true";
					} else {
						DBDuratonDisclaimer = "null";
					}
					String DBTimeofLearning = rs.getString("time_of_learning");
					String DBAffiliationId = String.valueOf(rs
							.getInt("affiliated_university_id"));
					String DBAffiliationName = rs
							.getString("affiliated_university_name");
					String DBAffiliationScope = rs
							.getString("affiliated_university_scope");

					// verify Parent Institute name of Course
					if (DBInstituteName.equals(jsonPath
							.getString("data.instituteName"))) {
						Reporter.log(
								"Institue Name is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Institue Name is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Institue Name is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.instituteName")
										+ " Expected Result: "
										+ DBInstituteName, file);
						result = true;
					}

					// Verify course id in Json Object
					if (DBCourseId.equals(jsonPath.getString("data.courseId"))) {
						Reporter.log(
								"Course Id is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Course Id is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Course Id is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.courseId")
										+ " Expected Result: " + DBCourseId,
								file);
						result = true;
					}

					// verify Parent Institute Id
					if (DBInstituteId.equals(jsonPath
							.getString("data.instituteId"))) {
						Reporter.log(
								"Institue Id is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Institue Id is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Institue Id is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.instituteId")
										+ " Expected Result: " + DBInstituteId,
								file);
						result = true;
					}

					// verify Course Name
					if (DBCourseName.equals(jsonPath
							.getString("data.courseName"))) {
						Reporter.log(
								"Institue Id is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Institue Id is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Institue Id is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.courseName")
										+ " Expected Result: " + DBCourseName,
								file);
						result = true;
					}

					// verify PackType of Course
					if (DBPackType.equals(jsonPath.getString("data.packType"))) {
						Reporter.log(
								"pack Type is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"pack Type is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"pack Type is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.packType")
										+ " Expected Result: " + DBPackType,
								file);
						result = true;
					}

					// verify Course Variant
					if (DBCourseVariant.equals(jsonPath
							.getString("data.courseVariant"))) {
						Reporter.log(
								"course Variant is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Course Variant is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Course Variant is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.courseVariant")
										+ " Expected Result: "
										+ DBCourseVariant, file);
						result = true;
					}

					// Verify Difficulty Level and name
					if (!DBDifficultylevelId.equals("0")) {
						try {
							if (!jsonPath.getMap("data.difficultyLevel")
									.isEmpty()) {

								if (DBDifficultylevelId.equals(jsonPath
										.getString("data.difficultyLevel.id"))) {
									Reporter.log(
											"Difficulty level Id is appear correct for CourseId:"
													+ courseId, true);
								} else {
									Reporter.log(
											"Difficulty level Id is not appear correct for CourseId:"
													+ courseId, true);
									filewriter(
											"Difficulty level Id is not appear correct for CourseId:"
													+ courseId, file);
									filewriter(
											"Actual Result: "
													+ jsonPath.getString("data.difficultyLevel.id")
													+ " Expected Result: "
													+ DBDifficultylevelId, file);
									result = true;
								}
								ResultSet rsDifficultyName = DatabaseCommon.baseAttributeList(DBDifficultylevelId);
								if (getCountOfResultSet(rsDifficultyName) > 0) {
									while (rsDifficultyName.next()) {
										String DBDifficultyLevelName = rsDifficultyName
												.getString("value_name");
										if (DBDifficultyLevelName
												.equals(jsonPath
														.getString("data.difficultyLevel.name"))) {
											Reporter.log(
													"Difficulty level Name is appear correct for CourseId:"
															+ courseId, true);
										} else {
											Reporter.log(
													"Difficulty level Name is not appear correct for CourseId:"
															+ courseId, true);
											filewriter(
													"Difficulty level Name is not appear correct for CourseId:"
															+ courseId, file);
											filewriter(
													"Actual Result: "
															+ jsonPath
																	.getString("data.difficultyLevel.name")
															+ " Expected Result: "
															+ DBDifficultyLevelName,
													file);
											result = true;
										}
									}
								}
							}
						} catch (NullPointerException e) {
							Reporter.log("DifficultyLevel Appear to be Empty",
									true);
							String jsonDifficultyLevel = jsonPath
									.getString("data.difficultyLevel");
							if (jsonDifficultyLevel == null) {
								Reporter.log("Diffculty Appear null in DB",
										true);
							} else {
								Reporter.log("Diffculty Appear not null in DB"
										+ jsonDifficultyLevel, true);
								filewriter(
										"Diffculty Appear not null in DB for courseId: "
												+ courseId, file);
								filewriter("Diffculty Appear not null in DB: "
										+ DBDifficultylevelId, file);
								result = true;
							}
						}
					} else {
						String jsonDifficultyLevelId = jsonPath
								.getString("data.difficultyLevel");
						if(DBDifficultylevelId.equals("0")){
							DBDifficultylevelId = null;
						}
						if (StringUtils.equalsIgnoreCase(DBDifficultylevelId,jsonDifficultyLevelId)) {
							Reporter.log(
									"Difficulty level Id is appear correct for CourseId:"
											+ courseId, true);
						} else {
							Reporter.log(
									"Difficulty level Id is not appear correct for CourseId:"
											+ courseId, true);
							filewriter(
									"Difficulty level Id is not appear correct for CourseId:"
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath.getString("data.difficultyLevel")
											+ " Expected Result: "
											+ DBDifficultylevelId, file);
							result = true;
						}
					}

					// Verify Duration Unit
					if (DBDurationUnit.equals(jsonPath
							.getString("data.durationUnit"))) {
						Reporter.log(
								"Duration Unit is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Duration Unit is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Duration Unit is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath
												.getString("data.durationUnit")
										+ " Expected Result: " + DBDurationUnit,
								file);
						result = true;
					}

					// verify Duration Value
					if (DBDurationValue.equals(jsonPath
							.getString("data.durationValue"))) {
						Reporter.log(
								"Duration Value is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Duration Value is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Duration Value is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.durationValue")
										+ " Expected Result: "
										+ DBDurationValue, file);
						result = true;
					}

					// Verify Duration Disclaimer
					if (DBDuratonDisclaimer.trim().equals(
							String.valueOf(jsonPath
									.getString("data.showDurationDisclaimer")))) {
						Reporter.log(
								"Duration Disclaimer is appear correct for CourseId:"
										+ courseId, true);
					} else {
						Reporter.log(
								"Duration Disclaimer is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Duration Disclaimer is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.showDurationDisclaimer")
										+ " Expected Result: "
										+ DBDuratonDisclaimer, file);
						result = true;
					}

					// Verify Institute Url
					ResultSet rsInstituteUrl = exceuteDbQuery(
							"select * from listings_main where listing_type_id ="
									+ DBInstituteId
									+ " and listing_type = 'institute' and status = 'live';",
							"shiksha");
					if (getCountOfResultSet(rsInstituteUrl) > 0) {
						while (rsInstituteUrl.next()) {
							String DBInstituteUrl = rsInstituteUrl
									.getString("listing_seo_url");
							String jsonInstituteUrl = jsonPath
									.getString("data.instituteUrl");
//							 jsonInstituteUrl =
//							 jsonInstituteUrl.substring(jsonInstituteUrl.indexOf(".com/")+4);
							if (DBInstituteUrl.equals(jsonInstituteUrl)) {
								Reporter.log(
										"Institute Url is appear correct for CourseId:"
												+ courseId, true);
							} else {
								Reporter.log(
										"Institute Url is not appear correct for CourseId:"
												+ courseId, true);
								filewriter(
										"Institute Url is not appear correct for CourseId:"
												+ courseId, file);
								filewriter(
										"Actual Result: " + jsonInstituteUrl
												+ " Expected Result: "
												+ DBInstituteUrl, file);
								result = true;
							}
						}
					}
					// Verify Institute Accrediation
					try {
						DBinstituteaccreditation = DBinstituteaccreditation
								.substring(DBinstituteaccreditation
										.indexOf("grade_") + 6);
						DBinstituteaccreditation = DBinstituteaccreditation
								.toUpperCase();
					} catch (NullPointerException e) {
						DBinstituteaccreditation = "null";
					} finally {
						if (DBinstituteaccreditation
								.equals(String.valueOf(jsonPath
										.getString("data.instituteAccrediation")))) {
							Reporter.log(
									"Institute accreditation is appear correct for CourseId:"
											+ courseId, true);
						} else {
							Reporter.log(
									"Institute accreditation is not appear correct for CourseId:"
											+ courseId, true);
							filewriter(
									"Institute accreditation is not appear correct for CourseId:"
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath.getString("data.instituteAccrediation")
											+ " Expected Result: "
											+ DBinstituteaccreditation, file);
							result = true;
						}
					}

					// Verify Course Accerditation
					ResultSet rsCourseAcc = exceuteDbQuery(
							"select bal.value_id,bal.value_name from base_attribute_list as bal join shiksha_courses_additional_info as scdi on bal.value_id = scdi.attribute_value_id where scdi.course_id ="
									+ courseId
									+ " and scdi.info_type ='recognition' and scdi.status ='live' and bal.value_name ='NBA';",
							"shiksha");
					if (getCountOfResultSet(rsCourseAcc) > 0) {
						try {
							if ("55".equals(jsonPath
									.getString("data.courseAccreditation.id"))
									&& "NBA".equals(jsonPath
											.getString("data.courseAccreditation.name"))) {
								Reporter.log(
										"Course Acceditation is appear correct",
										true);
							} else {
								Reporter.log(
										"Course Accreditation is not appear correct: "
												+ courseId, true);
								filewriter(
										"Course Accreditation is not appear correct for course: "
												+ courseId, file);
								filewriter(
										"Actual Result: id: "
												+ jsonPath
														.getString("data.courseAccreditation.id")
												+ " Name: "
												+ jsonPath
														.getString("data.courseAccreditation.name"),
										file);
								filewriter("Expected Result: id: 55 Name: NBA",
										file);
								result = true;
							}
						} catch (NullPointerException e) {
							Reporter.log(
									"Course accreditation is appear null in Json",
									true);
							result = true;
						}
						continue;
					} else {
						if (StringUtils.equals("null",String.valueOf(jsonPath.getString(
								"data.courseAccreditation")))) {
							Reporter.log(
									"Course Acceditation is appear correct",
									true);
						} else {
							Reporter.log(
									"Course Accreditation is not appear correct: "
											+ courseId, true);
							filewriter(
									"Course Accreditation is not appear correct: "
											+ courseId, file);
							filewriter(
									"Actual Result: id: "
											+ jsonPath
													.getString("data.courseAccreditation"),
									file);
							filewriter("Expected Result: id: 55 Name: NBA",
									file);
							result = true;
						}
					}

					// Verify recognitions
					ResultSet rsrecognition = exceuteDbQuery(
							"select bal.value_id,bal.value_name from base_attribute_list as bal join shiksha_courses_additional_info as scdi on bal.value_id = scdi.attribute_value_id where scdi.course_id ="
									+ courseId
									+ " and scdi.info_type ='recognition' and scdi.status ='live' and bal.value_name !='NBA' order by bal.value_name asc;",
							"shiksha");
					int recognition = getCountOfResultSet(rsrecognition);
					if (recognition > 0) {
						DBList.clear();
						while (rsrecognition.next()) {
							innerMap = new HashMap<String, String>();
							innerMap.put("id", String.valueOf(rsrecognition
									.getInt("value_id")));
							innerMap.put("name",
									rsrecognition.getString("value_name"));
							DBList.add(innerMap);
						}
						try {
							List<HashMap<String, String>> Jsonrecognitation = new ArrayList<HashMap<String, String>>(
									jsonPath.getList("data.recognitions"));
							if (DBList.size() == Jsonrecognitation.size()) {
								for (int j = 0; j < Jsonrecognitation.size(); j++) {
									HashMap<String, String> jsonTempMap = Jsonrecognitation
											.get(j);
									HashMap<String, String> dbTempMap = DBList
											.get(j);

									for (Map.Entry<String, String> jsonmap : jsonTempMap
											.entrySet()) {
										String jsonkey = jsonmap.getKey();
										String jsonvalue = String
												.valueOf(jsonmap.getValue());
										String dbvalue = dbTempMap.get(jsonkey);

										if (jsonvalue.equals(dbvalue)) {
											Reporter.log(
													"Recogintion Matches Successfully for key: "
															+ jsonkey+" for Course Id: "+courseId, true);
										} else {
											Reporter.log(
													"Recogintion Matches Failed for key: "
															+ jsonkey+" for Course Id: "+courseId, true);
											filewriter(
													"Actual Result: JsonKey: "
															+ jsonkey
															+ " JsonValue: "
															+ jsonvalue, file);
											filewriter(
													"expected Result: dbKey: "
															+ jsonkey
															+ " dbValue: "
															+ dbvalue, file);
											filewriter("Actual Result: "
													+ Jsonrecognitation, file);
											filewriter("expected Result: "
													+ DBList, file);
											result = true;
										}
									}
								}
							} else {
								Reporter.log(
										"Size of both list for recognitions are not same for CourseId:"
												+ courseId, true);
								filewriter(
										"Size of both list for recognitions are not same for CourseId:"
												+ courseId, file);
								filewriter("Actual result: "
										+ Jsonrecognitation, file);
								filewriter("Expected result: " + DBList,
										file);
								result = true;
							}
						} catch (NullPointerException e) {
							e.printStackTrace();
						}
					} else {
						if (jsonPath.getList("data.recognitions").isEmpty()) {
							Reporter.log(
									"recognitions is appear correct for Course: "
											+ courseId, true);
						} else {
							Reporter.log(
									"recognitions is not appear correct for Course: "
											+ courseId, true);
							filewriter(
									"Recognitions Is not appear correct for courseId: "
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath
													.getList("data.recognitions"),
									file);
							result = true;
						}
					}

					// verify Time of Learning
					try {
						if (!DBTimeofLearning.equals("null")) {
							ResultSet rstimeofLearning =  DatabaseCommon.baseAttributeList(DBTimeofLearning);
							if (getCountOfResultSet(rstimeofLearning) > 0) {
								while (rstimeofLearning.next()) {
									String DBTimeofLearningName = rstimeofLearning
											.getString("value_name");
									if (DBTimeofLearning
											.equals(jsonPath
													.getString("data.timeOfLearning.id"))
											&& DBTimeofLearningName
													.equals(jsonPath
															.getString("data.timeOfLearning.name"))) {
										Reporter.log(
												"timeOfLearning is appear correct for Course: "
														+ courseId, true);
									} else {
										Reporter.log(
												"timeOfLearning is appear correct for Course: "
														+ courseId, true);
										filewriter(
												"timeOfLearning Is not appear correct for courseId: "
														+ courseId, file);
										filewriter(
												"Actual Result: "
														+ jsonPath
																.getList("data.timeOfLearning"),
												file);
										filewriter("Expected Result: id: "
												+ DBTimeofLearning + " Name: "
												+ DBTimeofLearningName, file);
										result = true;
									}
								}
							}
						}
					} catch (NullPointerException e) {
						if ("null".equals(String.valueOf(jsonPath
								.getString("data.timeOfLearning")))) {
							Reporter.log(
									"TimeofLearning is appear correct for Courseid: "
											+ courseId, true);
						} else {
							Reporter.log(
									"TimeofLearning is not appear correct for Courseid: "
											+ courseId, true);
							filewriter("TimeofLearning is not appear correct for Courseid: "
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath
													.getList("data.timeOfLearning"),
									file);
						}
					}

					// verify Affiliations Data
					if (!DBAffiliationId.equals("0")) {
						ResultSet rsAffurl = exceuteDbQuery("select listing_seo_url from listings_main where listing_type_id = "+DBAffiliationId+" and listing_type in('university_national', 'institute','university') and status = 'live';", "shiksha");
						while(rsAffurl.next()){
							String DBAffiliationUrl = rsAffurl.getString("listing_seo_url");
							String jsonAffiliationUrl = jsonPath.getString("data.affiliationData.url");
//							jsonAffiliationUrl =jsonAffiliationUrl.substring(jsonAffiliationUrl.indexOf(".com/")+4);
						if (DBAffiliationId
								.equals(jsonPath
										.getString("data.affiliationData.universityId"))
								&& DBAffiliationName
										.equals(jsonPath
												.getString("data.affiliationData.name"))
								&& DBAffiliationScope
										.equals(jsonPath
												.getString("data.affiliationData.scope"))
								&&	DBAffiliationUrl.equals(jsonAffiliationUrl)) {
							Reporter.log(
									"Affiliation data is appear correct for CourseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"Affiliation Data not appear correct fot Course Id:"
											+ courseId, true);
							filewriter(
									"Affiliation data is not appear correct for Course Id: "
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath
													.getMap("data.affiliationData"),
									file);
							filewriter("Expected Result: " + DBAffiliationId
									+ " : " + DBAffiliationName + " : "
									+ DBAffiliationScope+" : "+DBAffiliationUrl, file);
							result = true;
						}
					}
					} else {
						if ("null".equals(String.valueOf(jsonPath
								.getString("data.affiliationData")))) {
							Reporter.log(
									"Affiliation data is appear correct for CourseId: "
											+ courseId, true);
						} else {
							Reporter.log(
									"Affiliation Data not appear correct fot Course Id:"
											+ courseId, true);
							filewriter(
									"Affiliation data is not appear correct for Course Id: "
											+ courseId, file);
							filewriter(
									"Actual Result: "
											+ jsonPath
													.getList("data.affiliationData"),
									file);
							result = true;
						}
					}
					
					ResultSet rsEducationType = DatabaseCommon.baseAttributeList(DBEducatioTypeId);
					if(getCountOfResultSet(rsEducationType)>0){
						while(rsEducationType.next()){
							String DBEducationTypeName = rsEducationType.getString("value_name");
							if(DBEducatioTypeId.equals(jsonPath.getString("data.educationType.id"))&&DBEducationTypeName.equalsIgnoreCase(jsonPath.getString("data.educationType.name"))){
								Reporter.log("Education type is appear correct for CourseId: "+courseId, true);
							}
							else{
								Reporter.log("education type is not appear correct for courseId: "+courseId, true);
								filewriter("Education type is not appear correct for courseId: "+courseId, file);
								filewriter("Actual Result: id: "+jsonPath.getString("data.educationType.id")+" Name: "+jsonPath.getString("data.educationType.name"), file);
								filewriter("Expected Result: id: "+DBEducatioTypeId +" Name: "+DBEducationTypeName, file);
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		return result;
	}

	public boolean verifylocationDetails(JsonPath jsonPath, String courseId){
		 boolean result = false;
		try{
			
			//Fetching Dataof Locations from DB
			ResultSet rs = exceuteDbQuery(
					"select sil.listing_location_id, st.state_id, st.state_name, cct.city_id, cct.city_name, sil.locality_id"
							+ " ,scl.is_main from shiksha_institutes_locations as sil join shiksha_courses_locations as scl"
							+ " on sil.listing_location_id = scl.listing_location_id join stateTable as st"
							+ " on st.state_id = sil.state_id join countryCityTable as cct on cct.city_id = sil.city_id"
							+ " where scl.course_id ="
							+ courseId
							+ " and sil.status ='live' and scl.status ='live';",
					"shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					String DBListingLocationId = String.valueOf(rs.getInt("listing_location_id"));
					String DBStateId = String.valueOf(rs.getInt("state_id"));
					String DBStateName = rs.getString("state_name");
					String DBCityId = String.valueOf(rs.getInt("city_id"));
					String DBCityName = rs.getString("city_name");
					String DBLocalityId = String.valueOf(rs.getInt("locality_id"));
					int DBisMain = rs.getInt("is_main");
					String DBmain;
					if(DBisMain == 1){
						DBmain = "true";
					}
					else if(DBisMain ==0){
						DBmain = "false";
					}
					else{
						DBmain ="false";
						Reporter.log("is main Appear from db is nor 1 neither 0: "+DBisMain, true);
					}
					String DBLocalityName = "null";
					if(!DBLocalityId.equals("0")){
						ResultSet rsLocalityName = exceuteDbQuery("select localityName from localityCityMapping where localityId = "+DBLocalityId+" and status = 'live';", "shiksha");
						while(rsLocalityName.next()){
							DBLocalityName = rsLocalityName.getString("localityName");
							
							//verify Listing Location Id with json
							if (DBListingLocationId.equals(jsonPath
									.getString("data.locations." + DBListingLocationId
											+ ".listingLocationId"))) {
								Reporter.log(
										"Listing Location Id appear correct for courseId: "
												+ courseId, true);
							} else {
								Reporter.log("Listing Location Id is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"Listing Location Id is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".listingLocationId")
												+ " Expected Result: "
												+ DBListingLocationId, file);
								result = true;
							}

							if (DBStateId.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".stateId"))) {
								Reporter.log("State Id appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("State Id is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"State Id is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".stateId")
												+ " Expected Result: " + DBStateId,
										file);
								result = true;
							}

							if (DBCityId.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".cityId"))) {
								Reporter.log("City  Id appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("City Id is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"City Id is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".cityId")
												+ " Expected Result: " + DBCityId, file);
								result = true;
							}

							if (DBLocalityId.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".localityId"))) {
								Reporter.log("Locality Id appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("Locality Id is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"Locality Id is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".localityId")
												+ " Expected Result: " + DBLocalityId,
										file);
								result = true;
							}

							if (DBStateName.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".stateName"))) {
								Reporter.log("State Name appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("State Name is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"State Name is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".stateName")
												+ " Expected Result: " + DBStateName,
										file);
								result = true;
							}

							if (DBCityName.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".cityName"))) {
								Reporter.log("City name appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("City Name is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"City Name is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".cityName")
												+ " Expected Result: " + DBCityName,
										file);
								result = true;
							}

							if (DBLocalityName.equals(jsonPath.getString("data.locations."
									+ DBListingLocationId + ".localityName"))) {
								Reporter.log("Locality Name appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("Locality Name is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"Locality Name is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".localityName")
												+ " Expected Result: " + DBLocalityName,
										file);
								result = true;
							}
							
							if(DBmain.equals(jsonPath.getString("data.locations."+DBListingLocationId+".main"))){
								Reporter.log("Main Location is appear correct for courseId: "
										+ courseId, true);
							} else {
								Reporter.log("Main Location is not appear correct for courseId: "
										+ courseId);
								filewriter(
										"Main Location is not appear correct for courseId: "
												+ courseId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString("data.locations."
														+ DBListingLocationId
														+ ".main")
												+ " Expected Result: " + DBmain,
										file);
								result = true;
							}
						}
					}
					
					
					
				}
			}
		}
		catch(SQLException e){
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		return result;
	}
	
	public boolean verifyEntryTypeInfo(JsonPath jsonPath, String courseId) {
		boolean result = false;
		try {
			ArrayList<HashMap<String, String>> DBMap = new ArrayList<HashMap<String, String>>();
			ResultSet rs = exceuteDbQuery(
					"select type,credential,course_level, base_course, stream_id, substream_id, specialization_id, "
							+ "primary_hierarchy"
							+ " from shiksha_courses_type_information where course_id ="
							+ courseId + " and status ='live';", "shiksha");
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					innerMap = new HashMap<String, String>();
					innerMap.clear();
					String DBType = rs.getString("type");
					String DBCourseLevelId = String.valueOf(rs
							.getInt("course_level"));
					String DBBaseCourseId = String.valueOf(rs
							.getInt("base_course"));
					String DBCredentials = String.valueOf(rs
							.getInt("credential"));
					String DBStreamId = String.valueOf(rs.getInt("stream_id"));
					String DBSubStreamId = String.valueOf(rs
							.getInt("substream_id"));
					String DBSpecializationId = String.valueOf(rs
							.getInt("specialization_id"));
					String DBisPrimary = String.valueOf(rs
							.getInt("primary_hierarchy"));
					innerMap.put("stream_id", DBStreamId);
					innerMap.put("substream_id", DBSubStreamId);
					innerMap.put("specialization_id", DBSpecializationId);
					innerMap.put("primary_hierarchy", DBisPrimary);

					if (DBType.equalsIgnoreCase(jsonPath
							.getString("data.entryCourseTypeInformation.type"))) {
						Reporter.log(
								"Course Type is appear correct for CourseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Course Type is not appear correct for courseId: "
										+ courseId, true);
						filewriter(
								"Course Type is not appear correct for courseId: "
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath.getString("data.entryCourseTypeInformation.type")
										+ " Expected Result: " + DBType, file);
						result = true;
					}

					ResultSet rsCourseLevel = DatabaseCommon.baseAttributeList(DBCourseLevelId);
					if (getCountOfResultSet(rsCourseLevel) > 0) {
						while (rsCourseLevel.next()) {
							String DBCourseName = rsCourseLevel
									.getString("value_name");
							if (DBCourseLevelId
									.equals(jsonPath
											.getString("data.entryCourseTypeInformation.course_level.id"))
									&& DBCourseName
											.equalsIgnoreCase(jsonPath
													.getString("data.entryCourseTypeInformation.course_level.name"))) {
								Reporter.log(
										"Course Level is appear correct for CourseId:"
												+ courseId, true);
							} else {
								Reporter.log(
										"Course Level is not appear correct for CourseId:"
												+ courseId, true);
								filewriter(
										"Course Level is not appear correct for CourseId:"
												+ courseId, file);
								filewriter(
										"Actual Result: id: "
												+ jsonPath
														.getString("data.entryCourseTypeInformation.course_level.id")
												+ " name: "
												+ jsonPath
														.getString("data.entryCourseTypeInformation.course_level.name"),
										file);
								filewriter("Expected Result: id: "
										+ DBCourseLevelId + " name: "
										+ DBCourseName, file);
								result = true;
							}
						}
					}
					if (DBBaseCourseId
							.equals(jsonPath
									.getString("data.entryCourseTypeInformation.base_course"))) {
						Reporter.log("BaseCourseId appear correct for course: "
								+ courseId, true);
					} else {
						Reporter.log(
								"BaseCourseId is not appear correct for course: "
										+ courseId, true);
						filewriter(
								"BaseCourseId is not appear correct for course: "
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath
												.getString("data.entryCourseTypeInformation.base_course"),
								file);
						filewriter("Expected Result: " + DBBaseCourseId, file);
						result = true;
					}
					ResultSet rsCredential = DatabaseCommon.baseAttributeList(DBCredentials);
					if (getCountOfResultSet(rsCredential) > 0) {
						while (rsCredential.next()) {
							String DBCredentialName = rsCredential
									.getString("value_name");
							if (DBCredentials
									.equals(jsonPath
											.getString("data.entryCourseTypeInformation.credential.id"))
									&& DBCredentialName
											.equalsIgnoreCase(jsonPath
													.getString("data.entryCourseTypeInformation.credential.name"))) {
								Reporter.log(
										"credential is appear correct for CourseId:"
												+ courseId, true);
							} else {
								Reporter.log(
										"credentiall is not appear correct for CourseId:"
												+ courseId, true);
								filewriter(
										"credential is not appear correct for CourseId:"
												+ courseId, file);
								filewriter(
										"Actual Result: id: "
												+ jsonPath
														.getString("data.entryCourseTypeInformation.credential.id")
												+ " name: "
												+ jsonPath
														.getString("data.entryCourseTypeInformation.credential.name"),
										file);
								filewriter("Expected Result: id: "
										+ DBCredentials + " name: "
										+ DBCredentialName, file);
								result = true;
							}
						}
					}
					DBMap.add(innerMap);
				}
			}
			List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>(
					jsonPath.getList("data.entryCourseTypeInformation.hierarchy"));
			if (jsonList.size() == DBMap.size()) {
				for (int j = 0; j < jsonList.size(); j++) {
					HashMap<String, String> jsonTempMap = jsonList.get(j);
					HashMap<String, String> dbTempMap = DBMap.get(j);

					for (Map.Entry<String, String> jsonmap : jsonTempMap
							.entrySet()) {
						String jsonkey = jsonmap.getKey();
						String jsonvalue = String.valueOf(jsonmap.getValue());
						String dbvalue = dbTempMap.get(jsonkey);

						if (jsonvalue.equals(dbvalue)) {
							Reporter.log(
									"hierarchy Matches Successfully for key: "
											+ jsonkey, true);
							Assert.assertTrue(true);
						} else {
							Reporter.log("hierarchy Matches Failed for key: "
									+ jsonkey + " CourseId: " + courseId, true);
							filewriter("hierarchy Matches Failed for key: "
									+ jsonkey + " CourseId: " + courseId, file);
							filewriter("Actual Result: JsonKey: " + jsonkey
									+ " JsonValue: " + jsonvalue, "hierarchy");
							filewriter("expected Result: dbKey: " + jsonkey
									+ " dbValue: " + dbvalue, "hierarchy");
							filewriter("Actual Result: " + jsonList,
									"hierarchy");
							filewriter("expected Result: " + DBMap, "hierarchy");
							result = true;
						}
					}
				}
			} else {
				Reporter.log("Size of both list for PaidBanner are not same for CourseId:"
						+ courseId);
				filewriter("Actual result: " + jsonList, "hierarchy");
				filewriter("Expected result: " + DBMap, "hierarchy");
				result = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public boolean verifymediumOfInstruction(JsonPath jsonPath, String courseId){
		boolean result = false;
		try {
			if (!jsonPath.getList("data.mediumOfInstruction").isEmpty()) {
				HashMap<String ,HashMap<String, String>> DBMap = new HashMap<String, HashMap<String, String>>();
				ResultSet rs = exceuteDbQuery(
						"select scai.attribute_value_id,bal.value_name from base_attribute_list as bal join shiksha_courses_additional_info as scai "
								+ "on bal.value_id = scai.attribute_value_id where scai.course_id ="
								+ courseId
								+ " and scai.info_type = 'instruction_medium' "
								+ "and scai.status ='live' and bal.status ='live' order by scai.attribute_value_id asc;",
						"shiksha");
				if (getCountOfResultSet(rs) > 0) {
					while (rs.next()) {
						innerMap = new HashMap<String, String>();
						innerMap.clear();
						String attribId= String.valueOf(rs.getInt("attribute_value_id"));
						innerMap.put("id",
								attribId);
						innerMap.put("name", rs.getString("value_name"));
						DBMap.put(attribId, innerMap);
					}
				
					List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>(
							jsonPath.getList("data.mediumOfInstruction"));
					if (jsonList.size() == DBMap.size()) {
						for (int j = 0; j < jsonList.size(); j++) {
							HashMap<String, String> jsonTempMap = jsonList
									.get(j);
							String moiId = String.valueOf(jsonTempMap.get("id"));
							HashMap<String, String> dbTempMap = DBMap.get(moiId);

							for (Map.Entry<String, String> jsonmap : jsonTempMap
									.entrySet()) {
								String jsonkey = jsonmap.getKey();
								String jsonvalue = String.valueOf(jsonmap
										.getValue());
								String dbvalue = dbTempMap.get(jsonkey);

								if (jsonvalue.equals(dbvalue)) {
									Reporter.log(
											"mediumOfInstruction Matches Successfully for key: "
													+ jsonkey, true);
									Assert.assertTrue(true);
								} else {
									Reporter.log(
											"mediumOfInstruction Matches Failed for key: "
													+ jsonkey+ "for courseId: "+courseId, true);
									filewriter("Actual Result: JsonKey: "
											+ jsonkey + " JsonValue: "
											+ jsonvalue,file);
									filewriter("expected Result: dbKey: "
											+ jsonkey + " dbValue: " + dbvalue,
											file);
									filewriter("Actual Result: " + jsonList,
											file);
									filewriter("expected Result: " + DBMap,
											file);
									result = true;
								}
							}
						}
					} else {
						Reporter.log("Size of both list for mediumofinstruction are not same for CourseId: "
								+ courseId);
						filewriter("Actual result: " + jsonList,
								file);
						filewriter("Expected result: " + DBMap,
								file);
						result = true;
					}
			}
			}
			else{
				if(jsonPath.getList("data.mediumOfInstruction").isEmpty()){
					Reporter.log("mediumOfInstruction is appear correct", true);
				} else {
					Reporter.log(
							"mediumOfInstruction Is not appear correct and appear null in db",
							true);
					filewriter(
							"mediumOfInstruction Is not appear correct and appear null in db: "
									+ jsonPath.getList("data.mediumOfInstruction"), file);
					result = true;	
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean verifyCurrentLocationandContactDetails(JsonPath jsonPath, String courseId, String cityId, String localityId){
		 boolean result = false;
		try {
			ResultSet rs = DatabaseCommon.getListingLocationId(courseId,
					cityId, localityId);
			while (rs.next()) {
				String DBcityId = String.valueOf(rs.getInt("city_id"));
				String DBlocalityId = String.valueOf(rs.getInt("locality_id"));
				String DBListingLocationId = String.valueOf(rs
						.getInt("listing_location_id"));
				String DBstateId = String.valueOf(rs.getInt("state_id"));
				String DBisMain = String.valueOf(rs.getInt("is_main"));
				String DBCityName = DatabaseCommon.getCityName(DBcityId);
				String DBStateName = DatabaseCommon.getStateName(DBstateId);
				String DBLocalityName = null;
				if (!DBlocalityId.equals("0")) {
					DBLocalityName = DatabaseCommon
							.getlocalityName(DBlocalityId);
					if (DBLocalityName.equals(jsonPath
							.getString("data.currentLocation.locality_name"))) {
						Reporter.log(
								"Locality name appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Locality name is not appear correct for CourseId:"
										+ courseId, true);
						filewriter(
								"Locality name is not appear correct for CourseId:"
										+ courseId + " : " + cityId + " : "
										+ localityId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath
												.getString("data.currentLocation.locality_name"),
								file);
						filewriter("Expected Result: " + DBLocalityName, file);
						result = true;
					}
				} else {
					String jsonLocalityName = String.valueOf(jsonPath
							.getString("data.currentLocation.locality_name"));
					if ("null".equals(jsonLocalityName)) {
						Reporter.log(
								"Locality name appear correct for courseId: "
										+ courseId, true);
					} else {
						Reporter.log(
								"Locality name is not appear correct for CourseId:"
										+ courseId + " : " + cityId + " : "
										+ localityId, true);
						filewriter(
								"Locality name is not appear correct for CourseId:"
										+ courseId, file);
						filewriter(
								"Actual Result: "
										+ jsonPath
												.getString("data.currentLocation.locality_name"),
								file);
						filewriter("Expected Result: " + DBLocalityName, file);
						result = true;
					}
				}

				if (DBcityId.equals(jsonPath
						.getString("data.currentLocation.city_id"))) {
					Reporter.log("City Id appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log("City Id is not appear correct for CourseId:"
							+ courseId, true);
					filewriter("City Id is not appear correct for CourseId:"
							+ courseId + " : " + cityId + " : " + localityId,
							file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.city_id"),
							file);
					filewriter("Expected Result: " + DBcityId, file);
					result = true;
				}

				if (DBlocalityId.equals(jsonPath
						.getString("data.currentLocation.locality_id"))) {
					Reporter.log("Locality Id appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log(
							"Locality Id is not appear correct for CourseId:"
									+ courseId, true);
					filewriter(
							"Locality Id is not appear correct for CourseId:"
									+ courseId + " : " + cityId + " : "
									+ localityId, file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.locality_id"),
							file);
					filewriter("Expected Result: " + DBlocalityId, file);
					result = true;
				}

				if (DBListingLocationId.equals(jsonPath
						.getString("data.currentLocation.listing_location_id"))) {
					Reporter.log(
							"Listing Location Id appear correct for courseId: "
									+ courseId, true);
				} else {
					Reporter.log(
							"Listing Location Id is not appear correct for CourseId:"
									+ courseId, true);
					filewriter(
							"Listing Location Id is not appear correct for CourseId:"
									+ courseId + " : " + cityId + " : "
									+ localityId, file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.listing_location_id"),
							file);
					filewriter("Expected Result: " + DBListingLocationId, file);
					result = true;
				}

				if (DBstateId.equals(jsonPath
						.getString("data.currentLocation.state_id"))) {
					Reporter.log("State Id appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log("State Id is not appear correct for CourseId:"
							+ courseId, true);
					filewriter("State Id is not appear correct for CourseId:"
							+ courseId + " : " + cityId + " : " + localityId,
							file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.state_id"),
							file);
					filewriter("Expected Result: " + DBstateId, file);
					result = true;
				}

				if (DBisMain.equals(jsonPath
						.getString("data.currentLocation.is_main"))) {
					Reporter.log("isMain appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log("isMain is not appear correct for CourseId:"
							+ courseId, true);
					filewriter("isMain is not appear correct for CourseId:"
							+ courseId + " : " + cityId + " : " + localityId,
							file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.is_main"),
							file);
					filewriter("Expected Result: " + DBisMain, file);
					result = true;
				}

				if (DBCityName.equals(jsonPath
						.getString("data.currentLocation.city_name"))) {
					Reporter.log("City name appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log(
							"City name is not appear correct for CourseId:"
									+ courseId, true);
					filewriter("City name is not appear correct for CourseId:"
							+ courseId + " : " + cityId + " : " + localityId,
							file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.city_name"),
							file);
					filewriter("Expected Result: " + DBCityName, file);
					result = true;
				}

				if (DBStateName.equals(jsonPath
						.getString("data.currentLocation.state_name"))) {
					Reporter.log("State name appear correct for courseId: "
							+ courseId, true);
				} else {
					Reporter.log(
							"State name is not appear correct for CourseId:"
									+ courseId, true);
					filewriter("State name not appear correct for CourseId:"
							+ courseId + " : " + cityId + " : " + localityId,
							file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.state_name"),
							file);
					filewriter("Expected Result: " + DBStateName, file);
					result = true;
				}

				if (DBListingLocationId
						.equals(jsonPath
								.getString("data.currentLocation.contact_details.listing_location_id"))) {
					Reporter.log(
							"Listing LocationId in contact details appear correct for courseId: "
									+ courseId, true);
				} else {
					Reporter.log(
							"Listing LocationId in contact details is not appear correct for CourseId:"
									+ courseId, true);
					filewriter(
							"Listing LocationId in contact details is not appear correct for CourseId:"
									+ courseId + " : " + cityId + " : "
									+ localityId, file);
					filewriter(
							"Actual Result: "
									+ jsonPath
											.getString("data.currentLocation.contact_details.listing_location_id"),
							file);
					filewriter("Expected Result: " + DBListingLocationId, file);
					result = true;
				}
				if (!sqlQuery.equals("ignoreInput")) {
					ResultSet rsContactDetails = exceuteDbQuery(sqlQuery,
							"shiksha");
					if (getCountOfResultSet(rsContactDetails) > 0) {
						while (rsContactDetails.next()) {
							String DBWebsiteUrl = rsContactDetails
									.getString("website_url");
							String DBAddress = rsContactDetails
									.getString("address");
							String DBlatituted = rsContactDetails
									.getString("latitude");
							if ("null".equalsIgnoreCase(DBlatituted)) {
								DBlatituted = "";
							}
							String DBlongitude = rsContactDetails
									.getString("longitude");
							if ("null".equalsIgnoreCase(DBlongitude)) {
								DBlongitude = "";
							}
							String DBAdmissionNumber = rsContactDetails
									.getString("admission_contact_number");
							String DBAdmissionEmail = rsContactDetails
									.getString("admission_email");
							String DBGenericNumber = rsContactDetails
									.getString("generic_contact_number");
							String DBGenericEmail = rsContactDetails
									.getString("generic_email");
							;
							String DBGoogleUrl = rsContactDetails
									.getString("google_url");
							// Reporter.log("size: "+DBGoogleUrl.length(),
							// true);
							// if(DBGoogleUrl!=null && DBGoogleUrl.length()==0){
							// DBGoogleUrl = "null";
							// }
							String DBActualListingLocationId = ExpectedActualListingId;
							if (DBActualListingLocationId
									.equalsIgnoreCase("na")) {
								DBActualListingLocationId = null;
							}

							String path = "data.currentLocation.contact_details";
							String jsonWebsiteUrl = jsonPath.getString(path
									+ ".website_url");
							if (StringUtils
									.equals(DBWebsiteUrl, jsonWebsiteUrl)) {
								Reporter.log(
										"websiteUrl in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"websiteUrl in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"websiteUrl in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".website_url"), file);
								filewriter("Expected Result: " + DBWebsiteUrl,
										file);
								result = true;
							}

							String jsonAddress = jsonPath.getString(path
									+ ".address");
							if (StringUtils.equals(DBAddress, jsonAddress)) {
								Reporter.log(
										"Address in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"Address in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"Address in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".address"), file);
								filewriter("Expected Result: " + DBAddress,
										file);
								result = true;
							}
							String jsonlatitute = jsonPath.getString(path
									+ ".latitude");
							if (StringUtils.equals(DBlatituted, jsonlatitute)) {
								Reporter.log(
										"latitude in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"latitude in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"latitude in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".latitude"), file);
								filewriter("Expected Result: " + DBlatituted,
										file);
								result = true;
							}
							String jsonlongitude = jsonPath.getString(path
									+ ".longitude");
							if (StringUtils.equals(DBlongitude, jsonlongitude)) {
								Reporter.log(
										"longitude in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"longitude in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"longitude in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".longitude"), file);
								filewriter("Expected Result: " + DBlongitude,
										file);
								result = true;
							}
							String jsonAdmissionnumber = jsonPath
									.getString(path
											+ ".admission_contact_number");
							if (StringUtils.equals(DBAdmissionNumber,
									jsonAdmissionnumber)) {
								Reporter.log(
										"admission_contact_number in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"admission_contact_number in ContactDetail is not appear correct for CourseId: "
												+ courseId
												+ " : "
												+ cityId
												+ " : " + localityId, true);
								filewriter(
										"admission_contact_number in ContactDetail is not appear correct for Courseid: "
												+ courseId
												+ " : "
												+ cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath
														.getString(path
																+ ".admission_contact_number"),
										file);
								filewriter("Expected Result: "
										+ DBAdmissionNumber, file);
								result = true;
							}
							String jsonAdmissionEmail = jsonPath.getString(path
									+ ".admission_email");
							if (StringUtils.equals(DBAdmissionEmail,
									jsonAdmissionEmail)) {
								Reporter.log(
										"admission_email in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"admission_email in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"admission_email in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".admission_email"),
										file);
								filewriter("Expected Result: "
										+ DBAdmissionEmail, file);
								result = true;
							}
							String jsonGenericNumber = jsonPath.getString(path
									+ ".generic_contact_number");
							if (StringUtils.equals(DBGenericNumber,
									jsonGenericNumber)) {
								Reporter.log(
										"generic_contact_number in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"generic_contact_number in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"generic_contact_number in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath
														.getString(path
																+ ".generic_contact_number"),
										file);
								filewriter("Expected Result: "
										+ DBGenericNumber, file);
								result = true;
							}
							String jsonGenericEmail = jsonPath.getString(path
									+ ".generic_email");
							if (StringUtils.equals(DBGenericEmail,
									jsonGenericEmail)) {
								Reporter.log(
										"generic_email in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"generic_email in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"generic_email in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".generic_email"),
										file);
								filewriter(
										"Expected Result: " + DBGenericEmail,
										file);
								result = true;
							}
							String jsonGoogleUrl = jsonPath.getString(path
									+ ".google_url");
							if (jsonGoogleUrl != null
									&& jsonGoogleUrl.length() > 0) {
								jsonGoogleUrl = jsonGoogleUrl
										.substring(jsonGoogleUrl
												.indexOf(".com/") + 4);
							}
							if (StringUtils.equals(DBGoogleUrl, jsonGoogleUrl)) {
								Reporter.log(
										"google_url in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"google_url in ContactDetail is not appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
								filewriter(
										"google_url in ContactDetail is not appear correct for Courseid: "
												+ courseId + " : " + cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath.getString(path
														+ ".google_url"), file);
								filewriter("Expected Result: " + DBGoogleUrl,
										file);
								result = true;
							}
							String jsonActualListingLocationId = jsonPath
									.getString(path
											+ ".actual_listing_location_id");
							if (StringUtils.equals(DBActualListingLocationId,
									jsonActualListingLocationId)) {
								Reporter.log(
										"actual_listing_location_id in ContactDetail is appear correct for CourseId: "
												+ courseId + " : " + cityId
												+ " : " + localityId, true);
							} else {
								Reporter.log(
										"actual_listing_location_id in ContactDetail is not appear correct for CourseId: "
												+ courseId
												+ " : "
												+ cityId
												+ " : " + localityId, true);
								filewriter(
										"actual_listing_location_id in ContactDetail is not appear correct for Courseid: "
												+ courseId
												+ " : "
												+ cityId
												+ " : " + localityId, file);
								filewriter(
										"Actual Result: "
												+ jsonPath
														.getString(path
																+ ".actual_listing_location_id"),
										file);
								filewriter("Expected Result: "
										+ DBActualListingLocationId, file);
								result = true;
							}
							ResultSet rsFeeOnLocation = DatabaseCommon
									.getFeesOnLocation(courseId,
											DBListingLocationId);
							DBList.clear();
							if (getCountOfResultSet(rsFeeOnLocation) > 0) {
								while (rsFeeOnLocation.next()) {
									innerMap = new HashMap<String, String>();
									innerMap.clear();
									innerMap.put("category", rsFeeOnLocation.getString("category"));
									innerMap.put("period", rsFeeOnLocation.getString("period"));
									innerMap.put("fees_value", rsFeeOnLocation.getString("fees_value"));
									innerMap.put("fees_unit", rsFeeOnLocation.getString("fees_unit"));
									innerMap.put("fees_type", rsFeeOnLocation.getString("fees_type"));
									innerMap.put("fees_disclaimer",
											rsFeeOnLocation.getString("fees_disclaimer"));
									innerMap.put("fees_unit_name",
											rsFeeOnLocation.getString("fees_unit_name"));
									DBList.add(innerMap);
								}
								try {
									List<HashMap<String, Object>> jsonFeesOnLocation = new ArrayList<>(
											jsonPath.getList("data.currentLocation.fees"));
									if (DBList.size() == jsonFeesOnLocation
											.size()) {
										for (int j = 0; j < jsonFeesOnLocation
												.size(); j++) {
											HashMap<String, Object> jsonTempMap = jsonFeesOnLocation
													.get(j);
											HashMap<String, String> dbTempMap = DBList
													.get(j);

											for (Map.Entry<String, Object> jsonmap : jsonTempMap
													.entrySet()) {
												String jsonkey = jsonmap
														.getKey();
												String jsonvalue = String
														.valueOf(jsonmap
																.getValue());
												if(jsonkey.equals("total_includes")){
													@SuppressWarnings("unchecked")
													Map<String, String> totalIncludedMap = (Map<String, String>)jsonFeesOnLocation.get(j).get("total_includes");
													if(totalIncludedMap.isEmpty()){
														Reporter.log(
																"Total Includes in current location appear as empty that is correct for courseId: "
																		+ courseId
																		+ " : "
																		+ cityId
																		+ " : "
																		+ localityId,
																true);
													}
													else{
														Reporter.log("Total Includes in current location is not appear empty for courseid:"+courseId+" :"+cityId+" : "+localityId, true);
														filewriter("Total Includes in current location is not appear empty for courseid:"+courseId+" :"+cityId+" : "+localityId, file);
														filewriter("Includes List: "+jsonFeesOnLocation.get(j).get("total_includes"), file);
														result =true;
													}
												}
												else{
												String dbvalue = dbTempMap
														.get(jsonkey);

												if (jsonvalue.equals(dbvalue)) {
													Reporter.log(
															"Fees Matches Successfully for key: "
																	+ jsonkey
																	+ " for Course Id: "
																	+ courseId,
															true);
												} else {
													Reporter.log(
															"Fees Matches Failed for key: "
																	+ jsonkey
																	+ " for Course Id: "
																	+ courseId,
															true);
													filewriter(
															"Actual Result: JsonKey: "
																	+ jsonkey
																	+ " JsonValue: "
																	+ jsonvalue,
															file);
													filewriter(
															"expected Result: dbKey: "
																	+ jsonkey
																	+ " dbValue: "
																	+ dbvalue,
															file);
													filewriter(
															"Actual Result: "
																	+ jsonFeesOnLocation,
															file);
													filewriter(
															"expected Result: "
																	+ DBList,
															file);
													result = true;
												}
											}
											}
										}
									} else {
										Reporter.log(
												"Size of both list for fees are not same for CourseId:"
														+ courseId+ " : "
																+ cityId
																+ " : "
																+ localityId, true);
										filewriter(
												"Size of both list for fees are not same for CourseId:"
														+ courseId+ " : "
																+ cityId
																+ " : "
																+ localityId, file);
										filewriter("Actual result: "
												+ jsonFeesOnLocation, file);
										filewriter(
												"Expected result: " + DBList,
												file);
										result = true;
									}
								} catch (NullPointerException e) {
									e.printStackTrace();
								}
							} else {
								String jsonFees = jsonPath.getString("data.currentLocation.fees");
								if (jsonFees!=null) {
									Reporter.log(
											"fees is not appear correct for Course: "
													+ courseId+" : "+cityId+" : "+localityId, true);
									filewriter(
											"fees Is not appear correct for courseId: "
													+ courseId+" : "+cityId+" : "+localityId, file);
									filewriter(
											"Actual Result: "
													+ jsonPath
															.getList("data.currentLocation.fees"),
											file);
									result = true;
								} else {
									Reporter.log(
											"fees is appear correct for Course: "
													+ courseId+" : "+cityId+" : "+localityId, true);
									
								}
							}

						}
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public boolean verifyHighlights(JsonPath jsonPath, String courseId) {
		boolean result = false;
		try {
			ArrayList<HashMap<String, String>> DBMap = new ArrayList<HashMap<String, String>>();
			ResultSet rs = DatabaseCommon.getHighlights(courseId);
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					innerMap = new HashMap<String, String>();
					innerMap.clear();
					innerMap.put("description", rs.getString("description"));
					innerMap.put("description_type", rs.getString("info_type"));
					DBMap.add(innerMap);
				}

				List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>(
						jsonPath.getList("data.highlights"));
				if (jsonList.size() == DBMap.size()) {
					for (int j = 0; j < jsonList.size(); j++) {
						HashMap<String, String> jsonTempMap = jsonList.get(j);
						HashMap<String, String> dbTempMap = DBMap.get(j);

						for (Map.Entry<String, String> jsonmap : jsonTempMap
								.entrySet()) {
							String jsonkey = jsonmap.getKey();
							String jsonvalue = String.valueOf(jsonmap
									.getValue());
							String dbvalue = dbTempMap.get(jsonkey);

							if (jsonvalue.equals(dbvalue)) {
								Reporter.log(
										"Highlights Matches Successfully for key: "
												+ jsonkey, true);
								Assert.assertTrue(true);
							} else {
								Reporter.log(
										"Highlights Matches Failed for key: "
												+ jsonkey + " CourseId: "
												+ courseId, true);
								filewriter("Actual Result: JsonKey: " + jsonkey
										+ " JsonValue: " + jsonvalue, file);
								filewriter("expected Result: dbKey: " + jsonkey
										+ " dbValue: " + dbvalue, file);
								filewriter("Actual Result: " + jsonList, file);
								filewriter("expected Result: " + DBMap, file);
								result = true;
							}
						}
					}
				} else {
					Reporter.log("Size of both list for Highlights are not same for CourseId:"
							+ courseId);
					filewriter("Actual result: " + jsonList, file);
					filewriter("Expected result: " + DBMap, file);
					result = true;
				}
			} else {
//				int size = jsonPath.getList("data.highlights").size();
				if (jsonPath.getList("data.highlights").isEmpty()) {
					Reporter.log("Highlights is appear correct", true);
				} else {
					Reporter.log(
							"Highlights Is not appear correct and appear null in db",
							true);
					filewriter(
							"Highlights Is not appear correct and appear null in db: "
									+ jsonPath.getList("data.highlights"), file);
					result = true;
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean verifyCourseStructure(JsonPath jsonPath, String courseId){
		boolean result = false;
		ArrayList<String> innerList = null ;
		String PeriodpreviousValue = null;
		String DBPeriodType = null;
		Map<String, ArrayList<String>> DBMap = new HashMap<String, ArrayList<String>>();
		try{
			ResultSet rs = DatabaseCommon.getCourseStructure(courseId);
			if(getCountOfResultSet(rs)>0){
				while(rs.next()){
					DBPeriodType = rs.getString("period");
					String DBPeriodValue = rs.getString("period_value");
					if(!DBPeriodValue.equals(PeriodpreviousValue)){
						innerList=new ArrayList<String>();
					}
					String DBCourseOffered = rs.getString("courses_offered");
					innerList.add(DBCourseOffered);
					PeriodpreviousValue = DBPeriodValue;
					DBMap.put(DBPeriodValue,innerList);
				}
				Map<String, List<String>> jsonList = new HashMap<String, List<String>>(jsonPath.getMap("data.courseStructure.periodWiseCourses"));
				if(jsonList.size()==DBMap.size()){
					if(DBPeriodType.equals(jsonPath.getString("data.courseStructure.period"))){
						Reporter.log("Period Matches Sucessfully for CourseId: "+courseId, true);
					}
					else{
						Reporter.log("Period not Matches Sucessfully for CourseId: "+courseId, true);
						filewriter("Period not Matches Sucessfully for CourseId: "+courseId, file);
						result =true;
					}
					for(Map.Entry<String, List<String>> entry : jsonList.entrySet()){
						String jsonTempPeriodValue = entry.getKey();
						List<String> jsonTempList = entry.getValue();
						List<String> DBTempList = DBMap.get(jsonTempPeriodValue);
						if(jsonTempList.equals(DBTempList)){
							Reporter.log("Course Structure List Matches Sucessfully for CourseId: "+courseId, true);
						}
						else{
							Reporter.log("Course Structure List not Matches Sucessfully for CourseId: "+courseId, true);
							filewriter("Course Structure List not Matches Sucessfully for CourseId: "+courseId+" Period: "+jsonTempPeriodValue, file);
							filewriter("jsonList: "+jsonTempList, file);
							filewriter("DBList: "+DBTempList, file);
							result =true;
						}
					}
				}
				else{
					Reporter.log("Size of Both maps are not same for CourseId:"+courseId, true);
					filewriter("Size of Both maps are not same for CourseId:"+courseId, file);
					filewriter("jsonMap: "+jsonList, file);
					filewriter("DBMap: "+DBMap, file);
					result =true;
				}
			}
			else{
				try{
					if((!jsonPath.getString("data.courseStructure").equals(null))){
						Reporter.log("Course Structure appear in DB is 0 But appear in Result", true);
						filewriter("Course Structure appear in DB is 0 But appear in Result for courseId: "+courseId, file);
						filewriter("Json result"+jsonPath.getMap("data.courseStructure"), file);
						result =true;
					}
					else{
						Reporter.log("Course Structure appear correct", true);
					}
				}
				catch(NullPointerException e){
					Reporter.log("Course Structure appear correct", true);
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public boolean verifyRankingBySourceAndLocation(JsonPath jsonPath,
			String courseId) {
		try {
			ResultSet rs = DatabaseCommon.getRankingBySources(courseId);
			
			if (getCountOfResultSet(rs) > 0) {
				while (rs.next()) {
					innerMap =new HashMap<String, String>();
					innerMap.clear();
					String DBrankingPageId=String.valueOf(rs.getInt("ranking_page_id"));
					String DBpublisherId = String.valueOf(rs.getInt("publisher_id"));
					innerMap.put("sourceId", String.valueOf(rs.getInt("source_id")));
					innerMap.put("publisherId", DBpublisherId);
					innerMap.put("publisherName", rs.getString("publisher_name"));
					innerMap.put("rankingPageId", DBrankingPageId);
					innerMap.put("rank", String.valueOf(rs.getInt("rank")));
					innerMap.put("instituteId", String.valueOf(rs.getInt("institute_id")));
					innerMap.put("courseId", String.valueOf(rs.getInt("course_id")));
					innerMap.put("year", String.valueOf(rs.getInt("year")));
					innerMap.put("url", DBrankingPageId+"-2-0-0-0");
					int i;
					boolean data = false;
					for(i=0; i<DBList.size();i++){
						String mapId = DBList.get(i).get("publisherId");
						if(mapId.equals(DBpublisherId)){
							data = true;
						}
					}
					if(data!=true){
						DBList.add(innerMap);
					}
				}
				List<HashMap<String, String>> jsonList = new ArrayList<HashMap<String, String>>(
						jsonPath.getList("data.rankingsBySource"));
				List<HashMap<String, String>> jsonRankingList = new ArrayList<HashMap<String, String>>();
				for (int i = 0; i < jsonList.size(); i++) {
					HashMap<String, String> hashmap = jsonList
							.get(i);
					String url = hashmap.get("url");
					url = url
							.substring(url.indexOf("india/") + 6);
					hashmap.put("url", url);
					jsonRankingList.add(hashmap);
				}
				if (jsonList.size() == DBList.size()) {
					for (int j = 0; j < jsonList.size(); j++) {
						HashMap<String, String> jsonTempMap = jsonList.get(j);
						HashMap<String, String> dbTempMap = DBList.get(j);

						for (Map.Entry<String, String> jsonmap : jsonTempMap
								.entrySet()) {
							String jsonkey = jsonmap.getKey();
							String jsonvalue = String.valueOf(jsonmap
									.getValue());
							String dbvalue = dbTempMap.get(jsonkey);

							if (jsonvalue.equals(dbvalue)) {
								Reporter.log(
										"RankingBySource Matches Successfully for key: "
												+ jsonkey, true);
								Assert.assertTrue(true);
							} else {
								Reporter.log(
										"RankingBySource Matches Failed for key: "
												+ jsonkey + " CourseId: "
												+ courseId, true);
								filewriter(
										"RankingBySource Matches Failed for key: "
												+ jsonkey + " CourseId: "
												+ courseId, file);
								filewriter("Actual Result: JsonKey: " + jsonkey
										+ " JsonValue: " + jsonvalue, file);
								filewriter("expected Result: dbKey: " + jsonkey
										+ " dbValue: " + dbvalue, file);
								filewriter("Actual Result: " + jsonList, file);
								filewriter("expected Result: " + DBList, file);
								result = true;
							}
						}
					}
				} else {
					Reporter.log("Size of both list for RankingBySource are not same for CourseId:"
							+ courseId);
					filewriter("Actual result: " + jsonList, file);
					filewriter("Expected result: " + DBList, file);
					result = true;
				}
			
			rs = DatabaseCommon.getListingLocationId(courseId, "", "");
			if(getCountOfResultSet(rs)>0){
				String rankingPageId = DBList.get(1).get("rankingPageId");
				while(rs.next()){
					String CityId = String.valueOf(rs.getInt("city_id"));
					String StateId = String.valueOf(rs.getInt("state_id"));
					ResultSet rsVirtualCity = DatabaseCommon.getVirtualCityId(CityId);
					if(getCountOfResultSet(rsVirtualCity)>0){
						while(rs.next()){
						CityId = String.valueOf(rsVirtualCity.getInt("virtualCityId"));
						}
					}
					ResultSet rsRankingPageDetails = DatabaseCommon.getRankingPageDetails(rankingPageId);
					if(getCountOfResultSet(rsRankingPageDetails)>0){
						DBListforRankingLocation.clear();
						while(rsRankingPageDetails.next()){
							innerMap = new HashMap<String, String>();
							HashMap<String, String> innerMap2 = new HashMap<String, String>();
							innerMap.put("rankingPageId", rankingPageId);
							innerMap.put("rankingPageName",rsRankingPageDetails.getString("ranking_page_text"));
							innerMap.put("url",rankingPageId+"-2-0-"+CityId+"-0");
							innerMap.put("cityId", CityId);
							innerMap.put("stateId", "0");
							innerMap.put("locationName",DatabaseCommon.getCityName(CityId));
							innerMap.put("examId", "0");
							innerMap.put("examName","null");
							
							innerMap2.put("rankingPageId", rankingPageId);
							innerMap2.put("rankingPageName",rsRankingPageDetails.getString("ranking_page_text"));
							innerMap2.put("url",rankingPageId+"-2-"+StateId+"-0-0");
							innerMap2.put("cityId", "0");
							innerMap2.put("stateId", StateId);
							innerMap2.put("locationName",DatabaseCommon.getStateName(StateId));
							innerMap2.put("examId", "0");
							innerMap2.put("examName","null");
							
							DBListforRankingLocation.add(innerMap);
							DBListforRankingLocation.add(innerMap2);
						}
					}
				}
				List<HashMap<String, String>> jsonListRankingBylocation = new ArrayList<HashMap<String, String>>(
						jsonPath.getList("data.rankingsByLocation"));
				List<HashMap<String, String>> jsonRankingLocationList = new ArrayList<HashMap<String, String>>();
				for (int i = 0; i < jsonListRankingBylocation.size(); i++) {
					HashMap<String, String> hashmap = jsonListRankingBylocation
							.get(i);
					String url = hashmap.get("url");
					url = url
							.substring(url.indexOf("/"+rankingPageId)+1);
					hashmap.put("url", url);
					jsonRankingLocationList.add(hashmap);
				}
				if (jsonListRankingBylocation.size() == DBListforRankingLocation.size()) {
					for (int j = 0; j < jsonListRankingBylocation.size(); j++) {
						HashMap<String, String> jsonTempMap = jsonListRankingBylocation.get(j);
						HashMap<String, String> dbTempMap = DBListforRankingLocation.get(j);

						for (Map.Entry<String, String> jsonmap : jsonTempMap
								.entrySet()) {
							String jsonkey = jsonmap.getKey();
							String jsonvalue = String.valueOf(jsonmap
									.getValue());
							String dbvalue = dbTempMap.get(jsonkey);

							if (jsonvalue.equals(dbvalue)) {
								Reporter.log(
										"RankingByLocation Matches Successfully for key: "
												+ jsonkey, true);
								Assert.assertTrue(true);
							} else {
								Reporter.log(
										"RankingByLocation Matches Failed for key: "
												+ jsonkey + " CourseId: "
												+ courseId, true);
								filewriter(
										"RankingByLocation Matches Failed for key: "
												+ jsonkey + " CourseId: "
												+ courseId, file);
								filewriter("Actual Result: JsonKey: " + jsonkey
										+ " JsonValue: " + jsonvalue, file);
								filewriter("expected Result: dbKey: " + jsonkey
										+ " dbValue: " + dbvalue, file);
								filewriter("Actual Result: " + jsonListRankingBylocation, file);
								filewriter("expected Result: " + DBListforRankingLocation, file);
								result = true;
							}
						}
					}
				} else {
					Reporter.log("Size of both list for RankingByLocation are not same for CourseId:"
							+ courseId);
					filewriter("Actual result: " + jsonListRankingBylocation, file);
					filewriter("Expected result: " + DBListforRankingLocation, file);
					result = true;
				}
			}
			}
			else{
				if(jsonPath.getList("data.rankingsBySource").size()==0&&jsonPath.getList("data.rankingsByLocation").size()==0){
					Reporter.log("RankingBySource and RankingByLocation Passed for courseId:"+courseId, true);
				}
				else{
					Reporter.log("RankingBySource and RankingByLocation is not appear empty for courseId"+courseId, true);
					filewriter("RankingBySource and RankingByLocation is not appear empty for courseId"+courseId, file);
					filewriter("size: "+jsonPath.getList("data.rankingsBySource").size()+" : "+jsonPath.getList("data.rankingsByLocation").size(), file);
					filewriter("rankingsBySource: "+jsonPath.getList("data.rankingsBySource"), file);
					filewriter("RankingByLocation: "+jsonPath.getList("data.rankingsByLocation"), file);
				}
				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	@Test(priority = 2)
	public static void respnseTimeStats_GetCoursedata() {
		respnseTimeStatsCommon(responseTimes,
				apiPath.substring(apiPath.lastIndexOf("/") + 1));
	}
}
